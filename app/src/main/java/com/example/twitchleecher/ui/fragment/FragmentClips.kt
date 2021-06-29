package com.example.twitchleecher.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitchleecher.R
import com.example.twitchleecher.adapters.AdapterClip
import com.example.twitchleecher.adapters.AdapterGame
import com.example.twitchleecher.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentClips(val gameId : String) : Fragment() {
    var client = TwitchClient()
    lateinit var adapter : AdapterClip
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = GridLayoutManager(requireContext(),1)
        adapter = AdapterClip(requireContext(),requireFragmentManager())
        recycler.adapter = adapter
        get()
    }
    fun get(){
        client.retrofit.create(GetClips::class.java).top(gameId,"100").enqueue(object : Callback<Clips>{
            override fun onResponse(call: Call<Clips>, response: Response<Clips>) {
                adapter.set(response.body()!!.data)
            }
            override fun onFailure(call: Call<Clips>, t: Throwable) {}
        })
    }
    companion object {

    }
}