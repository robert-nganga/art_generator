package com.robert.artgenerator

import android.graphics.Paint
import android.graphics.Path
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import com.robert.artgenerator.PaintView.Companion.colorList
import com.robert.artgenerator.PaintView.Companion.pathList

class MainActivity : AppCompatActivity() {

    companion object{
        var path = Path()
        var paintBrush = Paint()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val blackButton = findViewById<ImageButton>(R.id.blackButton)
        val clearButton = findViewById<ImageButton>(R.id.clearButton)

        blackButton.setOnClickListener {
            Toast.makeText(this, "Black Button Clicked", Toast.LENGTH_SHORT).show()
        }

        clearButton.setOnClickListener {
            Toast.makeText(this, "Clear Button Clicked", Toast.LENGTH_SHORT).show()
            pathList.clear()
            colorList.clear()
            path.reset()
        }
    }
}