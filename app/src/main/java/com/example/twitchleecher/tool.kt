package com.example.twitchleecher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import okhttp3.*
import okio.GzipSource
import okio.buffer
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager



fun urlBase(url: String) : String{ return url.substringAfter("//").substringBefore("/") }
fun urlName(url: String) : String{ return urlBase(url).substringBeforeLast(".").substringAfter(".") }

fun headerString(url: String) : String{
    return MainActivity.shareHeader.getString(urlBase(url), "")!!
}

fun glide(url: String, imageView: ImageView, context: Context){
    var url = url
    val header = stringToHeader(MainActivity.shareHeader.getString(urlBase(url), "")!!)
    var cookie = ""
    try {
        cookie = CookieManager.getInstance().getCookie(url)
    }catch (e: Exception){}
    if (cookie!=null){
        header["Cookie"] = cookie
    }
    try {
        val glideUrl = GlideUrl(url) { header }
        Glide.with(context).load(glideUrl).centerCrop().onlyRetrieveFromCache(true)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Glide.with(context).load(url).centerCrop()
                        .addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                //Picasso.get().load(url).into(imageView)
                                return true
                            }
                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                imageView.setImageDrawable(resource)
                                return true
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(object : CustomTarget<Drawable>(){
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                imageView.setImageDrawable(resource)
                            }

                        })
                    return true
                }
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    imageView.setImageDrawable(resource)
                    return true
                }
            })
            .into(object : CustomTarget<Drawable>(){
                override fun onLoadCleared(placeholder: Drawable?) {
                }
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    imageView.setImageDrawable(resource)
                }
            })
    }catch (e:Exception){ }
}
fun truncate(str: String, len: Int): String? {
    return if (str.length > len) {
        str.substring(0, len) + "..."
    } else {
        str
    }
}
class GzipInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest: okhttp3.Request.Builder = chain.request().newBuilder()
        val response: Response = chain.proceed(newRequest.build())
        return if (isGzipped(response)) {
            unzip(response)
        } else {
            response
        }
    }

    @Throws(IOException::class)
    private fun unzip(response: Response): Response {
        if (response.body == null) {
            return response
        }
        val gzipSource = GzipSource(response.body!!.source())
        val bodyString: String = gzipSource.buffer().readUtf8()
        val responseBody: ResponseBody = ResponseBody.create(response.body!!.contentType(), bodyString)
        val strippedHeaders = response.headers.newBuilder()
            .removeAll("Content-Encoding")
            .removeAll("Content-Length")
            .build()
        val r = response.newBuilder()
            .headers(strippedHeaders)
        return r.body(responseBody)
            .message(response.message)
            .build()
    }
    private fun isGzipped(response: Response): Boolean {
        return response.header("Content-Encoding") != null && response.header("Content-Encoding").equals("gzip")
    }
}
fun isInternet(context: Context):Boolean{
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo=connectivityManager.activeNetworkInfo
    val check = networkInfo!=null && networkInfo.isConnected
    if (!check){
        Toast.makeText(context,"Check Internet",Toast.LENGTH_LONG).show()
    }
    return  true
}
class DoAsync(val handler: () -> Unit,val onPostExecute: () -> Unit) : Thread() {
    override fun run() {
        try {
            handler()
        }catch (e:Exception){}
        Handler(Looper.getMainLooper()).post {
            try {
                onPostExecute()
            }catch (e:Exception){}
        }
    }
}
fun getStringAsset(name:String,context: Context): String {
    val stream = context.assets.open(name)
    val size = stream.available()
    val b = ByteArray(size)
    stream.read(b)
    return String(b)
}

fun unSafeOkHttpClient() :OkHttpClient.Builder {
    val okHttpClient = OkHttpClient.Builder()
    try {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts:  Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?){}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate>  = arrayOf()
        })

        // Install the all-trusting trust manager
        val  sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        if (trustAllCerts.isNotEmpty() &&  trustAllCerts.first() is X509TrustManager) {
            okHttpClient.sslSocketFactory(sslSocketFactory, trustAllCerts.first() as X509TrustManager)
            okHttpClient.hostnameVerifier(HostnameVerifier { _, _ -> true })
        }
        return okHttpClient
    } catch (e: Exception) {
        return okHttpClient
    }
}