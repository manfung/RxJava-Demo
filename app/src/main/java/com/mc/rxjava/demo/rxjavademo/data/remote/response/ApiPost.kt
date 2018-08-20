package com.mc.rxjava.demo.rxjavademo.data.remote.response

import com.google.gson.annotations.SerializedName

data class ApiPost(
        @SerializedName("userId") val userId: Int,
        @SerializedName("id") val id: Int,
        @SerializedName("title") val title: String,
        @SerializedName("body") val body: String
)