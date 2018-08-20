package com.tephra.mc.latestnews.data.repository

import com.mc.rxjava.demo.rxjavademo.data.remote.response.ApiPost
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.GET

/**
 * REST API endpoints
 */
interface ApiService {

    @GET("posts/")
    fun getAllPostsAsObservable(): Observable<List<ApiPost>>

    @GET("posts/")
    fun getAllPostsAsSingle(): Single<List<ApiPost>>
}