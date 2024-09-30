package com.saitejajanjirala.musicdemo

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class Track(
    val name: String,
    val desc: String,
    @RawRes val id: Int,
    @DrawableRes val image:Int
){
    constructor():this("","",R.raw.song1,R.drawable.img)
}

val songs = listOf(
    Track(
        name = "First song",
        desc = "First song description",
        R.raw.song1,
        R.drawable.img,
    ),Track(
        name = "Second song",
        desc = "Second song description",
        R.raw.song2,
        R.drawable.img,
    ),Track(
        name = "Third song",
        desc = "Third song description",
        R.raw.song3,
        R.drawable.img,
    ),Track(
        name = "Four song",
        desc = "Four song description",
        R.raw.song4,
        R.drawable.img,
    ),Track(
        name = "Five song",
        desc = "Five song description",
        R.raw.song5,
        R.drawable.img,
    ),
)