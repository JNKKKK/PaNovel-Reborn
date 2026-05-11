package cc.aoeiuv020.panovel.ad

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.util.hide
import cc.aoeiuv020.panovel.util.show
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by AoEiuV020 on 2021.04.26-02:23:16.
 */
class TestAdListHelper :
    AdListHelper<String, TestAdListHelper.TestAdItem, TestAdListHelper.TestAdViewHolder>() {
    override val nativeAdEnabled: Boolean
        get() = false

    private val adService: ExecutorService by lazy {
        Executors.newCachedThreadPool()
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @SuppressLint("SimpleDateFormat")
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS")

    override fun realRequestAd(requestAdCount: Int) {
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    List(requestAdCount) {
                        Thread.sleep((200..600).random().toLong())
                        sdf.format(Date())
                    }
                }
                onRequestAdResult(data)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "请求广告异常")
            }
        }
    }

    private fun createAdView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_test_ad, parent, false)
    }

    override fun createAdViewHolder(parent: ViewGroup): TestAdViewHolder {
        return TestAdViewHolder(createAdView(parent))
    }

    override fun createItem(): TestAdItem {
        return TestAdItem()
    }

    override fun onAdDestroy() {
        scope.cancel()
        adList.forEach {
            it.isClosed = true
            it.text = null
        }
    }

    class TestAdItem : AdItem<String>() {
        var text: String? = null
        override fun isAdInit(): Boolean {
            return text != null
        }

        override fun bind(ad: String) {
            text = ad
        }
    }

    class TestAdViewHolder(itemView: View) : AdViewHolder<TestAdItem>(itemView) {
        private val rlContainer: FrameLayout = itemView.findViewById(R.id.rlContainer)
        private val tvText: TextView = itemView.findViewById(R.id.tvText)

        override fun bind(item: TestAdItem) {
            if (item.isClosed || !item.isAdInit()) {
                rlContainer.hide()
            } else {
                rlContainer.show()
            }
            if (item.isAdInit()) {
                tvText.text = item.text
            }
            tvText.setOnClickListener { v ->
                item.isClosed = true
                rlContainer.hide()
            }
        }
    }
}
