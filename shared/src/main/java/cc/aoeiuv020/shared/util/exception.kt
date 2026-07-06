package cc.aoeiuv020.shared.util

/**
 * 有时候需要主动中断当前操作，直接抛异常，
 */
fun interrupt(message: String): Nothing = throw IllegalStateException(message)
