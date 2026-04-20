package com.adamjohnson.pokebinder.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.ui.feed.SocialPagesFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.ProfileViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signedInMessageTextView: TextView = view.findViewById(R.id.signedInMessageTextView)
        val displayNameTextView: TextView = view.findViewById(R.id.displayNameTextView)
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val publishedPagesCountTextView: TextView =
            view.findViewById(R.id.publishedPagesCountTextView)
        val likesReceivedCountTextView: TextView =
            view.findViewById(R.id.likesReceivedCountTextView)
        val commentsReceivedCountTextView: TextView =
            view.findViewById(R.id.commentsReceivedCountTextView)
        val profileMessageTextView: TextView = view.findViewById(R.id.profileMessageTextView)
        val yourLikesButton: Button = view.findViewById(R.id.yourLikesButton)
        val yourCommentsButton: Button = view.findViewById(R.id.yourCommentsButton)
        val signOutButton: Button = view.findViewById(R.id.signOutButton)
        val loadingView: View = view.findViewById(R.id.loadingView)

        yourLikesButton.setOnClickListener {
            openSocialPages(SocialPagesFragment.MODE_LIKES)
        }

        yourCommentsButton.setOnClickListener {
            openSocialPages(SocialPagesFragment.MODE_COMMENTS)
        }

        signOutButton.setOnClickListener {
            profileViewModel.signOut()
            navigateToLogin()
        }

        profileViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            renderCurrentUser(
                user = user,
                signedInMessageTextView = signedInMessageTextView,
                displayNameTextView = displayNameTextView,
                emailTextView = emailTextView,
                yourLikesButton = yourLikesButton,
                yourCommentsButton = yourCommentsButton,
                signOutButton = signOutButton
            )
        }

        profileViewModel.summaryState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        publishedPagesCountTextView = publishedPagesCountTextView,
                        likesReceivedCountTextView = likesReceivedCountTextView,
                        commentsReceivedCountTextView = commentsReceivedCountTextView,
                        profileMessageTextView = profileMessageTextView,
                        yourLikesButton = yourLikesButton,
                        yourCommentsButton = yourCommentsButton,
                        loadingView = loadingView
                    )
                }

                is UiState.Success -> {
                    renderSummary(
                        summary = state.data,
                        publishedPagesCountTextView = publishedPagesCountTextView,
                        likesReceivedCountTextView = likesReceivedCountTextView,
                        commentsReceivedCountTextView = commentsReceivedCountTextView,
                        profileMessageTextView = profileMessageTextView,
                        yourLikesButton = yourLikesButton,
                        yourCommentsButton = yourCommentsButton,
                        loadingView = loadingView
                    )
                }

                is UiState.Error -> {
                    renderSummaryError(
                        message = state.message,
                        publishedPagesCountTextView = publishedPagesCountTextView,
                        likesReceivedCountTextView = likesReceivedCountTextView,
                        commentsReceivedCountTextView = commentsReceivedCountTextView,
                        profileMessageTextView = profileMessageTextView,
                        yourLikesButton = yourLikesButton,
                        yourCommentsButton = yourCommentsButton,
                        loadingView = loadingView
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadProfile()
    }

    private fun renderCurrentUser(
        user: User?,
        signedInMessageTextView: TextView,
        displayNameTextView: TextView,
        emailTextView: TextView,
        yourLikesButton: Button,
        yourCommentsButton: Button,
        signOutButton: Button
    ) {
        if (user == null || user.userId.isBlank()) {
            signedInMessageTextView.text = "You must be signed in to view your profile."
            displayNameTextView.text = "PokeBinder User"
            emailTextView.text = ""
            emailTextView.visibility = View.GONE
            yourLikesButton.isEnabled = false
            yourCommentsButton.isEnabled = false
            signOutButton.isEnabled = false
            return
        }

        val username = user.username.trim().ifBlank { "PokeBinder User" }
        val email = user.email.trim()

        signedInMessageTextView.text = if (email.isBlank()) {
            "You are signed into PokeBinder as $username."
        } else {
            "You are signed into PokeBinder as $username ($email)."
        }

        displayNameTextView.text = username

        if (email.isBlank()) {
            emailTextView.text = ""
            emailTextView.visibility = View.GONE
        } else {
            emailTextView.text = "Email: $email"
            emailTextView.visibility = View.VISIBLE
        }

        yourLikesButton.isEnabled = true
        yourCommentsButton.isEnabled = true
        signOutButton.isEnabled = true
    }

    private fun renderLoading(
        publishedPagesCountTextView: TextView,
        likesReceivedCountTextView: TextView,
        commentsReceivedCountTextView: TextView,
        profileMessageTextView: TextView,
        yourLikesButton: Button,
        yourCommentsButton: Button,
        loadingView: View
    ) {
        publishedPagesCountTextView.text = "0"
        likesReceivedCountTextView.text = "0"
        commentsReceivedCountTextView.text = "0"
        profileMessageTextView.visibility = View.GONE
        yourLikesButton.isEnabled = false
        yourCommentsButton.isEnabled = false
        loadingView.visibility = View.VISIBLE
    }

    private fun renderSummary(
        summary: ProfileViewModel.ProfileSummary,
        publishedPagesCountTextView: TextView,
        likesReceivedCountTextView: TextView,
        commentsReceivedCountTextView: TextView,
        profileMessageTextView: TextView,
        yourLikesButton: Button,
        yourCommentsButton: Button,
        loadingView: View
    ) {
        publishedPagesCountTextView.text = summary.publishedPageCount.toString()
        likesReceivedCountTextView.text = summary.likesReceivedCount.toString()
        commentsReceivedCountTextView.text = summary.commentsReceivedCount.toString()
        profileMessageTextView.visibility = View.GONE
        yourLikesButton.isEnabled = true
        yourCommentsButton.isEnabled = true
        loadingView.visibility = View.GONE
    }

    private fun renderSummaryError(
        message: String,
        publishedPagesCountTextView: TextView,
        likesReceivedCountTextView: TextView,
        commentsReceivedCountTextView: TextView,
        profileMessageTextView: TextView,
        yourLikesButton: Button,
        yourCommentsButton: Button,
        loadingView: View
    ) {
        publishedPagesCountTextView.text = "0"
        likesReceivedCountTextView.text = "0"
        commentsReceivedCountTextView.text = "0"
        profileMessageTextView.text = message
        profileMessageTextView.visibility = View.VISIBLE
        yourLikesButton.isEnabled = true
        yourCommentsButton.isEnabled = true
        loadingView.visibility = View.GONE
    }

    private fun openSocialPages(mode: String) {
        if (findNavController().currentDestination?.id == R.id.profileFragment) {
            findNavController().navigate(
                R.id.socialPagesFragment,
                bundleOf(SocialPagesFragment.ARG_LIST_MODE to mode)
            )
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