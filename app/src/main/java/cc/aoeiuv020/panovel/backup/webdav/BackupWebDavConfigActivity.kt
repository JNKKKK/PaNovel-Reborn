package cc.aoeiuv020.panovel.backup.webdav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.databinding.ActivityBackupWebDavConfigBinding
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.panovel.util.tip
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

class BackupWebDavConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupWebDavConfigBinding
    private val backupHelper = BackupWebDavHelper()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupWebDavConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnTest.setOnClickListener {
            if (!checkInput()) {
                return@setOnClickListener
            }
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        backupHelper.test(
                            getInput(binding.llServer),
                            getInput(binding.llUsername),
                            getInput(binding.llPassword).takeIf { it.isNotEmpty() }
                                ?: backupHelper.password
                        )
                    }
                    Toast.makeText(this@BackupWebDavConfigActivity, "测试成功", Toast.LENGTH_SHORT).show()
                } catch (t: Exception) {
                    Timber.e(t, "测试失败：")
                    tip("测试失败：" + t.message)
                }
            }
        }
        binding.btnSave.setOnClickListener {
            if (!checkInput()) {
                return@setOnClickListener
            }
            backupHelper.server = getInput(binding.llServer)
            backupHelper.fileName = getInput(binding.llFileName)
            backupHelper.username = getInput(binding.llUsername)
            getInput(binding.llPassword).takeIf { it.isNotEmpty() }?.let {
                backupHelper.password = it
            }
            finish()
        }
        setInput(binding.llServer, backupHelper.server)
        setInput(binding.llFileName, backupHelper.fileName)
        setInput(binding.llUsername, backupHelper.username)
        if (backupHelper.password.isNotEmpty()) {
            setInputHint(binding.llPassword, "密码不变")
        }

        binding.tvJianguoyun.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://blog.jianguoyun.com/?p=2748")))
            } catch (e: Exception) {
                Timber.e(e, "打开浏览器失败")
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun checkInput(): Boolean {
        getInput(binding.llServer).takeIf { it.isNotBlank() }?.let {
            runCatching { it.toHttpUrl() }.getOrNull()
        }?.also {
            if (it.host == "dav.jianguoyun.com" && (it.encodedPath == "/dav" || it.encodedPath == "/dav/")) {
                tip("坚果云根目录不允许存放文件，请指定子目录，如：\nhttps://dav.jianguoyun.com/dav/panovel")
                return false
            }
        } ?: return false.also {
            Toast.makeText(this, "服务器地址不合法", Toast.LENGTH_SHORT).show()
        }
        getInput(binding.llFileName).takeIf { it.isNotEmpty() } ?: return false.also {
            Toast.makeText(this, "文件名不能为空", Toast.LENGTH_SHORT).show()
        }
        getInput(binding.llUsername).takeIf { it.isNotEmpty() } ?: return false.also {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
        }
        getInput(binding.llPassword).takeIf { it.isNotEmpty() || backupHelper.password.isNotEmpty() }
            ?: return false.also {
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show()
            }
        return true
    }

    private fun getInput(layout: LinearLayout): String {
        return (layout.getChildAt(1) as EditText).text.notNullOrReport().toString()
    }

    private fun setInput(layout: LinearLayout, text: String) {
        (layout.getChildAt(1) as EditText).setText(text)
    }

    private fun setInputHint(layout: LinearLayout, text: String) {
        (layout.getChildAt(1) as EditText).hint = text
    }
}
