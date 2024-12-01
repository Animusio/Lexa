package com.example.myapplication.UserPost

import android.os.Parcel
import android.os.Parcelable

// Класс User с добавленным полем avatar_uri и реализацией Parcelable
data class User(
    val id: Int,
    val login: String,
    val password: String,
    var avatar_uri: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),                                // id
        parcel.readString() ?: "",                      // login
        parcel.readString() ?: "",                      // password
        parcel.readString()                              // avatar_uri
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)                             // id
        parcel.writeString(login)                      // login
        parcel.writeString(password)                   // password
        parcel.writeString(avatar_uri)                 // avatar_uri
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}
