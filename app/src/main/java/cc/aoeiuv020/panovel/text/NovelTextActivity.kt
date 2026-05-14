package cc.aoeiuv020.panovel.text

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.api.NovelChapter
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.detail.NovelDetailActivity
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.search.FuzzySearchActivity
import cc.aoeiuv020.panovel.settings.DownloadSettings
import cc.aoeiuv020.panovel.settings.Margins
import cc.aoeiuv020.panovel.settings.ReaderSettings
import cc.aoeiuv020.panovel.util.*
import cc.aoeiuv020.reader.*
import cc.aoeiuv020.reader.AnimationMode
import cc.aoeiuv020.reader.ReaderConfigName.*
import cc.aoeiuv020.regex.pick
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import cc.aoeiuv020.panovel.databinding.DialogSelectColorSchemeBinding
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.TimeUnit
import timber.log.Timber
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope


/**
 *
 * Created by AoEiuV020 on 2017.10.03-19:06:44.
 */
class NovelTextActivity : NovelTextBaseFullScreenActivity(), MvpView {
    companion object {
        fun start(context: Context, novel: Novel) {
            start(context, novel.nId)
        }

        fun start(context: Context, id: Long) {
            context.startActivity(Intent(context, NovelTextActivity::class.java).putExtra(Novel.KEY_ID, id))
        }

        fun start(context: Context, novel: Novel, index: Int) {
            context.startActivity(Intent(context, NovelTextActivity::class.java)
                    .putExtra(Novel.KEY_ID, novel.nId)
                    .putExtra("index", index))
        }
    }

    private lateinit var alertDialog: AlertDialog
    private lateinit var progressDialog: ProgressDialogCompat

    private val backgroundImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                reader.config.backgroundImage = it
            } catch (e: SecurityException) {
                Timber.e(e, "读取背景图失败")
                cacheUri = it
                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), 0)
            } catch (e: FileNotFoundException) {
                Timber.e(e, "神奇，图片找不到，")
            }
        }
    }

    private val fontLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                ReaderSettings.font = it
                setFont(ReaderSettings.tfFont)
            } catch (e: SecurityException) {
                Timber.e(e, "读取字体失败")
                cacheUri = it
                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE), 1)
            } catch (e: FileNotFoundException) {
                Timber.e(e, "神奇，图片找不到，")
            }
        }
    }
    lateinit var presenter: NovelTextPresenter
    private var chaptersAsc: List<NovelChapter> = listOf()
    private var navigation: NovelTextNavigation? = null
    private lateinit var reader: INovelReader

    // 缓存传入的索引，阅读器准备好后跳到这一章，-1表示最后一章，
    private var index: Int? = null
    private var text: Int? = null
    private var _novel: Novel? = null
    private var novel: Novel
        get() = _novel.notNullOrReport()
        set(value) {
            _novel = value
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _novel ?: return
        outState.putInt("index", novel.readAtChapterIndex)
        outState.putInt("text", novel.readAtTextIndex)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alertDialog = AlertDialog.Builder(this).create()
        progressDialog = ProgressDialogCompat(this)
        progressDialog.setOnCancelListener { finish() }

        val id = intent?.getLongExtra(Novel.KEY_ID, -1L)
        Timber.d("receive id: $id")
        if (id == null || id == -1L) {
            Reporter.unreachable()
            finish()
            return
        }
        title = id.toString()

        index = savedInstanceState?.getInt("index", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
                ?: intent?.getIntExtra("index", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        text = savedInstanceState?.getInt("text", Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }

        presenter = NovelTextPresenter(id)

        loading(progressDialog, R.string.novel_chapters)
        presenter.attach(this)
        // 进去后根据id得到小说对象，
        // 只查询数据库，认为很快，所以不考虑没有小说对象时的用户操作，
        presenter.start()
    }

    private var autoSaveJob: Job? = null

    private fun startAutoSave() {
        autoSaveJob = lifecycleScope.launch {
            while (isActive) {
                delay(TimeUnit.SECONDS.toMillis(1) * ReaderSettings.autoSaveReadStatus)
                presenter.saveReadStatus(novel)
            }
        }
    }

    fun showNovel(novel: Novel) {
        this.novel = novel
        initReader(novel)
        navigation = NovelTextNavigation(this, novel, binding.navView.root)
        try {
            binding.urlTextView.text = presenter.getDetailUrl()
        } catch (e: Exception) {
            val message = "获取小说《${novel.name}》<${novel.site}, ${novel.detail}>详情页地址失败，"
            // 按理说每个网站的extra都是设计好的，可以得到完整地址的，
            // 但就算失败了在这里也没什么关系，
            Reporter.post(message, e)
            Timber.e(e, message)
            showError(message, e)
        }
        binding.urlBar.setOnClickListener {
            // urlTextView只显示完整地址，以便点击打开，
            // 只支持打开网络地址，本地小说不支持调用其他app打开，
            binding.urlTextView.text?.takeIf { it.startsWith("http") }
                    ?.also { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it.toString()))) }
                    ?: showError("本地小说不支持外部打开，")
        }
        if (ReaderSettings.autoSaveReadStatus > 0) {
            startAutoSave()
        } else {
            presenter.saveReadStatus(novel)
        }

        cancelNotify(novel.nId.toInt())

        presenter.requestChapters()
    }

    private val contentRequester: TextRequester = object : TextRequester {
        // MayBeConstant是个bug, 1.2.50修复，
        // https://youtrack.jetbrains.com/issue/KT-23756
        @Suppress("MayBeConstant")
        private val imagePattern = "^!\\[img\\]\\((.*)\\)$"
        private val intent = ReaderSettings.segmentIndentation

        override fun requestParagraph(string: String): Any {
            return try {
                // 是图片就返回阅读器识别的Image类对象，
                Image(URL(string.pick(imagePattern).first()))
            } catch (e: Exception) {
                // 则否加上段首空格，
                "$intent$string"
            }
        }

        override fun requestImage(
                image: Image,
                exceptionHandler: (Throwable) -> Unit,
                block: (File) -> Unit
        ) {
            val glideUrl = object : GlideUrl(image.url) {
                override fun getHeaders(): MutableMap<String, String> {
                    return mutableMapOf("Referer" to binding.urlTextView.text.toString())
                }
            }
            Glide.with(this@NovelTextActivity.applicationContext)
                    .downloadOnly()
                    .load(glideUrl)
                    .listener(object : RequestListener<File> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>, isFirstResource: Boolean): Boolean {
                            exceptionHandler(e.notNullOrReport())
                            // 一般来说就是网络问题，
                            // TODO: smart case? Contracts,
                            Reporter.post("加载图片<${image.url}>失败", e.notNullOrReport())
                            return true
                        }

                        override fun onResourceReady(resource: File, model: Any, target: Target<File>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            block(resource)
                            return true
                        }
                    })
                    .into(object : CustomTarget<File>() {
                        override fun onResourceReady(resource: File, transition: Transition<in File?>?) {
                        }
                        override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        }
                    })
        }

        override fun requestChapter(index: Int, refresh: Boolean): List<String> {
            return presenter.requestContent(index, chaptersAsc[index], refresh)
        }
    }

    private fun initReader(novel: Novel) {
        if (::reader.isInitialized) {
            reader.destroy()
        }
        reader = Readers.getReader(this, novel.name,
                binding.flContent, contentRequester, ReaderSettings.makeReaderConfig()).apply {
            menuListener = object : MenuListener {
                override fun hide() {
                    this@NovelTextActivity.hide()
                }

                override fun show() {
                    this@NovelTextActivity.show()
                }

                override fun toggle() {
                    this@NovelTextActivity.toggle()
                }
            }
            readingListener = object : ReadingListener {
                override fun onReading(chapter: Int, text: Int) {
                    // 阅读时退出全屏，
                    hide()
                    if (chapter in chaptersAsc.indices) {
                        onChapterSelected(chapter)
                        novel.readAtTextIndex = text
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (ReaderSettings.backPressOutOfFullScreen && !mVisible) {
            show()
        } else {
            super.onBackPressed()
        }
    }

    override fun show() {
        super.show()
        navigation?.reset(reader.maxTextProgress, reader.textProgress)
    }

    fun previousChapter() {
        selectChapter(reader.currentChapter - 1)
    }

    fun nextChapter() {
        selectChapter(reader.currentChapter + 1)
    }

    fun setTextProgress(progress: Int) {
        novel.readAtTextIndex = progress
        reader.textProgress = progress
    }

    fun refreshCurrentChapter() {
        reader.refreshCurrentChapter()
    }

    /**
     * 切换动画时调用,
     * 重置阅读器，
     */
    private fun resetReader() {
        reader.destroy()
        binding.flContent.removeAllViews() // 多余，上面已经移除，
        initReader(novel)
        showChaptersAsc(chaptersAsc)
    }

    fun setAnimationSpeed(animationSpeed: Float) {
        reader.config.animationSpeed = animationSpeed
    }

    fun setAnimationMode(animationMode: AnimationMode, oldAnimationMode: AnimationMode) {
        Timber.d("setAnimationMode $oldAnimationMode to $animationMode")
        if ((animationMode == AnimationMode.SIMPLE && oldAnimationMode != AnimationMode.SIMPLE)
                || (animationMode != AnimationMode.SIMPLE && oldAnimationMode == AnimationMode.SIMPLE)) {
            resetReader()
        } else {
            reader.config.animationMode = animationMode
        }
    }

    fun setMargins(margins: Margins, name: ReaderConfigName) {
        when (name) {
            ContentMargins -> reader.config.contentMargins = margins
            PaginationMargins -> reader.config.paginationMargins = margins
            TimeMargins -> reader.config.timeMargins = margins
            BatteryMargins -> reader.config.batteryMargins = margins
            BookNameMargins -> reader.config.bookNameMargins = margins
            ChapterNameMargins -> reader.config.chapterNameMargins = margins
            else -> {
            }
        }
    }

    private fun requestBackgroundImage() {
        backgroundImageLauncher.launch(arrayOf("image/*"))
    }

    fun requestFont() {
        fontLauncher.launch(arrayOf("*/*"))
    }

    fun resetFont() {
        ReaderSettings.font = null
        setFont(null)
    }

    private var cacheUri: Uri? = null


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> cacheUri?.let { uri ->
                try {
                    // 不在这里做永久保存，
                    reader.config.backgroundImage = uri
                } catch (e: SecurityException) {
                    Timber.e(e, "读取背景图还是失败")
                    cacheUri = null
                }
            }
            1 -> cacheUri?.let { uri ->
                try {
                    ReaderSettings.font = uri
                    setFont(ReaderSettings.tfFont)
                } catch (e: SecurityException) {
                    Timber.e(e, "读取字体还是失败")
                    cacheUri = null
                }
            }
        }
    }

    private fun setFont(font: Typeface?) {
        reader.config.font = font
    }

    fun setParagraphSpacing(progress: Int) {
        reader.config.paragraphSpacing = progress
    }

    fun setLineSpacing(progress: Int) {
        reader.config.lineSpacing = progress
    }

    fun setMessageSize(textSize: Int) {
        reader.config.messageSize = textSize
    }

    fun setTextSize(textSize: Int) {
        reader.config.textSize = textSize
    }

    override fun onDestroy() {
        autoSaveJob?.cancel()
        if (::presenter.isInitialized) {
            presenter.detach()
        }
        if (::reader.isInitialized) {
            reader.destroy()
        }
        super.onDestroy()
    }

    /**
     * 这个是代码主动选择章节，
     */
    private fun selectChapter(index: Int) {
        if (index !in chaptersAsc.indices) {
            // 超出范围直接无视，
            return
        }
        reader.currentChapter = index
        onChapterSelected(index)
    }

    /**
     * 这个无论是用户翻页切换章节还是其他跳章节都要调用，
     * 改变界面上显示的内容，
     */
    private fun onChapterSelected(index: Int) {
        Timber.d("onChapterSelected $index")
        // 可能重复赋值，但是无所谓了，
        novel.readAt(index, chaptersAsc)
        if (index in chaptersAsc.indices) {
            val chapter = chaptersAsc[index]
            title = "${novel.name} - ${chapter.name}"
            binding.urlTextView.text = presenter.getContentUrl(chapter)
        }
    }

    fun showError(message: String, e: Throwable? = null) {
        progressDialog.dismiss()
        if (e == null) {
            alert(alertDialog, message)
        } else {
            alertError(alertDialog, message, e)
        }
        show()
    }

    /**
     * 给定id找不到小说也就不用继续了，
     * 按理说不会到这里，
     */
    @Suppress("UNUSED_PARAMETER")
    fun showNovelNotFound(message: String, e: Throwable) {
        android.widget.Toast.makeText(this, "$message${e.message}", android.widget.Toast.LENGTH_LONG).show()
        finish()
    }

    fun showChaptersAsc(chaptersAsc: List<NovelChapter>) {
        Timber.d("chapters loaded ${chaptersAsc.size}")
        this.chaptersAsc = chaptersAsc
        if (chaptersAsc.isEmpty()) {
            // 真有小说空章节的，不知道怎么回事，
            // https://m.qidian.com/book/2346657
            alert(alertDialog, R.string.novel_not_support)
            // 无法浏览的情况显示状态栏标题栏导航栏，方便离开，
            show()
            // 进度条可以收起来了，
            progressDialog.dismiss()
            return
        }
        index?.let {
            // 以防万一index再被使用，不知道是否必要，
            index = null
            // 支持跳到最后一章，
            val chapterIndex = if (it < 0) {
                chaptersAsc.lastIndex
            } else {
                it
            }
            // 如果有传入章节索引，就修改novel里存的阅读进度，
            novel.readAt(chapterIndex, chaptersAsc)
            // 如果是从savedInstanceState恢复的，就有章节内进度text,
            // 否则章节内进度改成开头，
            novel.readAtTextIndex = text?.also { text = null } ?: 0
        }
        if (novel.readAtChapterIndex > chaptersAsc.lastIndex
                || novel.readAtChapterIndex < 0) {
            // 以防万一，比如更新后章节反而减少了，
            // 总觉得还有其他可能，但是找不到，
            // 主要是太乱了，找不到到底什么情况会出现-1,
            novel.readAt(chaptersAsc.lastIndex, chaptersAsc)
        }
        onChapterSelected(novel.readAtChapterIndex)
        progressDialog.dismiss()
        lifecycleScope.launch {
            try {
                val chapterList = withContext(Dispatchers.IO) {
                    chaptersAsc.map { it.name }
                }
                Timber.d("load status: <${novel.run { "$readAtChapterIndex.$readAtChapterName/$readAtTextIndex" }}")
                reader.chapterList = chapterList
                reader.currentChapter = novel.readAtChapterIndex
                reader.textProgress = novel.readAtTextIndex
            } catch (e: Exception) {
                val message = "处理小说章节列表失败，"
                Reporter.post(message, e)
                Timber.e(e, message)
                runOnUiThread {
                    showError(message, e)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 要是没得到小说对象，避免进入后面的保存进度，
        _novel ?: return
        // 得到小说novel对象后，进度始终保存在其中，
        // 这里刷一下数据库就好，
        presenter.saveReadStatus(novel)
    }

    private fun refineSearch() {
        FuzzySearchActivity.start(this, novel)
    }

    fun refreshChapterList() {
        loading(progressDialog, R.string.novel_chapters)
        Timber.d("refreshChapterList")
        if (_novel == null) {
            // 以防万一，太乱了，
            return
        }
        // 保存一下的进度，
        if (::reader.isInitialized) {
            novel.readAt(reader.currentChapter, chaptersAsc)
            novel.readAtTextIndex = reader.textProgress
            Timber.d("save status: <${novel.run { "$readAtChapterIndex.$readAtChapterName/$readAtTextIndex" }}")
        }
        presenter.refreshChapterList()
    }

    /**
     * 上一对配色，文字色/背景（图|色），
     */
    fun lastColorScheme() {
        // 交换设置中的两套配色，
        tempColorPref.textColor = ReaderSettings.lastTextColor
        tempColorPref.backgroundColor = ReaderSettings.lastBackgroundColor
        tempColorPref.backgroundImage = ReaderSettings.lastBackgroundImage
        ReaderSettings.lastTextColor = ReaderSettings.textColor
        ReaderSettings.lastBackgroundColor = ReaderSettings.backgroundColor
        ReaderSettings.lastBackgroundImage = ReaderSettings.backgroundImage
        ReaderSettings.textColor = tempColorPref.textColor
        ReaderSettings.backgroundColor = tempColorPref.backgroundColor
        ReaderSettings.backgroundImage = tempColorPref.backgroundImage
        // 切换到上次的配色，已经是现在的配色了，
        // 先设置图片，因为每次设置都会刷新全部，图片可能Uri存在但是文件已经被删除了，
        reader.config.backgroundImage = ReaderSettings.backgroundImage
        reader.config.textColor = ReaderSettings.textColor
        reader.config.backgroundColor = ReaderSettings.backgroundColor
    }

    private val tempColorPref = object : Pref {
        override val name: String
            get() = "TempColor"

        // 默认值没有用，
        var textColor: Int by Delegates.int(0xff000000.toInt())
        var backgroundColor: Int by Delegates.int(0xffffe3aa.toInt())
        var backgroundImage: Uri? by Delegates.uri()
    }

    /**
     * 弹出对话框选择配色，文字色/背景（图|色），
     */
    @SuppressLint("InflateParams")
    fun selectColorScheme() {
        if (!::reader.isInitialized) {
            // 以防万一，
            return
        }
        // 备份当前的配色，
        // 对话框中如果选择取消，就恢复临时配色，
        // 如果确定，临时配色保存到last上次设置的配色，
        tempColorPref.textColor = ReaderSettings.textColor
        tempColorPref.backgroundColor = ReaderSettings.backgroundColor
        tempColorPref.backgroundImage = ReaderSettings.backgroundImage
        AlertDialog.Builder(this@NovelTextActivity).apply {
            setTitle(R.string.select_color_scheme)
            val dialogBinding = DialogSelectColorSchemeBinding.inflate(layoutInflater)
            val view = dialogBinding.root
            dialogBinding.tvBackgroundImage.setOnClickListener {
                requestBackgroundImage()
            }
            dialogBinding.tvInputBackgroundColor.setOnClickListener {
                changeColor(reader.config.backgroundColor) { color ->
                    reader.config.backgroundImage = null
                    reader.config.backgroundColor = color
                }
            }
            dialogBinding.llBackgroundColor.apply {
                val listener = View.OnClickListener {
                    val color = ((it as ImageView).drawable as ColorDrawable).color
                    reader.config.backgroundImage = null
                    reader.config.backgroundColor = color
                }
                (getChildAt(0) as ImageView).apply {
                    setImageDrawable(ColorDrawable(ReaderSettings.backgroundColor))
                    setOnClickListener(listener)
                }
                (getChildAt(1) as ImageView).apply {
                    setImageDrawable(ColorDrawable(ReaderSettings.lastBackgroundColor))
                    setOnClickListener(listener)
                }
                for (index in 2 until 7) {
                    val ivColor = getChildAt(index)
                    ivColor.setOnClickListener(listener)
                }
            }
            dialogBinding.tvInputTextColor.setOnClickListener {
                changeColor(reader.config.textColor) { color ->
                    reader.config.textColor = color
                }
            }
            dialogBinding.llTextColor.apply {
                val listener = View.OnClickListener {
                    val color = ((it as ImageView).drawable as ColorDrawable).color
                    reader.config.textColor = color
                }
                (getChildAt(0) as ImageView).apply {
                    setImageDrawable(ColorDrawable(ReaderSettings.textColor))
                    setOnClickListener(listener)
                }
                (getChildAt(1) as ImageView).apply {
                    setImageDrawable(ColorDrawable(ReaderSettings.lastTextColor))
                    setOnClickListener(listener)
                }
                for (index in 2 until 7) {
                    val ivColor = getChildAt(index)
                    ivColor.setOnClickListener(listener)
                }
            }
            setView(view)
            setPositiveButton(android.R.string.ok) { _, _ ->
                // 确定，临时配色保存到last上次设置的配色，
                ReaderSettings.lastTextColor = tempColorPref.textColor
                ReaderSettings.lastBackgroundColor = tempColorPref.backgroundColor
                ReaderSettings.lastBackgroundImage = tempColorPref.backgroundImage
                // 当前配色保存，
                ReaderSettings.textColor = reader.config.textColor
                ReaderSettings.backgroundColor = reader.config.backgroundColor
                ReaderSettings.backgroundImage = reader.config.backgroundImage
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                // 选择取消，就恢复临时配色，全程没有操作ReaderSettings永久保存的设置，
                reader.config.textColor = tempColorPref.textColor
                setBackground(tempColorPref.backgroundColor, tempColorPref.backgroundImage)
            }
        }.setCancelable(false).create().also {
            // 去除对话框的灰背景，
            it.window.notNullOrReport().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }.safelyShow()
        // 弹对话框时退出全屏，
        hide()
    }

    private fun setBackground(color: Int, image: Uri?) {
        reader.config.backgroundColor = color
        if (image != null) {
            // 避免重复设置覆盖背景色，
            reader.config.backgroundImage = image
        }
    }

    private fun detail() {
        NovelDetailActivity.start(this, novel)
    }

    fun showContents(cachedList: Collection<String>) {
        AlertDialog.Builder(this@NovelTextActivity)
                .setTitle(R.string.contents)
                .setSingleChoiceItems(NovelContentsAdapter(this@NovelTextActivity, novel, chaptersAsc, cachedList), novel.readAtChapterIndex) { dialog, index ->
                    selectChapter(index)
                    dialog.dismiss()
                }.create().apply {
                    listView.isFastScrollEnabled = true
                }.safelyShow()
    }


    fun download() {
        val index = reader.currentChapter
        val count = DownloadSettings.downloadCount
        when {
            count < 0 -> askDownload()
            count == 0 -> presenter.download(index, Int.MAX_VALUE)
            else -> presenter.download(index, count)
        }
    }

    fun askDownload(): Boolean {
        presenter.askDownload(this, reader.currentChapter)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (ReaderSettings.volumeKeyScroll) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> scrollNext()
                KeyEvent.KEYCODE_VOLUME_UP -> scrollPrev()
                else -> return super.onKeyDown(keyCode, event)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (ReaderSettings.volumeKeyScroll) {
            return when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> true
                KeyEvent.KEYCODE_VOLUME_UP -> true
                else -> super.onKeyUp(keyCode, event)
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun scrollNext() {
        reader.scrollNext()
    }

    private fun scrollPrev() {
        reader.scrollPrev()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> refreshChapterList()
            R.id.search -> refineSearch()
            R.id.detail -> detail()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_text, menu)
        return true
    }
}

