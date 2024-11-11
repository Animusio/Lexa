package com.example.myapplication.DatabasePostadapterProfile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainMenu.LentaActivity
import com.example.myapplication.R
import com.example.myapplication.UserPost.User

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val avatar: ImageView = findViewById(R.id.imageView)
        val button: Button = findViewById(R.id.button_back)
        val posts: RecyclerView = findViewById(R.id.lentaList)
        val nickname: TextView = findViewById(R.id.nickname)

        nickname.text = intent.getStringExtra("username")
        val avatarName = intent.getStringExtra("avatar")
        val user = intent.getParcelableExtra<User>("user")

        var avatarId = this.resources.getIdentifier(
            avatarName,// название картинки типо avatar1
            "drawable",// путь файла
            this.packageName // база наверно
        )
        avatar.setImageResource(avatarId)

        button.setOnClickListener{

            val intent = Intent(this, LentaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }
    }
}