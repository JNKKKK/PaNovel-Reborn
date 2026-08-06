package cc.aoeiuv020.panovel.download

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import cc.aoeiuv020.panovel.MvpView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityDownloadBinding

class DownloadActivity : AppCompatActivity(), MvpView {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DownloadActivity::class.java))
        }
    }

    private lateinit var presenter: DownloadPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.cache)

        presenter = DownloadPresenter()
        presenter.attach(this)
        presenter.start()
    }
}
