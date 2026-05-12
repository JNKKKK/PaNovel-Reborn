package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cc.aoeiuv020.panovel.databinding.ContentDisclaimerBinding

class DisclaimerFragment : Fragment() {
    private var _binding: ContentDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentDisclaimerBinding.inflate(inflater, container, false)
        binding.tvDisclaimer.text = requireActivity().assets.open("Disclaimer.txt").reader().readText()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
