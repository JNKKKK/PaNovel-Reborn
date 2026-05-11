package cc.aoeiuv020.log

import org.slf4j.Logger

/**
 * SLF4J Logger extension functions providing lazy message evaluation.
 */

inline fun Logger.debug(message: () -> Any?) {
    if (isDebugEnabled) {
        debug(message()?.toString())
    }
}

inline fun Logger.info(message: () -> Any?) {
    if (isInfoEnabled) {
        info(message()?.toString())
    }
}

inline fun Logger.warn(message: () -> Any?) {
    if (isWarnEnabled) {
        warn(message()?.toString())
    }
}

inline fun Logger.error(message: () -> Any?) {
    if (isErrorEnabled) {
        error(message()?.toString())
    }
}

inline fun Logger.error(e: Throwable, message: () -> Any?) {
    if (isErrorEnabled) {
        error(message()?.toString(), e)
    }
}

inline fun Logger.warn(e: Throwable, message: () -> Any?) {
    if (isWarnEnabled) {
        warn(message()?.toString(), e)
    }
}

inline fun Logger.debug(e: Throwable, message: () -> Any?) {
    if (isDebugEnabled) {
        debug(message()?.toString(), e)
    }
}
