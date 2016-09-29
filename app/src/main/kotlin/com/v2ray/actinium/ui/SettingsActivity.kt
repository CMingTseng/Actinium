package com.v2ray.actinium.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.v2ray.actinium.R
import com.v2ray.actinium.aidl.IV2RayService
import com.v2ray.actinium.service.V2RayVpnService
import de.psdev.licensesdialog.LicensesDialogFragment
import org.jetbrains.anko.act
import org.jetbrains.anko.startActivity

class SettingsActivity : BaseActivity() {
    companion object {
        const val PREF_START_ON_BOOT = "pref_start_on_boot"
        const val PREF_PER_APP_PROXY = "pref_per_app_proxy"
        const val PREF_EDIT_BYPASS_LIST = "pref_edit_bypass_list"
        const val PREF_LICENSES = "pref_licenses"
        const val PREF_DONATE = "pref_donate"
        const val PREF_FEEDBACK = "pref_feedback"
        const val PREF_AUTO_RESTART = "pref_auto_restart"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragment() {
        val blacklist by lazy { findPreference(PREF_PER_APP_PROXY) as CheckBoxPreference }
        val autoRestart by lazy { findPreference(PREF_AUTO_RESTART) as CheckBoxPreference }
        val editBlacklist: Preference by lazy { findPreference(PREF_EDIT_BYPASS_LIST) }
        val licenses: Preference by lazy { findPreference(PREF_LICENSES) }
        val donate: Preference by lazy { findPreference(PREF_DONATE) }
        val feedback: Preference by lazy { findPreference(PREF_FEEDBACK) }

        val conn = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val bgService = IV2RayService.Stub.asInterface(service)

                val isV2RayRunning = bgService.isRunning

                autoRestart.isEnabled = !isV2RayRunning

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (isV2RayRunning) {
                        blacklist.isEnabled = false
                        editBlacklist.isEnabled = false
                    } else {
                        editBlacklist.setOnPreferenceClickListener {
                            startActivity<BypassListActivity>()
                            true
                        }
                    }
                } else {
                    blacklist.summary = getString(R.string.summary_pref_per_app_proxy_pre_lollipop)
                    blacklist.isEnabled = false
                    editBlacklist.isEnabled = false
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_settings)

            licenses.setOnPreferenceClickListener {
                val fragment = LicensesDialogFragment.Builder(act)
                        .setNotices(R.raw.licenses)
                        .setIncludeOwnLicense(false)
                        .build()
                fragment.show((act as AppCompatActivity).supportFragmentManager, null)
                true
            }

            donate.setOnPreferenceClickListener {
                openUri("https://blockchain.info/address/191Ky8kA4BemiG3RfPiJjStEUqFcQ4DdAB")
                true
            }

            feedback.setOnPreferenceClickListener {
                openUri("https://github.com/V2Ray-Android/Actinium/issues/new")
                true
            }

            val intent = Intent(act.applicationContext, V2RayVpnService::class.java)
            act.bindService(intent, conn, BIND_AUTO_CREATE)
        }

        override fun onDestroy() {
            super.onDestroy()
            act.unbindService(conn)
        }

        private fun openUri(uriString: String) {
            val uri = Uri.parse(uriString)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}