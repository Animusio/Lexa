package com.example.myapplication.UserPost

import android.os.Parcel
import android.os.Parcelable

// Класс Post реализует Parcelable
data class Post(
    val id: Int,
    val user_id: Int,              // ID пользователя, сделавшего пост
    val nickname: String,
    var post: String,
    var likes_count: Int,
    var media_url: String? = null
) : Parcelable {

    // Конструктор для создания объекта Post из Parcel
    constructor(parcel: Parcel) : this(
        parcel.readInt(),           // id
        parcel.readInt(),           // user_id
        parcel.readString() ?: "",  // nickname
        parcel.readString() ?: "",  // post
        parcel.readInt(),           // likes_count
        parcel.readString()         // media_url
    )

    // Запись полей объекта Post в Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)         // id
        parcel.writeInt(user_id)    // user_id
        parcel.writeString(nickname) // nickname
        parcel.writeString(post)    // post
        parcel.writeInt(likes_count) // likes_count
        parcel.writeString(media_url) // media_url
    }

    // Метод описания содержимого (обычно возвращает 0)
    override fun describeContents(): Int = 0

    // Объект-компаньон для создания экземпляров Post из Parcel
    companion object CREATOR : Parcelable.Creator<Post> {
        override fun createFromParcel(parcel: Parcel): Post {
            return Post(parcel)
        }

        override fun newArray(size: Int): Array<Post?> {
            return arrayOfNulls(size)
        }
    }
}
