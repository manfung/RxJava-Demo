package com.mc.rxjava.demo.rxjavademo.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mc.rxjava.demo.rxjavademo.R
import com.mc.rxjava.demo.rxjavademo.data.db.AppDatabase
import com.mc.rxjava.demo.rxjavademo.data.model.Post
import com.mc.rxjava.demo.rxjavademo.data.remote.response.ApiPost
import com.mc.rxjava.demo.rxjavademo.utils.plusAssign
import com.tephra.mc.latestnews.data.repository.ApiService
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    companion object {
        @JvmField val TAG: String = MainActivity::class.java.simpleName // avoid reflection and no need to import the kotlin-reflect.jar
    }

    private val apiService by lazy {

        Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService::class.java)

    }

    private val postsDao by lazy {
        AppDatabase.getInstance(applicationContext).postDao()
    }

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun getPostsFromRemote(v: View) {

        compositeDisposable += apiService.getAllPostsAsSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { posts ->
                            // onNext
                            log(posts.size.toString() + " post retrieved from remote")
                        },
                        { error ->
                            // onError
                            log("Error retrieving posts from remote: " + error.message)
                        }
                )
    }

    fun getPostsFromRemoteAndSaveToDatabase(v: View) {

        compositeDisposable += apiService.getAllPostsAsObservable()
                .subscribeOn(Schedulers.io())
                .map { posts ->  convertToListOfDatabaseEntities(posts) } // convert from ApiPost to Post objects
                .doOnNext {  posts ->  postsDao.insertPosts(posts)} // save to database
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { posts ->
                            // onNext
                            log(posts.size.toString() + " post retrieved from remote")
                        },
                        { error ->
                            // onError
                            log("Error retrieving posts from remote: " + error.message)
                        },
                        {
                            // omComplete
                            log("remote call complete")
                        }
                )
    }

    private fun convertToListOfDatabaseEntities(posts:List<ApiPost>):List<Post> {

        return posts.map { convertToDatabaseEntity(it) }
    }


    private fun convertToDatabaseEntity(post:ApiPost):Post {

        return Post(userId = post.userId,
                id = post.id,
                title = post.title,
                body = post.body
            )
    }

    override fun onPause() {
        super.onPause()
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }

    fun getPostsFromDb(v: View) {

        compositeDisposable += postsDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { posts ->
                            // onNext
                            log(posts.size.toString() + " post retrieved from database")
                        },
                        { error ->
                            // onError
                            log("Error retrieving posts from db: " + error.message)
                        }
                )
    }

    fun clearDb(v: View) {

        compositeDisposable += Completable.fromAction {
                postsDao.deleteAll()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                log("Database cleared")
            }

    }

    private fun log(msg: String) {
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
        Log.i(TAG,msg)
    }

}
