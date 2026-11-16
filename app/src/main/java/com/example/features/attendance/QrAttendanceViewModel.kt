package com.example.features.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.AppConfig
import com.example.database.GymConfigDoc
import com.example.database.MemberFirestoreRepository
import com.example.database.toFriendlyFirestoreMessage
import com.example.models.AttendanceRecord
import com.example.models.MemberProfile
import com.example.models.PersonalStats
import com.example.models.QrScanUiState
import com.example.models.TodayAttendanceState
import com.example.models.UserRole
import com.example.utils.QrAntiFraudVerifier
import com.example.utils.QrVerificationResult
import com.example.utils.StreakAndLeaderboardEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class QrAttendanceScreenState(
    val isLoading: Boolean = true,
    val currentMember: MemberProfile? = null,
    val expectedEntranceCode: String = AppConfig.COMMON_ENTRANCE_QR_CODE,
    val todayStatus: TodayAttendanceState = TodayAttendanceState.NOT_CHECKED_IN,
    val scanState: QrScanUiState = QrScanUiState.ReadyToScan,
    val recentHistory: List<AttendanceRecord> = emptyList(),
    val permissionDeniedOnce: Boolean = false
)

class QrAttendanceViewModel(
    private val repository: MemberFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _scanState = MutableStateFlow<QrScanUiState>(QrScanUiState.ReadyToScan)
    private val _permissionDeniedOnce = MutableStateFlow(false)
    private val _gymConfig = MutableStateFlow<GymConfigDoc?>(null)

    init {
        viewModelScope.launch {
            _gymConfig.value = runCatching { repository.getGymConfig() }.getOrNull()
        }
    }

    val uiState: StateFlow<QrAttendanceScreenState> = combine(
        repository.observeUserProfile(currentUserId),
        repository.observeAttendanceRecords(currentUserId),
        _scanState,
        _permissionDeniedOnce,
        _gymConfig
    ) { profileDoc, records, scanState, permDenied, gymConfig ->
        val cal = Calendar.getInstance()
        val todayKey = repository.todayDateKey(cal)
        val metrics = StreakAndLeaderboardEngine.computeAttendanceMetrics(records, cal)

        val member = profileDoc?.let {
            MemberProfile(
                id = it.userId.ifBlank { currentUserId },
                memberCode = it.memberCode.ifBlank { "GP-MEMBER" },
                fullName = it.fullName.ifBlank { "Club Member" },
                phone = it.phone.orEmpty(),
                email = it.email,
                avatarUri = it.avatarData,
                role = if (it.role.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.MEMBER,
                joinedDate = it.joinedDate,
                fitnessGoal = it.fitnessGoal ?: "Fitness Goal",
                stats = PersonalStats(
                    currentStreakDays = metrics.currentStreakDays,
                    longestStreakDays = metrics.longestStreakDays,
                    totalWorkouts = metrics.totalWorkouts,
                    monthlyWorkouts = metrics.monthlyVisits,
                    weeklyTargetMetWeeks = metrics.weeklyTargetMetWeeks,
                    referralsCount = 0,
                    leaderboardRank = 0,
                    totalGymMembers = 1
                )
            )
        }

        val mappedHistory = records.map { rec ->
            AttendanceRecord(
                id = rec.id.ifBlank { rec.dateKey },
                dateLabel = rec.dateLabel.ifBlank { rec.dateKey },
                checkInTime = rec.checkInTime,
                zoneLabel = rec.zoneLabel.ifBlank { "Main Entrance Gate" },
                verifiedViaCommonQr = rec.verifiedViaCommonQr
            )
        }

        val checkedInToday = records.any { it.dateKey == todayKey }
        val expectedCode = gymConfig?.staticEntranceCode?.ifBlank { AppConfig.COMMON_ENTRANCE_QR_CODE }
            ?: AppConfig.COMMON_ENTRANCE_QR_CODE

        QrAttendanceScreenState(
            isLoading = false,
            currentMember = member,
            expectedEntranceCode = expectedCode,
            todayStatus = if (checkedInToday) TodayAttendanceState.CHECKED_IN else TodayAttendanceState.NOT_CHECKED_IN,
            scanState = scanState,
            recentHistory = mappedHistory,
            permissionDeniedOnce = permDenied
        )
    }.catch { error ->
        emit(
            QrAttendanceScreenState(
                isLoading = false,
                scanState = QrScanUiState.Error(
                    error.toFriendlyFirestoreMessage("Unable to load attendance status.")
                )
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = QrAttendanceScreenState(isLoading = true)
    )

    fun onPermissionResult(granted: Boolean) {
        _permissionDeniedOnce.value = !granted
    }

    fun resetScanState() {
        _scanState.value = QrScanUiState.ReadyToScan
    }

    fun scanLiveReceptionQr(context: Context) {
        val code = uiState.value.expectedEntranceCode
        val windowSeconds = _gymConfig.value?.rotationWindowSeconds ?: AppConfig.QR_ROTATION_WINDOW_SECONDS
        val livePayload = QrAntiFraudVerifier.generateRotatingEntrancePayload(
            entranceCode = code,
            windowSeconds = windowSeconds
        )
        verifyAndSubmitQr(context = context, rawPayload = livePayload)
    }

    fun verifyAndSubmitQr(context: Context, rawPayload: String) {
        val state = uiState.value
        val member = state.currentMember
        if (member == null) {
            _scanState.value = QrScanUiState.Error("Member profile is still syncing. Please retry.")
            return
        }
        if (state.scanState is QrScanUiState.ScanningEntranceQr) return

        val gymConfig = _gymConfig.value
        val (memberLat, memberLng) = readOptionalCoarseLocation(context)
        val verification = QrAntiFraudVerifier.verifyCheckIn(
            rawPayload = rawPayload,
            expectedEntranceCode = state.expectedEntranceCode,
            alreadyCheckedInToday = state.todayStatus == TodayAttendanceState.CHECKED_IN,
            windowSeconds = gymConfig?.rotationWindowSeconds ?: AppConfig.QR_ROTATION_WINDOW_SECONDS,
            memberLat = memberLat,
            memberLng = memberLng,
            gymLat = gymConfig?.gymLatitude,
            gymLng = gymConfig?.gymLongitude,
            allowedRadiusMeters = gymConfig?.allowedRadiusMeters
        )

        when (verification) {
            is QrVerificationResult.Invalid -> {
                _scanState.value = QrScanUiState.Error(verification.reason)
            }
            is QrVerificationResult.Valid -> {
                _scanState.value = QrScanUiState.ScanningEntranceQr
                viewModelScope.launch {
                    try {
                        val todayKey = repository.todayDateKey()
                        val recorded = repository.recordDailyEntranceAttendance(
                            dateKey = todayKey,
                            gymCode = verification.entranceCode,
                            verificationToken = verification.verificationToken,
                            zoneLabel = "Main Entrance Gate",
                            memberNameForLeaderboard = member.fullName,
                            avatarDataForLeaderboard = member.avatarUri
                        )
                        _scanState.value = QrScanUiState.Confirmed(
                            memberName = member.fullName,
                            memberCode = member.memberCode,
                            timestamp = recorded.checkInTime,
                            entranceCode = verification.entranceCode,
                            updatedStreakDays = member.stats.currentStreakDays + 1
                        )
                    } catch (e: Exception) {
                        _scanState.value = QrScanUiState.Error(
                            e.toFriendlyFirestoreMessage("Check-in failed. Please retry.")
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readOptionalCoarseLocation(context: Context): Pair<Double?, Double?> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null to null

        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null to null
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            loc?.latitude to loc?.longitude
        }.getOrDefault(null to null)
    }
}
