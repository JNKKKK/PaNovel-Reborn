package cc.aoeiuv020.panovel.settings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceFragmentCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.util.Pref
import cc.aoeiuv020.panovel.util.attach

abstract class BasePreferenceFragment(
    private val prefObj: Pref,
    private val prefId: Int
) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        attach(prefObj)
        setPreferencesFromResource(prefId, rootKey)
    }
}

class GeneralPreferenceFragment : BasePreferenceFragment(GeneralSettings, R.xml.pref_general)

class ListPreferenceFragment : BasePreferenceFragment(ListSettings, R.xml.pref_list)

class ReaderPreferenceFragment : BasePreferenceFragment(ReaderSettings, R.xml.pref_read)

class DownloadPreferenceFragment : BasePreferenceFragment(DownloadSettings, R.xml.pref_download)

class LocationPreferenceFragment : BasePreferenceFragment(LocationSettings, R.xml.pref_location),
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "cacheLocation" -> DataManager.resetCacheLocation(requireActivity())
        }
    }

    private fun requestPermissions() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager())) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${requireActivity().packageName}")
            requireActivity().startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
}

class ServerPreferenceFragment : BasePreferenceFragment(ServerSettings, R.xml.pref_server)

class OthersPreferenceFragment : BasePreferenceFragment(OtherSettings, R.xml.pref_others)

class InterfacePreferenceFragment : BasePreferenceFragment(InterfaceSettings, R.xml.pref_interface)
