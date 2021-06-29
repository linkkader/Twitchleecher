package com.example.twitchleecher

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.FragmentTransaction
import com.example.twitchleecher.ui.fragment.FragmentDownload
import com.example.twitchleecher.ui.fragment.FragmentGames
import com.example.twitchleecher.ui.fragment.FragmentSearch
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    companion object{
        lateinit var fa: AppCompatActivity
        lateinit var shareHeader : SharedPreferences
        var mainContainerId : Int = 0
        var mainContainerId2 : Int = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shareHeader = getSharedPreferences("shareheader",Context.MODE_PRIVATE)
        fa = this

        val mainContainer = findViewById<FrameLayout>(R.id.mainContainer)
        mainContainerId = mainContainer.id
        val mainContainer2 = findViewById<FrameLayout>(R.id.mainContainer2)
        mainContainerId2 = mainContainer2.id
        val nav = findViewById<BottomNavigationView>(R.id.nav)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(mainContainer.id, FragmentSearch() , "fragmentSearch")
        transaction.add(mainContainer.id, FragmentDownload() , "fragmentDownload")
        transaction.add(mainContainer.id, FragmentGames() , "fragmentGame")
        transaction.commitAllowingStateLoss()

        nav.setOnNavigationItemSelectedListener {
            val support = supportFragmentManager
            val transaction = support.beginTransaction()
            when(it.itemId){
                R.id.games -> {
                    hide(transaction)
                    val f = support.findFragmentByTag("fragmentGame")
                    if (f!=null){
                        transaction.show(f)
                        transaction.commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                    transaction.add(mainContainer.id, FragmentGames() , "fragmentGame")
                    transaction.commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.download -> {
                    hide(transaction)
                    val f = support.findFragmentByTag("fragmentDownload")
                    if (f!=null){
                        transaction.show(f)
                        transaction.commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                    transaction.add(mainContainer.id, FragmentDownload() , "fragmentDownload")
                    transaction.commit()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.search -> {
                    hide(transaction)
                    val f = support.findFragmentByTag("fragmentSearch")
                    if (f!=null){
                        transaction.show(f)
                        transaction.commit()
                        return@setOnNavigationItemSelectedListener true
                    }
                    transaction.add(mainContainer.id, FragmentSearch() , "fragmentSearch")
                    transaction.commit()
                    return@setOnNavigationItemSelectedListener true
                }
            }

            return@setOnNavigationItemSelectedListener true
        }

    }
    fun hide(transation : FragmentTransaction){
        var f = supportFragmentManager.findFragmentByTag("fragmentDownload")
        if (f!=null) transation.hide(f)
        f = supportFragmentManager.findFragmentByTag("fragmentSearch")
        if (f!=null) transation.hide(f)
        f = supportFragmentManager.findFragmentByTag("fragmentGame")
        if (f!=null) transation.hide(f)
    }
}