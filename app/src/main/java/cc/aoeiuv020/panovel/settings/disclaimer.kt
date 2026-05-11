package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cc.aoeiuv020.panovel.databinding.ContentDisclaimerBinding

/**
 *
 * Created by AoEiuV020 on 2017.12.09-18:24:40.
 */
class DisclaimerFragment : Fragment() {
    private var _binding: ContentDisclaimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvDisclaimer.text = requireActivity().assets.open("Disclaimer.txt").reader().readText()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}