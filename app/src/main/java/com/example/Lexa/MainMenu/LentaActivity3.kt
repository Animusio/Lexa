package com.example.Lexa.MainMenu

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Lexa.DatabasePostadapterProfile.PostsAdapter
import com.example.Lexa.R
import com.example.Lexa.UserPost.Post
import com.example.Lexa.UserPost.User

class LentaActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lenta3)

        val lentaList : RecyclerView = findViewById(R.id.lentaList)
        val lentas = arrayListOf<Post>()
        val button1: TextView = findViewById(R.id.button1)
        val button2: TextView = findViewById(R.id.button2)
        val user = intent.getParcelableExtra<User>("user")

        //lentas.add(Post(2,"avatar2", "boby", "робби лох",0))

        val adapter = PostsAdapter(this, lifecycleScope, user!!)  // Передаем только контекст
        lentaList.layoutManager = LinearLayoutManager(this)
        lentaList.adapter = adapter

        // Устанавливаем список постов через submitList
        adapter.submitList(lentas)

        val dividerItemDecoration = DividerItemDecoration(lentaList.context, (lentaList.layoutManager as LinearLayoutManager).orientation)

        lentaList.addItemDecoration(dividerItemDecoration)

        button1.setOnClickListener{
            val intent = Intent(this, LentaActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }
        button2.setOnClickListener{
            val intent = Intent(this, LentaActivity2::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("user", user)
            startActivity(intent)
        }

    }
}