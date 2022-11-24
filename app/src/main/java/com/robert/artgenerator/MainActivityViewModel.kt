package com.robert.artgenerator

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robert.artgenerator.models.Result
import com.robert.artgenerator.retrofithelpers.ApiInterface
import com.robert.artgenerator.retrofithelpers.RetrofitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class MainActivityViewModel: ViewModel() {
    private val _imageApi = RetrofitHelper.getInstance().create(ApiInterface::class.java)

    private var _processedImg = MutableLiveData<String>()
    val processedImage: LiveData<String>
       get() = _processedImg

    //Getting the encoded string of the processed image
    private fun getProcessedImage(encoded: String) {
        val image = Result(encoded)
        val call: Call<Result> = _imageApi.postImage(image)
        call.enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                val imgString = response.body()?.image
                _processedImg.value = imgString?.substringAfter('\'')
                Log.d("MainActivityViewModel", _processedImg.value!!)

            }

            override fun onFailure(call: Call<Result>, t: Throwable) {
                Log.d("Error", t.message.toString())
            }

        })
    }

    //Converting bitmap to a base64 string
    fun convertBitmapToString(extraBitmap: Bitmap){

        val byteArrayOutputStream = ByteArrayOutputStream()
        extraBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)

        viewModelScope.launch {
            withContext(Dispatchers.IO){
                getProcessedImage(encoded)
            }
        }
    }

}