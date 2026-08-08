package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.panovel.util.attach
import cc.aoeiuv020.panovel.util.showWithNeutralSurface

abstract class BasePreferenceFragment(
    private val prefObj: Pref,
    private val prefId: Int
) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        attach(prefObj)
        setPreferencesFromResource(prefId, rootKey)
        removeIconSpace(preferenceScreen)
    }

    // The framework's EditTextPreference dialog builds its own AlertDialog outside our helper
    // path (so it shows M3's tinted surface) and reaching it requires the deprecated
    // target-fragment API. Show our own simple edit dialog instead, routed through
    // showWithNeutralSurface so it matches the app surface.
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference) {
            val editText = EditText(requireContext()).apply {
                inputType = preference.editInputType
                setText(preference.text)
                setSelectAllOnFocus(true)
            }
            // Wrap so the field gets the standard dialog horizontal margins (setView adds
            // none, so a bare EditText sits flush against the dialog edges).
            val pad = (24 * resources.displayMetrics.density).toInt()
            val container = android.widget.FrameLayout(requireContext()).apply {
                setPadding(pad, pad / 2, pad, 0)
                addView(editText)
            }
            AlertDialog.Builder(requireContext())
                .setTitle(preference.title)
                .setView(container)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val value = editText.text.toString()
                    if (preference.callChangeListener(value)) preference.text = value
                }
                .setNegativeButton(android.R.string.cancel, null)
                .showWithNeutralSurface()
        } else if (preference is ListPreference) {
            // ListPreference uses its own framework single-choice dialog (also the M3 tinted
            // surface + deprecated target-fragment). Show our own single-choice dialog.
            val entries = preference.entries
            val values = preference.entryValues
            val checked = preference.findIndexOfValue(preference.value)
            AlertDialog.Builder(requireContext())
                .setTitle(preference.title)
                .setSingleChoiceItems(entries, checked) { d, which ->
                    val value = values[which].toString()
                    if (preference.callChangeListener(value)) preference.value = value
                    d.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .showWithNeutralSurface()
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}

class ReaderPreferenceFragment : BasePreferenceFragment(ReaderSettings, R.xml.pref_read)

class DownloadPreferenceFragment : BasePreferenceFragment(DownloadSettings, R.xml.pref_download)

fun removeIconSpace(group: PreferenceGroup) {
    for (i in 0 until group.preferenceCount) {
        val pref = group.getPreference(i)
        pref.isIconSpaceReserved = false
        if (pref is PreferenceGroup) removeIconSpace(pref)
    }
}
