package com.robert.artgenerator.retrofithelpers

import com.robert.artgenerator.models.Result
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {

    @POST("predict")
    fun postImage(@Body result: Result): Call<Result>
}