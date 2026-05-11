package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.databinding.ContentAboutBinding
import cc.aoeiuv020.panovel.main.Check
import cc.aoeiuv020.panovel.server.ServerManager
import cc.aoeiuv020.panovel.util.VersionUtil
import cc.aoeiuv020.panovel.util.notNullOrReport
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import cc.aoeiuv020.panovel.util.safelyShow
import cc.aoeiuv020.regex.compilePattern
import cc.aoeiuv020.regex.pick

/**
 *
 * Created by AoEiuV020 on 2017.11.23-10:26:44.
 */
class AboutFragment : Fragment() {
    private var _binding: ContentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentVersionName = VersionUtil.getAppVersionName(requireActivity())
        binding.tvVersion.text = currentVersionName
        binding.tvEmail.setOnClickListener {
            val addr = binding.tvEmail.text.toString()
            val subject = "${requireActivity().getString(R.string.feedback)}[${requireActivity().getString(R.string.app_name)}]$currentVersionName"
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$addr")).putExtra(Intent.EXTRA_SUBJECT, subject))
        }
        // 可能没有连接上服务器，就用固定的群号，
        val number = ServerManager.config?.qqGroup ?: binding.tvGroup.text.toString()
        val qqClick = View.OnClickListener {
            val urlQQ = "mqqwpa://im/chat?chat_type=group&uin=${number}&version=1"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlQQ)))
        }
        binding.llGroup.setOnClickListener(qqClick)
        binding.tvGroup.setOnClickListener(qqClick)
        val telegramClick = View.OnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/${binding.tvTelegram.text}")))
        }
        binding.llTelegram.setOnClickListener(telegramClick)
        binding.tvTelegram.setOnClickListener(telegramClick)
        binding.tvChangeLog.text = requireActivity().assets.open("ChangeLog.txt").reader().readText()

        binding.tvLicenses.setOnClickListener {
            // [jsoup](https://github.com/jhy/jsoup)
            val pattern = compilePattern("\\[(\\S*)\\]\\((\\S*)\\)")
            val (nameList, linkList) = requireActivity().assets.open("Licenses.txt").reader().readLines()
                    .mapNotNull {
                        try {
                            val (name, link) = it.pick(pattern)
                            Pair(name, link)
                        } catch (_: Exception) {
                            null
                        }
                    }.unzip()
            AlertDialog.Builder(requireActivity())
                .setTitle(requireActivity().getString(R.string.library))
                .setItems(nameList.toTypedArray()) { _, i ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkList[i])))
                }
                .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                .create().safelyShow()
        }

        binding.tvUpdate.setOnClickListener {
            // 异步检查是否有更新，
            Check.asyncCheckVersion(requireActivity().notNullOrReport(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}