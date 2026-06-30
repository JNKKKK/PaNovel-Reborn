package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.panovel.util.attach

abstract class BasePreferenceFragment(
    private val prefObj: Pref,
    private val prefId: Int
) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        attach(prefObj)
        setPreferencesFromResource(prefId, rootKey)
        removeIconSpace(preferenceScreen)
    }
}

class GeneralPreferenceFragment : BasePreferenceFragment(GeneralSettings, R.xml.pref_general)

class ListPreferenceFragment : BasePreferenceFragment(ListSettings, R.xml.pref_list)

class ReaderPreferenceFragment : BasePreferenceFragment(ReaderSettings, R.xml.pref_read)

class DownloadPreferenceFragment : BasePreferenceFragment(DownloadSettings, R.xml.pref_download)

class OthersPreferenceFragment : BasePreferenceFragment(OtherSettings, R.xml.pref_others)

fun removeIconSpace(group: PreferenceGroup) {
    for (i in 0 until group.preferenceCount) {
        val pref = group.getPreference(i)
        pref.isIconSpaceReserved = false
        if (pref is PreferenceGroup) removeIconSpace(pref)
    }
}
