package com.tongji.palmdetection.service

import com.tongji.palmdetection.model.DetectResponse
import com.tongji.palmdetection.model.RegisterResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


interface UserService {

    @POST("register")
    fun register(@Body requestBody: RequestBody): Call<RegisterResponse>


    @POST("detect")
    fun detect(@Body requestBody: RequestBody): Call<DetectResponse>

}

