package com.robert.artgenerator

import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {


    @GET("predict")
    fun getImage(): Call<Result>
}