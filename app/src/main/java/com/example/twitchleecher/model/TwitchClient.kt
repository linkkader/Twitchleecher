package com.example.twitchleecher.model

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*



class TwitchClient{
    val retrofit:Retrofit
    var urlBase = "https://api.twitch.tv/helix/"
    init {
        retrofit = Retrofit.Builder().baseUrl(urlBase).addConverterFactory(GsonConverterFactory.create()).build()
    }
}
interface GetGames{
    @Headers("Client-Id: a7oo6kgk5ggmplngkidcm1eh3j71p2","Authorization: Bearer 1ifk3vzinzn78ut3idt77fk9srgjo3")
    @GET("games/top")
    fun top(@Query("first") first : String) : Call<Games>
}
interface GetClips{
    @Headers("Client-Id: a7oo6kgk5ggmplngkidcm1eh3j71p2","Authorization: Bearer 1ifk3vzinzn78ut3idt77fk9srgjo3")
    @GET("clips")
    fun top(@Query("game_id") gameId : String,@Query("first") first : String) : Call<Clips>
}