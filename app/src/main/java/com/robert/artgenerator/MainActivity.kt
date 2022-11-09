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
import com.bumptech.glide.Glide
import com.robert.artgenerator.views.PaintView.Companion.colorList
import com.robert.artgenerator.views.PaintView.Companion.currentBrush
import com.robert.artgenerator.views.PaintView.Companion.pathList
import com.robert.artgenerator.models.Result
import com.robert.artgenerator.retrofithelpers.ApiInterface
import com.robert.artgenerator.retrofithelpers.RetrofitHelper
import com.robert.artgenerator.views.PaintView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.*
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private val backgroundColor = Color.BLACK
    private val drawColor = Color.WHITE
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    private lateinit var paintView: PaintView
    private lateinit var imageView: ImageView
    private var processedImg: String? = null
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
    val imageApi = RetrofitHelper.getInstance().create(ApiInterface::class.java)

    companion object{
        var myPath = Path()
        var paintBrush = Paint()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val blackButton = findViewById<ImageButton>(R.id.blackButton)
        val redButton = findViewById<ImageButton>(R.id.redButton)
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

        redButton.setOnClickListener {
            paintBrush.color = Color.RED
            currentColor(paintBrush.color)
        }

        blackButton.setOnClickListener {
            paintBrush.color = Color.BLACK
            currentColor(paintBrush.color)
        }

        clearButton.setOnClickListener {
            pathList.clear()
            colorList.clear()
            myPath.reset()
        }
    }

    private fun currentColor(color: Int) {
        currentBrush = color
        myPath = Path()
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
                    withContext(IO){
                        processedImg = getProcessedImage(getEncodedString())
                    }
                }

            }
        }
    }

    private fun getProcessedImage(encoded: String): String? {
        val image = Result(encoded)
        var processedImage: String? = null
        val call: Call<Result> = imageApi.postImage(image)
        call.enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                processedImage = response.body()?.image
                Log.d("Output Image", processedImage!!)
                GlobalScope.launch {
                    withContext(IO){
                        loadImageOnMainThread(processedImage?.substringAfter('\'')!!)
                    }
                }

            }

            override fun onFailure(call: Call<Result>, t: Throwable) {
                Log.d("Error", t.message.toString())
                Toast.makeText(this@MainActivity, "Error:: ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })
        return processedImage
    }

    private suspend fun getEncodedString(): String {
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
        //loadImageOnMainThread(encoded)
        //Log.d("TAG", encoded)
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