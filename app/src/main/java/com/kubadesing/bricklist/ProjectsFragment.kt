package com.kubadesing.bricklist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kubadesing.bricklist.models.Inventory
import kotlinx.android.synthetic.main.fragment_projects.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ProjectsFragment : Fragment() {
    private var dbHelper: DatabaseHelper? = null
    private var inventories = ArrayList<Inventory>()
    override fun onAttach(context: Context) {
        val thread = Thread {
            dbHelper = DatabaseHelper(context)
            prepareDatabase(dbHelper!!)
            dbHelper?.openDatabase()
            inventories = dbHelper!!.getInventories(getArchive())
            dbHelper?.closeDatabase()
        }
        thread.start()
        super.onAttach(context)
        thread.join()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    private fun getArchive(): Boolean {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this.context)
        return sharedPreferences.getBoolean("archive_preference", false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttons = List(inventories.size) { Button(this.context, null, 0, R.style.ButtonStyle) }
        for (i in 0 until inventories.size) {
            addButton(buttons[i], inventories[i].name, inventories[i].id, inventories[i].active)
        }
        if (inventories.size == 0) {
            no_projects_text_view.visibility = View.VISIBLE
        }

    }

    private fun prepareDatabase(dbHelper: DatabaseHelper) {
        val database: File = activity?.applicationContext!!.getDatabasePath(dbHelper.dbName)
        if (database.length() < 6000000) {
            dbHelper.readableDatabase
            copyDatabase(this.context, dbHelper)
        }
    }

    private fun copyDatabase(context: Context?, db: DatabaseHelper): Boolean {
        return try {
            val inputStream: InputStream = context?.assets!!.open(db.dbName)
            val outFileName = db.dbPath + db.dbName
            val outputStream: OutputStream = FileOutputStream(outFileName)
            val buffer = ByteArray(1024)
            while (inputStream.read(buffer) > 0) {
                outputStream.write(buffer)
            }
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun addButton(button: Button, projectName: String, projectId: Int, projectActive: Int) {
        Thread {
            activity?.runOnUiThread {
                button.text = projectName
                main_layout.addView(button)
            }
            button.setOnClickListener {
                val intent = Intent(activity, InventoryActivity::class.java)
                intent.putExtra("PROJECT_NAME", projectName)
                intent.putExtra("PROJECT_ID", projectId)
                intent.putExtra("PROJECT_ACTIVE", projectActive)
                startActivity(intent)
                activity?.overridePendingTransition(R.anim.in_down, R.anim.out_up)

            }
        }.start()

    }

}
