package cc.aoeiuv020.panovel.backup.webdav

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityBackupWebDavConfigBinding
import cc.aoeiuv020.panovel.util.notNullOrReport
import cc.aoeiuv020.panovel.util.tip
import okhttp3.HttpUrl
import org.jetbrains.anko.*

class BackupWebDavConfigActivity : AppCompatActivity(), AnkoLogger {
    private lateinit var binding: ActivityBackupWebDavConfigBinding
    private val backupHelper = BackupWebDavHelper()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupWebDavConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnTest.setOnClickListener {
            if (!checkInput()) {
                return@setOnClickListener
            }
            doAsync({ t ->
                error("测试失败：", t)
                runOnUiThread {
                    tip("测试失败：" + t.message)
                }
            }) {
                backupHelper.test(getInput(binding.llServer), getInput(binding.llUsername), getInput(binding.llPassword).takeIf { it.isNotEmpty() }
                        ?: backupHelper.password)
                uiThread {
                    toast("测试成功")
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

        binding.tvJianguoyun.setOnClickListener { v ->
            browse("https://blog.jianguoyun.com/?p=2748")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun checkInput(): Boolean {
        getInput(binding.llServer).takeIf { it.isNotBlank() }?.let { HttpUrl.parse(it) }?.also {
            if (it.host() == "dav.jianguoyun.com" && (it.encodedPath() == "/dav" || it.encodedPath() == "/dav/")) {
                tip("坚果云根目录不允许存放文件，请指定子目录，如：\nhttps://dav.jianguoyun.com/dav/panovel")
                return false
            }
        } ?: return false.also {
            toast("服务器地址不合法")
        }
        getInput(binding.llFileName).takeIf { it.isNotEmpty() } ?: return false.also {
            toast("文件名不能为空")
        }
        getInput(binding.llUsername).takeIf { it.isNotEmpty() } ?: return false.also {
            toast("用户名不能为空")
        }
        getInput(binding.llPassword).takeIf { it.isNotEmpty() || backupHelper.password.isNotEmpty() }
                ?: return false.also {
                    toast("密码不能为空")
                }
        return true
    }

    private fun getInput(layout: LinearLayout): String {
        return (layout.getChildAt(1) as EditText).text.notNullOrReport().toString()
    }

    private fun setInput(layout: LinearLayout, text: String) {
        return (layout.getChildAt(1) as EditText).setText(text)
    }

    private fun setInputHint(layout: LinearLayout, text: String) {
        return (layout.getChildAt(1) as EditText).setHint(text)
    }
}