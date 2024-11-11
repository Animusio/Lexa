package com.example.myapplication.UserPost

import com.example.myapplication.network.RetrofitInstance
import okhttp3.MultipartBody
import retrofit2.Response

class PostRepository {

    private val apiService = RetrofitInstance.apiService

    // Функция для загрузки медиафайлов
    suspend fun uploadMedia(file: MultipartBody.Part): Response<MediaResponse> {
        return apiService.uploadMedia(file)
    }

    // Получение всех постов с сервера
    suspend fun getAllPosts(): List<Post> {
        val response = apiService.getAllPosts()
        return if (response.isSuccessful && response.body() != null) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Создание нового поста
    suspend fun createPost(post: Post): Post? {
        val response = apiService.createPost(post)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    // Обновление поста
    suspend fun updatePost(post: Post): Post? {
        val response = apiService.updatePost(post.id, post)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    // Удаление поста
    suspend fun deletePost(postId: Int): Boolean {
        val response = apiService.deletePost(postId)
        return response.isSuccessful
    }

    // Лайк поста
    suspend fun likePost(postId: Int, like: Boolean): Post? {
        val response = apiService.likePost(postId, like)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
}
