package com.kubadesing.bricklist

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.kubadesing.bricklist.models.Part
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class InventoryActivity : AppCompatActivity() {

    private var dbHelper: DatabaseHelper? = null
    private var id: Int? = null
    private var active: Int? = null
    private var parts: ArrayList<Part>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        val thread = Thread {
            id = intent.getIntExtra("PROJECT_ID", 0)
            active = intent.getIntExtra("PROJECT_ACTIVE", 0)

            dbHelper = DatabaseHelper(this)
            dbHelper!!.openDatabase()
            parts = dbHelper?.getParts(id!!)
        }
        thread.start()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_downward)
        this.title = intent.getStringExtra("PROJECT_NAME")
        thread.join()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper!!.closeDatabase()
    }

    override fun onStart() {
        super.onStart()
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val parent = findViewById<ViewGroup>(R.id.items_layout)
        dbHelper?.updateLastAccessed(id!!)
        Thread {
            for (i in 0 until parts?.size!!) {
                addPart(
                    inflater,
                    parent,
                    parts!![i].id,
                    parts!![i].colorName,
                    parts!![i].partName,
                    parts!![i].quantityInSet,
                    parts!![i].quantityInStore,
                    parts!![i].imageCode
                )
            }
        }.start()

    }

    private fun addPart(
        inflater: LayoutInflater,
        parent: ViewGroup,
        partId: Int,
        colorName: String,
        partName: String,
        quantityInSet: Int,
        quantityInStore: Int,
        imageCode: String
    ) {
        val self = R.layout.item_view
        val view: View = inflater.inflate(self, parent, false)
        val itemName = view.findViewById<TextView>(R.id.item_name)
        val itemColor = view.findViewById<TextView>(R.id.item_color)
        val quantity = view.findViewById<TextView>(R.id.item_quantity)
        val addButton = view.findViewById<Button>(R.id.add_button)
        val subtractButton = view.findViewById<Button>(R.id.subtract_button)
        val itemImage = view.findViewById<ImageView>(R.id.item_image)

        val address = "https://www.lego.com/service/bricks/5/2/$imageCode"
        try {
            val inputStream: InputStream = URL(address).content as InputStream
            val drawable = Drawable.createFromStream(inputStream, null)
            itemImage.setImageDrawable(drawable)
        } catch (e: Exception) {
            itemImage.setImageResource(R.drawable.blank)
        }


        val nameString = "${getString(R.string.part_name_prefix)} $partName"
        val colorString = "${getString(R.string.part_color_prefix)} $colorName"
        val quantityString = "${getString(R.string.part_quantity_prefix)} ${String.format(
            "%d of %d",
            quantityInStore,
            quantityInSet
        )}"
        itemName.text = nameString
        itemColor.text = colorString
        quantity.text = quantityString
        addButton.setOnClickListener {
            addButtonAction(quantityInSet, quantity, partId)
        }
        subtractButton.setOnClickListener {
            subtractButtonAction(quantityInSet, quantity, partId)
        }
        runOnUiThread {
            parent.addView(view)
        }
    }

    private fun addButtonAction(quantityInSet: Int, quantity: TextView, partId: Int) {
        var quantityValue = dbHelper?.getQuantity(partId)!!
        if (quantityValue < quantityInSet) {
            quantityValue++
            dbHelper?.increaseQuantity(partId)
            val quantityString = "${getString(R.string.part_quantity_prefix)} ${String.format(
                "%d of %d",
                quantityValue,
                quantityInSet
            )}"
            quantity.text = quantityString
        }
    }

    private fun subtractButtonAction(quantityInSet: Int, quantity: TextView, partId: Int) {
        var quantityValue = dbHelper?.getQuantity(partId)!!
        if (quantityValue > 0) {
            quantityValue--
            dbHelper?.decreaseQuantity(partId)
            val quantityString = "${getString(R.string.part_quantity_prefix)} ${String.format(
                "%d of %d",
                quantityValue,
                quantityInSet
            )}"
            quantity.text = quantityString
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.inventory_menu, menu)
        val archivedCheckbox = menu.findItem(R.id.archived_checkbox)
        archivedCheckbox.isChecked = dbHelper?.getArchive(this.id!!)!!
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.in_up, R.anim.out_down)
                true
            }
            R.id.archived_checkbox -> {
                item.isChecked = !item.isChecked
                dbHelper?.archiveInventory(this.id!!, !item.isChecked)
                true
            }
            R.id.export_xml_option -> {
                createXML()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun finish() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.in_up, R.anim.out_down)
        super.finish()
    }

    private fun createXML() {

        val docBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()

        val inventoryElement: Element = doc.createElement("INVENTORY")

        for (part in this.parts!!) {
            if (part.quantityInSet - dbHelper?.getQuantity(part.id)!! > 0) {
                val itemElement: Element = doc.createElement("ITEM")

                val itemType: Element = doc.createElement("ITEMTYPE")
                itemType.appendChild(doc.createTextNode("P"))
                val itemId: Element = doc.createElement("ITEMID")
                itemId.appendChild(doc.createTextNode(part.id.toString()))
                val color: Element = doc.createElement("COLOR")
                color.appendChild(doc.createTextNode(part.colorName))
                val qtyFilled: Element = doc.createElement("QTYFILLED")
                qtyFilled.appendChild(
                    doc.createTextNode(
                        (part.quantityInSet - dbHelper?.getQuantity(
                            part.id
                        )!!).toString()
                    )
                )

                itemElement.appendChild(itemType)
                itemElement.appendChild(itemId)
                itemElement.appendChild(color)
                itemElement.appendChild(qtyFilled)
                inventoryElement.appendChild(itemElement)
            }
        }

        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        doc.appendChild(inventoryElement)

        val path = this.filesDir
        val outDir = File(path, "xml_files")
        outDir.mkdir()

        val file = File(outDir, "Wanted_List_" + this.title.replace("\\s".toRegex(), "_") + ".xml")
        transformer.transform(DOMSource(doc), StreamResult(file))

        val authority = "com.kubadesing.bricklist.fileprovider"
        val contentUri = FileProvider.getUriForFile(applicationContext, authority, file)

        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.type = "text/xml"
        intentShareFile.putExtra(
            Intent.EXTRA_STREAM,
            contentUri
        )
        startActivity(Intent.createChooser(intentShareFile, getString(R.string.share_text)))

    }
}
