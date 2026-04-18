package com.adamjohnson.pokebinder

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "pokebinder_prefs"
        private const val KEY_IS_SIGNED_IN = "is_signed_in"
    }

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            val startDestination = if (isUserSignedIn()) {
                R.id.feedFragment
            } else {
                R.id.loginFragment
            }
            navGraph.setStartDestination(startDestination)
            navController.graph = navGraph
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

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
                    navController.navigate(R.id.binderEditorFragment)
                    true
                }

                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.cardSearchFragment,
                R.id.binderEditorFragment,
                R.id.draftsFragment,
                R.id.pageDetailFragment -> {
                    bottomNavigationView.visibility = View.GONE
                }

                R.id.feedFragment,
                R.id.collectionFragment,
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    bottomNavigationView.menu.findItem(destination.id)?.isChecked = true
                }

                else -> {
                    bottomNavigationView.visibility = View.GONE
                }
            }
        }
    }

    private fun isUserSignedIn(): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_IS_SIGNED_IN, false)
    }
}