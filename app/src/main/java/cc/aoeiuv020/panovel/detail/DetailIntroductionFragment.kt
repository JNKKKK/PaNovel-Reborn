package cc.aoeiuv020.panovel.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cc.aoeiuv020.panovel.R

class DetailIntroductionFragment : Fragment() {
    companion object {
        private const val ARG_TEXT = "introduction"

        fun newInstance(introduction: String): DetailIntroductionFragment {
            return DetailIntroductionFragment().apply {
                arguments = Bundle().apply { putString(ARG_TEXT, introduction) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_detail_introduction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvIntroduction).text = arguments?.getString(ARG_TEXT) ?: ""
    }

    fun updateText(text: String) {
        arguments = Bundle().apply { putString(ARG_TEXT, text) }
        view?.findViewById<TextView>(R.id.tvIntroduction)?.text = text
    }
}
