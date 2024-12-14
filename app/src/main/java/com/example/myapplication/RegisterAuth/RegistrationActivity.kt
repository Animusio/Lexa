package com.example.myapplication.RegisterAuth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.myapplication.MainMenu.LentaActivity
import com.example.myapplication.R
import com.example.myapplication.UserPost.User
import com.example.myapplication.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPrefsFile = "encrypted_prefs"
        val masterKeyAlias = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = try {
            EncryptedSharedPreferences.create(
                this,
                sharedPrefsFile,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("EncryptedSharedPrefs", "Ошибка при инициализации EncryptedSharedPreferences", e)
            // Удаляем повреждённые данные
            deleteSharedPreferences(sharedPrefsFile)
            // Повторяем инициализацию
            EncryptedSharedPreferences.create(
                this,
                sharedPrefsFile,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        val savedIdw = sharedPreferences.getInt("id", 0)
        Log.d("ProfileActivity", "loggedInUserId: $savedIdw")
        val savedUsername = sharedPreferences.getString("username", null)
        val savedPassword = sharedPreferences.getString("password", null)

        if (savedUsername != null && savedPassword != null) {
            // Пользователь уже авторизован, переходим на главную активность
            val intent = Intent(this, LentaActivity::class.java)
            val user = User(
                id = savedIdw,              // Используйте 0, если `id` пока неизвестен
                login = savedUsername,
                password = savedPassword,
                avatar_uri = null    // Можно передать null, если аватар пока неизвестен
            )
            intent.putExtra("user", user)
            startActivity(intent)
            finish()  // Закрыть текущую активность
        }

        val userLogin: EditText = findViewById(R.id.nameText)
        val userPass: EditText = findViewById(R.id.passText)
        val userDate: EditText = findViewById(R.id.birthText)
        val buttonReg: Button = findViewById(R.id.button_reg)
        val linkToReg2: TextView = findViewById(R.id.textViewLink2)

        linkToReg2.setOnClickListener {
            Toast.makeText(this, "$savedUsername $savedPassword", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonReg.setOnClickListener {
            // Получаем текст из полей и назначаем его переменным
            val login = userLogin.text.toString().trim()
            val pass = userPass.text.toString().trim()
            val date = userDate.text.toString().trim()

            if (login.isEmpty() || pass.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            } else if (pass.length < 3) {
                Toast.makeText(this, "Пароль должен содержать не менее 3 символов", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    try {
                        var user = RetrofitInstance.apiService.createUser(User(
                            id = 228,              // Используйте 0, если `id` пока неизвестен
                            login = login,
                            password = pass,
                            avatar_uri = null    // Можно передать null, если аватар пока неизвестен
                        ))
                        // Сохраняем данные пользователя в EncryptedSharedPreferences
                        withContext(Dispatchers.IO) {
                            sharedPreferences.edit().apply {
                                putInt("id", RetrofitInstance.apiService.getUserByLogin(user).id)
                                putString("username", login)
                                putString("password", pass)
                                apply()
                            }
                        }
                        // Переходим обратно в основной поток для обновления UI
                        withContext(Dispatchers.Main) {
                            userLogin.text.clear()
                            userPass.text.clear()
                            userDate.text.clear()
                            Toast.makeText(
                                this@RegistrationActivity,
                                "Регистрация прошла успешно ($login) 😊",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@RegistrationActivity, LentaActivity::class.java)

                            intent.putExtra("user", user)
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        // Обрабатываем ошибки
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@RegistrationActivity,
                                "Ошибка при регистрации ❌",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("RegistrationError", "Ошибка при регистрации", e)
                        }
                    }
                }
            }
        }
    }
}
