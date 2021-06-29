package com.example.twitchleecher.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitchleecher.MainActivity
import com.example.twitchleecher.R
import com.example.twitchleecher.adapters.AdapterGame
import com.example.twitchleecher.database.download.Download
import com.example.twitchleecher.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentSearch : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editText = view.findViewById<EditText>(R.id.editText)
        val btn = view.findViewById<Button>(R.id.btn)
        btn.setOnClickListener {
            val clip = Clip()
            clip.url = editText.text.toString()
            clip.title = clip.url.substringAfterLast("/")
            clip.id = clip.title
            requireFragmentManager().beginTransaction().add(MainActivity.mainContainerId2,FragmentDownloadItem(clip)).addToBackStack(null).commitAllowingStateLoss()
        }
    }
}