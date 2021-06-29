package com.example.twitchleecher.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitchleecher.MainActivity
import com.example.twitchleecher.R
import com.example.twitchleecher.glide
import com.example.twitchleecher.model.Clip
import com.example.twitchleecher.ui.fragment.FragmentDownloadItem

class AdapterClip(val context: Context,val fragmentManager: FragmentManager) : RecyclerView.Adapter<AdapterClip.ViewHolder>() {
    var clips = mutableListOf<Clip>()
    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        var img = itemView.findViewById<ImageView>(R.id.img)
        var gameImg = itemView.findViewById<ImageView>(R.id.game_img)
        var title = itemView.findViewById<TextView>(R.id.title)
        var download = itemView.findViewById<ImageButton>(R.id.download)
    }

    override fun getItemViewType(position: Int): Int {
        return  position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_clip,null,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        glide(clips[position].thumbnailURL,holder.img,context)
        holder.title.text = clips[position].title
        holder.download.setOnClickListener {
            fragmentManager.beginTransaction().add(MainActivity.mainContainerId2,FragmentDownloadItem(clips[position])).addToBackStack(null).commitAllowingStateLoss()
        }
    }

    override fun getItemCount() = clips.size
    fun set(clips: List<Clip>) {
        this.clips.addAll(clips)
        notifyDataSetChanged()
    }
}
