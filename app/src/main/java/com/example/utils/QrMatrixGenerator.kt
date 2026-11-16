package com.example.utils

import kotlin.math.abs

/**
 * Lightweight pure-Kotlin ISO-18004 QR Code matrix generator (Versions 1..6, Byte Mode, EC Level L).
 * Renders scannable reception entrance QR matrices without external heavy dependencies.
 */
object QrMatrixGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalCodewords: Int,
        val ecCodewords: Int,
        val dataBytesCapacity: Int,
        val alignmentCenters: IntArray
    )

    private val specs = listOf(
        VersionSpec(1, 21, 26, 7, 17, intArrayOf()),
        VersionSpec(2, 25, 44, 10, 32, intArrayOf(6, 18)),
        VersionSpec(3, 29, 70, 15, 53, intArrayOf(6, 22)),
        VersionSpec(4, 33, 100, 20, 78, intArrayOf(6, 26)),
        VersionSpec(5, 37, 134, 26, 106, intArrayOf(6, 30)),
        VersionSpec(6, 41, 172, 36, 134, intArrayOf(6, 34))
    )

    // Precomputed GF(256) exp/log tables with primitive polynomial 0x11d
    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11d
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    fun encode(content: String): Array<BooleanArray> {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val spec = specs.firstOrNull { bytes.size <= it.dataBytesCapacity } ?: specs.last()
        val payloadBytes = bytes.copyOf(minOf(bytes.size, spec.dataBytesCapacity))
        val dataCodewords = encodeDataCodewords(payloadBytes, spec.totalCodewords - spec.ecCodewords)
        val ecCodewords = computeReedSolomon(dataCodewords, spec.ecCodewords)
        val allCodewords = dataCodewords + ecCodewords

        val size = spec.size
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        placeFinderPatterns(matrix, reserved, size)
        placeTimingPatterns(matrix, reserved, size)
        placeAlignmentPatterns(matrix, reserved, spec.alignmentCenters)
        reserveFormatAreas(reserved, size)
        // Dark module
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        placeDataBits(matrix, reserved, size, allCodewords)

        // Evaluate masks 0..7 and choose lowest penalty
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestMatrix = matrix
        for (mask in 0..7) {
            val candidate = Array(size) { r -> BooleanArray(size) { c -> matrix[r][c] } }
            applyMask(candidate, reserved, size, mask)
            applyFormatBits(candidate, size, mask)
            val penalty = computePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestMatrix = candidate
            }
        }
        applyFormatBits(bestMatrix, size, bestMask)
        return bestMatrix
    }

    private fun encodeDataCodewords(payload: ByteArray, dataCapacity: Int): IntArray {
        val bits = ArrayList<Int>(dataCapacity * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Mode indicator: 0100 (Byte mode)
        appendBits(0b0100, 4)
        // Character count indicator (8 bits for versions 1..9)
        appendBits(payload.size, 8)
        for (b in payload) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zero bits)
        val maxBits = dataCapacity * 8
        repeat(minOf(4, maxBits - bits.size)) { bits.add(0) }
        // Pad to byte boundary
        while (bits.size % 8 != 0) bits.add(0)

        val result = IntArray(dataCapacity)
        var idx = 0
        var pos = 0
        while (pos + 8 <= bits.size && idx < dataCapacity) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bits[pos + b]
            }
            result[idx++] = byteVal
            pos += 8
        }
        var padToggle = true
        while (idx < dataCapacity) {
            result[idx++] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }
        return result
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        val gen = rsGeneratorPoly(ecCount)
        val rem = IntArray(ecCount)
        for (d in data) {
            val factor = d xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            if (factor != 0) {
                for (i in 0 until ecCount) {
                    rem[i] = rem[i] xor gfMul(gen[i], factor)
                }
            }
        }
        return rem
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = gfExp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        // Drop leading 1 for remainder division helper
        return poly.copyOfRange(1, poly.size)
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun placeFinderPatterns(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, size: Int) {
        val origins = listOf(0 to 0, 0 to (size - 7), (size - 7) to 0)
        for ((or, oc) in origins) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = or + r
                    val mc = oc + c
                    if (mr !in 0 until size || mc !in 0 until size) continue
                    reserved[mr][mc] = true
                    matrix[mr][mc] = if (r in 0..6 && c in 0..6) {
                        val border = r == 0 || r == 6 || c == 0 || c == 6
                        val center = r in 2..4 && c in 2..4
                        border || center
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun placeTimingPatterns(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, size: Int) {
        for (i in 8 until size - 8) {
            val dark = (i % 2) == 0
            if (!reserved[6][i]) {
                matrix[6][i] = dark
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = dark
                reserved[i][6] = true
            }
        }
    }

    private fun placeAlignmentPatterns(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, centers: IntArray) {
        if (centers.isEmpty()) return
        for (cr in centers) {
            for (cc in centers) {
                if (reserved[cr][cc]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dark = maxOf(abs(dr), abs(dc)) != 1
                        matrix[cr + dr][cc + dc] = dark
                        reserved[cr + dr][cc + dc] = true
                    }
                }
            }
        }
    }

    private fun reserveFormatAreas(reserved: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }
    }

    private fun placeDataBits(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, size: Int, codewords: IntArray) {
        var bitIdx = 0
        val totalBits = codewords.size * 8
        var up = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            val rows = if (up) (size - 1 downTo 0) else (0 until size)
            for (r in rows) {
                for (dc in 0..1) {
                    val c = col - dc
                    if (!reserved[r][c]) {
                        val bit = if (bitIdx < totalBits) {
                            ((codewords[bitIdx / 8] ushr (7 - (bitIdx % 8))) and 1) == 1
                        } else {
                            false
                        }
                        matrix[r][c] = bit
                        bitIdx++
                    }
                }
            }
            up = !up
            col -= 2
        }
    }

    private fun applyMask(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, size: Int, mask: Int) {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (reserved[r][c]) continue
                val invert = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> ((r / 2) + (c / 3)) % 2 == 0
                    5 -> ((r * c) % 2) + ((r * c) % 3) == 0
                    6 -> (((r * c) % 2) + ((r * c) % 3)) % 2 == 0
                    else -> (((r + c) % 2) + ((r * c) % 3)) % 2 == 0
                }
                if (invert) matrix[r][c] = !matrix[r][c]
            }
        }
    }

    private fun applyFormatBits(matrix: Array<BooleanArray>, size: Int, mask: Int) {
        // EC Level L = 01, Format info BCH(15, 5) with 0x5412 mask
        val data = (0b01 shl 3) or (mask and 0b111)
        var rem = data shl 10
        val poly = 0b10100110111
        for (i in 14 downTo 10) {
            if (((rem ushr i) and 1) != 0) {
                rem = rem xor (poly shl (i - 10))
            }
        }
        val format = ((data shl 10) or rem) xor 0b101010000010010

        val coords1 = listOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, 8 to 7, 8 to 8,
            7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        val coords2 = listOf(
            (size - 1) to 8, (size - 2) to 8, (size - 3) to 8, (size - 4) to 8,
            (size - 5) to 8, (size - 6) to 8, (size - 7) to 8,
            8 to (size - 8), 8 to (size - 7), 8 to (size - 6), 8 to (size - 5),
            8 to (size - 4), 8 to (size - 3), 8 to (size - 2), 8 to (size - 1)
        )
        for (i in 0 until 15) {
            val bit = ((format ushr i) and 1) == 1
            val (r1, c1) = coords1[i]
            val (r2, c2) = coords2[i]
            matrix[r1][c1] = bit
            matrix[r2][c2] = bit
        }
    }

    private fun computePenalty(matrix: Array<BooleanArray>, size: Int): Int {
        var penalty = 0
        for (r in 0 until size) {
            var run = 1
            for (c in 1 until size) {
                if (matrix[r][c] == matrix[r][c - 1]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        for (c in 0 until size) {
            var run = 1
            for (r in 1 until size) {
                if (matrix[r][c] == matrix[r - 1][c]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        return penalty
    }
}
