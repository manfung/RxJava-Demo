package com.mc.rxjava.demo.rxjavademo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mc.rxjava.demo.rxjavademo.data.model.Post
import io.reactivex.Observable

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPosts(posts: List<Post>)

    @Query("SELECT * from Post")
    fun getAll(): Observable<List<Post>>

    @Query("DELETE from Post")
    fun deleteAll()

}