package cc.aoeiuv020.panovel.settings

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val currentVersionName = VersionUtil.getAppVersionName(activity)
        binding.tvVersion.text = currentVersionName
        binding.tvChangeLog.text = "加载中..."
        Check.asyncLoadChangeLog { text ->
            _binding?.tvChangeLog?.text = text
        }

        binding.tvLicenses.setOnClickListener {
            val pattern = compilePattern("\\[(\\S*)\\]\\((\\S*)\\)")
            val (nameList, linkList) = activity.assets.open("Licenses.txt").reader().readLines()
                    .mapNotNull {
                        try {
                            val (name, link) = it.pick(pattern)
                            Pair(name, link)
                        } catch (_: Exception) {
                            null
                        }
                    }.unzip()
            AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.library))
                .setItems(nameList.toTypedArray()) { _, i ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkList[i])))
                }
                .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                .create().safelyShow()
        }

        binding.tvUpdate.setOnClickListener {
            Check.asyncCheckVersion(activity.notNullOrReport(), true)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}