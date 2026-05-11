package cc.aoeiuv020.anull

fun <T> T?.notNull(): T = this ?: throw IllegalStateException("Value must not be null")

fun <T> T?.notNull(message: String): T = this ?: throw IllegalStateException(message)
