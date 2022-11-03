package com.robert.artgenerator

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiInterface {


    @GET("predict")
    fun getImage(): Call<Result>

    @POST("predict")
    fun postImage(@Body result: Result): Call<Result>
}