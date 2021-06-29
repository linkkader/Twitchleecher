package com.example.twitchleecher.model
import com.google.gson.annotations.SerializedName


data class Games(
    val data: List<Game>,
    val pagination: Pagination
)
data class Game (
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("box_art_url")
    val boxArtURL: String
)
data class Pagination (
    @SerializedName("cursor")
    val cursor: String
)