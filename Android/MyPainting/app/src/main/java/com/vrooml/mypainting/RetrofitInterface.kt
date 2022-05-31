package com.vrooml.mypainting

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.io.File


interface RetrofitInterface {
    @Multipart
    @POST("/processPicture")
    fun processPicture(
        @Header("style") style: String,
        @Part image: MultipartBody.Part?
    ): Call<ResponseBody>
}