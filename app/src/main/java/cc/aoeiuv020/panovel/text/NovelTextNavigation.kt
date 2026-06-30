package cc.aoeiuv020.panovel.text

import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.databinding.DialogSelectColorSchemeBinding
import cc.aoeiuv020.panovel.settings.Margins
import cc.aoeiuv020.panovel.settings.ReaderSettings
import cc.aoeiuv020.panovel.text.NovelTextNavigation.Direction.*
import cc.aoeiuv020.panovel.util.*
import cc.aoeiuv020.reader.AnimationMode
import cc.aoeiuv020.reader.ReaderConfigName
import com.google.android.material.tabs.TabLayout
import timber.log.Timber

class NovelTextNavigation(val view: NovelTextActivity, val novel: Novel, navigation: View) {
    private val mPanelDefault: View = navigation.findViewById(R.id.panelDefault)

    init {
        mPanelDefault.findViewById<View>(R.id.ivContents).setOnClickListener {
            view.presenter.loadContents()
        }
        mPanelDefault.findViewById<View>(R.id.ivSettings).setOnClickListener {
            showSettings()
        }
        mPanelDefault.findViewById<CheckableImageView>(R.id.ivStar).apply {
            isChecked = novel.bookshelf
            setOnClickListener {
                toggle()
                view.presenter.updateBookshelf(isChecked)
            }
        }
        mPanelDefault.findViewById<View>(R.id.ivRefresh).setOnClickListener {
            view.refreshCurrentChapter()
            // 收起导航面板，否则刷新的正文被面板挡住，看起来像没反应，
            view.hide()
        }
        mPanelDefault.findViewById<View>(R.id.ivDownload).setOnClickListener {
            view.askDownload()
        }
        mPanelDefault.findViewById<View>(R.id.tvPreviousChapter).setOnClickListener {
            view.previousChapter()
        }
        mPanelDefault.findViewById<View>(R.id.tvNextChapter).setOnClickListener {
            view.nextChapter()
        }
        mPanelDefault.findViewById<SeekBar>(R.id.sbTextProgress).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    view.setTextProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        // 进入阅读时应用保存的亮度，与设置对话框无关，所以放在这里而不是设置对话框中，
        view.setBrightness(ReaderSettings.brightness)
        if (ReaderSettings.keepScreenOn) {
            view.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * 弹出对话框显示阅读设置，字号/亮度/动画/配色/正文间距/正文边距/信息栏/字体都在同一个对话框里分区展示，
     * 编辑时即时预览并保存，底部三个按钮：恢复默认 / 取消 / 应用，
     * “取消”恢复到打开弹窗时的快照；“应用”保留当前设置；“恢复默认”把设置改成默认并刷新控件，但不关闭弹窗，
     * 去除对话框的灰背景，方便即时预览阅读效果，
     */
    private fun showSettings() {
        val dialogView = View.inflate(view, R.layout.dialog_reader_settings, null)

        // 拍下当前设置快照，用于“取消”恢复，
        view.snapshotReaderSettings()
        bindSections(dialogView)

        val dialog = AlertDialog.Builder(view)
            .setTitle(R.string.settings)
            .setView(dialogView)
            .create().apply {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }

        dialogView.findViewById<Button>(R.id.btnApply).setOnClickListener {
            view.commitReaderSettings()
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            view.revertReaderSettings()
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.btnRestoreDefault).setOnClickListener {
            // 恢复默认值并重新绑定控件以刷新显示，不关闭弹窗，
            view.restoreDefaultReaderSettings()
            bindSections(dialogView)
        }
        // 点外面或返回键关闭等同取消，恢复快照，
        dialog.setOnCancelListener {
            view.revertReaderSettings()
        }

        dialog.safelyShow()
        // 弹对话框时退出全屏，
        view.hide()
    }

    /**
     * 给设置弹窗的各分区绑定数据和事件，恢复默认后会再调一次以刷新控件，
     */
    private fun bindSections(dialogView: View) {
        initFontSizeSection(dialogView)
        initBrightnessSection(dialogView)
        initAnimationSection(dialogView)
        initColorSchemeSection(dialogView)
        initTypesettingSection(dialogView)
        initInfoBarSection(
            dialogView, R.id.tabTopInfo, R.id.topInfoMargins,
            listOf(
                ReaderSettings.chapterNameMargins to ReaderConfigName.ChapterNameMargins,
                ReaderSettings.timeMargins to ReaderConfigName.TimeMargins,
            )
        )
        initInfoBarSection(
            dialogView, R.id.tabBottomInfo, R.id.bottomInfoMargins,
            listOf(
                ReaderSettings.batteryMargins to ReaderConfigName.BatteryMargins,
                ReaderSettings.paginationMargins to ReaderConfigName.PaginationMargins,
                ReaderSettings.bookNameMargins to ReaderConfigName.BookNameMargins,
            )
        )
        initFontSection(dialogView)
    }

    private fun initFontSizeSection(dialogView: View) {
        val messageSizeTextView = dialogView.findViewById<TextView>(R.id.messageSizeTextView)
        val messageSizeSeekBar = dialogView.findViewById<SeekBar>(R.id.messageSizeSeekBar)
        val textSizeTextView = dialogView.findViewById<TextView>(R.id.textSizeTextView)
        val textSizeSeekBar = dialogView.findViewById<SeekBar>(R.id.textSizeSeekBar)

        val messageSize = ReaderSettings.messageSize
        Timber.d("load messageSize = $messageSize")
        messageSizeTextView.text = view.getString(R.string.text_size_placeholders, messageSize)
        messageSizeSeekBar.progress = messageSize - 12
        messageSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val iTextSize = 12 + progress
                messageSizeTextView.text = view.getString(R.string.text_size_placeholders, iTextSize)
                view.setMessageSize(iTextSize)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val iTextSize = 12 + seekBar.progress
                ReaderSettings.messageSize = iTextSize
            }
        })

        val textSize = ReaderSettings.textSize
        Timber.d("load textSize = $textSize")
        textSizeTextView.text = view.getString(R.string.text_size_placeholders, textSize)
        textSizeSeekBar.progress = textSize - 12
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val iTextSize = 12 + progress
                textSizeTextView.text = view.getString(R.string.text_size_placeholders, iTextSize)
                view.setTextSize(iTextSize)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val iTextSize = 12 + seekBar.progress
                ReaderSettings.textSize = iTextSize
            }
        })
    }

    private fun initBrightnessSection(dialogView: View) {
        val brightnessSeekBar = dialogView.findViewById<SeekBar>(R.id.brightnessSeekBar)
        val tvBrightnessFollowSystem = dialogView.findViewById<TextView>(R.id.tvBrightnessFollowSystem)

        // 先清监听再设进度，避免恢复默认重新绑定时把跟随系统(-1)误改成0，
        brightnessSeekBar.setOnSeekBarChangeListener(null)
        brightnessSeekBar.progress = ReaderSettings.brightness.coerceAtLeast(0)
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                view.setBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReaderSettings.brightness = seekBar.progress
            }
        })
        tvBrightnessFollowSystem.setOnClickListener {
            view.setBrightnessFollowSystem()
            ReaderSettings.brightness = -1
        }
    }

    private fun initTypesettingSection(dialogView: View) {
        val lineSpacingTextView = dialogView.findViewById<TextView>(R.id.lineSpacingTextView)
        val lineSpacingSeekBar = dialogView.findViewById<SeekBar>(R.id.lineSpacingSeekBar)
        val paragraphSpacingTextView = dialogView.findViewById<TextView>(R.id.paragraphSpacingTextView)
        val paragraphSpacingSeekBar = dialogView.findViewById<SeekBar>(R.id.paragraphSpacingSeekBar)
        val llMargins = dialogView.findViewById<LinearLayout>(R.id.llMargins)

        val lineSpacing = ReaderSettings.lineSpacing
        lineSpacingTextView.text = view.getString(R.string.line_spacing_placeholder, lineSpacing)
        lineSpacingSeekBar.progress = lineSpacing
        lineSpacingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                lineSpacingTextView.text = view.getString(R.string.line_spacing_placeholder, progress)
                view.setLineSpacing(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReaderSettings.lineSpacing = seekBar.progress
            }
        })

        val paragraphSpacing = ReaderSettings.paragraphSpacing
        paragraphSpacingTextView.text = view.getString(R.string.paragraph_spacing_placeholder, paragraphSpacing)
        paragraphSpacingSeekBar.progress = paragraphSpacing
        paragraphSpacingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                paragraphSpacingTextView.text = view.getString(R.string.paragraph_spacing_placeholder, progress)
                view.setParagraphSpacing(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReaderSettings.paragraphSpacing = seekBar.progress
            }
        })

        // 正文边距直接展示在对话框里，所以隐藏其显示开关，
        initLayoutMargins(llMargins, ReaderSettings.contentMargins, ReaderConfigName.ContentMargins)
        llMargins.findViewById<View>(R.id.llDisplay).hide()
    }

    /**
     * 顶部信息栏：章节名/时间，底部信息栏：电池/页码/书名，
     * 每个信息栏用一个 tab 切换不同信息项的边距设置，共用同一个边距编辑控件，
     */
    private fun initInfoBarSection(
        dialogView: View,
        tabId: Int,
        marginsId: Int,
        items: List<Pair<Margins, ReaderConfigName>>,
    ) {
        val tabLayout = dialogView.findViewById<TabLayout>(tabId)
        val marginsLayout = dialogView.findViewById<LinearLayout>(marginsId)

        fun bind(index: Int) {
            val (margins, name) = items[index]
            initLayoutMargins(marginsLayout, margins, name)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                bind(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })
        // 初始绑定第一个 tab，
        bind(tabLayout.selectedTabPosition.coerceAtLeast(0))
    }

    private fun initFontSection(dialogView: View) {
        val rgFont = dialogView.findViewById<RadioGroup>(R.id.rgFont)

        // 切换单选时不触发回调地设置选中项，用于初始化和文件选择取消后的恢复，
        fun checkSilently(id: Int) {
            rgFont.setOnCheckedChangeListener(null)
            rgFont.check(id)
            rgFont.setOnCheckedChangeListener(fontCheckedListener)
        }

        fontCheckedListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCustomFont -> {
                    // 弹文件选择器，选择成功保持选中“自定义字体”，取消则回到“默认字体”，
                    view.requestFont { selected ->
                        if (!selected) {
                            checkSilently(R.id.rbDefaultFont)
                        }
                    }
                }
                R.id.rbDefaultFont -> {
                    view.resetFont()
                }
            }
        }

        // 根据当前是否设置了自定义字体，初始化单选状态，
        checkSilently(if (ReaderSettings.font != null) R.id.rbCustomFont else R.id.rbDefaultFont)
    }

    private var fontCheckedListener: RadioGroup.OnCheckedChangeListener? = null

    private fun initAnimationSection(dialogView: View) {
        val tvAnimationSpeed = dialogView.findViewById<TextView>(R.id.tvAnimationSpeed)
        val sbAnimationSpeed = dialogView.findViewById<SeekBar>(R.id.sbAnimationSpeed)
        val rgAnimationMode = dialogView.findViewById<RadioGroup>(R.id.rgAnimationMode)

        val maxSpeed = 3f
        val animationSpeed: Float = ReaderSettings.animationSpeed
        tvAnimationSpeed.text = view.getString(R.string.animation_speed_placeholder, animationSpeed)
        sbAnimationSpeed.let { sb ->
            sb.progress = (animationSpeed / maxSpeed * sb.max).toInt()
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                private fun progressToSpeed(seekBar: SeekBar) = maxSpeed / seekBar.max * seekBar.progress

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    tvAnimationSpeed.text = view.getString(R.string.animation_speed_placeholder, progressToSpeed(seekBar))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    progressToSpeed(seekBar).let {
                        view.setAnimationSpeed(it)
                        ReaderSettings.animationSpeed = it
                    }
                }

            })
        }

        // 先清监听再设选中项，避免恢复默认后重新绑定时误触发，
        rgAnimationMode.setOnCheckedChangeListener(null)
        rgAnimationMode.check(when (ReaderSettings.animationMode) {
            AnimationMode.SIMULATION -> R.id.rbSimulation
            AnimationMode.COVER -> R.id.rbCover
            AnimationMode.SLIDE -> R.id.rbSlide
            AnimationMode.NONE -> R.id.rbNone
            AnimationMode.SCROLL -> R.id.rbScroll
        })
        rgAnimationMode.setOnCheckedChangeListener { _, checkedId ->
            val animationMode = when (checkedId) {
                R.id.rbSimulation -> AnimationMode.SIMULATION
                R.id.rbCover -> AnimationMode.COVER
                R.id.rbSlide -> AnimationMode.SLIDE
                R.id.rbNone -> AnimationMode.NONE
                R.id.rbScroll -> AnimationMode.SCROLL
                else -> AnimationMode.SIMULATION
            }
            if (ReaderSettings.animationMode != animationMode) {
                ReaderSettings.animationMode = animationMode
                view.setAnimationMode(animationMode)
            }
        }
    }

    private fun initColorSchemeSection(dialogView: View) {
        // 配色区即时应用并保存，交给 Activity 复用配色控件绑定逻辑，
        val binding = DialogSelectColorSchemeBinding.bind(dialogView.findViewById(R.id.colorScheme))
        view.bindColorSchemeSection(binding)
    }

    private fun initLayoutMargins(llMargins: LinearLayout, margins: Margins, name: ReaderConfigName) {
        Timber.d("$name: $margins")
        val cbDisplay = llMargins.findViewById<CheckBox>(R.id.cbDisplay)
        cbDisplay.setOnCheckedChangeListener(null)
        cbDisplay.isChecked = margins.enabled
        cbDisplay.setOnCheckedChangeListener { _, isChecked ->
            margins.enabled = isChecked
            view.setMargins(margins, name)
        }

        initMarginSeekBar(llMargins.findViewById(R.id.iLeft), LEFT, margins, name)
        initMarginSeekBar(llMargins.findViewById(R.id.iRight), RIGHT, margins, name)
        initMarginSeekBar(llMargins.findViewById(R.id.iTop), TOP, margins, name)
        initMarginSeekBar(llMargins.findViewById(R.id.iBottom), BOTTOM, margins, name)
    }

    private enum class Direction {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private fun initMarginSeekBar(layout: View, direction: Direction, margins: Margins, name: ReaderConfigName) {
        val minValue = -1
        val nameTextView: TextView = layout.findViewById(R.id.tvMarginName)
        val decreaseTextView: TextView = layout.findViewById(R.id.tvDecrease)
        val seekBar: SeekBar = layout.findViewById(R.id.sbMargin)
        val increaseTextView: TextView = layout.findViewById(R.id.tvIncrease)
        val valueTextView: TextView = layout.findViewById(R.id.tvMarginValue)
        fun getValue() = when (direction) {
            LEFT -> margins.left
            RIGHT -> margins.right
            TOP -> margins.top
            BOTTOM -> margins.bottom
        }

        fun setValue(value: Int) = when (direction) {
            LEFT -> margins.left = value
            RIGHT -> margins.right = value
            TOP -> margins.top = value
            BOTTOM -> margins.bottom = value
        }
        nameTextView.text = view.getString(R.string.margin_name_placeholder, view.getString(when (direction) {
            LEFT -> R.string.left
            RIGHT -> R.string.right
            TOP -> R.string.top
            BOTTOM -> R.string.bottom
        }))
        valueTextView.text = view.getString(R.string.margin_value_placeholder, getValue())
        decreaseTextView.setOnClickListener {
            val value = seekBar.progress + minValue - 1
            seekBar.progress = value - minValue
            setValue(value)
            view.setMargins(margins, name)
            valueTextView.text = view.getString(R.string.margin_value_placeholder, value)

        }
        increaseTextView.setOnClickListener {
            val value = seekBar.progress + minValue + 1
            seekBar.progress = value - minValue
            setValue(value)
            view.setMargins(margins, name)
            valueTextView.text = view.getString(R.string.margin_value_placeholder, value)
        }
        seekBar.progress = getValue() + 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) {
                    return
                }
                val value = progress + minValue
                setValue(value)
                view.setMargins(margins, name)
                valueTextView.text = view.getString(R.string.margin_value_placeholder, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val value = seekBar.progress + minValue
                setValue(value)
            }
        })
    }

    fun reset(currentTextCount: Int, currentTextProgress: Int) {
        mPanelDefault.findViewById<SeekBar>(R.id.sbTextProgress).apply {
            max = currentTextCount
            progress = currentTextProgress
        }
    }
}
