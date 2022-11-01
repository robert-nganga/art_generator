package com.robert.artgenerator

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.robert.artgenerator.MainActivity.Companion.myPath
import com.robert.artgenerator.MainActivity.Companion.paintBrush
import java.io.ByteArrayOutputStream

class PaintView: View {

    var params: ViewGroup.LayoutParams? = null
    companion object{
        var pathList = ArrayList<Path>()
        var colorList = ArrayList<Int>()
        var currentBrush = Color.BLACK
    }

    constructor(context: Context) : super(context, null){
        init()
    }
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0){
        init()
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        init()
    }



    private fun init(){
        paintBrush.isAntiAlias = true
        paintBrush.color = currentBrush
        paintBrush.style = Paint.Style.STROKE
        paintBrush.strokeJoin = Paint.Join.ROUND
        paintBrush.strokeWidth = 3f

        params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        //to avoid memory leak
//        if (::extraBitmap.isInitialized) extraBitmap.recycle()
//        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        extraCanvas = Canvas(extraBitmap)
//        extraCanvas.drawColor(backgroundColor)
//    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        var x = event.x
//        var y = event.y
//        when(event.action){
//            MotionEvent.ACTION_DOWN -> {
//                myPath.moveTo(x, y)
//                return true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                myPath.lineTo(x, y)
//                pathList.add(myPath)
//                colorList.add(currentBrush)
//            }
//            MotionEvent.ACTION_UP -> {
//                for(i in pathList.indices){
//                    paintBrush.setColor(colorList[i])
//                    extraCanvas.drawPath(pathList[i], paint)
//                }
//                val byteArrayOutputStream = ByteArrayOutputStream()
//                extraBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
//                val byteArray = byteArrayOutputStream.toByteArray()
//                val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
//                Log.d("TAG", encoded)
//            }
//            else -> return false
//        }
//        postInvalidate()
//        return false
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for(i in pathList.indices){
            paintBrush.setColor(colorList[i])
            canvas.drawPath(pathList[i], paintBrush)
            invalidate()
        }
    }

}

