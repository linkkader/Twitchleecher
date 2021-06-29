package com.example.twitchleecher

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import okhttp3.*
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.*

class Xdownload{
    companion object{
        private var fa : Context? = null
        private var totalLength : MutableMap<String,Long> = mutableMapOf()
        private var db: SQLiteDatabase? = null
        private var infos : MutableMap<Long, Info> = mutableMapOf()
        private var start : MutableMap<String,Boolean> = mutableMapOf()
        fun getAllInfo():List<Info>{
            infos = mutableMapOf()
            db.let {
                val c = it!!.rawQuery("select * from task", null)
                if (c.moveToFirst()){
                    do {
                        val i = Info(c.getLong(0), c.getString(1), c.getString(2), File(c.getString(3)), c.getInt(4), stringToHeader(c.getString((5))), c.getLong(6), c.getLong(7), c.getString(8))
                        infos[i.id] = i
                    }while (c.moveToNext())
                }
                c.close()
            }
            return infos.values.toList()
        }
    }
    //all info of on task
    data class Info(val id: Long, val url: String, val fileName: String, val dir: File, val idm: Int, val headers: MutableMap<String, String>, val totalLength: Long, val downloaded: Long, val cacheDir: String)
    //

    fun getTasks():List<Task>{
        val infos = getAllInfo()
        var tasks = listOf<Task>()
        for (i in infos){
            val t = Task(i.url,i.dir)
            t.setIdm(i.idm)
            t.cacheDir = i.cacheDir
            t.setFileName(i.fileName)
            t.url = i.url
            t.headers = i.headers
            tasks = tasks.plus(t)
        }
        return tasks
    }
    fun initialize(context: Context){

        fa = context

        // initialyze db
        val dbHelper = DbHelper(context)
        db = dbHelper.readableDatabase
        infos = mutableMapOf()
        db.let {
            val c = it!!.rawQuery("select * from task", null)
            if (c.moveToFirst()){
                do {
                    val i = Info(c.getLong(0), c.getString(1), c.getString(2), File(c.getString(3)), c.getInt(4), stringToHeader(c.getString((5))), c.getLong(6), c.getLong(7), c.getString(8))
                    totalLength[i.url] = i.totalLength
                    infos[i.id] = i
                }while (c.moveToNext())
            }
            c.close()
        }
    }
    fun deteleTastk(taskId: Long){
        db?.delete("task", "id=$taskId", null)
    }
    fun updateUrl(url: String, taskId: Long){
        val v = ContentValues()
        v.put("url", url)
        db?.update("task", v, "id=$taskId", null)
    }
    fun updateHeaders(headers: MutableMap<String, String>, taskId: Long){
        val v = ContentValues()
        v.put("header", headerToString(headers))
        db?.update("task", v, "id=$taskId", null)
    }
    private fun updateHeader(headers: MutableMap<String, String>, taskId: Long){
        val v = ContentValues()
        v.put("header", headerToString(headers))
        db?.update("task", v, "id=$taskId", null)
    }
    class Task(var url: String, var dir: File){
        fun getId():Long{
            var list = getAllInfo()
            for (i in list){
                if (i.url==url) return i.id
            }
            return (-1).toLong()
        }
        private var info : Info? = null
        var cacheDir = safeDirName(url)
        private var fileName : String = ""
        // max numbers of part
        var idm = 1
        var headers : MutableMap<String, String> = mutableMapOf()
        fun addHeaders(headers: MutableMap<String, String>):Task{
            for (h in headers){
                this.headers[h.key] = h.value
            }
            return this
        }
        fun addHeader(key: String, value: String){
            headers[key] = value
        }
        fun removeHeader(key: String){
            headers.remove(key)
        }
        fun removeHeaders(headers: MutableMap<String, String>){
            for (h in headers){
                headers.remove(h.key)
            }
        }
        fun setIdm(i: Int):Task{
            idm = i
            return this
        }
        fun setFileName(name: String):Task{
            if (name!="")fileName = name
            return this
        }
        fun getInfo() = info
        fun getFileName() = fileName
        fun build():DownloadTask{
            //check if task is already saved in db
            var exist = false
            val list = getAllInfo()
            var id = 0L
            for (i in list){
                if (this.fileName == i.fileName && i.dir.toString()==this.dir.toString() && i.url!= this.url ){
                    return DownloadTask(Task("File Name Already exist same dir",File("File Name Already exist same dir")))
                }
                if (i.url==this.url){
                    id = i.id
                    exist = true
                    //if already saved idm must be same
                    setIdm(i.idm)
                    this.info = i
                    if (this.dir.toString()!=i.dir.toString())
                        this.dir = i.dir
                    this.cacheDir = i.cacheDir
                    if (this.fileName=="") this.fileName = i.fileName
                    // update header if he is null
                    if (this.headers.isEmpty()){
                        this.headers = i.headers
                    }
                    break
                }
            }
            //if file name is null try generate it
            if (this.fileName==""){
                this.fileName = getFileNameFromURL(this.url)
            }

            if (!exist){
                db?.insert("task", null, taskToValue(this))
                // if file is not in db init task info
                info = Info(id,url,fileName,dir,idm,headers,0L,0L,cacheDir)
            }
            /*else{
                if (this.dir.toString()!=info?.dir.toString() || info?.fileName!=this.fileName){

                    db?.update("task",taskToValue(this), "id=$id",null)
                }
            }*/

            idm = 1
            return DownloadTask(this)
        }
        fun buildPart():DownloadTask{
            //check if task is already saved in db
            var exist = false
            val list = getAllInfo()
            var id = 0L
            for (i in list){
                if (i.url==this.url){
                    id = i.id
                    exist = true
                    //if already saved idm must be same
                    setIdm(i.idm)
                    this.info = i
                    if (this.dir.toString()!=i.dir.toString())
                        this.dir = i.dir
                    this.cacheDir = i.cacheDir
                    if (this.fileName=="") this.fileName = i.fileName
                    break
                }
            }
            //if file name is null try generate it
            if (this.fileName==""){
                this.fileName = getFileNameFromURL(this.url)
            }

            if (!exist){
                db?.insert("task", null, taskToValue(this))
                // if file is not in db init task info
                info = Info(id,url,fileName,dir,idm,headers,0L,0L,cacheDir)
            }
            /*else{
                if (this.dir.toString()!=info?.dir.toString() || info?.fileName!=this.fileName){

                    db?.update("task",taskToValue(this), "id=$id",null)
                }
            }*/
            return DownloadTask(this)
        }

        private fun taskToValue(t: Task):ContentValues{
            val v = ContentValues()
            v.put("url", t.url)
            v.put("dir", t.dir.toString())
            v.put("fileName", t.fileName)
            v.put("idm", idm)
            v.put("header", headerToString(headers))
            v.put("totalLenght", 0L)
            v.put("completed", 0L)
            v.put("cachedir", t.cacheDir)
            return v
        }
    }
    private class DbHelper(context: Context):SQLiteOpenHelper(context, "linkkader.db", null, 1){
        override fun onCreate(db: SQLiteDatabase?) {
            db!!.execSQL("CREATE TABLE IF NOT EXISTS task(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "url TEXT," +
                    "fileName TEXT," +
                    "dir TEXT," +
                    "idm INTEGER," +
                    "header TEXT," +
                    "totalLenght INTEGER," +
                    "completed INTEGER," +
                    "cachedir String)")
        }
        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
    }

    class DownloadTask(val task: Task){
        private var call : Call? = null
        private val client = OkHttpClient().newBuilder().build()
        private var requestNum = 0
        private var started  = false
        private var isDownloaded = false
        private var cachePath =  task.dir.toString()+"/"+task.cacheDir+"/"
        private var maxPart = 0
        private var totalLength : Long? = null
        private var downloaded : Long? = null
        private var downloadListener : DownloadTaskListener? = null
        private var downloadTaskPartListener : DownloadTaskPartListener? = null
        private var speed = 0L
        fun addDownloadListener(listener: DownloadTaskListener):DownloadTask{
            downloadListener = listener
            return this
        }
        fun isDownloaded() = isDownloaded
        fun addDownloadPartListener(listener: DownloadTaskPartListener):DownloadTask{
            downloadTaskPartListener = listener
            return this
        }
        private fun downloadByPart(){
            val r = Request.Builder().url(task.url)
            for (h in task.headers){
                r.addHeader(h.key,h.value)
            }
            val request = r.build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (downloadListener != null) {
                        Handler(Looper.getMainLooper()).post{
                            downloadListener!!.onDownloadFailed(e)
                        }
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    val size = response.body!!.contentLength()
                    //check if file is already download
                    if(File(cachePath, task.getFileName()).length() == size){
                        Handler(Looper.myLooper()!!).post {
                            isDownloaded = true
                            downloaded = size
                            downloadListener?.onDownloadFinish(size,task.idm,0L)
                        }
                        return
                    }
                    if (downloadListener != null) {
                        android.os.Handler(Looper.getMainLooper()).post {
                            downloadListener!!.onDownloadStart(call, size)
                        }
                    }
                    downloaded = 0
                    totalLength = size
                    task.getInfo().let {
                        if (it!!.totalLength==0L){
                            Handler(Looper.getMainLooper()).post{
                                upadateTotalLenght(totalLength!!)
                            }
                        }
                        //if download file size is changed restart to zero
                        if (it.totalLength != 0L && it.totalLength != totalLength) {
                            deleteRecursive(File(cachePath))
                            File(task.dir.toString() + "/" + task.getFileName())
                            Handler(Looper.getMainLooper()).post{
                                upadateTotalLenght(totalLength!!)
                            }
                        }
                    }

                    //update speed per second
                    downloadListener?.let {
                        val handler = Handler(Looper.getMainLooper())
                        var previous = 0L
                        handler.post(object : Runnable {
                            override fun run() {
                                speed = downloaded!! - previous
                                previous = downloaded!!
                                if (downloaded != totalLength) handler.postDelayed(this, 1000)
                            }
                        })
                    }

                    //size of one part
                    var partSize = size / (if (task.idm<1)1 else if (task.idm>8) 8 else task.idm)
                    if ((if (task.idm<1)1 else if (task.idm>8) 8 else task.idm)!=0 && partSize<16000000L){
                        partSize = 16000000L
                        var i = size/partSize
                        if (i<1L) i=1L
                        upadateIdm(i.toInt())
                    }
                    //check if another part can be download
                    check(1,0,object : Xdownload.ListenerCheckRequest{
                        override fun onFailed(e: IOException) {
                        }
                        override fun onSuccess(response: Response) {
                            downloadPart(1, totalLength!!, 0L , response, partSize)
                        }
                    })
                }
            })
        }
        private fun download(){
            try {
                if(task.getInfo() !=null) {
                    if (task.getInfo()!!.totalLength!=0L&&task.getInfo()!!.downloaded==task.getInfo()!!.totalLength){
                        isDownloaded = true
                        downloadListener?.onDownloadFinish(task.getInfo()!!.downloaded,task.idm,0L)
                        return
                    }
                }
            }catch (ex:Exception){
                val e = IOException(ex.toString())
                Handler(Looper.getMainLooper()).post{
                    downloadListener?.onDownloadFailed(e)
                }
            }
            cachePath = getPathFile(task.dir.toString(), fa!!)
            if (task.getInfo()!=null){
                val v  = task.getInfo()!!.totalLength
                if (v>0L){
                    if(File(cachePath, task.getFileName()).length() == v){
                        Handler(Looper.myLooper()!!).post {
                            isDownloaded = true
                            downloaded = v
                            downloadListener?.onDownloadFinish(v,task.idm,0L)
                        }
                        return
                    }
                }
            }
            if (task.url=="File Name Already exist and same dir"){
                if (downloadListener != null) {
                    val e = IOException("File Name Already and exist same dir")
                    Handler(Looper.getMainLooper()).post{
                        downloadListener?.onDownloadFailed(e)
                    }
                }
                return
            }
            try {
                if (fa==null){
                    Handler(Looper.getMainLooper()).post{
                        val e = IOException("Not Init")
                        downloadListener?.onDownloadFailed(e)
                    }
                    return
                }
                if (!File(cachePath).exists()){
                    File(cachePath).mkdirs()
                }
                val mediaFile = File(cachePath, task.getFileName())
                var lenght = 0L
                if (mediaFile.exists()){
                    lenght = mediaFile.length()//+1.toLong()
                }
                if (!mediaFile.exists()){
                    mediaFile.createNewFile()
                }
                val r = Request.Builder().tag(task.url).url(task.url).header("Range", "bytes=" + lenght.toString() + "-")
                for (h in task.headers){
                    r.addHeader(h.key,h.value)
                }
                val request = r.build()

                this@DownloadTask.call = client.newCall(request)
                this@DownloadTask.call!!.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (downloadListener != null) {
                            Handler(Looper.getMainLooper()).post{
                                downloadListener!!.onDownloadFailed(e)
                            }
                        }
                    }
                    override fun onResponse(call: Call, response: Response) {
                        if (response.body!!.contentLength()<0L){
                            if (downloadListener != null) {
                                val e = IOException("ContentLenght must be > 0")
                                Handler(Looper.getMainLooper()).post{
                                    downloadListener!!.onDownloadFailed(e)
                                }
                            }
                            return
                        }
                        val size = response.body!!.contentLength()
                        if(File(cachePath, task.getFileName()).length() == size){
                            Handler(Looper.myLooper()!!).post {
                                isDownloaded = true
                                downloaded = size
                                downloadListener?.onDownloadFinish(size,task.idm,0L)
                            }
                            return
                        }
                        //check if file is already download
                        if (downloadListener != null) {
                            android.os.Handler(Looper.getMainLooper()).post {
                                downloadListener!!.onDownloadStart(call, size)
                            }
                        }
                        try {
                            downloaded = 0
                            if (totalLength!=null){
                                totalLength = if(size>totalLength!!) size else totalLength
                            }else
                                totalLength = if(size> task.getInfo()!!.totalLength) size else size
                        }catch (e:Exception){}

                        downloaded = 0
                        if (Xdownload.totalLength[task.url]==null){
                            Xdownload.totalLength[task.url] = size
                            Handler(Looper.getMainLooper()).post{
                                upadateTotalLenght(totalLength!!)
                            }
                        }
                        totalLength = Xdownload.totalLength[task.url]

                        task.getInfo().let {
                            if (it!!.totalLength==0L){
                                Handler(Looper.getMainLooper()).post{
                                    upadateTotalLenght(totalLength!!)
                                }
                            }
                        }
                        //update speed per second
                        downloadListener?.let {
                            val handler = Handler(Looper.getMainLooper())
                            var previous = 0L
                            handler.post(object : Runnable {
                                override fun run() {
                                    speed = downloaded!! - previous
                                    previous = downloaded!!
                                    if (downloaded != totalLength) handler.postDelayed(this, 1000)
                                }
                            })
                        }
                        if (totalLength!! > 0.toLong())downloadSingle(task.getFileName(), totalLength!!, lenght , response)

                    }
                })
            }catch (e:Exception){
                downloadListener?.onDownloadFailed(IOException(e.message))
            }
        }
        private fun downloadSingle(part: String, end: Long, start: Long, response: Response){


            try {
                this.maxPart = 1
                var end = end
                val dir = File(cachePath)
                if (!dir.exists()){
                    dir.mkdirs()
                }
                // part of file in cache dir
                val mediaFile = File(cachePath, part)
                if (!mediaFile.exists()){
                    mediaFile.createNewFile()
                }
                val lenght = mediaFile.length()
                // add lenght if some bit is already downloaded
                this@DownloadTask.downloaded = this@DownloadTask.downloaded?.plus(lenght)
                // size of part downloaded for just this part
                var downloaded = start //+ lenght
                try {
                    val bufferSize = 32000
                    val buff = ByteArray(bufferSize)
                    val output = FileOutputStream(mediaFile, true)
                    //bufferring part
                    while (true) {
                        val readed = response.body!!.byteStream().read(buff)
                        if (readed == -1) break
                        output.write(buff, 0, readed)
                        downloaded += readed
                        this@DownloadTask.downloaded = this@DownloadTask.downloaded?.plus(readed)
                        if (downloadListener!=null){
                            Handler(Looper.getMainLooper()).post {
                                downloadListener!!.onDownloadProgress(this@DownloadTask.downloaded!!, this@DownloadTask.totalLength!!,speed)
                            }
                        }
                        if (downloadTaskPartListener!=null){
                            Handler(Looper.getMainLooper()).post {
                                downloadTaskPartListener!!.onDownloadPartProgress(1, start, downloaded, end, maxPart)
                            }
                        }
                        if (Companion.start[this.task.url] == false) return
                        //if part exceed
                    }
                    output.flush()
                    output.close()
                    Handler(Looper.getMainLooper()).post {
                        try {
                            downloadTaskPartListener!!.onDownloadPartFinish(1, start, end, maxPart)
                        }catch (e:Exception){}
                        //update database on size of already download
                        updateDownloaded(this.downloaded!!)
                    }
                    Handler(Looper.getMainLooper()).post {
                        //update db
                        downloaded = totalLength!!
                        if (Build.VERSION.SDK_INT >= 29){
                            if (!moveFile(mediaFile,mediaFile.name, fa!!))
                                return@post
                        }
                        isDownloaded = true
                        updateDownloaded(this.totalLength!!)
                        downloadListener?.onDownloadFinish(totalLength!!, maxPart,speed)
                    }
                }catch (e:Exception){
                    downloadTaskPartListener?.onDownloadPartFailed(1,start,downloaded,end,maxPart,totalLength!!,e)
                }
            }catch (e:java.lang.Exception){
                try {
                    downloadListener?.onDownloadFailed(e as IOException)
                }catch (e:Exception){

                }
            }
        }



        private fun upadateTotalLenght(totalLenght: Long){
            try {
                val v = ContentValues()
                v.put("totalLenght", totalLenght)
                db?.update("task", v, "url='${task.url}'", null)
            }catch (e:Exception){downloadListener?.onDownloadDbError(e)}
        }


        private fun upadateIdm(idm: Int){
            try {
                val v = ContentValues()
                v.put("idm", idm)
                db?.update("task", v, "url='${task.url}'", null)
            }catch (e:Exception){downloadListener?.onDownloadDbError(e)}
        }
        private fun updateDownloaded(downloaded: Long){
            try {
                val v = ContentValues()
                v.put("downloaded", downloaded)
                db?.update("task", v, "url='${task.url}'", null)
            }catch (e:java.lang.Exception){}
        }
        private fun check(part: Int, start: Long, listener: ListenerCheckRequest){
            //check if part of file is already downnload
            val mediaFile = File(cachePath, part.toString())
            var lenght = 0L
            if (mediaFile.exists()){
                lenght = mediaFile.length()
            }
            val request = Request.Builder().url(task.url)
                    //specify just get part of file
                    .header("Range", "bytes=" + (start + lenght).toString() + "-")
                    .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requestNum--
                    //if all request is stop
                    if (requestNum==0){
                        if (!Xdownload.start[this@DownloadTask.task.url]!!){
                            started = false
                        }
                    }
                    listener.onFailed(e)
                    downloadTaskPartListener?.onDownloadPartFailed(part, start,0L,0L,maxPart,totalLength!!,e)
                }
                override fun onResponse(call: Call, response: Response) {
                    requestNum++
                    listener.onSuccess(response)
                }
            })
        }

        //download one part
        private fun downloadPart(part: Int, end: Long, start: Long, response: Response, partSize: Long){
            try {
                this.maxPart = part
                var end = end
                val dir = File(cachePath)
                if (!dir.exists()){
                    dir.mkdirs()
                }
                // part of file in cache dir
                val mediaFile = File(cachePath, part.toString())
                if (!mediaFile.exists()){
                    mediaFile.createNewFile()
                }
                val lenght = mediaFile.length()

                // add lenght if some bit is already downloaded
                this@DownloadTask.downloaded = this@DownloadTask.downloaded?.plus(lenght)
                // size of part downloaded for just this part
                var downloaded = start + lenght
                try {
                    val bufferSize = 32000
                    val buff = ByteArray(bufferSize)
                    val output = FileOutputStream(mediaFile, true)

                    //bool for check if part exceed
                    var check = false

                    // for check if another part can be download
                    check(part + 1, start + partSize + 1.toLong(), object : ListenerCheckRequest{
                        override fun onFailed(e: IOException) {}
                        override fun onSuccess(response: Response) {
                            if((start+partSize+100)<end) {
                                //for not lost total length
                                val e = end
                                // another part can be download (update end of this part)
                                end = start+partSize
                                // if file exceed need trunc it
                                if (check){
                                    truncateFile(mediaFile, end - start)
                                }
                                //download new partt
                                downloadPart(part + 1, e, start + partSize + 1.toLong(), response, partSize)
                            }
                        }})

                    //bufferring part
                    while (true) {
                        val readed = response.body!!.byteStream().read(buff)
                        if (readed == -1) break
                        output.write(buff, 0, readed)
                        downloaded += readed

                        this@DownloadTask.downloaded = this@DownloadTask.downloaded?.plus(readed)
                        if (downloadListener!=null){
                            Handler(Looper.getMainLooper()).post {
                                downloadListener!!.onDownloadProgress(this@DownloadTask.downloaded!!, this@DownloadTask.totalLength!!,speed)
                            }
                        }
                        if (downloadTaskPartListener!=null){
                            Handler(Looper.getMainLooper()).post {
                                downloadTaskPartListener!!.onDownloadPartProgress(part, start, downloaded, end, maxPart)
                            }
                        }
                        //if part exceed
                        if (downloaded > end) break
                    }
                    check = true
                    output.flush()
                    output.close()
                    //trunc part if exceed
                    truncateFile(mediaFile, end - start)


                    if (downloadTaskPartListener!=null){
                        Handler(Looper.getMainLooper()).post {
                            downloadTaskPartListener!!.onDownloadPartFinish(part, start, end, maxPart)
                            //update database on size of already download
                            updateDownloaded(this.downloaded!!)
                        }
                    }

                    var bool = true
                    var i = 0

                    //get all parts
                    val files = File(cachePath).listFiles().toList()
                    val size = files.size

                    //check if all part is downloaded (normaly next part >= previous part)
                    while (i<size-1){
                        if(files[i].length()>files[i + 1].length()){
                            bool = false
                            break
                        }
                        i++
                    }

                    //if all part is download
                    if (bool){
                        try {

                            // append all part to finally file
                            if (appendFile(files, File(task.dir, task.getFileName()), this.downloadListener, totalLength!!))

                            {
                                this.downloaded = this.totalLength!!
                                //delete all part
                                val f = File(cachePath)
                                deleteRecursive(f)
                            }
                            Handler(Looper.getMainLooper()).post {
                                //update db
                                isDownloaded = true
                                updateDownloaded(this.totalLength!!)
                                downloadListener?.onDownloadFinish(totalLength!!, maxPart,speed)
                            }
                        }catch (e:Exception) {
                            Handler(Looper.getMainLooper()).post{
                                downloadListener?.onDownloadAppendFilesError(e)
                            }
                        }
                    }
                }catch (e:Exception){
                    downloadTaskPartListener?.onDownloadPartFailed(part,start,downloaded,end,maxPart,totalLength!!,e)
                }
            }catch (e:java.lang.Exception){
                try {
                    downloadListener?.onDownloadFailed(e as IOException)
                }catch (e:Exception){

                }
            }
        }//download one part

        fun getTotaLength() = totalLength

        fun getdownloaded() =  downloaded

        fun start(){
            if (start[this.task.url]==null){
                start[this.task.url] = false
            }
            if (!start[this.task.url]!!){
                start[this.task.url] = true
                download()
            }
        }

        // check if task  is start
        fun isStarted() : Boolean {
            if (start[this.task.url]==null) return false
            return start[this.task.url]!!
        }

        //stop task
        fun cancel(){
            if (start[this.task.url]==null){
                start[this.task.url] = false
            }
            if (start[this.task.url]==true){
                client.dispatcher.cancelAll()
                start[this.task.url] = false
            }
        }


        //delete all file in directory and directory
    }

    //listerner check if new part can be download
    private interface ListenerCheckRequest{
        fun onFailed(e: IOException)
        fun onSuccess(response: Response)
    }
    //listener for one task
    interface DownloadTaskListener{
        fun onDownloadDbError(e: Exception)
        fun onDownloadFailed(e: IOException)
        fun onDownloadStart(call: Call, total: Long)
        fun onDownloadProgress(downloaded: Long, total: Long,speed: Long)
        fun onDownloadAppendFiles(maxPart: Int, part: Int, offset: Long, totalLength: Long)
        fun onDownloadFinish(total: Long, maxPart: Int, speed: Long)
        fun onDownloadAppendFilesError(e: Exception)
    }
    //listener for one part of task
    interface DownloadTaskPartListener{
        fun onDownloadPartFailed(part: Int,start: Long, downloaded: Long, end: Long, maxPart: Int,totalLength: Long,e:java.lang.Exception)
        fun onDownloadPartProgress(part: Int, start: Long, downloaded: Long, end: Long, maxPart: Int)
        fun onDownloadPartFinish(part: Int, start: Long, end: Long, maxPart: Int)
    }

}
fun createFileWithSize(file: File, size: Long){
    try {
        val randomAccessFile = RandomAccessFile(file, "rw")
        randomAccessFile.setLength(size)
        randomAccessFile.close()
    }catch (e: Exception){

    }
}
fun headerToString(header: MutableMap<String, String>) : String{
    var h = ""
    for (a in header){
        h += "$a|*"
    }
    return h.substringAfter("[").substringBeforeLast("]")
}
fun stringToHeader(string: String) : MutableMap<String, String>{
    val header = mutableMapOf<String, String>()
    for (s in string.substringAfter("{").substringBeforeLast("}").split("|*")){
        if (!s.contains("=")) continue
        header[s.substringBefore("=")] = s.substringAfter("=")
    }
    return header
}
fun truncateFile(target: File, size: Long){
    val outPut = FileOutputStream(target, true)
    outPut.channel.truncate(size)
    outPut.flush()
    outPut.close()
}
fun humanReadableByteCountSI(bytes: Long): String? {
    var bytes = bytes
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1000
        ci.next()
    }
    return java.lang.String.format("%.1f %cB", bytes / 1000.0, ci.current())
}
fun joinByteArray(byte1: ByteArray, byte2: ByteArray): ByteArray{
    return ByteBuffer.allocate(byte1.size + byte2.size)
            .put(byte1)
            .put(byte2)
            .array()
}
fun mergeFile(file1: File, file2: File, outpout: File){
    val outpout = FileOutputStream(outpout)
    val siStream = SequenceInputStream(FileInputStream(file1), FileInputStream(file2))
    val buff = ByteArray(4096)
    var downloaded = 0L
    while (true){
        val readed = siStream.read(buff)
        if (readed==-1)break
        outpout.write(buff, 0, readed)
        downloaded += readed
    }
    outpout.flush()
    outpout.close()
}
fun moveFile(file : File,name: String,context: Context):Boolean{
    try {
        val resolver = context.contentResolver
        val value = ContentValues()
        value.put(MediaStore.Video.Media.DISPLAY_NAME,name)
        value.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES+"/")
        value.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,value)
        val fOut = resolver.openOutputStream(Objects.requireNonNull(uri!!))
        val input = FileInputStream(context.contentResolver.openFileDescriptor(uri, "w")!!.getFileDescriptor())
        val siStream = SequenceInputStream(FileInputStream(file), input)
        val buff = ByteArray(4096)
        var downloaded = 0L
        while (true){
            val readed = siStream.read(buff)
            if (readed==-1)break
            fOut!!.write(buff, 0, readed)
            downloaded += readed
        }
        fOut!!.close()
        deleteRecursive(file)
        return true
    }catch (e:java.lang.Exception){
        Toast.makeText(MainActivity.fa, "downloaded error $e",Toast.LENGTH_LONG).show()
        return false
    }
}
fun appendFile(file: File, output: File){
    val outpout = FileOutputStream(output, true)
    val sistream = FileInputStream(file)
    val buff = ByteArray(4096)
    while (true){
        val readed = sistream.read(buff)
        if (readed==-1)break
        outpout.write(buff, 0, readed)
    }
    outpout.flush()
    outpout.close()
}
fun appendFile(files: List<File>, output: File, downloadListener: Xdownload.DownloadTaskListener?, totalLength: Long): Boolean {
    output.delete()
    val outpout = FileOutputStream(output, true)
    val buff = ByteArray(4096)
    val maxPart = files.size
    var offset = 0L
    for ((i, file) in files.withIndex()){
        val sistream = FileInputStream(file)
        while (true){
            val readed = sistream.read(buff)
            offset += readed
            Handler(Looper.getMainLooper()).post{
                downloadListener?.onDownloadAppendFiles(maxPart, i, offset, totalLength)
            }
            if (readed==-1)break
            outpout.write(buff, 0, readed)
        }
    }
    outpout.flush()
    outpout.close()
    Handler(Looper.getMainLooper()).post{
        downloadListener?.onDownloadAppendFiles(maxPart, maxPart, totalLength, totalLength)
    }
    return true
}
fun safeDirName(string: String) :String {
    /*
    val a = string.replace(" ", "")
            .replace(":", "").replace("\"", "")
            .replace("/", "")
            .replace("[", "").replace(".", "")
            .replace("{", "").replace("}", "")
            .replace("]", "").replace("?", "").replace("!", "")
            .replace("$", "")

     */
    val a = getFileNameFromURL(string)
    if (a.length>20)
        return a.substring(0,20)
    return a
}
fun getFileNameFromURL(url: String?): String {
    if (url == null) {
        return ""
    }
    try {
        val resource = URL(url)
        val host: String = resource.getHost()
        if (host.length > 0 && url.endsWith(host)) {
            // handle ...example.com
            return ""
        }
    } catch (e: MalformedURLException) {
        return ""
    }
    val startIndex = url.lastIndexOf('/') + 1
    val length = url.length

    // find end index for ?
    var lastQMPos = url.lastIndexOf('?')
    if (lastQMPos == -1) {
        lastQMPos = length
    }

    // find end index for #
    var lastHashPos = url.lastIndexOf('#')
    if (lastHashPos == -1) {
        lastHashPos = length
    }

    // calculate the end index
    val endIndex = Math.min(lastQMPos, lastHashPos)
    return url.substring(startIndex, endIndex)

}
fun getPathFile(dir : String,context: Context): String{
    if (Build.VERSION.SDK_INT<29){
        return dir
    }
    val localStorage = context.getExternalFilesDir(dir) ?: return "null"
    return localStorage.absolutePath+"/"+dir
}
fun getRealSizeFromUri(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Video.Media.SIZE)
        cursor = context.contentResolver.query(uri, proj, null, null, null)
        val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        cursor.moveToFirst()
        cursor.getString(column_index)
    } finally {
        cursor?.close()
    }
}
fun deleteRecursive(fileOrDirectory: File) {
    try {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()) deleteRecursive(child)
    }catch (e:Exception){}
    fileOrDirectory.delete()
}
