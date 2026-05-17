package cc.aoeiuv020.panovel.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.ProgressDialogCompat
import timber.log.Timber
import kotlinx.coroutines.*

class CacheClearPreferenceFragment : PreferenceFragmentCompat() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_cache_clear, rootKey)
        removeIconSpace(preferenceScreen)

        val map = mapOf("cache" to {
            DataManager.cleanAllCache()
        }, "cookie" to {
            DataManager.removeAllCookies()
        }, "bookshelf" to {
            DataManager.cleanBookshelf()
        }, "book_list" to {
            DataManager.cleanBookList()
        }, "history" to {
            DataManager.cleanHistory()
        })

        map.keys.forEach { key ->
            findPreference<Preference>(key)?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { p ->
                    map[p.key]?.also { clear ->
                        AlertDialog.Builder(requireContext()).apply {
                            setTitle(getString(R.string.confirm_clear))
                            setMessage(p.title.toString())
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                val dialog = ProgressDialogCompat(requireContext()).apply {
                                    setMessage(getString(R.string.removing, p.title))
                                    show()
                                }
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            clear.invoke()
                                        }
                                        dialog.dismiss()
                                    } catch (e: Exception) {
                                        val message = "清除失败，"
                                        Reporter.post(message, e)
                                        Timber.e(e, message)
                                        dialog.dismiss()
                                    }
                                }
                            }
                            setNegativeButton(android.R.string.cancel, null)
                        }.show()
                    }
                    true
                }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
