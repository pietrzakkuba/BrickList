package com.kubadesing.bricklist

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment

class SimpleDialog(private val title: String, private val message: String) :
    AppCompatDialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val builder = AlertDialog.Builder(this.requireActivity())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { _: DialogInterface, _: Int ->

            }
        return builder.create()

    }
}