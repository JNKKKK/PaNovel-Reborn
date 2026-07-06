package cc.aoeiuv020.panovel.settings

import android.graphics.Typeface
import android.net.Uri
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.Delegates
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.reader.AnimationMode
import cc.aoeiuv020.reader.ReaderConfig

object ReaderSettings : Pref {
    override val name: String
        get() = "Reader"

    /**
     * 阅读显示设置各项的默认值，单一来源：
     * 既作为对应 Delegate 的默认值，也用于设置弹窗“恢复默认”，
     */
    const val DEFAULT_TEXT_SIZE = 26
    const val DEFAULT_MESSAGE_SIZE = 12
    const val DEFAULT_BRIGHTNESS = -1
    const val DEFAULT_LINE_SPACING = 13
    const val DEFAULT_PARAGRAPH_SPACING = 0
    const val DEFAULT_ANIMATION_SPEED = 0.8f
    val DEFAULT_ANIMATION_MODE = AnimationMode.SIMULATION
    val DEFAULT_TEXT_COLOR = 0xff000000.toInt()
    val DEFAULT_BACKGROUND_COLOR = 0xffffe3aa.toInt()

    /**
     * 段首缩进的全角空格数量，可选 0/1/2/4，ListPreference 存字符串，
     */
    private val segmentIndentationCount: String by Delegates.string("2")
    /**
     * 段首缩进，根据数量生成对应个数的全角空格，
     */
    val segmentIndentation: String
        get() = "　".repeat(segmentIndentationCount.toIntOrNull() ?: 0)
    /**
     * 自动保存阅读进度的间隔，单位秒，固定不可配置，
     */
    const val autoSaveReadStatus = 60
    var fitWidth: Boolean by Delegates.boolean(true)
    var fitHeight: Boolean by Delegates.boolean(true)
    var volumeKeyScroll: Boolean by Delegates.boolean(true)
    var centerPercent: Float by Delegates.float(0.5f)
    // 亮度，0-255, 负数代表亮度跟随系统，
    var brightness: Int by Delegates.int(DEFAULT_BRIGHTNESS)
    // 保持屏幕长亮，
    val keepScreenOn: Boolean by Delegates.boolean(false)
    // 启用全屏阅读，
    val fullScreen: Boolean by Delegates.boolean(true)
    /**
     * 阅读界面点击退出全屏的延迟，固定不可配置，
     * 有点延迟看着顺眼点，
     */
    const val fullScreenDelay = 300
    var textSize: Int by Delegates.int(DEFAULT_TEXT_SIZE)
    var lineSpacing: Int by Delegates.int(DEFAULT_LINE_SPACING)
    var paragraphSpacing: Int by Delegates.int(DEFAULT_PARAGRAPH_SPACING)

    /**
     * 小说内容的留白，
     */
    val contentMargins: Margins = Margins("ContentMargins", true, 1, 3, 1, 3)
    val paginationMargins: Margins = Margins("PaginationMargins", true, -1, -1, 3, 1)
    val bookNameMargins: Margins = Margins("BookNameMargins", true, 50, -1, -1, 1)
    val chapterNameMargins: Margins = Margins("ChapterNameMargins", true, 3, 1, -1, -1)
    val timeMargins: Margins = Margins("TimeMargins", true, -1, 1, 3, -1)
    val batteryMargins: Margins = Margins("BatteryMargins", true, 3, -1, -1, 1)

    /**
     * 所有边距设置，供设置弹窗快照、恢复默认时统一遍历，
     */
    val allMargins: List<Margins>
        get() = listOf(
            contentMargins, paginationMargins, bookNameMargins,
            chapterNameMargins, timeMargins, batteryMargins,
        )
    /**
     * 对应上面几个，也就是页眉页脚那些信息的字体大小，
     */
    var messageSize: Int by Delegates.int(DEFAULT_MESSAGE_SIZE)
    var autoRefreshInterval: Int by Delegates.int(60)
    // 页眉页脚时间格式，固定不可配置，
    const val dateFormat = "HH:mm"
    var font: Uri? by Delegates.uri()
    val tfFont: Typeface?
        get() = font?.let {
            try {
                Typeface.createFromFile(it.path)
            } catch (e: Exception) {
                // 文件损坏的情况，
                Reporter.post("字体生成失败", e)
                null
            }
        }
    var textColor: Int by Delegates.int(DEFAULT_TEXT_COLOR)
    var backgroundColor: Int by Delegates.int(DEFAULT_BACKGROUND_COLOR)
    var backgroundImage: Uri? by Delegates.uri()
    /**
     * 保存两对配色，假装有夜间模式，
     * 修改配色时，当前配色存到last里，
     * 也就是last保存前一次设置的颜色，
     */
    var lastTextColor: Int by Delegates.int(0xff737273.toInt())
    var lastBackgroundColor: Int by Delegates.int(0xff191B19.toInt())
    var lastBackgroundImage: Uri? by Delegates.uri()
    var animationMode: AnimationMode by Delegates.enum(DEFAULT_ANIMATION_MODE)
    var animationSpeed: Float by Delegates.float(DEFAULT_ANIMATION_SPEED)

    fun makeReaderConfig() = ReaderConfig(
            textSize,
            lineSpacing,
            paragraphSpacing,
            contentMargins,
            paginationMargins,
            bookNameMargins,
            chapterNameMargins,
            timeMargins,
            batteryMargins,
            messageSize,
            dateFormat,
            textColor,
            backgroundColor,
            backgroundImage,
            animationMode,
            animationSpeed,
            tfFont,
            centerPercent,
            autoRefreshInterval,
            fitWidth,
            fitHeight
    )

    /**
     * 把设置弹窗里能调的阅读显示设置恢复成默认值，
     * 字体也恢复成默认（清空自定义字体），背景图清空，
     * 不动其它不在设置弹窗里的项，
     */
    fun restoreDefaults() {
        textSize = DEFAULT_TEXT_SIZE
        messageSize = DEFAULT_MESSAGE_SIZE
        brightness = DEFAULT_BRIGHTNESS
        lineSpacing = DEFAULT_LINE_SPACING
        paragraphSpacing = DEFAULT_PARAGRAPH_SPACING
        animationMode = DEFAULT_ANIMATION_MODE
        animationSpeed = DEFAULT_ANIMATION_SPEED
        textColor = DEFAULT_TEXT_COLOR
        backgroundColor = DEFAULT_BACKGROUND_COLOR
        backgroundImage = null
        font = null
        allMargins.forEach { it.resetToDefault() }
    }
}