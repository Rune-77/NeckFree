package com.example.neckfree.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.neckfree.R
import com.example.neckfree.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            if (result.success != null) {
                // Save user ID to SharedPreferences
                val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) ?: return@observe
                with (sharedPref.edit()) {
                    putLong("logged_in_user_id", result.success.id)
                    apply()
                }
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_main_content)
            } else {
                Toast.makeText(context, result.error, Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) {
                viewModel.login(username, password)
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToSignUpTextView.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
