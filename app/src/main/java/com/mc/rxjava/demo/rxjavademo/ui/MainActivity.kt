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
import io.reactivex.Observable
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
    private var postsInCache = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun getPostsFromRemote(v: View) {

        compositeDisposable += apiService.getAllPostsAsSingle() // get the observable, in this instance its a Single (has only 1 response)
                .subscribeOn(Schedulers.io())               // observable will run on IO thread.
                .observeOn(AndroidSchedulers.mainThread())  // observer will run on main thread.
                .subscribe(                                 // ubscribe the observer, which runs on the thread define in 'observeOn'
                        {
                            // onSucess - Single, Maybe and Completable observables have onNext() and onComplete()
                            // combined to onSucess as the stream has only one single item (1 response) to emit
                            log(it.size.toString() + " post retrieved from remote")
                        },
                        {
                            // onError
                            log("Error retrieving posts from remote: " + it.message)
                        }
                )
    }

    fun getPostsFromRemoteAndSaveToDatabase(v: View) {

        compositeDisposable += apiService.getAllPostsAsSingle() // gets an Single observable
                .subscribeOn(Schedulers.io()) //observer will run on main thread.
                .map {
                    // convert from ApiPost to Post objects using the map operator
                    log("convert from ApiPost to Post", showToast = false)
                    convertToListOfDatabaseEntities(it)
                }
                .doOnSuccess {
                    // save to database on the 'subscribeOn' thread
                    log("Saving to database", showToast = false)
                    postsDao.insertPosts(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( // runs on the 'observeOn' thread
                        {
                            // onSuccess
                            log(it.size.toString() + " post retrieved from remote")
                        },
                        {
                            // onError
                            log("Error retrieving posts from remote: " + it.message)
                        }
                )
    }

    fun getPostsFromDb(v: View) {

        compositeDisposable += postsDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            // onNext
                            log(it.size.toString() + " post retrieved from database")
                        },
                        {
                            // onError
                            log("Error retrieving posts from db: " + it.message)
                        }
                )
    }

    fun clearDatabaseAndCache(v: View) {

        compositeDisposable += Completable // Use a Completable as we don't care about any response just weather success/error of execution
                .fromAction {
                    log("Deleting all posts in database", showToast = false)
                    postsDao.deleteAll()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                    {
                        // onComplete
                        log("Database cleared")
                    },
                    {
                        // onError
                        log("Error trying to clear database")
                    }
                )

        postsInCache.clear()
    }

    /**
    *  Use map instead of filter, in this example the Dao.getAll() is not calling onComplete otherwise
    *  see flatMapIterable and toList
    *  https://stackoverflow.com/questions/26599891/collecting-observables-to-a-list-doesnt-seem-to-emit-the-collection-at-once
    **/
    fun getFilteredPostsFromDb(v: View) {

        compositeDisposable += postsDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map {
                    log("filter posts", showToast = false)
                    it.filter { it.userId == 1 }
                }
                .subscribe(
                        {
                            // onNext
                            log(it.size.toString() + " filtered post retrieved from database")
                        },
                        {
                            // onError
                            log("Error retrieving filtered posts from db: " + it.message)
                        }
                )
    }

    fun getPostsFromRemoteAndDatabase(v: View) {
        compositeDisposable += getPostsObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            // onNext
                            log(postsInCache.size.toString() + " posts retrieved from remote and database")
                        },
                        {
                            // onError
                            log("Error retrieving posts from remote and db: " + it.message)
                        }
                )
    }

    private fun getPostsObservable():Observable<List<Post>> {

        val postsFromDB = postsDao.getAll()
                .subscribeOn(Schedulers.io())
                .doOnNext {
                    log(it.size.toString() + " posts From Database", showToast = false)
                    cacheInMemory(it)
                }

        val postsFromRemote = apiService.getAllPostsAsObservable()  // get the observable, in this instance its a Single (has only 1 response)
                .subscribeOn(Schedulers.io())                       // observable will run on IO thread.
                .map {
                    // convert from ApiPost to Post objects using the map operator
                    log("convert from ApiPost to Post", showToast = false)
                    convertToListOfDatabaseEntities(it)
                }
                .doOnNext {
                    log(it.size.toString() + " posts From Remote", showToast = false)
                    cacheInMemory(it)
                }

        return Observable.concat(postsFromRemote, postsFromDB )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }

    private fun cacheInMemory(posts: List<Post>) {
        postsInCache.addAll(posts)
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

    private fun log(msg: String, showToast: Boolean = true) {
        if (showToast) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        Log.i(TAG, msg + " - Running on thread: " + Thread.currentThread().id)
    }

}
