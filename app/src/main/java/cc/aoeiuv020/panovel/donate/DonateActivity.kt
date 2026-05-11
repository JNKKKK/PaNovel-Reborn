package cc.aoeiuv020.panovel.donate

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ActivityDonateBinding
import cc.aoeiuv020.panovel.settings.AdSettings
import org.jetbrains.anko.ctx
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit


class DonateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonateBinding

    companion object {
        fun start(context: Context) {
            context.startActivity<DonateActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.lPaypal.setOnClickListener {
            Donate.paypal.pay(ctx)
        }

        binding.lAlipay.setOnClickListener {
            Donate.alipay.pay(ctx)
        }

        binding.lWeChatPay.setOnClickListener {
            Donate.weChatPay.pay(ctx)
        }

        binding.tvDonateExplain.text = assets.open("Donate.txt").reader().readText()
    }

    private var stopTime: Long = 0

    override fun onStart() {
        super.onStart()
        if (stopTime > 0 && System.currentTimeMillis() - stopTime > TimeUnit.SECONDS.toMillis(5)) {
            AdSettings.adEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        stopTime = System.currentTimeMillis()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

}
