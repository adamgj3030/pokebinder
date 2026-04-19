package com.adamjohnson.pokebinder.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.AuthViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileMessageTextView: TextView = view.findViewById(R.id.profileMessageTextView)
        val signOutButton: Button = view.findViewById(R.id.signOutButton)

        signOutButton.setOnClickListener {
            authViewModel.signOut()
        }

        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    signOutButton.isEnabled = false
                }

                is UiState.Error -> {
                    signOutButton.isEnabled = true
                }

                is UiState.Success -> {
                    if (state.data == null) {
                        navigateToLogin()
                    } else {
                        profileMessageTextView.text =
                            getString(R.string.profile_signed_in_message)
                        signOutButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val navController = NavHostFragment.findNavController(this)

        if (navController.currentDestination?.id != R.id.profileFragment) {
            return
        }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.id, true)
            .setLaunchSingleTop(true)
            .build()

        navController.navigate(R.id.loginFragment, null, navOptions)
    }
}