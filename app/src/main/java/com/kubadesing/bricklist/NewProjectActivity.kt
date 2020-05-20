package com.kubadesing.bricklist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import com.kubadesing.bricklist.models.Item
import kotlinx.android.synthetic.main.activity_new_project.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class NewProjectActivity : AppCompatActivity() {

    private var items = ArrayList<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_upward)

        new_project_button.setOnClickListener {
            addNewProject()
        }
        project_name.addTextChangedListener {
            enableButton()
        }
        project_number.addTextChangedListener {
            enableButton()
        }
    }

    override fun onStart() {
        super.onStart()
        loading_panel.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        showKeyboard()
    }


    private fun showKeyboard() {
        val imm =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onPause() {
        closeKeyboard()
        super.onPause()
    }


    private fun enableButton() {
        new_project_button.isEnabled =
            project_name.text.isNotBlank() && project_number.text.isNotBlank()
    }

    private fun addNewProject() {
        Thread {
            try {
                runOnUiThread {
                    loading_panel.visibility = View.VISIBLE
                }
                val projectName: String = (project_name.text).toString().trim()
                val setNumber: String = (project_number.text).toString()
                val xmlPath: String = getPrefix() + setNumber + ".xml"

                val url = URL(xmlPath)
                val conn = url.openConnection()
                val factory = DocumentBuilderFactory.newInstance()
                val builder: DocumentBuilder = factory.newDocumentBuilder()
                val xmlDoc = builder.parse(conn.getInputStream())
                xmlDoc.documentElement.normalize()

                val itemList: NodeList = xmlDoc.getElementsByTagName("ITEM")
                for (i in 0 until itemList.length) {
                    val itemNode: Node = itemList.item(i)
                    if (itemNode.nodeType == Node.ELEMENT_NODE) {
                        val elem = itemNode as Element
                        val alternate = elem.getElementsByTagName("ALTERNATE").item(0).textContent
                        val itemType = elem.getElementsByTagName("ITEMTYPE").item(0).textContent
                        if (alternate == "N" && itemType == "P") {
                            val itemId = elem.getElementsByTagName("ITEMID").item(0).textContent
                            val qty = (elem.getElementsByTagName("QTY").item(0).textContent).toInt()
                            val color = elem.getElementsByTagName("COLOR").item(0).textContent
                            val extra = elem.getElementsByTagName("EXTRA").item(0).textContent
                            items.add(
                                Item(
                                    itemType,
                                    itemId,
                                    qty,
                                    color,
                                    extra
                                )
                            )
                        }
                    }
                }
                val dbHelper = DatabaseHelper(this)
                val time = System.currentTimeMillis()
                dbHelper.openDatabase()
                dbHelper.newInventory(projectName, time)
                val inventoryId = dbHelper.getInventoryId(time)
                for (item in items) {
                    dbHelper.newItem(
                        inventoryId,
                        item.itemType,
                        item.itemId,
                        item.qty,
                        item.color,
                        item.extra
                    )
                }
                dbHelper.closeDatabase()

                runOnUiThread {
                    loading_panel.visibility = View.GONE
                    Toast.makeText(
                        this.applicationContext,
                        getString(R.string.new_project_toast),
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.in_up, R.anim.out_down)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loading_panel.visibility = View.GONE
                }
                errorDialog()
            }
        }.start()


    }

    private fun errorDialog() {
        SimpleDialog(getString(R.string.error_message_title), getString(R.string.error_message))
            .show(supportFragmentManager, null)
    }

    private fun getPrefix(): String? {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)

        return sharedPreferences.getString(
            "url_preference",
            getString(R.string.settings_prefix_default)
        )
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                overridePendingTransition(R.anim.in_up, R.anim.out_down)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.in_up, R.anim.out_down)
    }

}
