package com.example.Lexa.UserPost

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
//ApiService — это интерфейс, который описывает все возможные API-эндпоинты, к которым ваше приложение будет обращаться.
//В Retrofit этот интерфейс аннотируется специальными аннотациями для указания HTTP-методов, путей и параметров запросов.
interface ApiService {

    @POST("/users/login")
    suspend fun getUserByLogin(@Body user: User): User

    @POST("/users/create")
    suspend fun createUser(@Body newUser: User): User


    //посты

    @GET("/posts")
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("/posts/user/{user_id}")
    suspend fun getPostsByUser(@Path("user_id") userId: Int): Response<List<Post>>

    @POST("/posts/create")
    suspend fun createPost(@Body post: Post): Response<Post>

    @PUT("/posts/update/{id}")
    suspend fun updatePost(@Path("id") postId: Int, @Body post: Post): Response<Post>

    @PUT("/posts/{id}/like")
    suspend fun likePost(@Path("id") postId: Int, @Query("like") like: Boolean): Response<Post>

    @DELETE("/posts/delete/{id}")
    suspend fun deletePost(@Path("id") postId: Int): Response<Unit>





    //медиа
    @Multipart
    @POST("/upload/")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part
    ): Response<MediaResponse>

    // Запрос для получения медиафайла по его URL
    @Streaming
    @GET("/media/{media_type}/{filename}")
    suspend fun getMediaFile(
        @Path("media_type") mediaType: String,
        @Path("filename") filename: String
    ): Response<ResponseBody>




    @PUT("users/{user_id}/avatar")
    suspend fun updateUserAvatar(
        @Path("user_id") userId: Int,
        @Body body: Map<String, String>
    ): User

    @Streaming
    @GET("/users/{user_id}/avatar_image")
    suspend fun getUserAvatarImage(
        @Path("user_id") userId: Int
    ): Response<ResponseBody>


    @GET("/users/{user_id}")
    suspend fun getUserById(@Path("user_id") userId: Int): User

}
