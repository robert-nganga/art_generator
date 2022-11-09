package com.robert.artgenerator

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun Context.toast(text: String?) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun Context.showPermissionRequestDialog(
    title: String,
    body: String,
    callback: () -> Unit
) {
    AlertDialog.Builder(this).also {
        it.setTitle(title)
        it.setMessage(body)
        it.setPositiveButton("Ok") { _, _ ->
            callback()
        }
    }.create().show()
}