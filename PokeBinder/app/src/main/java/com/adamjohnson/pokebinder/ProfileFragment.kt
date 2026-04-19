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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.ui.detail.PageDetailFragment
import com.adamjohnson.pokebinder.ui.feed.FeedAdapter
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.ProfileViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var feedAdapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayNameTextView: TextView = view.findViewById(R.id.displayNameTextView)
        val signOutButton: Button = view.findViewById(R.id.signOutButton)
        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val profileRecyclerView: RecyclerView = view.findViewById(R.id.profileRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        feedAdapter = FeedAdapter { binderPage ->
            openPublishedPageDetail(binderPage)
        }

        profileRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        profileRecyclerView.adapter = feedAdapter

        signOutButton.setOnClickListener {
            signOutButton.isEnabled = false
            profileViewModel.signOut()
        }

        profileViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user == null) {
                navigateToLogin()
            } else {
                renderUser(
                    user = user,
                    displayNameTextView = displayNameTextView,
                    signOutButton = signOutButton
                )
            }
        }

        profileViewModel.publishedPagesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        resultCountTextView = resultCountTextView,
                        profileRecyclerView = profileRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        publishedPages = state.data,
                        resultCountTextView = resultCountTextView,
                        profileRecyclerView = profileRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }

                is UiState.Error -> {
                    renderError(
                        message = state.message,
                        resultCountTextView = resultCountTextView,
                        profileRecyclerView = profileRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadProfile()
    }

    private fun renderUser(
        user: User,
        displayNameTextView: TextView,
        signOutButton: Button
    ) {
        displayNameTextView.text = user.displayName.ifBlank {
            getString(R.string.profile_display_name_fallback)
        }
        signOutButton.isEnabled = true
    }

    private fun openPublishedPageDetail(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        if (findNavController().currentDestination?.id == R.id.profileFragment) {
            findNavController().navigate(
                R.id.pageDetailFragment,
                bundleOf(PageDetailFragment.ARG_PAGE_ID to binderPage.pageId)
            )
        }
    }

    private fun renderLoading(
        resultCountTextView: TextView,
        profileRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        resultCountTextView.text = getString(R.string.profile_results_default)
        profileRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        publishedPages: List<BinderPage>,
        resultCountTextView: TextView,
        profileRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        feedAdapter.submitList(publishedPages)
        resultCountTextView.text = getString(R.string.profile_results_label, publishedPages.size)
        loadingView.visibility = View.GONE

        if (publishedPages.isEmpty()) {
            profileRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.profile_empty_title)
            emptyMessageTextView.text = getString(R.string.profile_empty_message)
        } else {
            profileRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun renderError(
        message: String,
        resultCountTextView: TextView,
        profileRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        feedAdapter.submitList(emptyList())
        resultCountTextView.text = getString(R.string.profile_results_default)
        profileRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.profile_error_title)
        emptyMessageTextView.text = message
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