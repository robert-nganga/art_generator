package com.robert.artgenerator

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.robert.artgenerator.views.PaintView.Companion.colorList
import com.robert.artgenerator.views.PaintView.Companion.currentBrush
import com.robert.artgenerator.views.PaintView.Companion.pathList
import com.robert.artgenerator.views.PaintView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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
    //Permission Request Handler
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    val viewModel: MainActivityViewModel by viewModels()

    companion object{
        var myPath = Path()
        var paintBrush = Paint()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setPermissionCallback()

        val blackButton = findViewById<ImageButton>(R.id.blackButton)
        val redButton = findViewById<ImageButton>(R.id.redButton)
        val clearButton = findViewById<ImageButton>(R.id.clearButton)
        val saveButton = findViewById<ImageButton>(R.id.saveButton)
        paintView = findViewById(R.id.paintView)
        imageView = findViewById(R.id.imageView)

        paintView.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                handleOnTouchEvent(event!!)
                paintView.postInvalidate()
                return true
            }
        })

        viewModel.processedImage.observe(this) { img ->
            processedImg = img
            loadImage(img)
        }

        saveButton.setOnClickListener {
            if (processedImg != null) {
                checkPermissionAndSaveBitmap(processedImg!!)
                toast("Image Saved")
            }else{
                toast("No image available")
            }
        }

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
                        if(checkForInternet(this@MainActivity)){
                            viewModel.convertBitmapToString(drawBitmap())
                        }else{
                            Snackbar.make(imageView, "No internet connection", Snackbar.LENGTH_SHORT).show()
                        }

                    }
                }

            }
        }
    }

    //Allowing activity to automatically handle permission request
    private fun setPermissionCallback() {
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    processedImg?.let { getBitmapFromString(it) }
                }
            }
    }

    //function to check and request storage permission
    private fun checkPermissionAndSaveBitmap(image: String) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                getBitmapFromString(image)
            }
            shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE) -> {
                showPermissionRequestDialog(
                    getString(R.string.permission_title),
                    getString(R.string.write_permission_request)
                ) {
                    requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {
                requestPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    //the function saves the Bitmap to external storage
    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            //toast("Saved to Photos")
        }
    }

    private fun getBitmapFromString(processedImg: String) {
        GlobalScope.launch {
            withContext(IO){
                val imageBytes = Base64.decode(processedImg, 0)
                val bitmapImg = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                saveMediaToStorage(bitmapImg)
            }
        }

    }

    private fun currentColor(color: Int) {
        currentBrush = color
        myPath = Path()
    }

    private fun drawBitmap(): Bitmap{
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(paintView.width, paintView.height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        for(i in pathList.indices){
            paintBrush.setColor(colorList[i])
            extraCanvas.drawPath(pathList[i], paint)
        }
        return extraBitmap
    }

    private fun loadImage(encoded: String) {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        Glide.with(this)
            .asBitmap()
            .load(bytes)
            .into(imageView)
    }

    private fun checkForInternet(context: Context): Boolean {
        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {

            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}