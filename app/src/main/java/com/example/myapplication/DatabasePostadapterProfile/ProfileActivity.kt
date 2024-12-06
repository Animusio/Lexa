package com.example.myapplication.DatabasePostadapterProfile

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
import com.bumptech.glide.Glide
import com.example.myapplication.MainMenu.LentaActivity
import com.example.myapplication.R
import com.example.myapplication.UserPost.User
import com.example.myapplication.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var profileAvatar: ImageView
    private lateinit var profileUsername: TextView
    private lateinit var buttonChangeAvatar: Button
    private var selectedImageUri: Uri? = null
    private var user: User? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileAvatar = findViewById(R.id.profile_avatar)
        profileUsername = findViewById(R.id.profile_username)
        buttonChangeAvatar = findViewById(R.id.button_change_avatar)

        user = intent.getParcelableExtra("user")

        profileUsername.text = user?.login ?: "No username"
        if (!user?.avatar_uri.isNullOrEmpty()) {
            Glide.with(this)
                .load("http://188.18.54.95:8000/media/images/${user!!.avatar_uri}")
                .placeholder(R.drawable.avatar1)
                .error(R.drawable.avatar1)
                .into(profileAvatar)
        } else {
            profileAvatar.setImageResource(R.drawable.avatar1)
        }

        buttonChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
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
                val response = RetrofitInstance.apiService.updateUserAvatar(userId, mapOf("avatar_uri" to mediaFileName))
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
