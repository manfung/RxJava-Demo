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

        compositeDisposable += apiService.getAllPostsAsSingle() // get the observable, in this instance its a Single (has only 1 response)
                .subscribeOn(Schedulers.io())               // observable will run on IO thread.
                .observeOn(AndroidSchedulers.mainThread())  // observer will run on main thread.
                .subscribe(                                 // ubscribe the observer, which runs on the thread define in 'observeOn'
                        { posts ->
                            // onSucess - Single, Maybe and Completable observables have onNext() and onComplete()
                            // combined to onSucess as the stream has only one single item (1 response) to emit
                            log(posts.size.toString() + " post retrieved from remote")
                        },
                        { error ->
                            // onError
                            log("Error retrieving posts from remote: " + error.message)
                        }
                )
    }

    fun getPostsFromRemoteAndSaveToDatabase(v: View) {

        compositeDisposable += apiService.getAllPostsAsObservable() // gets an Observable object
                .subscribeOn(Schedulers.io()) //observer will run on main thread.
                .map {
                    // convert from ApiPost to Post objects using the map operator
                    posts ->
                        log("convert from ApiPost to Post", showToast = false)
                        convertToListOfDatabaseEntities(posts)
                }
                .doOnNext {
                    // save to database on the 'subscribeOn' thread
                    posts ->
                        log("Saving to database", showToast = false)
                        postsDao.insertPosts(posts)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( // runs on the 'observeOn' thread
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
                log("Deleting all posts in database", showToast = false)
                postsDao.deleteAll()}
             .subscribeOn(Schedulers.io())
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe {
                log("Database cleared")
             }
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

    private fun log(msg: String, showToast: Boolean = true) {
        if (showToast) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        Log.i(TAG, msg + " - Running on thread:" + Thread.currentThread().id)
    }

}
