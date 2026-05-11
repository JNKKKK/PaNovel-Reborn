package cc.aoeiuv020.panovel.text

import android.content.DialogInterface
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.settings.Margins
import cc.aoeiuv020.panovel.settings.ReaderSettings
import cc.aoeiuv020.panovel.text.NovelTextNavigation.Direction.*
import cc.aoeiuv020.panovel.util.*
import cc.aoeiuv020.reader.AnimationMode
import cc.aoeiuv020.reader.ReaderConfigName
import timber.log.Timber

class NovelTextNavigation(val view: NovelTextActivity, val novel: Novel, navigation: View) {
    private val mPanelDefault: View = navigation.findViewById(R.id.panelDefault)
    private val mPanelSettings: View = navigation.findViewById(R.id.panelSettings)
    private val mPanelTypesetting: View = navigation.findViewById(R.id.panelTypesetting)
    private val mPanelAnimation: View = navigation.findViewById(R.id.panelAnimation)
    private val mPanelMargins: LinearLayout = navigation.findViewById(R.id.panelMargins)

    private fun showLayout(view: View) {
        listOf(mPanelDefault, mPanelSettings, mPanelTypesetting, mPanelAnimation, mPanelMargins).forEach {
            it.takeIf { it == view }?.show()
                    ?: it.hide()
        }
    }

    init {
        mPanelDefault.findViewById<View>(R.id.ivContents).setOnClickListener {
            view.presenter.loadContents()
        }
        mPanelDefault.findViewById<View>(R.id.ivSettings).setOnClickListener {
            showLayout(mPanelSettings)
            view.fullScreen()
        }
        mPanelDefault.findViewById<CheckBox>(R.id.ivStar).apply {
            isChecked = novel.bookshelf
            setOnClickListener {
                toggle()
                view.presenter.updateBookshelf(isChecked)
            }
        }
        mPanelDefault.findViewById<View>(R.id.ivColor).setOnClickListener {
            view.selectColorScheme()
        }
        mPanelDefault.findViewById<View>(R.id.ivColor).setOnLongClickListener {
            view.lastColorScheme()
            true
        }
        mPanelDefault.findViewById<View>(R.id.ivRefresh).apply {
            setOnClickListener {
                view.refreshCurrentChapter()
            }
            setOnLongClickListener {
                view.refreshChapterList()
                true
            }
        }
        mPanelDefault.findViewById<View>(R.id.ivDownload).setOnClickListener {
            view.download()
        }
        mPanelDefault.findViewById<View>(R.id.ivDownload).setOnLongClickListener {
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

        run {
            val messageSizeTextView = mPanelSettings.findViewById<TextView>(R.id.messageSizeTextView)
            val messageSizeSeekBar = mPanelSettings.findViewById<SeekBar>(R.id.messageSizeSeekBar)
            val textSizeTextView = mPanelSettings.findViewById<TextView>(R.id.textSizeTextView)
            val textSizeSeekBar = mPanelSettings.findViewById<SeekBar>(R.id.textSizeSeekBar)
            val tvFont = mPanelSettings.findViewById<TextView>(R.id.tvFont)
            val tvTypesetting = mPanelSettings.findViewById<TextView>(R.id.tvTypesetting)
            val tvAnimation = mPanelSettings.findViewById<TextView>(R.id.tvAnimation)
            val tvBrightness = mPanelSettings.findViewById<TextView>(R.id.tvBrightness)

            val messageSize = ReaderSettings.messageSize
            Timber.d("load textSite = $messageSize")
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
            Timber.d("load textSite = $textSize")
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

            tvFont.setOnClickListener {
                AlertDialog.Builder(view)
                    .setTitle(R.string.select_font)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        view.requestFont()
                    }
                    .setNegativeButton(R.string.set_default) { _, _ ->
                        view.resetFont()
                    }
                    .create().safelyShow()
            }

            tvTypesetting.setOnClickListener {
                showLayout(mPanelTypesetting)
            }

            tvAnimation.setOnClickListener {
                showLayout(mPanelAnimation)
            }

            view.setBrightness(ReaderSettings.brightness)
            tvBrightness.setOnClickListener {
                AlertDialog.Builder(view).apply {
                    setTitle(R.string.brightness)
                    val layout = View.inflate(view, R.layout.dialog_seekbar, null)
                    setView(layout)
                    layout.findViewById<SeekBar>(R.id.seekBar).apply {
                        progress = ReaderSettings.brightness
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                view.setBrightness(progress)
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar) {
                                ReaderSettings.brightness = seekBar.progress
                            }
                        })
                    }
                    setNeutralButton(R.string.follow_system) { _, _ ->
                        view.setBrightnessFollowSystem()
                        ReaderSettings.brightness = -1
                    }
                    @Suppress("ObjectLiteralToLambda")
                    val emptyListener = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                        }
                    }
                    setPositiveButton(android.R.string.yes, emptyListener)
                }.create().apply {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }.safelyShow()
                view.hide()
            }

            if (ReaderSettings.keepScreenOn) {
                view.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        run {
            val lineSpacingTextView = mPanelTypesetting.findViewById<TextView>(R.id.lineSpacingTextView)
            val lineSpacingSeekBar = mPanelTypesetting.findViewById<SeekBar>(R.id.lineSpacingSeekBar)
            val paragraphSpacingTextView = mPanelTypesetting.findViewById<TextView>(R.id.paragraphSpacingTextView)
            val paragraphSpacingSeekBar = mPanelTypesetting.findViewById<SeekBar>(R.id.paragraphSpacingSeekBar)
            val llMargins = mPanelTypesetting.findViewById<LinearLayout>(R.id.llMargins)
            val tvPagination = mPanelTypesetting.findViewById<TextView>(R.id.tvPagination)
            val tvTime = mPanelTypesetting.findViewById<TextView>(R.id.tvTime)
            val tvBattery = mPanelTypesetting.findViewById<TextView>(R.id.tvBattery)
            val tvBookName = mPanelTypesetting.findViewById<TextView>(R.id.tvBookName)
            val tvChapterName = mPanelTypesetting.findViewById<TextView>(R.id.tvChapterName)

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

            initLayoutMargins(llMargins, ReaderSettings.contentMargins, ReaderConfigName.ContentMargins)
            llMargins.findViewById<View>(R.id.llDisplay).hide()

            tvPagination.setOnClickListener {
                showLayout(mPanelMargins)
                initLayoutMargins(mPanelMargins, ReaderSettings.paginationMargins, ReaderConfigName.PaginationMargins)
            }
            tvTime.setOnClickListener {
                showLayout(mPanelMargins)
                initLayoutMargins(mPanelMargins, ReaderSettings.timeMargins, ReaderConfigName.TimeMargins)
            }
            tvBattery.setOnClickListener {
                showLayout(mPanelMargins)
                initLayoutMargins(mPanelMargins, ReaderSettings.batteryMargins, ReaderConfigName.BatteryMargins)
            }
            tvBookName.setOnClickListener {
                showLayout(mPanelMargins)
                initLayoutMargins(mPanelMargins, ReaderSettings.bookNameMargins, ReaderConfigName.BookNameMargins)
            }
            tvChapterName.setOnClickListener {
                showLayout(mPanelMargins)
                initLayoutMargins(mPanelMargins, ReaderSettings.chapterNameMargins, ReaderConfigName.ChapterNameMargins)
            }
        }

        run {
            val tvAnimationSpeed = mPanelAnimation.findViewById<TextView>(R.id.tvAnimationSpeed)
            val sbAnimationSpeed = mPanelAnimation.findViewById<SeekBar>(R.id.sbAnimationSpeed)
            val rgAnimationMode = mPanelAnimation.findViewById<RadioGroup>(R.id.rgAnimationMode)

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

            rgAnimationMode.check(when (ReaderSettings.animationMode) {
                AnimationMode.SIMPLE -> R.id.rbSimple
                AnimationMode.SIMULATION -> R.id.rbSimulation
                AnimationMode.COVER -> R.id.rbCover
                AnimationMode.SLIDE -> R.id.rbSlide
                AnimationMode.NONE -> R.id.rbNone
                AnimationMode.SCROLL -> R.id.rbScroll
            })
            rgAnimationMode.setOnCheckedChangeListener { _, checkedId ->
                val animationMode = when (checkedId) {
                    R.id.rbSimple -> AnimationMode.SIMPLE
                    R.id.rbSimulation -> AnimationMode.SIMULATION
                    R.id.rbCover -> AnimationMode.COVER
                    R.id.rbSlide -> AnimationMode.SLIDE
                    R.id.rbNone -> AnimationMode.NONE
                    R.id.rbScroll -> AnimationMode.SCROLL
                    else -> AnimationMode.SIMPLE
                }
                val oldAnimationMode = ReaderSettings.animationMode
                if (oldAnimationMode != animationMode) {
                    ReaderSettings.animationMode = animationMode
                    view.setAnimationMode(animationMode, oldAnimationMode)
                }
            }
        }
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
        showLayout(mPanelDefault)

        mPanelDefault.findViewById<SeekBar>(R.id.sbTextProgress).apply {
            max = currentTextCount
            progress = currentTextProgress
        }
    }
}
