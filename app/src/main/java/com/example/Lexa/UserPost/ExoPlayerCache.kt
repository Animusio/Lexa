package com.example.Lexa.utils

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object ExoPlayerCache {
    private var simpleCache: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            val cacheSize: Long = 200 * 1024 * 1024 // 200 МБ
            val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
            val databaseProvider = ExoDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    // Опционально: метод для освобождения кэша, если потребуется
    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}
