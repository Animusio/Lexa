package com.example.Lexa.DatabasePostadapterProfile

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.Lexa.R
import com.example.Lexa.utils.ExoPlayerCache
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.*

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var mediaUrl: String
    private lateinit var mediaType: String
    private var player: ExoPlayer? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем SimpleCache, если он еще не создан


        setContentView(R.layout.activity_media_viewer)

        // Получаем данные из Intent
        mediaUrl = intent.getStringExtra("media_url") ?: ""
        mediaType = intent.getStringExtra("media_type") ?: ""
        val backButton: ImageButton = findViewById(R.id.imageButton2)

        mediaUrl = "http://188.18.54.95:8000/media/${mediaType}/${mediaUrl}"

        val imageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val playerView = findViewById<PlayerView>(R.id.fullscreenPlayerView)

        if (mediaType == "images") {
            imageView.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            showImage(imageView)
        } else if (mediaType == "videos") {
            imageView.visibility = View.GONE
            playerView.visibility = View.VISIBLE
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showImage(imageView: ImageView) {
        Glide.with(this)
            .load(mediaUrl)
            .placeholder(R.drawable.avatar4)
            .error(R.drawable.avatar4)
            .into(imageView)
    }

    private fun initializePlayer(playerView: PlayerView) {
        if (player == null) {
            // Получаем общий экземпляр SimpleCache
            val cache = ExoPlayerCache.getInstance(this)

            // Создаем DataSource.Factory с кэшем
            val dataSourceFactory = DefaultDataSource.Factory(this)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            // Создаем MediaSourceFactory с использованием кэша
            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

            // Создаем экземпляр ExoPlayer с установленным MediaSourceFactory
            player = ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()

            playerView.player = player

            // Устанавливаем MediaItem и подготавливаем плеер
            val mediaItem = MediaItem.fromUri(mediaUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        } else {
            playerView.player = player
        }
    }


    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onStart() {
        super.onStart()
        if (mediaType == "videos") {
            val playerView = findViewById<PlayerView>(R.id.fullscreenPlayerView)
            initializePlayer(playerView)
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaType == "videos") {
            releasePlayer()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обработка изменения ориентации экрана
    }
}
