package com.thing.appbizsdk.familybiz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.Navigation.findNavController
import com.thing.appbizsdk.familybiz.databinding.ActivityFamilyBinding
import com.thing.appbizsdk.familybiz.model.FamilyManagerModel

class FamilyManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFamilyBinding

    private val viewModel: FamilyManagerModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFamilyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener {
            if(!findNavController(this,R.id.nav_host_fragment).popBackStack()){
                finish()
            }
        }
        initNav()
    }

    private fun initNav() {
        val navController = findNavController(this,R.id.nav_host_fragment)
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        graph.startDestination = R.id.fragment_family_list
        navController.setGraph(graph, intent.extras)
    }

}