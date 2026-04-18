package com.adamjohnson.pokebinder

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.feedFragment,
                R.id.collectionFragment,
                R.id.profileFragment -> {
                    if (navController.currentDestination?.id != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                    true
                }

                R.id.binderEditorFragment -> {
                    if (navController.currentDestination?.id != R.id.binderEditorFragment) {
                        navController.navigate(R.id.binderEditorFragment)
                    }
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateBottomNavigation(destination.id)
        }

        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            val startDestination = if (authRepository.isSignedIn()) {
                R.id.feedFragment
            } else {
                R.id.loginFragment
            }
            navGraph.setStartDestination(startDestination)
            navController.graph = navGraph
        }

        updateBottomNavigation(navController.currentDestination?.id)
    }

    private fun updateBottomNavigation(destinationId: Int?) {
        when (destinationId) {
            R.id.feedFragment,
            R.id.collectionFragment,
            R.id.profileFragment -> {
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.menu.findItem(destinationId)?.isChecked = true
            }

            R.id.loginFragment,
            R.id.cardSearchFragment,
            R.id.binderEditorFragment,
            R.id.draftsFragment,
            R.id.pageDetailFragment -> {
                bottomNavigationView.visibility = View.GONE
            }

            else -> {
                bottomNavigationView.visibility = View.GONE
            }
        }
    }
}