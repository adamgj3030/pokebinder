package com.adamjohnson.pokebinder

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
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
                R.id.draftsFragment,
                R.id.profileFragment -> {
                    navigateToTopLevelDestination(navController, item.itemId)
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

        enforceAuthEntryDestination(navController)
        updateBottomNavigation(navController.currentDestination?.id)
    }

    private fun navigateToTopLevelDestination(
        navController: NavController,
        destinationId: Int
    ) {
        if (navController.currentDestination?.id == destinationId) {
            return
        }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.feedFragment, false)
            .setLaunchSingleTop(true)
            .build()

        navController.navigate(destinationId, null, navOptions)
    }

    private fun enforceAuthEntryDestination(navController: NavController) {
        val currentDestinationId = navController.currentDestination?.id ?: return

        if (authRepository.isSignedIn()) {
            if (currentDestinationId == R.id.loginFragment) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .setLaunchSingleTop(true)
                    .build()

                navController.navigate(R.id.feedFragment, null, navOptions)
            }
        } else {
            if (currentDestinationId != R.id.loginFragment) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .setLaunchSingleTop(true)
                    .build()

                navController.navigate(R.id.loginFragment, null, navOptions)
            }
        }
    }

    private fun updateBottomNavigation(destinationId: Int?) {
        when (destinationId) {
            R.id.feedFragment,
            R.id.collectionFragment,
            R.id.draftsFragment,
            R.id.profileFragment -> {
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.menu.findItem(destinationId)?.isChecked = true
            }

            R.id.cardSearchFragment -> {
                bottomNavigationView.visibility = View.VISIBLE
                bottomNavigationView.menu.findItem(R.id.collectionFragment)?.isChecked = true
            }

            R.id.loginFragment,
            R.id.binderEditorFragment,
            R.id.pageDetailFragment,
            R.id.socialPagesFragment -> {
                bottomNavigationView.visibility = View.GONE
            }

            else -> {
                bottomNavigationView.visibility = View.GONE
            }
        }
    }
}