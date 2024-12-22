package com.example.Lexa.RegisterAuth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.Lexa.R
import com.example.Lexa.MainMenu.LentaActivity
import com.example.Lexa.UserPost.User
import com.example.Lexa.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val masterKeyAlias = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        // Инициализация EncryptedSharedPreferences
        val encryptedSharedPreferences = EncryptedSharedPreferences.create(
            this,
            "encrypted_prefs",  // Название файла SharedPreferences
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val userLogin: EditText = findViewById(R.id.nameText_auth)
        val userPass: EditText = findViewById(R.id.passText_auth)
        val button_auth: Button = findViewById(R.id.button_auth)
        val linkToReg: TextView = findViewById(R.id.textViewLink)

        button_auth.setOnClickListener{
            val login = userLogin.text.toString().trim()
            val pass = userPass.text.toString().trim()


            if(login == "" || pass==""){
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
            else{
                lifecycleScope.launch {
                    try {
                        val temporaryUser = User(
                            id = 123,  // Установите временное значение, например, 0
                            login = login,
                            password = pass,
                            avatar_uri = null  // Можно передать null, если аватар пока неизвестен
                        )
                        var user = RetrofitInstance.apiService.getUserByLogin(temporaryUser)
                        withContext(Dispatchers.Main) {
                            userLogin.text.clear()
                            userPass.text.clear()

                            val editor = encryptedSharedPreferences.edit()

                            editor.putInt("id", user.id)
                            editor.putString("username", login)
                            editor.putString("password", pass)
                            editor.apply()  // сохраняем данные

                            Toast.makeText(
                                this@AuthActivity,
                                "Авторизация успешна\uD83E\uDD75",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@AuthActivity, LentaActivity::class.java)
                            intent.putExtra("user", user)
                            startActivity(intent)
                            finish()
                        }
                    }
                    catch (e: HttpException) {
                        // Обрабатываем ошибки
                        withContext(Dispatchers.Main) {
                            if(e.code() == 404){
                                Toast.makeText(
                                    this@AuthActivity,
                                    "Неправильный логин или пароль \uD83D\uDE25 ",
                                    Toast.LENGTH_SHORT
                            ).show()
                            }
                        }
                    }
                    catch (e: Exception) {
                        // Обрабатываем ошибки
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AuthActivity,
                                "Ошибка при авторизации",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        linkToReg.setOnClickListener{
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}