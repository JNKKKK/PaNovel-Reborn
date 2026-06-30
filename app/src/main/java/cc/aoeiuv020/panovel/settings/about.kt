package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cc.aoeiuv020.panovel.databinding.ContentAboutBinding
import cc.aoeiuv020.panovel.main.Check
import cc.aoeiuv020.panovel.util.VersionUtil
import cc.aoeiuv020.panovel.util.notNullOrReport

class AboutFragment : Fragment() {
    private var _binding: ContentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentAboutBinding.inflate(inflater, container, false)
        val currentVersionName = VersionUtil.getAppVersionName(requireContext())
        binding.tvVersion.text = currentVersionName
        binding.tvChangeLog.text = "加载中..."
        Check.asyncLoadChangeLog { text ->
            _binding?.tvChangeLog?.text = text
        }

        binding.tvUpdate.setOnClickListener {
            Check.asyncCheckVersion(requireActivity().notNullOrReport(), true)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
