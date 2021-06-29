package com.example.twitchleecher.adapters
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.twitchleecher.R
import com.example.twitchleecher.Xdownload
import com.example.twitchleecher.database.download.Download
import com.example.twitchleecher.getPathFile
import com.example.twitchleecher.humanReadableByteCountSI
import com.example.twitchleecher.ui.fragment.FragmentDownload
import okhttp3.Call
import java.io.File
import java.io.IOException
class AdapterListDownload(var context : Context?) : BaseAdapter() {
    var deleted = 0
    var list = listOf<Download>()
    var check = mutableMapOf<String,Boolean>()
    var check2 = mutableMapOf<String,View>()

    fun set(list1 : List<Download>){
        val size = this.list.size
        this.list= list1
        notifyDataSetChanged()
    //notifyItemRangeChanged(size,list.size)
    }
    
    override fun getCount(): Int = (list.size)

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView = inflater.inflate(R.layout.item_download,parent,false)
        val img = itemView.findViewById<ImageView>(R.id.img)
        val text1 = itemView.findViewById<TextView>(R.id.text1)
        val status = itemView.findViewById<TextView>(R.id.text2)
        val text3 = itemView.findViewById<TextView>(R.id.text3)
        val title = itemView.findViewById<TextView>(R.id.title)
        val episode = itemView.findViewById<TextView>(R.id.episode)
        val progressBar = itemView.findViewById<ProgressBar>(R.id.download_progressBar)
        val menu :ImageButton = itemView.findViewById(R.id.menu)
        if (check[list[position].downloadurl]==null){
            var a = list[position]
            check[list[position].downloadurl] = true
            var bool = true
            var header = a.header.substringAfter("[").substringBeforeLast("]")
            title.text = a.name
            episode.text=a.episode
            var parentFile = Environment.getExternalStorageDirectory().path+"/zanime/"+a.name
            Log.d("downloadPath",parentFile)
            val xtask = Xdownload.Task(a.downloadurl,File(parentFile)).setFileName(a.episode+".mp4")
            progressBar.max = 100
            for (s in header.substringAfter("{").substringBeforeLast("}").split("|*")){
                if (!s.contains("=")) continue
                xtask.addHeader(s.substringBefore("="),s.substringAfter("="))
                Log.d("kaderDown",s.substringBefore("=")+":"+s.substringAfter("="))
            }
            progressBar.max = 100
            progressBar.progress = 0

            val xDownloadTask = xtask.build()
            val info = xDownloadTask.task.getInfo()
            xDownloadTask.addDownloadListener(object : Xdownload.DownloadTaskListener{
                override fun onDownloadDbError(e: Exception) {
                    status.text = e.message
                }
                override fun onDownloadFailed(e: IOException) {
                    status.text = e.message!!
                }
                override fun onDownloadStart(call: Call, total: Long) {
                    Log.d("test78","start"+" "+ humanReadableByteCountSI(total))
                    android.os.Handler(Looper.getMainLooper()).post{
                        status.text = "start"
                    }
                    //}
                    progressBar.max = 100
                }
                override fun onDownloadProgress(downloaded: Long, total: Long, speed: Long) {
                    val v = ((downloaded*100)/total).toInt()
                    if (v==0) return
                    progressBar.progress = v
                    status.text = humanReadableByteCountSI(downloaded)+"/"+ humanReadableByteCountSI(total)+"   "+humanReadableByteCountSI(speed)
                }
                override fun onDownloadAppendFiles(maxPart: Int, part: Int, offset: Long, totalLength: Long) {}
                override fun onDownloadFinish(total: Long, maxPart: Int, speed: Long) {
                    progressBar.progress = 100
                    Log.d("test78","finish" + " " + humanReadableByteCountSI(speed))
                    status.text = "finished" + " " + humanReadableByteCountSI(total)
                }
                override fun onDownloadAppendFilesError(e: Exception) {}
            }).addDownloadPartListener(object : Xdownload.DownloadTaskPartListener{
                override fun onDownloadPartFailed(part: Int, start: Long, downloaded: Long, end: Long, maxPart: Int, totalLength: Long, e: java.lang.Exception) {}
                override fun onDownloadPartProgress(part: Int, start: Long, downloaded: Long, end: Long, maxPart: Int) {}
                override fun onDownloadPartFinish(part: Int, start: Long, end: Long, maxPart: Int) {}
            })
            //initStatus(status,progressBar,task)
            status.text = a.state
            itemView.setOnClickListener{
                bool =!bool
                if (xDownloadTask.isDownloaded()||(info!=null && info.totalLength!=0L && info.totalLength==info.downloaded)){
                    try {
                        val file = File(getPathFile(parentFile,context!!) +"/" + a.episode+".mp4")
                        val intent = Intent(Intent.ACTION_VIEW)
                        val uri = FileProvider.getUriForFile(context!!, context!!.packageName+".provider",file)
                        intent.setDataAndType(uri, "video/*")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context!!.startActivity(intent)
                    }catch (e:Exception){
                        Toast.makeText(context!!,e.message,Toast.LENGTH_LONG).show()
                    }
                    return@setOnClickListener
                }
                if (xDownloadTask.isStarted()){
                    xDownloadTask.cancel()
                    status.text = "resumed"
                    FragmentDownload.viewModel!!.updateState("pause",a.id)
                    img.setImageDrawable(context!!.getDrawable(R.drawable.ic_stop_button))
                }else{
                    FragmentDownload.viewModel!!.updateState("resume",a.id)
                    img.setImageDrawable(context!!.getDrawable(R.drawable.ic_play_arrow))
                    xDownloadTask.start()
                }
            }
            if (a.state=="pause"){
                img.setImageDrawable(context!!.getDrawable(R.drawable.ic_stop_button))
                bool=false
            }else {
                img.setImageDrawable(context!!.getDrawable(R.drawable.ic_play_arrow))
                //    startTask(status,progressBar,task)
                bool = true
            }
            if (info!=null){
                if (info.totalLength!=0L){
                    if (info.totalLength==info.downloaded){
                        progressBar.progress = 100
                        status.text = "finished"
                    }else{
                        progressBar.progress = ((info.downloaded*100)/info.totalLength).toInt()
                        if (bool){
                            xDownloadTask.start()
                        }
                    }
                }else{
                    xDownloadTask.start()
                }
            }
            //menu.visibility = View.GONE
            menu.setOnClickListener {
                val popMenu = PopupMenu(context,it)
                popMenu.inflate(R.menu.delete)
                popMenu.menu.findItem(R.id.delete).setOnMenuItemClickListener {
                    FragmentDownload.viewModel!!.delete(a.id)
                    if (xDownloadTask.isStarted()) {
                        xDownloadTask.cancel()
                    }
                    return@setOnMenuItemClickListener true
                }
                popMenu.show()
            }
            check2[a.downloadurl] = itemView
        }else
            return check2[list[position].downloadurl]!!
        return itemView
    }
}
