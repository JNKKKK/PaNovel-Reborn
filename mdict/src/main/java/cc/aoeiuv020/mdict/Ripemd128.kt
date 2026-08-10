package cc.aoeiuv020.mdict

/**
 * RIPEMD-128 —— MDX 2.0「Encrypted=2」的键索引块用它派生解密密钥（见 [MdxDictionary] 里
 * 的 fast-decrypt）。标准算法，与具体词典无关；只实现 MDX 用得到的一次性 [hash]。
 *
 * 实现按 RIPEMD-128 规范（小端字节序、64 步双流），并用官方测试向量校验过
 * （""、"abc"、"message digest"）。
 */
internal object Ripemd128 {
    // 左流/右流每步选用的消息字下标，
    private val R = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
    )
    private val RR = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
    )
    private val S = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
    )
    private val SS = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
    )
    private val K = intArrayOf(0x00000000, 0x5a827999, 0x6ed9eba1, 0x8f1bbcdc.toInt())
    private val KK = intArrayOf(0x50a28be6.toInt(), 0x5c4dd124.toInt(), 0x6d703ef3.toInt(), 0x00000000)

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
        j < 16 -> x xor y xor z
        j < 32 -> (x and y) or (x.inv() and z)
        j < 48 -> (x or y.inv()) xor z
        else -> (x and z) or (y and z.inv())
    }

    private fun rol(x: Int, n: Int): Int = (x shl n) or (x ushr (32 - n))

    /** 计算 [msg] 的 RIPEMD-128 摘要，返回 16 字节。 */
    fun hash(msg: ByteArray): ByteArray {
        val len = msg.size
        val nBlocks = ((len + 8) ushr 6) + 1
        val words = IntArray(nBlocks * 16)
        for (i in 0 until len) words[i ushr 2] = words[i ushr 2] or ((msg[i].toInt() and 0xFF) shl ((i % 4) * 8))
        words[len ushr 2] = words[len ushr 2] or (0x80 shl ((len % 4) * 8))
        words[nBlocks * 16 - 2] = (len * 8)

        var h0 = 0x67452301
        var h1 = -0x10325477 // 0xefcdab89
        var h2 = -0x67452302 // 0x98badcfe
        var h3 = 0x10325476

        for (blk in 0 until nBlocks) {
            var a = h0; var b = h1; var c = h2; var d = h3
            var ap = h0; var bp = h1; var cp = h2; var dp = h3
            val base = blk * 16
            for (j in 0 until 64) {
                val rnd = j ushr 4
                var t = a + f(j, b, c, d) + words[base + R[j]] + K[rnd]
                t = rol(t, S[j])
                a = d; d = c; c = b; b = t
                var tp = ap + f(63 - j, bp, cp, dp) + words[base + RR[j]] + KK[rnd]
                tp = rol(tp, SS[j])
                ap = dp; dp = cp; cp = bp; bp = tp
            }
            val t = h1 + c + dp
            h1 = h2 + d + ap
            h2 = h3 + a + bp
            h3 = h0 + b + cp
            h0 = t
        }

        val out = ByteArray(16)
        for ((i, h) in intArrayOf(h0, h1, h2, h3).withIndex()) {
            out[i * 4] = (h and 0xFF).toByte()
            out[i * 4 + 1] = ((h ushr 8) and 0xFF).toByte()
            out[i * 4 + 2] = ((h ushr 16) and 0xFF).toByte()
            out[i * 4 + 3] = ((h ushr 24) and 0xFF).toByte()
        }
        return out
    }
}
