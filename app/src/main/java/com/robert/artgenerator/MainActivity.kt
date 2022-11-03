package com.robert.artgenerator

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.robert.artgenerator.PaintView.Companion.colorList
import com.robert.artgenerator.PaintView.Companion.pathList
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.create
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private val backgroundColor = Color.BLACK
    private val drawColor = Color.WHITE
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    private lateinit var paintView: PaintView
    private lateinit var imageView: ImageView
    val imageApi = RetrofitHelper.getInstance().create(ApiInterface::class.java)
    private val paint = Paint().apply {
        color = drawColor
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }

    companion object{
        var myPath = Path()
        var paintBrush = Paint()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val blackButton = findViewById<ImageButton>(R.id.blackButton)
        val clearButton = findViewById<ImageButton>(R.id.clearButton)
        paintView = findViewById(R.id.paintView)
        imageView = findViewById(R.id.imageView)

        paintView.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                handleOnTouchEvent(event!!)
                paintView.postInvalidate()
                return true
            }
        })

        blackButton.setOnClickListener {
            Toast.makeText(this, "Black Button Clicked", Toast.LENGTH_SHORT).show()
        }

        clearButton.setOnClickListener {
            Toast.makeText(this, "Clear Button Clicked", Toast.LENGTH_SHORT).show()
            pathList.clear()
            colorList.clear()
            myPath.reset()
        }
    }


    private fun handleOnTouchEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                myPath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                myPath.lineTo(x, y)
                pathList.add(myPath)
                colorList.add(PaintView.currentBrush)
            }
            MotionEvent.ACTION_UP -> {
                GlobalScope.launch {
                    withContext(Default){
                        getImage(onTouchUp())
                    }
                }

            }
        }
    }

    private fun getImage(encoded: String) {

    }

    private suspend fun onTouchUp(): String {
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(paintView.width, paintView.height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        for(i in pathList.indices){
            paintBrush.setColor(colorList[i])
            extraCanvas.drawPath(pathList[i], paint)
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        extraBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
        loadImageOnMainThread(encoded)
        Log.d("TAG", encoded)
        return encoded

        //print(encoded)
    }

    private suspend fun loadImageOnMainThread(encoded: String) {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        withContext(Main){
            loadImage(bytes)
        }
    }

    private fun loadImage(bytes: ByteArray) {
        Glide.with(this)
            .asBitmap()
            .load(bytes)
            .into(imageView)
    }
}