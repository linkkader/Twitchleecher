package com.example.twitchleecher.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.example.twitchleecher.MainActivity.Companion.fa
import com.example.twitchleecher.R
import com.example.twitchleecher.database.download.Download
import com.example.twitchleecher.model.Clip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gurudev.fullscreenvideowebview.FullScreenVideoWebView

class FragmentDownloadItem(val clip: Clip)  : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val floating = view.findViewById<FloatingActionButton>(R.id.floating_action_button)
        val webView = view.findViewById<FullScreenVideoWebView>(R.id.webView)
        webView.visibility = View.VISIBLE
        var downloadUrl = ""
        webView.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request!!.url.toString()
                if (url.contains("production")&&url.contains("assets")){
                    downloadUrl = url
                    Handler(Looper.getMainLooper()).post {
                        floating.visibility = View.VISIBLE
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        floating.setOnClickListener {
            if (FragmentDownload.viewModel!=null){
                val d = Download()
                d.downloadurl = downloadUrl
                d.name = clip.title
                if (isStoragePermissionGranted()){
                    FragmentDownload.viewModel!!.insert(d)
                    it.visibility = View.GONE
                }else{
                }
            }
        }
        webView.loadUrl(clip.url)
    }
    @SuppressLint("WrongConstant")
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionChecker.checkSelfPermission(
                    fa,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    fa,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_download, container, false)
    }
}