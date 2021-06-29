package com.example.twitchleecher.adapters
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twitchleecher.MainActivity
import com.example.twitchleecher.R
import com.example.twitchleecher.glide
import com.example.twitchleecher.model.Game
import com.example.twitchleecher.ui.fragment.FragmentClips

class AdapterGame(val context: Context,val fragmentManager: FragmentManager) : RecyclerView.Adapter<AdapterGame.ViewHolder>() {
    var games = mutableListOf<Game>()
    class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        var img = itemView.findViewById<ImageView>(R.id.img)
        var title = itemView.findViewById<TextView>(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_game,null,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        glide(games[position].boxArtURL.replace("{width}x{height}","300x500"),holder.img,context)
        holder.title.text = games[position].name
        holder.itemView.setOnClickListener {
            fragmentManager.beginTransaction().add(MainActivity.mainContainerId2,FragmentClips(games[position].id)).addToBackStack(null).commitAllowingStateLoss()
        }
    }

    override fun getItemCount() = games.size
    fun set(games: List<Game>) {
        this.games.addAll(games)
        notifyDataSetChanged()
    }
}
