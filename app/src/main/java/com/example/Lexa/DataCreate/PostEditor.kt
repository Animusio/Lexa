package com.example.Lexa.DataCreate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.Lexa.MainMenu.LentaActivity
import com.example.Lexa.R
import com.example.Lexa.UserPost.Post
import com.example.Lexa.UserPost.PostRepository
import com.example.Lexa.UserPost.User
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.InputStream

class PostEditor : AppCompatActivity() {

    private val postRepository = PostRepository()
    private lateinit var selectMediaLauncher: ActivityResultLauncher<Intent>
    private var selectedMediaUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var playerView: PlayerView
    private lateinit var removeMediaButton: ImageButton
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_editor)

        val postEditText: EditText = findViewById(R.id.editTextPost)
        val publishButton: Button = findViewById(R.id.buttonEditPost)
        val imageButton: ImageButton = findViewById(R.id.MediaButton)
        val user = intent.getParcelableExtra<User>("user")

        imageView = findViewById(R.id.imageView4)
        playerView = findViewById(R.id.playerView)  // Используем правильный идентификатор
        removeMediaButton = findViewById(R.id.removeMediaButton)

        // Изначально скрываем элементы медиа и кнопку удаления
        imageView.visibility = View.GONE
        playerView.visibility = View.GONE
        removeMediaButton.visibility = View.GONE

        // Инициализируем ExoPlayer
        initializePlayer()

        // Инициализируем selectMediaLauncher
        selectMediaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedMediaUri = result.data?.data
                selectedMediaUri?.let { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType?.startsWith("image/") == true) {
                        imageView.setImageURI(uri)
                        imageView.visibility = View.VISIBLE
                        playerView.visibility = View.GONE
                        // Останавливаем плеер, если видео было воспроизведено
                        exoPlayer?.stop()
                    } else if (mimeType?.startsWith("video/") == true) {
                        imageView.visibility = View.GONE
                        playerView.visibility = View.VISIBLE
                        playVideo(uri)
                    }
                    // Показываем кнопку удаления после выбора медиа
                    removeMediaButton.visibility = View.VISIBLE
                }
            }
        }

        // Обработчик выбора медиафайла
        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "*/*"
                val mimeTypes = arrayOf("image/*", "video/*")
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            selectMediaLauncher.launch(intent)
        }

        // Обработчик нажатия на кнопку удаления медиа
        removeMediaButton.setOnClickListener {
            // Удаляем выбранное медиа
            selectedMediaUri = null
            imageView.visibility = View.GONE
            playerView.visibility = View.GONE
            exoPlayer?.stop()
            removeMediaButton.visibility = View.GONE
        }

        publishButton.setOnClickListener {
            val postText = postEditText.text.toString().trim()
            if (postText.isNotEmpty() || selectedMediaUri != null) {
                val newPost = Post(
                    0,
                    user_id = user?.id ?: 0,
                    user?.login ?: "Неизвестный пользователь",
                    post = postText,
                    likes_count = 0
                )

                lifecycleScope.launch {
                    if (selectedMediaUri != null) {
                        val uri = selectedMediaUri!!
                        val inputStream: InputStream? = contentResolver.openInputStream(uri)
                        val fileName = run {
                            var fileName = "default_name"
                            val cursor = contentResolver.query(uri, null, null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    fileName = it.getString(nameIndex)
                                }
                            }
                            fileName
                        }
                        if (inputStream != null) {
                            val requestBody = RequestBody.create(
                                contentResolver.getType(uri)?.toMediaTypeOrNull(),
                                inputStream.readBytes()
                            )
                            val multipartBody = MultipartBody.Part.createFormData("file", fileName, requestBody)

                            try {
                                val response = postRepository.uploadMedia(multipartBody)
                                if (response.isSuccessful) {
                                    val mediaFileName = response.body()?.filename
                                    newPost.media_url = mediaFileName
                                } else {
                                    Toast.makeText(this@PostEditor, "Ошибка при загрузке медиа", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@PostEditor, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@PostEditor, "Не удалось получить путь к медиафайлу", Toast.LENGTH_SHORT).show()
                        }
                    }

                    val createdPost = postRepository.createPost(newPost)
                    if (createdPost != null) {
                        val intent = Intent(this@PostEditor, LentaActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        Toast.makeText(this@PostEditor, "Пост опубликован!", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@PostEditor, createdPost.user_id.toString(), Toast.LENGTH_SHORT).show()

                        if (createdPost == null) {
                            Log.d("PostDetails", "Post object is null")
                        } else {
                            Log.d("PostDetails", "Post Details: id=${createdPost.id}, nickname=${createdPost.nickname}, post=${createdPost.post}, likes_count=${createdPost.likes_count}, media_url=${createdPost.media_url}, user_id=${createdPost.user_id}")
                        }

                        intent.putExtra("newPost", createdPost)
                        intent.putExtra("user", user)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@PostEditor, "Ошибка при создании поста", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Введите текст поста или выберите медиафайл", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                super.onPlayerError(error)
                Toast.makeText(this@PostEditor, "Ошибка воспроизведения видео: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("PostEditor", "Error playing video: ${error.message}", error)
            }
        })
    }

    private fun playVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
