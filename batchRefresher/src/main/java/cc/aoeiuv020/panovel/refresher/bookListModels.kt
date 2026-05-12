package cc.aoeiuv020.panovel.refresher

import com.google.gson.annotations.SerializedName

/**
 * 分享书单时供gson解析的bean类，
 * 兼容旧版，
 *
 * Created by AoEiuV020 on 2018.05.28-13:50:33.
 */
class BookListV3(
        @SerializedName("name")
        val name: String,
        @SerializedName("list")
        val list: List<NovelMinimal>,
        @SerializedName("version")
        val version: Int,
        @SerializedName("uuid")
        val uuid: String
)

class BookListV2(
        @SerializedName("name")
        val name: String,
        @SerializedName("list")
        val list: List<NovelMinimal>,
        @SerializedName("version")
        val version: Int
)

class BookListV1(
        @SerializedName("name")
        val name: String,
        @SerializedName("list")
        val list: List<LegacyNovel>
)

class LegacyNovel(
        @SerializedName("name")
        val name: String,
        @SerializedName("author")
        val author: String,
        @SerializedName("site")
        val site: String,
        @SerializedName("requester")
        val requester: LegacyRequester
)

class LegacyRequester(
        @SerializedName("type")
        val type: String,
        @SerializedName("extra")
        val extra: String
)

data class NovelMinimal(
        var site: String,
        var author: String,
        var name: String,
        var detail: String
)

