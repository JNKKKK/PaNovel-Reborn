package cc.aoeiuv020.panovel.text

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import cc.aoeiuv020.mdict.DictEntry
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DictResult
import cc.aoeiuv020.panovel.databinding.DialogDictionaryBinding
import cc.aoeiuv020.panovel.util.applyBottomNavBarInsetPaddingDirect
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout

/**
 * 长按取词的底部弹窗。
 *
 * 结果里可能有多个按长度递增排列的命中词（如 瞬、瞬间），默认展示最长的那个；
 * 顶部一排「长度筹码」可切回更短的词。每个词可能有多个义项，用下方的义项标签切换。
 * 全部数据在 [DictResult] 里，切换只是就地重绑，不再查库。
 *
 * 配色跟随阅读页当前的正文色/背景色（含夜间），保证与正文观感一致。
 */
class DictionaryBottomSheet(
    context: Context,
    private val result: DictResult,
    private val textColor: Int,
    private val backgroundColor: Int,
) : BottomSheetDialog(context) {

    private val binding = DialogDictionaryBinding.inflate(layoutInflater)

    // 拼音/次要文字用正文色的半透明，
    private val secondaryColor = ColorUtils.setAlphaComponent(textColor, 0xB0)

    private var selectedWordIndex = 0
    private var selectedEntryIndex = 0

    // 自绘的长度筹码，按下标保存以便切换时更新选中态，
    // 必须在 init 之前声明，否则 init 里的 setupChips/selectWord 用到时它还是 null，
    private val chipViews = ArrayList<TextView>()

    init {
        setContentView(binding.root)
        applyColors()
        setupChips()
        selectWord(result.words.lastIndex.coerceAtLeast(0))
        // 让弹窗铺到导航栏之下（edge-to-edge），
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        // Material 的 BottomSheetDialog 会在 design_bottom_sheet 容器上拦截 window inset,
        // 内容视图收不到导航栏 inset，所以把「阅读页背景色 + 顶部圆角」的背景画在这个容器上，
        // 并按导航栏高度追加底部内边距：容器一直延伸到半透明导航栏之后（纯色，不透出正文），
        // 而顶部两个圆角是背景本身的圆角（角外透明，露出正文），恢复了圆角观感。
        // 内容视图 binding.root 自身背景透明，避免盖住容器的圆角。与阅读页底部工具栏同一套思路。
        setOnShowListener {
            val sheet = findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            val r = dp(16f)
            sheet.background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            sheet.applyBottomNavBarInsetPaddingDirect()
            // 限高：完全展开时也停在状态栏下方，不顶到状态栏（夜间白色状态栏图标压在浅背景上很难看）。
            val statusBarTop = ViewCompat.getRootWindowInsets(sheet)
                ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val parentHeight = (sheet.parent as? View)?.height ?: sheet.resources.displayMetrics.heightPixels
            BottomSheetBehavior.from(sheet).maxHeight = parentHeight - statusBarTop
        }
    }

    private fun applyColors() {
        // 面板背景由 design_bottom_sheet 容器绘制（见 init），内容视图保持透明，
        binding.root.setBackgroundColor(Color.TRANSPARENT)

        binding.tvHeadword.setTextColor(textColor)
        binding.tvPinyin.setTextColor(secondaryColor)
        binding.tvDefinition.setTextColor(textColor)
        binding.tvDefinition.movementMethod = LinkMovementMethod.getInstance()
        binding.divider.setBackgroundColor(textColor)

        binding.tabEntries.setBackgroundColor(Color.TRANSPARENT)
        binding.tabEntries.setTabTextColors(secondaryColor, textColor)
        binding.tabEntries.setSelectedTabIndicatorColor(textColor)
        // 去掉 TabLayout 默认的选中涟漪高亮底色（否则在浅背景上是一块深色），
        binding.tabEntries.tabRippleColor = android.content.res.ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(textColor, 0x22)
        )
    }

    private fun setupChips() {
        val words = result.words
        if (words.size <= 1) {
            // 只命中一个长度，不显示长度筹码，
            binding.chipScroll.visibility = android.view.View.GONE
            return
        }
        binding.chipScroll.visibility = android.view.View.VISIBLE
        binding.chipContainer.removeAllViews()
        chipViews.clear()

        words.forEachIndexed { index, w ->
            val chip = TextView(context).apply {
                text = w.word
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(dp(14f).toInt(), dp(6f).toInt(), dp(14f).toInt(), dp(6f).toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (index != selectedWordIndex) selectWord(index)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8f).toInt() }
            binding.chipContainer.addView(chip, lp)
            chipViews.add(chip)
        }
    }

    /** 更新所有筹码的选中外观（选中：正文色淡填充+实描边+正文色字；未选中：近透明+浅描边+次要色字）。 */
    private fun updateChipStyles() {
        chipViews.forEachIndexed { index, chip ->
            val selected = index == selectedWordIndex
            chip.background = GradientDrawable().apply {
                cornerRadius = dp(16f)
                setColor(ColorUtils.setAlphaComponent(textColor, if (selected) 0x28 else 0x0D))
                setStroke(dp(1f).toInt(),
                    ColorUtils.setAlphaComponent(textColor, if (selected) 0x99 else 0x30))
            }
            chip.setTextColor(if (selected) textColor else secondaryColor)
        }
    }

    /** 切换到第 [wordIndex] 个命中词，重建义项标签并展示第一个义项。 */
    private fun selectWord(wordIndex: Int) {
        val words = result.words
        if (wordIndex !in words.indices) return
        selectedWordIndex = wordIndex
        selectedEntryIndex = 0

        // 同步筹码选中态，
        if (chipViews.isNotEmpty()) {
            updateChipStyles()
        }

        val entries = words[wordIndex].entries
        setupEntryTabs(entries)
        bindEntry(entries.getOrNull(0))
    }

    private fun setupEntryTabs(entries: List<DictEntry>) {
        val tab = binding.tabEntries
        tab.clearOnTabSelectedListeners()
        tab.removeAllTabs()
        if (entries.size <= 1) {
            tab.visibility = android.view.View.GONE
            return
        }
        tab.visibility = android.view.View.VISIBLE
        entries.forEachIndexed { i, e ->
            tab.addTab(tab.newTab().setText(entryTabLabel(e, i)))
        }
        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab) {
                if (t.position != selectedEntryIndex) {
                    selectedEntryIndex = t.position
                    bindEntry(entries.getOrNull(t.position))
                }
            }

            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })
    }

    /**
     * 义项标签文案。多音/多义字（如 的 de/dí/dì）用拼音作标签最直观；
     * 没有可用拼音时退回带圈数字 ①②③…（超出后用「义N」）。
     */
    private fun entryTabLabel(entry: DictEntry, index: Int): String {
        val pinyin = entry.pinyin
        if (!pinyin.isNullOrBlank()) return pinyin
        val circled = "①②③④⑤⑥⑦⑧⑨⑩"
        return if (index < circled.length) circled[index].toString()
        else context.getString(R.string.dict_entry_label, index + 1)
    }

    private fun bindEntry(entry: DictEntry?) {
        entry ?: return
        binding.tvHeadword.text = entry.headword
        val pinyin = entry.pinyin
        if (pinyin.isNullOrBlank()) {
            binding.tvPinyin.visibility = android.view.View.GONE
        } else {
            binding.tvPinyin.visibility = android.view.View.VISIBLE
            binding.tvPinyin.text = pinyin
        }
        binding.tvDefinition.text = fromHtml(entry.definitionHtml)
    }

    private fun fromHtml(html: String): CharSequence =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }

    private fun dp(value: Float): Float =
        value * context.resources.displayMetrics.density
}
