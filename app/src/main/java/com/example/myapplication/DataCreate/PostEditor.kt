package com.example.myapplication.DataCreate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainMenu.LentaActivity
import com.example.myapplication.R
import com.example.myapplication.UserPost.Post
import com.example.myapplication.UserPost.PostRepository
import com.example.myapplication.UserPost.User
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
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_editor)

        val postEditText: EditText = findViewById(R.id.editTextPost)
        val publishButton: Button = findViewById(R.id.buttonEditPost)
        val imageButton: ImageButton = findViewById(R.id.MediaButton)
        val user = intent.getParcelableExtra<User>("user")

        imageView = findViewById(R.id.imageView4)
        videoView = findViewById(R.id.videoView2)
        videoView.visibility = VideoView.GONE
        imageView.visibility = ImageView.GONE

        // Обработчик выбора медиафайла
        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/* video/*"
            }
            selectMediaLauncher.launch(intent)
        }

        selectMediaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedMediaUri = result.data?.data
                selectedMediaUri?.let { uri ->
                    val mimeType = contentResolver.getType(uri)
                    if (mimeType?.startsWith("image/") == true) {
                        imageView.setImageURI(uri)
                        imageView.visibility = ImageView.VISIBLE
                        videoView.visibility = VideoView.GONE
                    } else if (mimeType?.startsWith("video/") == true) {
                        videoView.setVideoURI(uri)
                        videoView.start()
                        videoView.visibility = VideoView.VISIBLE
                        imageView.visibility = ImageView.GONE
                    }
                }
            }
        }

        publishButton.setOnClickListener {
            val postText = postEditText.text.toString().trim()
            if (postText.isNotEmpty() || selectedMediaUri != null) {
                val newPost = Post(0, "avatar1", user?.login ?: "Неизвестный пользователь", postText, 0)

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

                                val response = postRepository.uploadMedia(multipartBody) // Запрос на сервер для загрузки
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
                        Toast.makeText(this@PostEditor, createdPost.id.toString(), Toast.LENGTH_SHORT).show()
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
}
