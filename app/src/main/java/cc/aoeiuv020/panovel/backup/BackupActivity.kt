package cc.aoeiuv020.panovel.backup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityExportBinding
import cc.aoeiuv020.panovel.settings.BackupSettings
import cc.aoeiuv020.panovel.util.ProgressDialogCompat
import cc.aoeiuv020.panovel.util.loading
import cc.aoeiuv020.panovel.util.safelyShow
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : AppCompatActivity(), MvpView {
    private lateinit var binding: ActivityExportBinding

    companion object {
        // 备份文件名前缀，导出时预填，后面拼上时间方便区分，
        private const val BACKUP_NAME_PREFIX = "PaNovel-Backup"
        private const val BACKUP_MIME = "application/zip"

        fun start(context: Context) {
            context.startActivity(Intent(context, BackupActivity::class.java))
        }
    }

    private lateinit var progressDialog: ProgressDialogCompat
    private lateinit var presenter: BackupPresenter

    // 备份时待写出的内容选项，等用户在系统“另存为”里选好位置后再用，
    private var pendingExportOptions: Set<BackupOption> = emptySet()

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { confirmImport(it) }
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) {
            loading(progressDialog, getString(R.string.backup))
            presenter.export(pendingExportOptions, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialogCompat(this)
        binding.btnImport.setOnClickListener { requestImportFile() }
        binding.btnExport.setOnClickListener { showExportOptionsDialog() }

        presenter = BackupPresenter()
        presenter.attach(this)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    // ---- 导入 ----

    private fun requestImportFile() {
        try {
            openDocumentLauncher.launch(arrayOf(BACKUP_MIME, "application/octet-stream", "*/*"))
        } catch (e: ActivityNotFoundException) {
            showError(getString(R.string.no_file_explorer), e)
        }
    }

    private fun confirmImport(uri: Uri) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_backup_import)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                loading(progressDialog, getString(R.string.sImport))
                presenter.import(uri)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().safelyShow()
    }

    // ---- 备份 ----

    private fun showExportOptionsDialog() {
        val labels = arrayOf(
            getString(R.string.backup_option_bookshelf),
            getString(R.string.backup_option_book_list),
            getString(R.string.backup_option_history),
            getString(R.string.backup_option_settings),
        )
        val options = arrayOf(
            BackupOption.Bookshelf,
            BackupOption.BookList,
            BackupOption.History,
            BackupOption.Settings,
        )
        val checked = booleanArrayOf(
            BackupSettings.cbBookshelf,
            BackupSettings.cbBookList,
            BackupSettings.cbHistory,
            BackupSettings.cbSettings,
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_select_content)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.backup) { _, _ ->
                BackupSettings.cbBookshelf = checked[0]
                BackupSettings.cbBookList = checked[1]
                BackupSettings.cbHistory = checked[2]
                BackupSettings.cbSettings = checked[3]
                val selected = options.filterIndexed { index, _ -> checked[index] }.toSet()
                if (selected.isEmpty()) {
                    showMessage(getString(R.string.backup_nothing_selected))
                    return@setPositiveButton
                }
                requestExportFile(selected)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().safelyShow()
    }

    private fun requestExportFile(options: Set<BackupOption>) {
        pendingExportOptions = options
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH).format(Date())
        try {
            createDocumentLauncher.launch("$BACKUP_NAME_PREFIX-$stamp.zip")
        } catch (e: ActivityNotFoundException) {
            showError(getString(R.string.no_file_explorer), e)
        }
    }

    // ---- 结果反馈 ----

    fun showImportSuccess(result: String) {
        progressDialog.dismiss()
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_import_done)
            .setMessage(result)
            .setPositiveButton(android.R.string.ok, null)
            .create().safelyShow()
    }

    fun showExportSuccess(result: String) {
        progressDialog.dismiss()
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_export_done)
            .setMessage(result)
            .setPositiveButton(android.R.string.ok, null)
            .create().safelyShow()
    }

    private val snack: Snackbar by lazy {
        Snackbar.make(binding.clRoot, "", Snackbar.LENGTH_SHORT)
    }

    fun showMessage(message: String) {
        snack.setText(message)
        snack.show()
    }

    fun showError(message: String, e: Throwable) {
        progressDialog.dismiss()
        AlertDialog.Builder(this)
            .setMessage(message + e.message)
            .setPositiveButton(android.R.string.ok, null)
            .create().safelyShow()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
