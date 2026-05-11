@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.backup

import android.Manifest
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cc.aoeiuv020.panovel.IView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityExportBinding
import cc.aoeiuv020.panovel.settings.BackupSettings
import cc.aoeiuv020.panovel.util.confirm
import cc.aoeiuv020.panovel.util.loading
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.panovel.util.safelyShow
import com.google.android.material.snackbar.Snackbar
import org.jetbrains.anko.*


class BackupActivity : AppCompatActivity(), AnkoLogger, IView {
    private lateinit var binding: ActivityExportBinding

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity<BackupActivity>()
        }
    }

    lateinit var progressDialog: ProgressDialog
    private lateinit var presenter: BackupPresenter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initWidget()

        presenter = BackupPresenter()
        presenter.attach(this)
        presenter.start()
    }

    fun getCheckedOption(): Set<BackupOption> {
        val options = mutableSetOf<BackupOption>()
        binding.cbBookshelf.isChecked && options.add(BackupOption.Bookshelf)
        binding.cbBookList.isChecked && options.add(BackupOption.BookList)
        binding.cbProgress.isChecked && options.add(BackupOption.Progress)
        binding.cbSettings.isChecked && options.add(BackupOption.Settings)
        return options
    }

    fun getSelectPath(): Uri = when (binding.rgPath.checkedRadioButtonId) {
        R.id.rbDefaultOldUri -> binding.rbDefaultOldUri.text.toString()
        R.id.rbDefaultNewUri -> binding.rbDefaultNewUri.text.toString()
        R.id.rbOtherPath -> binding.etOtherPath.text.toString()
        else -> throw IllegalStateException("未知错误，")
    }.let {
        Uri.parse(it)
    }

    fun getSelectedId(): Int = binding.rgPath.checkedRadioButtonId

    private fun initWidget() {
        progressDialog = ProgressDialog(this)
        binding.btnImport.setOnClickListener {
            confirm(getString(R.string.confirm_backup_import), Runnable {
                loading(progressDialog, getString(R.string.sImport))
                saveSelected()
                presenter.import()
            })
        }
        binding.btnExport.setOnClickListener {
            confirm(getString(R.string.confirm_backup_export), Runnable {
                loading(progressDialog, getString(R.string.export))
                saveSelected()
                presenter.export()
            })
        }
        binding.btnChoose.setOnClickListener {
            requestFile()
        }
        loadSelected()
        repeat(binding.rgPath.childCount) { index ->
            binding.rgPath.getChildAt(index).setOnClickListener { v ->
                val backupHelper = presenter.getHelper(v.id) ?: return@setOnClickListener
                debug {
                    "backup click ${backupHelper.type}"
                }
                startConfig(backupHelper, index)
            }
        }
    }

    private fun loadSelected() {
        val checkedIndex = if (BackupSettings.checkedButtonIndex == -1) {
            binding.rgPath.childCount - 1
        } else {
            BackupSettings.checkedButtonIndex
        }
        binding.rgPath.check(binding.rgPath.getChildAt(checkedIndex).id)
        binding.cbBookshelf.isChecked = BackupSettings.cbBookshelf
        binding.cbBookList.isChecked = BackupSettings.cbBookList
        binding.cbProgress.isChecked = BackupSettings.cbProgress
        binding.cbSettings.isChecked = BackupSettings.cbSettings
    }

    private fun saveSelected() {
        repeat(binding.rgPath.childCount) { index ->
            val childAt = binding.rgPath.getChildAt(index)
            if (childAt.id == binding.rgPath.checkedRadioButtonId) {
                BackupSettings.checkedButtonIndex = if (index == binding.rgPath.childCount - 1) {
                    -1
                } else {
                    index
                }
            }
        }
        BackupSettings.cbBookshelf = binding.cbBookshelf.isChecked
        BackupSettings.cbBookList = binding.cbBookList.isChecked
        BackupSettings.cbProgress = binding.cbProgress.isChecked
        BackupSettings.cbSettings = binding.cbSettings.isChecked
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == 1 -> data?.data?.let { uri ->
                showOtherPath(uri.toString())
            }
            1000 <= requestCode && requestCode < 1000 + binding.rgPath.childCount -> {
                val index = requestCode - 1000
                val radioButton = binding.rgPath.getChildAt(index) as RadioButton
                val backupHelper = presenter.getHelper(radioButton.id).notNullOrReport()
                if (backupHelper.ready()) {
                    radioButton.text = backupHelper.notNullOrReport().configPreview()
                } else {
                    radioButton.text = getString(R.string.backup_click_for_reconfig, backupHelper.type)
                }
                showMessage("配置完成后请重新点击导入或者导出")
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                showMessage("赋予权限后请重新点击导入或者导出")
            }
        }
    }

    private fun requestFile() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.type = "*/*"
        try {
            startActivityForResult(intent, 1)
        } catch (e: ActivityNotFoundException) {
            showError(getString(R.string.no_file_explorer), e)
        }
    }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 1)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1)
        }
    }

    fun showImportSuccess(result: String) {
        progressDialog.dismiss()
        alert(
                message = result,
                title = "导入完成"
        ).safelyShow()
    }

    fun showExportSuccess(result: String) {
        progressDialog.dismiss()
        alert(
                message = result,
                title = "导出完成"
        ).safelyShow()
    }

    fun showDefaultPath(defaultOldUri: String, defaultNewUri: String) {
        binding.rbDefaultOldUri.text = defaultOldUri
        binding.rbDefaultNewUri.text = defaultNewUri
    }

    fun showOtherPath(defaultOtherUri: String) {
        binding.etOtherPath.setText(defaultOtherUri)
    }

    private val snack: Snackbar by lazy {
        // TODO: 有时候不会弹出，只在收起时闪一下，可能是设置了跟着软键盘弹起的原因，
        Snackbar.make(binding.clRoot, "", Snackbar.LENGTH_SHORT)
    }

    fun showMessage(message: String) {
        snack.setText(message)
        snack.show()
    }

    fun showError(message: String, e: Throwable) {
        progressDialog.dismiss()
        showMessage(message + e.message)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun showBackupHint(radioButtonId: Int, test: String) {
        binding.rgPath.find<RadioButton>(radioButtonId).text = test
    }

    fun startConfig(backupHelper: BackupHelper) {
        repeat(binding.rgPath.childCount) { index ->
            val childAt = binding.rgPath.getChildAt(index)
            if (childAt.id == binding.rgPath.checkedRadioButtonId) {
                startConfig(backupHelper, index)
            }
        }
    }

    private fun startConfig(backupHelper: BackupHelper, index: Int) {
        debug {
            "startConfig ${backupHelper.type}"
        }
        startActivityForResult(Intent(ctx, backupHelper.configActivity()), 1000 + index)
    }

}
