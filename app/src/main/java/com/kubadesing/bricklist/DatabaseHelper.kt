package com.kubadesing.bricklist

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kubadesing.bricklist.models.Inventory
import com.kubadesing.bricklist.models.Part

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, dbName, null, 1) {
    private var mContext: Context? = context
    var dbPath = ""
    val dbName = "BrickList.db"
    private var mDatabase: SQLiteDatabase? = null


    companion object {
        var dbName = "BrickList.db"
    }

    init {
        this.dbPath = "/data/data/" + context.packageName + "/databases/"
    }

    fun openDatabase() {
        val dbPath = mContext?.getDatabasePath(dbName)?.path
        if (mDatabase != null && mDatabase!!.isOpen) return
        mDatabase = SQLiteDatabase.openDatabase(dbPath!!, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun closeDatabase() {
        mDatabase?.close()
    }

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    fun newInventory(name: String, time: Long) {
        val query = String.format(
            """insert into Inventories (Name, Active, LastAccessed) values ('%s', %d, %d)""",
            name,
            1,
            time
        )
        mDatabase?.execSQL(query)
    }

    fun getInventoryId(time: Long): Int? {
        val query = String.format("""select id from Inventories where LastAccessed = %d""", time)
        val cursor = mDatabase?.rawQuery(query, null)
        cursor?.moveToFirst()
        val returnVal = cursor?.getInt(0)
        cursor?.close()
        return returnVal
    }

    fun newItem(
        inventoryId: Int?,
        itemType: String,
        itemId: String,
        qty: Int,
        color: String,
        extra: String
    ) {
        val itemTypeQuery = String.format("select id from ItemTypes where Code = '%s'", itemType)

        val itemIdQuery = String.format("select id from Parts where Code = '%s'", itemId)

        val colorQuery = String.format("select id from Colors where Code = '%s'", color)

        var cursor: Cursor?

        cursor = mDatabase?.rawQuery(itemTypeQuery, null)
        cursor?.moveToFirst()
        val itemTypeValue = cursor?.getInt(0)

        cursor = mDatabase?.rawQuery(itemIdQuery, null)
        cursor?.moveToFirst()
        val itemIdValue = cursor?.getInt(0)

        cursor = mDatabase?.rawQuery(colorQuery, null)
        cursor?.moveToFirst()
        val colorValue = cursor?.getInt(0)

        cursor?.close()

        val insertQuery = String.format(
            """
            insert into InventoriesParts (InventoryID, TypeID, ItemID, QuantityInSet, ColorID) 
            values (%d, %d, %d, %d, %d)
        """.trimMargin(), inventoryId, itemTypeValue, itemIdValue, qty, colorValue
        )

        mDatabase?.execSQL(insertQuery)
    }

    fun getInventories(archive: Boolean): ArrayList<Inventory> {
        val inventories = ArrayList<Inventory>()
        val query = if (archive) {
            String.format(
                """select * from Inventories 
                |  order by LastAccessed desc""".trimMargin()
            )
        } else {
            String.format(
                """select * from Inventories 
                        |where Active = 1
                |  order by LastAccessed desc""".trimMargin()
            )
        }
        val cursor: Cursor? = mDatabase?.rawQuery(query, null)
        cursor?.moveToFirst()
        while (!cursor?.isAfterLast!!) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val active = cursor.getInt(2)
            val lastAccessed = cursor.getInt(3)

            inventories.add(Inventory(id, name, active, lastAccessed))
            cursor.moveToNext()
        }
        cursor.close()
        return inventories
    }

    fun getParts(ProjectId: Int): ArrayList<Part> {
        val parts = ArrayList<Part>()
        val query = String.format(
            """
                select ip.id, c.Name, p.Name, ip.QuantityInSet, ip.QuantityInStore, cd.Code
                from InventoriesParts as ip
                inner join Colors as c
                on c.id = ip.ColorID
                inner join Parts as p
                on p.id = ip.ItemID
                left join Codes as cd
                on cd.ColorID = c.id and cd.ItemID = ip.ItemID
                where ip.InventoryID = %d
            """.trimIndent(), ProjectId
        )
        val cursor = mDatabase?.rawQuery(query, null)
        cursor?.moveToFirst()
        while (!cursor?.isAfterLast!!) {
            val partId = cursor.getInt(0)
            val colorName = cursor.getString(1)
            val partName = cursor.getString(2)
            val quantityInSet = cursor.getInt(3)
            val quantityInStore = cursor.getInt(4)
            var imageCode = cursor.getString(5)
            if (imageCode == null) {
                imageCode = ""
            }
            parts.add(Part(partId, colorName, partName, quantityInSet, quantityInStore, imageCode))
            cursor.moveToNext()
        }
        cursor.close()
        return parts
    }

    fun getQuantity(partId: Int): Int? {
        val query = String.format(
            """
                select QuantityInStore
                from InventoriesParts
                where id = %d
            """.trimIndent(), partId
        )
        val cursor = mDatabase?.rawQuery(query, null)
        cursor?.moveToFirst()
        val value = cursor?.getInt(0)
        cursor?.close()
        return value
    }

    fun increaseQuantity(partId: Int) {
        val query = String.format(
            """ 
                update InventoriesParts
                set QuantityInStore = QuantityInStore + 1
                where id = %d
                """.trimIndent(), partId
        )
        mDatabase?.execSQL(query)
    }

    fun decreaseQuantity(partId: Int) {
        val query = String.format(
            """ 
                update InventoriesParts
                set QuantityInStore = QuantityInStore - 1
                where id = %d
                """, partId
        )
        mDatabase?.execSQL(query)
    }

    fun updateLastAccessed(inventoryId: Int) {
        val query = String.format(
            """
                update Inventories
                set LastAccessed = %d
                where id = %d 
            """.trimIndent(),
            System.currentTimeMillis(),
            inventoryId
        )
        mDatabase?.execSQL(query)
    }

    fun getArchive(inventoryId: Int): Boolean {
        val query = String.format(
            """
                select Active
                from Inventories
                where id = %d
            """.trimIndent(),
            inventoryId
        )
        val cursor = mDatabase?.rawQuery(query, null)
        cursor?.moveToFirst()
        val value = cursor?.getInt(0)
        cursor?.close()
        if (value == 1)
            return false
        return true
    }

    fun archiveInventory(inventoryId: Int, isArchived: Boolean) {
        fun Boolean.toInt() = if (this) 1 else 0
        val query = String.format(
            """
                update Inventories
                set active = %d
                where id = %d
        """.trimIndent(),
            isArchived.toInt(),
            inventoryId
        )
        mDatabase?.execSQL(query)
    }


}