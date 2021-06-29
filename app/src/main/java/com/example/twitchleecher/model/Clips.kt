package com.example.twitchleecher.model
import com.google.gson.annotations.SerializedName



data class Clips (
    val data: List<Clip> = listOf(),
    val pagination: Pagination
)


data class Clip (
    var id: String="",
    var url: String="",
    @SerializedName("embed_url")
    val embedURL: String="",
    @SerializedName("broadcaster_id")
    val broadcasterID: String="",
    @SerializedName("broadcaster_name")
    val broadcasterName: String="",
    @SerializedName("creator_id")
    val creatorID: String="",
    @SerializedName("creator_name")
    val creatorName: String="",
    @SerializedName("video_id")
    val videoID: String="",
    @SerializedName("game_id")
    val gameID: String="",
    val language: String="",
    var title: String="",
    @SerializedName("view_count")
    val viewCount: Long = 0,
    @SerializedName("created_at")
    val createdAt: String="",
    @SerializedName("thumbnail_url")
    val thumbnailURL: String="",
    val duration: Double = 0.0
)




