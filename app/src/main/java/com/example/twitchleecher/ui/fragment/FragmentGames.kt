package com.example.twitchleecher.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitchleecher.R
import com.example.twitchleecher.adapters.AdapterGame
import com.example.twitchleecher.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentGames : Fragment() {
    var games = listOf<Game>()
    var client = TwitchClient()
    lateinit var adapter : AdapterGame
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = GridLayoutManager(requireContext(),3)
        adapter = AdapterGame(requireContext(),requireFragmentManager())
        recycler.adapter = adapter
        get()
    }
    fun get(){
        client.retrofit.create(GetGames::class.java).top("100").enqueue(object : Callback<Games>{
            override fun onResponse(call: Call<Games>, response: Response<Games>) {
                games = response.body()!!.data
                adapter.set(games)
            }
            override fun onFailure(call: Call<Games>, t: Throwable) {}
        })
    }
    companion object {

    }
}