package com.example.player

import android.graphics.Bitmap
import android.graphics.Color

class Track(
    var title : String = "",
    var album : String = "",
    var albumCover : Bitmap? = null,
    var squareAlbumCover : Bitmap? = null,
    var artist : String = "",
    var colors : List<Int> = listOf(Color.RED, Color.GREEN, Color.BLUE)

    )