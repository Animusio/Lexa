package com.example.Lexa.DatabasePostadapterProfile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Lexa.R
import com.example.Lexa.UserPost.Post
import com.example.Lexa.UserPost.PostRepository
import com.example.Lexa.UserPost.User
import com.example.Lexa.network.RetrofitInstance
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.Lexa.utils.ExoPlayerCache
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class PostsAdapter(
    private val context: Context,
    private val lifecycleScope: CoroutineScope,
    private val user: User
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    private val postRepository = PostRepository()
    private val playerMap = mutableMapOf<Int, ExoPlayer>()
    private var currentlyPlayingPostId: Int? = null  // ID текущего воспроизводимого поста



    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.post_list_avatar)
        val username: TextView = view.findViewById(R.id.post_list_nickname)
        val postText: TextView = view.findViewById(R.id.post_list_post)
        val postLike: TextView = view.findViewById(R.id.post_list_like)
        val postImageView: ImageView = view.findViewById(R.id.imageView3)
        val imageButton: ImageButton = view.findViewById(R.id.imageButton)
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val mediaContainer: FrameLayout = view.findViewById(R.id.mediaContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_in_list, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        val postId = post.id


        val postuserId = post.user_id
        Log.e("poch", "post user id = $postuserId")
        Log.e("poch", "user id = ${user.id}")


        // Сброс медиа-контейнера и прочего
        holder.mediaContainer.visibility = View.GONE
        holder.postImageView.visibility = View.GONE
        holder.playerView.visibility = View.GONE

        // Показать или скрыть ImageButton в зависимости от владельца поста
        holder.imageButton.visibility = if (post.nickname == user.login) View.VISIBLE else View.GONE

        // Отсоединяем предыдущий плеер от PlayerView
        holder.playerView.player = null

        // Установка текстовых полей
        holder.username.text = post.nickname
        holder.postText.text = post.post
        holder.postLike.text = "💜${post.likes_count}"

        // Загрузка аватара внутри onBindViewHolder:
        lifecycleScope.launch {
            try {
                val author = RetrofitInstance.apiService.getUserById(post.user_id)
                val avatarUrl = if (!author.avatar_uri.isNullOrEmpty()) {
                    "http://188.18.54.95:8000/media/images/${author.avatar_uri}"
                } else {
                    null
                }

                withContext(Dispatchers.Main) {
                    Glide.with(holder.itemView)
                        .load(avatarUrl)
                        .placeholder(R.drawable.avatar3)
                        .error(R.drawable.avatar3)
                        .into(holder.avatar)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Glide.with(holder.itemView)
                        .load(R.drawable.avatar3)
                        .into(holder.avatar)
                }
            }
        }

        // Загрузка медиа контента поста (изображение/видео)
        val mimeType = getMimeType(post.media_url ?: "")
        if (!post.media_url.isNullOrEmpty() && mimeType != null) {
            holder.mediaContainer.visibility = View.VISIBLE

            if (mimeType.startsWith("image")) {
                holder.postImageView.visibility = View.VISIBLE
                val imageUrl = "http://188.18.54.95:8000/media/images/${post.media_url}"
                Glide.with(holder.itemView)
                    .load(imageUrl)
                    .placeholder(R.drawable.avatar3)
                    .error(R.drawable.avatar3)
                    .into(holder.postImageView)
            } else if (mimeType.startsWith("video")) {
                holder.playerView.visibility = View.VISIBLE
                var player = playerMap[postId]
                if (player == null) {
                    val cache = ExoPlayerCache.getInstance(context)
                    val dataSourceFactory = DefaultDataSource.Factory(context)
                    val cacheDataSourceFactory = CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(dataSourceFactory)
                        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

                    val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
                    player = ExoPlayer.Builder(context)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build()

                    val videoUrl = "http://188.18.54.95:8000/media/videos/${post.media_url}"
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = false

                    playerMap[postId] = player
                }
                holder.playerView.player = player
            }
        }

        // Обработка лайка
        holder.postLike.setOnClickListener {
            holder.postLike.alpha = 0.5f
            lifecycleScope.launch {
                val updatedPost = postRepository.likePost(post.id, true)
                if (updatedPost != null) {
                    holder.postLike.text = "❤\uFE0F${updatedPost.likes_count}"
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "Ошибка при обновлении лайка",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                holder.postLike.alpha = 1f
            }
        }

        // Переход к профилю при клике на аватар
        holder.avatar.setOnClickListener {
            val username = post.nickname
            val postLike = post.post
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra("avatar", R.drawable.avatar1)
                putExtra("username", username)
                putExtra("postLike", postLike)
                putExtra("user", user)
                putExtra("user_id", post.user_id)
            }
            context.startActivity(intent)
        }

        // Переход к MediaViewerActivity при клике на изображение
        holder.postImageView.setOnClickListener {
            val intent = Intent(context, MediaViewerActivity::class.java).apply {
                putExtra("media_url", post.media_url)
                putExtra("media_type", "images")
                putExtra("user", user)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
        }

        // Переход к MediaViewerActivity при клике на видео
        holder.playerView.setOnClickListener {
            val intent = Intent(context, MediaViewerActivity::class.java).apply {
                putExtra("media_url", post.media_url)
                putExtra("media_type", "videos")
                putExtra("user", user)
            }
            stopPlaying()
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
        }

        // Обработка нажатия на imageButton (popup-меню)
        holder.imageButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view)
            popupMenu.menuInflater.inflate(R.menu.post_options_menu, popupMenu.menu)
            popupMenu.menu.findItem(R.id.menu_delete).isVisible = post.nickname == user.login
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        deletePost(post)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }



    fun stopPlaying() {
        currentlyPlayingPostId?.let { postId ->
            playerMap[postId]?.playWhenReady = false
        }
    }

    /**
     * Метод для воспроизведения видимого видео
     */
    fun playVisibleVideo(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        //Получает видимый диапазон элементов в RecyclerView
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        var found = false

        //Циклично проверяет каждый элемент в этом диапазоне
        for (position in firstVisiblePosition..lastVisiblePosition) {
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as? PostViewHolder ?: continue
            val player = holder.playerView.player ?: continue
            //используя метод isViewFullyVisible, чтобы определить, находится ли видео в полной видимости
            if (isViewFullyVisible(holder.playerView)) {
                if (currentlyPlayingPostId != getItem(position).id) {
                    // Останавливаем предыдущее воспроизведение
                    currentlyPlayingPostId?.let { prevPostId ->
                        playerMap[prevPostId]?.playWhenReady = false

                    }

                    // Запускаем новое воспроизведение
                    player.playWhenReady = true
                    currentlyPlayingPostId = getItem(position).id

                }
                found = true
                break  // Воспроизведение только одного видео одновременно
            }
        }

        if (!found) {
            // Если ни одно видео не видно, останавливаем текущее воспроизведение
            currentlyPlayingPostId?.let { prevPostId ->
                playerMap[prevPostId]?.playWhenReady = false

                currentlyPlayingPostId = null
            }
        }
    }


    /**
     * Проверяет, полностью ли виден View на экране
     */
    private fun isViewFullyVisible(view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewTop = location[1]
        val viewBottom = viewTop + view.height
        val screenHeight = view.context.resources.displayMetrics.heightPixels

        return viewTop >= 0 && viewBottom <= screenHeight
    }

    /**
     * Удаляет пост из списка и обновляет адаптер
     */
    private fun deletePost(post: Post) {
        lifecycleScope.launch {
            val success = postRepository.deletePost(post.id)
            val message = if (success) "Пост удалён" else "Не удалось удалить пост"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (success) {
                val currentList = currentList.toMutableList()
                currentList.remove(post)
                submitList(currentList)
            }
        }
    }

    /**
     * Останавливает воспроизведение видео при отсоединении ViewHolder от окна
     */
    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.playerView.player?.playWhenReady = false
    }

    /**
     * Отсоединяет плеер от PlayerView при переработке ViewHolder
     */
    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.playerView.player?.playWhenReady = false
        holder.playerView.player = null
    }

    /**
     * Освобождает все ExoPlayer при уничтожении адаптера
     */
    fun releaseAllPlayers() {
        for (player in playerMap.values) {
            player.release()
        }
        playerMap.clear()
    }

    /**
     * Получает MIME-тип файла по его URL
     */
    private fun getMimeType(url: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    /**
     * Класс для сравнения элементов списка
     */
    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

}
