
package com.example.Lexa.network

import com.example.Lexa.UserPost.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://188.18.54.95:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)//Метод create в Retrofit используется для создания реализации интерфейса ApiService
    }
}
