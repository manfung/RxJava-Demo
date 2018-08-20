package com.mc.rxjava.demo.rxjavademo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mc.rxjava.demo.rxjavademo.data.model.Post

@Database(entities = [Post::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao

    companion object {

        /**
         * The only instance
         */
        private var sInstance: AppDatabase? = null

        /**
         * Gets the singleton instance of SampleDatabase.
         *
         * @param context The context.
         * @return The singleton instance of SampleDatabase.
         */
        @Synchronized
        fun getInstance(context: Context): AppDatabase {
            if (sInstance == null) {
                sInstance = Room
                        .databaseBuilder(context, AppDatabase::class.java, "database.db")
                        .fallbackToDestructiveMigration()
                        .build()
            }
            return sInstance!!
        }
    }

}