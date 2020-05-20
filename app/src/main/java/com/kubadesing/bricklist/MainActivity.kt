package com.kubadesing.bricklist

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        add_floating_button.setOnClickListener {
            startActivity(Intent(this, NewProjectActivity::class.java))
            overridePendingTransition(R.anim.in_down, R.anim.out_up)
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main, ProjectsFragment())
            .commit()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.out_left, R.anim.in_right)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
