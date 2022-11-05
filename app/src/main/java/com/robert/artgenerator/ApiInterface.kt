package com.robert.artgenerator

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiInterface {

    @POST("predict")
    fun postImage(@Body result: Result): Call<Result>
}