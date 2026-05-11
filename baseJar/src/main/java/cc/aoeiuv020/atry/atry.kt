package cc.aoeiuv020.atry

/**
 * Execute the given block and return its result,
 * or return null if any exception is thrown.
 */
inline fun <T> tryOrNul(block: () -> T): T? {
    return try {
        block()
    } catch (_: Exception) {
        null
    }
}
