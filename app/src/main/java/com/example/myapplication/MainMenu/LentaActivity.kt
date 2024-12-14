package com.example.myapplication.MainMenu

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bumptech.glide.Glide
import com.example.myapplication.DataCreate.PostEditor
import com.example.myapplication.DatabasePostadapterProfile.PostsAdapter
import com.example.myapplication.DatabasePostadapterProfile.ProfileActivity
import com.example.myapplication.R
import com.example.myapplication.UserPost.Post
import com.example.myapplication.UserPost.PostRepository
import com.example.myapplication.UserPost.User
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class LentaActivity : AppCompatActivity() {

    private val postRepository = PostRepository()  // Репозиторий для работы с постами
    private lateinit var adapter: PostsAdapter
    private lateinit var lentaList: RecyclerView
    private val lentas = mutableListOf<Post>()  // Список для хранения постов

    private lateinit var client: OkHttpClient  // Клиент WebSocket
    private lateinit var webSocket: WebSocket  // Экземпляр WebSocket


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lenta)





        val imageView: ImageView = findViewById(R.id.imageView5)

        // Инициализация RecyclerView и адаптера
        lentaList = findViewById(R.id.lentaList)
        val user = intent.getParcelableExtra<User>("user")
        adapter = PostsAdapter(this, lifecycleScope, user!!)
        lentaList.layoutManager = LinearLayoutManager(this)
        lentaList.adapter = adapter

        Toast.makeText(this, "это user.id = ${user.id.toString()}", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "это user.avatar_uri = ${user.avatar_uri.toString()}", Toast.LENGTH_SHORT).show()


        val masterKeyAlias = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedSharedPreferences = EncryptedSharedPreferences.create(
            this,
            "encrypted_prefs",  // Название файла SharedPreferences
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val loggedInUserId = encryptedSharedPreferences.getInt("id", 0) // ID текущего авторизованного пользователя

        val imageView5: ImageView = findViewById(R.id.imageView5)
        imageView5.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }



        lentaList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    adapter.playVisibleVideo(rv)
                }
            }
        })



        // Загружаем посты с сервера при первом запуске
        lifecycleScope.launch {
            loadPostsFromServer()
        }

        // Добавление разделителя между элементами списка
        val dividerItemDecoration = DividerItemDecoration(
            lentaList.context,
            (lentaList.layoutManager as LinearLayoutManager).orientation
        )
        lentaList.addItemDecoration(dividerItemDecoration)

        // Инициализация и подключение к WebSocket
        initializeWebSocket()

        // Обработчик нажатий на кнопки
        val button2: TextView = findViewById(R.id.button2)
        val button3: TextView = findViewById(R.id.button3)
        val view: View = findViewById(R.id.view)
        val button1: TextView = findViewById(R.id.textViewLink)
        val line: TextView = findViewById(R.id.textView2)
        val InternetButton: Button = findViewById(R.id.InternetButton)

        button2.setOnClickListener {
            val intent = Intent(this, LentaActivity2::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }
        button3.setOnClickListener {
            val intent = Intent(this, LentaActivity3::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }
        InternetButton.setOnClickListener {
            val intent = Intent(this, PostEditor::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }
        lentaList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                    if (dy >= 10) {
                    // Прокрутка вниз - скрываем элементы
                    view.visibility = View.GONE
                    InternetButton.visibility = View.GONE

                } else if (dy <= -10) {

                    view.visibility = View.VISIBLE
                    InternetButton.visibility = View.VISIBLE

                }
            }
        })
    }
    // Функция для загрузки постов с сервера
    private suspend fun loadPostsFromServer() {
        try {
            val posts = postRepository.getAllPosts()
            if (posts != null) {
                lentas.clear()// Гадюка
                lentas.addAll(posts)  // Добавляем загруженные посты
                adapter.submitList(lentas.toList())  // Обновляем адаптер
                lentaList.scrollToPosition(0)
                Log.d("LentaActivity", "Добавлен новый пост с ID: ${lentas.toList()}")
            } else {
                Toast.makeText(this@LentaActivity, "Не удалось загрузить посты", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this@LentaActivity, "Ошибка при загрузке постов: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Инициализация WebSocket-соединения
    private fun initializeWebSocket() {
        // Инициализация WebSocket клиента
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)  // Ожидание на неопределенное время (не отключается)
            .build()

        val request = Request.Builder()
            .url("ws://188.18.54.95:8000/ws")
            .build()

        // Подключение к WebSocket-серверу
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Соединение установлено")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Обработка сообщения и обновление интерфейса
                lifecycleScope.launch{
                    val gson = Gson()
                    val newPost = gson.fromJson(text, Post::class.java)
                    lentas.add(0, newPost)
                    adapter.submitList(lentas.toList())
                    lentaList.smoothScrollToPosition(0)
                    Log.d("LentaActivity", "Добавлен новый пост с ID: ${newPost.id}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Ошибка соединения: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Соединение закрывается: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Соединение закрыто: $code / $reason")
            }
        })
        client.dispatcher.executorService.shutdown()
    }

    // Закрытие WebSocket при уничтожении активности
    override fun onDestroy() {
        super.onDestroy()
        adapter.releaseAllPlayers()
        webSocket.close(1000, null)  // Закрываем WebSocket
    }
}