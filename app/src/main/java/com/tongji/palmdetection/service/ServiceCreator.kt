package com.tongji.palmdetection.service

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ServiceCreator {
    private const val BASE_URL = "http://192.168.3.6:5000"

    private val okHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)
//    network中可以直接用下面的方法创建
//    val userService = ServiceCreator.create(UserService:: class.java)
//    调用class.java方法 获得类
//    也可以用下方的inline函数 val userService = ServiceCreator.create<UserService>()
//    inline fun <reified T> create(): T = create(T::class.java)
}

