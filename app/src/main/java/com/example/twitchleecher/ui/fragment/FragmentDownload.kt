package com.example.twitchleecher.ui.fragment
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.twitchleecher.R
import com.example.twitchleecher.Xdownload
import com.example.twitchleecher.adapters.AdapterListDownload
import com.example.twitchleecher.database.download.DownloadViewModel


class FragmentDownload : Fragment() {
    companion object{
        var viewModel : DownloadViewModel? = null
        lateinit var recyclerView : ListView
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Xdownload().initialize(requireContext())
        recyclerView = view.findViewById<ListView>(R.id.recycler)
        val adapter = AdapterListDownload(requireContext())
        recyclerView.adapter = adapter
        viewModel = ViewModelProvider(this@FragmentDownload).get(DownloadViewModel(requireActivity().application)::class.java)
        viewModel!!.all.observe(viewLifecycleOwner, {
            adapter.set(it)
        })
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download, container, false)
    }
}