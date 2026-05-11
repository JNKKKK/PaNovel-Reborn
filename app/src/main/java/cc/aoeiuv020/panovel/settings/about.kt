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
import cc.aoeiuv020.panovel.util.safelyShow
import cc.aoeiuv020.regex.compilePattern
import cc.aoeiuv020.regex.pick
import org.jetbrains.anko.alert
import org.jetbrains.anko.browse
import org.jetbrains.anko.email
import org.jetbrains.anko.yesButton

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
            email(binding.tvEmail.text.toString(),
                    "${requireActivity().getString(R.string.feedback)}[${requireActivity().getString(R.string.app_name)}]$currentVersionName")
        }
        // 可能没有连接上服务器，就用固定的群号，
        val number = ServerManager.config?.qqGroup ?: binding.tvGroup.text.toString()
        val qqClick = View.OnClickListener {
            val urlQQ = "mqqwpa://im/chat?chat_type=group&uin=${number}&version=1"
            browse(urlQQ)
        }
        binding.llGroup.setOnClickListener(qqClick)
        binding.tvGroup.setOnClickListener(qqClick)
        val telegramClick = View.OnClickListener {
            browse("https://t.me/${binding.tvTelegram.text}")
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
            requireActivity().alert {
                title = requireActivity().getString(R.string.library)
                items(nameList) { _, i ->
                    requireActivity().browse(linkList[i])
                }
                yesButton { it.dismiss() }
            }.safelyShow()
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