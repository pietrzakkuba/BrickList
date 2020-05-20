package com.kubadesing.bricklist

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private var archiveStart: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        archiveStart = getArchive()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getArchive(): Boolean {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean("archive_preference", false)
    }


    override fun finish() {
        super.finish()
        if (getArchive() == archiveStart) {
            NavUtils.navigateUpFromSameTask(this)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        overridePendingTransition(R.anim.out_right, R.anim.in_left)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            android.R.id.home -> {
                if (getArchive() == archiveStart) {
                    NavUtils.navigateUpFromSameTask(this)
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                overridePendingTransition(R.anim.out_right, R.anim.in_left)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}