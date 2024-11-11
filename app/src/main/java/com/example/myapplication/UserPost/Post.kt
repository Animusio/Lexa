package com.example.myapplication.UserPost

import android.os.Parcel
import android.os.Parcelable

// Класс Post реализует Parcelable
data class Post(
    val id: Int,
    val avatar: String,
    val nickname: String,
    var post: String,
    var likes_count: Int,
    var media_url: String? = null
) : Parcelable {

    // Конструктор для создания объекта Post из Parcel
    private constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString()
    )

    // Запись полей объекта Post в Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(avatar)
        parcel.writeString(nickname)
        parcel.writeString(post)
        parcel.writeInt(likes_count)
        parcel.writeString(media_url)
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
