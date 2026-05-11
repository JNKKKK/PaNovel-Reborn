@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.settings

import android.app.ProgressDialog
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.data.DataManager
import cc.aoeiuv020.panovel.report.Reporter
import cc.aoeiuv020.panovel.util.safelyShow
import timber.log.Timber
import kotlinx.coroutines.*

/**
 *
 * Created by AoEiuV020 on 2017.11.25-16:15:29.
 */
class CacheClearPreferenceFragment : PreferenceFragment() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_cache_clear)
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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

        val listener: Preference.OnPreferenceClickListener = Preference.OnPreferenceClickListener { p ->
            map[p.key]?.also { clear ->
                android.app.AlertDialog.Builder(activity).apply {
                    setTitle(getString(R.string.confirm_clear))
                    setMessage(p.title.toString())
                    setPositiveButton(android.R.string.yes) { _, _ ->
                        val dialog = ProgressDialog(activity).apply {
                            setMessage(getString(R.string.removing, p.title))
                            safelyShow()
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
                }.create().safelyShow()
            }
            true
        }
        repeat(preferenceScreen.preferenceCount) {
            val p = preferenceScreen.getPreference(it)
            p.onPreferenceClickListener = listener
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
