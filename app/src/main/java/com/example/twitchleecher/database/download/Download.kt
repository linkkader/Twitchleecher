package com.example.twitchleecher.database.download

import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "download")
data class Download (var name:String="", var episode:String="", var header:String ="", var downloadurl : String=""
                     , var url : String="", var state :String="",@PrimaryKey(autoGenerate = true)  var id:Int=0)
@Dao
interface DownloadDao{
    @Query("SELECT * From download  order by id DESC")
    fun getAll(): LiveData<List<Download>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(download : Download)
    @Query("DELETE from download WHERE id = :id ")
    fun delete(id : Int)
    @Query("Update download set state =:state WHERE id = :id ")
    fun updateState(state :String,id : Int)
    //@Query("DELETE FROM download15")
    //fun deleteAll()
}
class DownloadRepository(private val dao: DownloadDao) {
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(download: Download) {
        dao.insert(download)
    }
    //@Suppress("RedundantSuspendModifier")
    //@WorkerThread
    //suspend fun deleteAll() {
    //dao.deleteAll()
    //}
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateState(state:String,id:Int) {
        dao.updateState(state,id)
    }@Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun delete(id:Int) {
        dao.delete(id)
    }

    val all : LiveData<List<Download>> = dao.getAll()

}

@Database(entities = [Download::class], version = 4)
abstract class DownloadRoomDatabase : RoomDatabase() {
    abstract fun dao() : DownloadDao
    companion object{
        @Volatile
        private var INSTANCE: DownloadRoomDatabase? = null
        fun getDatabase(context : Context, scope : CoroutineScope) : DownloadRoomDatabase? {
            return INSTANCE ?: synchronized(this) {
                val i = Room.databaseBuilder(context.applicationContext, DownloadRoomDatabase::class.java, "download15")
                    .fallbackToDestructiveMigration().build()
                INSTANCE = i
                INSTANCE
            }
        }
    }
}
class DownloadViewModel(application: Application): AndroidViewModel(application) {
    private val repository : DownloadRepository
    val all : LiveData<List<Download>>
    init {
        val dao = DownloadRoomDatabase.getDatabase(application, viewModelScope)!!.dao()
        repository = DownloadRepository(dao)
        all = repository.all
    }
    fun insert(download: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(download)
    }
    //  fun deleteAll()= viewModelScope.launch(Dispatchers.IO){
    //    repository.deleteAll()
    //}
    fun updateState(state: String,id:Int)= viewModelScope.launch(Dispatchers.IO){
        repository.updateState(state,id)
    }
    fun delete(id:Int)= viewModelScope.launch(Dispatchers.IO){
        repository.delete(id)
    }

}