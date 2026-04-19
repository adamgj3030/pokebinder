package com.adamjohnson.pokebinder.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.AuthViewModel

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEditText: EditText = view.findViewById(R.id.emailEditText)
        val passwordEditText: EditText = view.findViewById(R.id.passwordEditText)
        val signInButton: Button = view.findViewById(R.id.signInButton)
        val createAccountButton: Button = view.findViewById(R.id.createAccountButton)
        val statusTextView: TextView = view.findViewById(R.id.statusTextView)

        signInButton.setOnClickListener {
            authViewModel.signIn(
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        createAccountButton.setOnClickListener {
            authViewModel.createAccount(
                emailEditText.text.toString(),
                passwordEditText.text.toString()
            )
        }

        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    setLoadingState(
                        isLoading = true,
                        emailEditText = emailEditText,
                        passwordEditText = passwordEditText,
                        signInButton = signInButton,
                        createAccountButton = createAccountButton
                    )
                    statusTextView.text = getString(R.string.auth_loading)
                    statusTextView.visibility = View.VISIBLE
                }

                is UiState.Error -> {
                    setLoadingState(
                        isLoading = false,
                        emailEditText = emailEditText,
                        passwordEditText = passwordEditText,
                        signInButton = signInButton,
                        createAccountButton = createAccountButton
                    )
                    statusTextView.text = state.message
                    statusTextView.visibility = View.VISIBLE
                }

                is UiState.Success -> {
                    setLoadingState(
                        isLoading = false,
                        emailEditText = emailEditText,
                        passwordEditText = passwordEditText,
                        signInButton = signInButton,
                        createAccountButton = createAccountButton
                    )
                    handleAuthSuccess(state.data, statusTextView)
                }
            }
        }
    }

    private fun handleAuthSuccess(user: User?, statusTextView: TextView) {
        if (user == null) {
            statusTextView.text = ""
            statusTextView.visibility = View.GONE
            return
        }

        val navController = NavHostFragment.findNavController(this)

        if (navController.currentDestination?.id != R.id.loginFragment) {
            return
        }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, true)
            .setLaunchSingleTop(true)
            .build()

        navController.navigate(R.id.feedFragment, null, navOptions)
    }

    private fun setLoadingState(
        isLoading: Boolean,
        emailEditText: EditText,
        passwordEditText: EditText,
        signInButton: Button,
        createAccountButton: Button
    ) {
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        signInButton.isEnabled = !isLoading
        createAccountButton.isEnabled = !isLoading
    }
}