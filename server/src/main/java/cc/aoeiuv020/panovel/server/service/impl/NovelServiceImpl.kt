package cc.aoeiuv020.panovel.server.service.impl

import cc.aoeiuv020.panovel.server.ServerAddress
import cc.aoeiuv020.panovel.server.common.bookId
import cc.aoeiuv020.panovel.server.common.toBean
import cc.aoeiuv020.panovel.server.common.toJson
import cc.aoeiuv020.panovel.server.dal.model.*
import cc.aoeiuv020.panovel.server.dal.model.autogen.Novel
import cc.aoeiuv020.panovel.server.service.NovelService
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 *
 * Created by AoEiuV020 on 2018.04.05-10:07:57.
 */
class NovelServiceImpl(private val serverAddress: ServerAddress) : NovelService {
    override val baseurl get() = serverAddress.baseUrl
    private val logger: Logger = LoggerFactory.getLogger(NovelServiceImpl::class.java.simpleName)
    private val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(LogInterceptor())
            // 超时设置短一些，连不上就放弃，不是很重要，
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()

    private inner class LogInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            logger.info("connect {}", request.url)
            if (logger.isDebugEnabled) {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                logger.debug("body {}", buffer.readUtf8())
            }
            val response = chain.proceed(request)
            logger.debug("response {}", response.request.url)
            // 应该没有不是网络请求的情况，但是不了解okhttp的缓存，但还是不要在这里用可能抛异常的拓展方法requestHeaders，
            logger.debug("request.headers {}", response.networkResponse?.request?.headers)
            logger.debug("response.headers {}", response.headers)
            return response
        }
    }

    private inline fun <reified I, reified R> postTyped(url: String, input: I): R {
        val mobRequest = MobRequest(input.toJson())
        val requestBody = mobRequest.toJson()
                .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        val call = client.newCall(request)
        val response: MobResponse = call.execute().body!!.string()!!
                .toBean()
        if (!response.isSuccess()) {
            throw IllegalStateException("请求失败: ${response.data}")
        }
        return response.getRealData()
    }

    override fun uploadUpdate(novel: Novel): Boolean {
        logger.debug("uploadUpdate <{}.{}.{}>", novel.site, novel.author, novel.name)
        return postTyped(serverAddress.updateUploadUrl, novel)
    }

    override fun needRefreshNovelList(count: Int): List<Novel> {
        logger.debug("needRefreshNovelList count = {}", count)
        return postTyped(serverAddress.needRefreshNovelListUrl, count)
    }

    override fun queryList(novelMap: Map<Long, Novel>): Map<Long, QueryResponse> {
        logger.debug("queryList {}", novelMap.map { "${it.key}=<${it.value.run { "$site.$author.$name" }}>" })
        return postTyped(serverAddress.queryListUrl, novelMap)
    }

    override fun touch(novel: Novel): Boolean {
        logger.debug("touch {}", novel.bookId)
        return postTyped(serverAddress.touchUrl, novel)
    }

    override fun minVersion(): String {
        logger.debug("minVersion")
        return postTyped(serverAddress.minVersionUrl, "")
    }

    override fun config(): Config {
        logger.debug("config")
        return postTyped(serverAddress.config, "")
    }

    override fun message(): Message {
        logger.debug("message")
        return postTyped(serverAddress.message, "")
    }
}