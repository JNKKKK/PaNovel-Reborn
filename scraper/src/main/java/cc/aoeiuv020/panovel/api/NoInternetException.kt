package cc.aoeiuv020.panovel.api

/**
 * 归到断网的异常，不上报，
 */
class NoInternetException(cause: Throwable)
    : RuntimeException("没有连接网络，", cause)