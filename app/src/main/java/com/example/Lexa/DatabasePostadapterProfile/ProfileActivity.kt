package com.example.Lexa.DatabasePostadapterProfile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Lexa.R
import com.example.Lexa.UserPost.Post
import com.example.Lexa.UserPost.User
import com.example.Lexa.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ProfileActivity : AppCompatActivity() {
    private lateinit var profileAvatar: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var lentaList: RecyclerView // Добавляем RecyclerView
    private var selectedImageUri: Uri? = null
    private var user: User? = null
    private val PICK_IMAGE_REQUEST = 1001
    private var userId: Int = -1
    private var postUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileAvatar = findViewById(R.id.profile_avatar)
        profileUsername = findViewById(R.id.profile_username)
        lentaList = findViewById(R.id.lentaList) // Инициализируем RecyclerView


        // Добавляем кнопку назад
        val backButton: Button = findViewById(R.id.button_back)
        backButton.setOnClickListener {
            finish()
        }

        user = intent.getParcelableExtra("user")
        postUsername = intent.getStringExtra("username") ?: "Неизвестный пользователь"
        profileUsername.text = user?.login ?: "No username"
        userId = intent.getIntExtra("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Ошибка: ID пользователя не передан", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Настраиваем RecyclerView
        setupRecyclerView()

        // Загружаем данные пользователя и посты
        lifecycleScope.launch {
            loadUserProfile(userId)
            loadUserPosts(userId) // Добавляем загрузку постов
        }

        if (user?.id == userId) {
            // Это профиль авторизованного пользователя
            profileAvatar.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }
        } else {
            // Это профиль другого пользователя
            profileAvatar.setOnClickListener {
                Toast.makeText(this, "Нанесён удар по ${postUsername}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Настройка RecyclerView
    private fun setupRecyclerView() {
        val postsAdapter = PostsAdapter(this, lifecycleScope, user!!) // Предполагаем, что PostsAdapter существует
        val layoutManager = LinearLayoutManager(this)
        lentaList.layoutManager = LinearLayoutManager(this)
        lentaList.adapter = postsAdapter

        val dividerItemDecoration = DividerItemDecoration(
            lentaList.context,
            layoutManager.orientation
        )
        lentaList.addItemDecoration(dividerItemDecoration)
    }

    // Загрузка постов пользователя
    private suspend fun loadUserPosts(userId: Int) {
        try {
            val response = RetrofitInstance.apiService.getPostsByUser(userId) // Предполагаем, что метод существует
            if (response.isSuccessful) {
                val posts = response.body() ?: emptyList()
                withContext(Dispatchers.Main) {
                    (lentaList.adapter as PostsAdapter).submitList(posts) // Обновляем адаптер
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProfileActivity, "Ошибка загрузки постов: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                profileAvatar.setImageURI(uri)  // Предварительный просмотр нового аватара
                uploadAvatar(uri) // Автоматически загружаем на сервер
            }
        }
    }

    private suspend fun loadUserProfile(userId: Int) {
        try {
            val user = RetrofitInstance.apiService.getUserById(userId)
            withContext(Dispatchers.Main) {
                profileUsername.text = user.login
                val avatarUrl = if (!user.avatar_uri.isNullOrEmpty()) {
                    "http://188.18.54.95:8000/media/images/${user.avatar_uri}"
                } else {
                    null
                }
                Glide.with(this@ProfileActivity)
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar3)
                    .error(R.drawable.avatar3)
                    .into(profileAvatar)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                //Toast.makeText(this@ProfileActivity, "Ошибка загрузки профиля: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val contentResolver = contentResolver
        val mimeType = contentResolver.getType(uri)
        if (mimeType == null) {
            Toast.makeText(this, "Неизвестный тип файла", Toast.LENGTH_SHORT).show()
            return
        }
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Toast.makeText(this, "Не удалось открыть выбранный файл", Toast.LENGTH_SHORT).show()
            return
        }
        val fileBytes = inputStream.readBytes()
        val requestBody = RequestBody.create(mimeType.toMediaTypeOrNull(), fileBytes)
        val multipartBody = MultipartBody.Part.createFormData("file", "avatar_${user?.id}.jpg", requestBody)
        // Сначала загружаем файл
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uploadResponse = RetrofitInstance.apiService.uploadMedia(multipartBody)
                withContext(Dispatchers.Main) {
                    if (uploadResponse.isSuccessful) {
                        val mediaFileName = uploadResponse.body()?.filename
                        if (mediaFileName != null && user?.id != null) {
                            // После успешной загрузки файла - обновляем avatar_uri пользователя на сервере
                            updateUserAvatarOnServer(user!!.id, mediaFileName)
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "Ошибка при загрузке аватара", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileActivity", "Ошибка при загрузке аватара", e)
                }
            }
        }
    }

    private fun updateUserAvatarOnServer(userId: Int, mediaFileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.apiService.updateUserAvatar(
                    userId,
                    mapOf("avatar_uri" to mediaFileName)
                )
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        // Обновляем локальную модель user
                        user = response
                        // Обновляем отображение аватара
                        Glide.with(this@ProfileActivity)
                            .load("http://188.18.54.95:8000/media/images/$mediaFileName")
                            .placeholder(R.drawable.avatar1)
                            .error(R.drawable.avatar1)
                            .into(profileAvatar)
                        Toast.makeText(this@ProfileActivity, "Аватар успешно обновлен!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Ошибка при обновлении аватара", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Ошибка при обновлении на сервере: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ProfileActivity", "Ошибка при обновлении аватара на сервере", e)
                }
            }
        }
    }
}