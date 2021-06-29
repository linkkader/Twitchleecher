package com.example.twitchleecher.ui.dialog

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.twitchleecher.R
import com.gurudev.fullscreenvideowebview.FullScreenVideoWebView

class DialogDownload(val url: String) : DialogFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var webView = view.findViewById<FullScreenVideoWebView>(R.id.webView)
        webView.visibility = View.VISIBLE
        webView.webViewClient = object : WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }
        webView.loadUrl(url)
        isCancelable = true
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_download,null)
    }
}