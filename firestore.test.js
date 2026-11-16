const path = require("node:path");
const fs = require("node:fs");
const rulesTestingEntry = require.resolve("@firebase/rules-unit-testing");
const nestedModules = path.resolve(path.dirname(rulesTestingEntry), "..", "node_modules");
if (fs.existsSync(nestedModules) && !module.paths.includes(nestedModules)) {
  module.paths.push(nestedModules);
}
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const { serverTimestamp } = require("firebase/firestore");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-gympulse-b0f35740-5dd4-48f2-a9dd-e00cdf78de59";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function getDb(context) {
  return context.firestore({ databaseId: DATABASE_ID });
}

test("Unauthenticated user: cannot read private profile, attendance, or leaderboard", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard_stats").get());
  await assertFails(unauthDb.collection("announcements").get());
});

test("User PII isolation: Bob cannot read or write Alice's private user profile", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      memberCode: "APX-1001",
      fullName: "Alice Vance",
      email: "alice@test.com",
      phone: "+1 555 0100",
      role: "MEMBER",
      joinedDate: "Nov 16, 2026",
      referralCode: "APX-REF-1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = getDb(testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ fullName: "Hacked" }));
});

test("Privilege escalation guard: Member cannot self-assign ADMIN role on registration", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      memberCode: "APX-1001",
      fullName: "Alice Vance",
      email: "alice@test.com",
      role: "ADMIN",
      joinedDate: "Nov 16, 2026",
      referralCode: "APX-REF-1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Attendance anti-duplication & common entrance QR validation", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const attRef = aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-16");

  // Invalid gym code must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("2026-11-15").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      dateLabel: "Nov 15, 2026",
      checkInTime: "07:00 AM",
      zoneLabel: "Fake Gym",
      gymCode: "FAKE-QR-CODE",
      verificationToken: "tok_123456",
      verifiedViaCommonQr: true,
      createdAt: serverTimestamp(),
    })
  );

  // Valid attendance succeeds once
  await assertSucceeds(
    attRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-16",
      dateLabel: "Nov 16, 2026",
      checkInTime: "07:00 AM",
      zoneLabel: "Main Entrance QR",
      gymCode: "APEXPULSE-MAIN-ENTRANCE-QR-001",
      verificationToken: "tok_valid_123",
      verifiedViaCommonQr: true,
      createdAt: serverTimestamp(),
    })
  );

  // Duplicate / update on same day fails
  await assertFails(attRef.update({ checkInTime: "08:00 AM" }));
});

test("Payment integrity: Member cannot verify their own payment or activate membership directly", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));

  // Unverified payment request succeeds ONLY as PENDING_VERIFICATION & isVerified=false
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payment_requests").doc("req_1").set({
      userId: ALICE_UID,
      memberName: "Alice Vance",
      memberCode: "APX-1001",
      amountFormatted: "$69.00",
      paymentMethod: "Instant Bank / UPI",
      paymentDateLabel: "Nov 16, 2026",
      status: "PENDING_VERIFICATION",
      isVerified: false,
      createdAt: serverTimestamp(),
    })
  );

  // Self-verifying payment request MUST FAIL
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("payment_requests").doc("req_2").set({
      userId: ALICE_UID,
      memberName: "Alice Vance",
      memberCode: "APX-1001",
      amountFormatted: "$69.00",
      paymentMethod: "Card",
      paymentDateLabel: "Nov 16, 2026",
      status: "VERIFIED",
      isVerified: true,
      createdAt: serverTimestamp(),
    })
  );

  // Self-creating ACTIVE membership MUST FAIL
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memberships").doc("current").set({
      userId: ALICE_UID,
      planName: "Apex Pro",
      monthlyFeeUsd: 69,
      membershipStartDate: "Nov 16, 2026",
      cycleStartDate: "Nov 16, 2026",
      cycleEndDate: "Dec 16, 2026",
      nextRenewalDate: "Dec 16, 2026",
      status: "ACTIVE",
      autoRenewEnabled: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Leaderboard integrity: Member cannot inflate streak or grant referrals/monthly winner", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const statRef = aliceDb.collection("leaderboard_stats").doc(ALICE_UID);

  // Cannot create with referrals > 0 or monthlyWinner == true
  await assertFails(
    statRef.set({
      userId: ALICE_UID,
      memberName: "Alice Vance",
      currentStreakDays: 1,
      longestStreakDays: 1,
      monthlyVisits: 1,
      weeklyTargetMetWeeks: 0,
      healthyHabitsScore: 80,
      verifiedReferralsCount: 5,
      isMonthlyWinner: true,
      monthKey: "2026-11",
      updatedAt: serverTimestamp(),
    })
  );

  // Valid create
  await assertSucceeds(
    statRef.set({
      userId: ALICE_UID,
      memberName: "Alice Vance",
      currentStreakDays: 1,
      longestStreakDays: 1,
      monthlyVisits: 1,
      weeklyTargetMetWeeks: 0,
      healthyHabitsScore: 80,
      verifiedReferralsCount: 0,
      isMonthlyWinner: false,
      monthKey: "2026-11",
      updatedAt: serverTimestamp(),
    })
  );

  // Cannot jump streak by +50 in one update
  await assertFails(
    statRef.update({
      currentStreakDays: 50,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Email spoofing test: Unverified admin email cannot write announcements", async () => {
  const spoofDb = getDb(
    testEnv.authenticatedContext("spoof_uid", {
      email: "hereyourmax@gmail.com",
      email_verified: false,
    })
  );
  await assertFails(
    spoofDb.collection("announcements").doc("ann_spoof").set({
      title: "Fake",
      description: "Fake",
      dateLabel: "Nov 16",
      category: "IMPORTANT",
      isImportant: true,
      createdAt: serverTimestamp(),
    })
  );
});

test("Phase 3 Admin boundaries: Normal member cannot read audit logs, QR events, or collectionGroup data", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }));

  await assertFails(aliceDb.collection("admin_audit_logs").get());
  await assertFails(aliceDb.collection("qr_security_events").get());
  await assertFails(aliceDb.collectionGroup("attendance").get());
  await assertFails(aliceDb.collectionGroup("payment_requests").get());

  await assertFails(
    aliceDb.collection("competition_configs").doc("attendance_streak").set({
      categoryId: "ATTENDANCE_STREAK",
      title: "Attendance Streak",
      periodLabel: "Nov 2026",
      targetDescription: "5 days",
      targetValue: 5,
      rewardTitle: "Free Month",
      isActive: true,
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("promotional_offers").doc("offer_hack").set({
      title: "Free Year",
      subtitle: "Hack",
      badgeText: "HACK",
      validUntilLabel: "Forever",
      targetAudience: "ALL_MEMBERS",
      isActive: true,
      createdAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("notifications_broadcast").doc("notif_hack").set({
      title: "Hacked",
      message: "Bad",
      type: "GENERAL",
      audience: "ALL_MEMBERS",
      isUrgent: true,
      createdByAdminUid: ALICE_UID,
      createdAt: serverTimestamp(),
    })
  );

  // Member CAN log their own blocked QR scan event for admin security review
  await assertSucceeds(
    aliceDb.collection("qr_security_events").doc("evt_alice_1").set({
      userId: ALICE_UID,
      memberName: "Alice Vance",
      memberCode: "GP-1001",
      eventType: "EXPIRED_TOKEN",
      reason: "Expired QR screenshot",
      reviewed: false,
      createdAt: serverTimestamp(),
    })
  );
});

test("Phase 3 Admin permissions: Verified admin can verify payments, query collectionGroups, configure competitions, publish engagement & audit logs", async () => {
  const adminUid = "admin_max_001";
  const adminDb = getDb(
    testEnv.authenticatedContext(adminUid, {
      email: "hereyourmax@gmail.com",
      email_verified: true,
    })
  );
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      memberCode: "GP-1001",
      fullName: "Alice Vance",
      email: "alice@test.com",
      role: "MEMBER",
      joinedDate: "Nov 16, 2026",
      referralCode: "GP-REF-1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Admin reads collectionGroups and user list
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collectionGroup("attendance").get());
  await assertSucceeds(adminDb.collectionGroup("payment_requests").get());

  // Admin writes competition config, promotional offer, announcement, broadcast, and immutable audit log
  await assertSucceeds(
    adminDb.collection("competition_configs").doc("attendance_streak").set({
      categoryId: "ATTENDANCE_STREAK",
      title: "Attendance Streak",
      periodLabel: "November 2026",
      targetDescription: "Longest consecutive gym check-in streak",
      targetValue: 20,
      rewardTitle: "1 Month Pro + Hoodie",
      isActive: true,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("promotional_offers").doc("offer_winter").set({
      title: "Winter Renewal Bonus",
      subtitle: "Renew 3 days early and get a recovery session.",
      badgeText: "20% OFF PT",
      discountCode: "WINTER20",
      validUntilLabel: "Nov 30, 2026",
      targetAudience: "ALL_MEMBERS",
      isActive: true,
      createdAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("notifications_broadcast").doc("notif_1").set({
      title: "Holiday Hours",
      message: "Club closes at 4 PM on Nov 26.",
      type: "IMPORTANT_UPDATE",
      audience: "ALL_MEMBERS",
      isUrgent: true,
      createdByAdminUid: adminUid,
      createdAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("admin_audit_logs").doc("log_1").set({
      adminUserId: adminUid,
      adminEmail: "hereyourmax@gmail.com",
      actionType: "PUBLISH_OFFER",
      targetId: "offer_winter",
      summary: "Published Winter Renewal Bonus offer",
      createdAt: serverTimestamp(),
    })
  );

  // Audit logs are immutable even to admin
  await assertFails(adminDb.collection("admin_audit_logs").doc("log_1").update({ summary: "Altered" }));
  await assertFails(adminDb.collection("admin_audit_logs").doc("log_1").delete());

  // Signed-in member can read published configs, offers & broadcasts
  await assertSucceeds(aliceDb.collection("competition_configs").get());
  await assertSucceeds(aliceDb.collection("promotional_offers").get());
  await assertSucceeds(aliceDb.collection("notifications_broadcast").get());
});

