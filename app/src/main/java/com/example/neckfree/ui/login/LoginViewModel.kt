package com.example.neckfree.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.neckfree.db.AppDatabase
import com.example.neckfree.db.User
import com.example.neckfree.db.UserRepository
import kotlinx.coroutines.launch
import java.security.MessageDigest

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _signUpResult = MutableLiveData<Boolean>()
    val signUpResult: LiveData<Boolean> = _signUpResult

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        val user = repository.getUserByUsername(username)
        if (user != null && user.passwordHash == hashPassword(password)) {
            _loginResult.postValue(LoginResult(success = user))
        } else {
            _loginResult.postValue(LoginResult(error = "Invalid username or password"))
        }
    }

    fun signUp(username: String, password: String) = viewModelScope.launch {
        try {
            val newUser = User(username = username, passwordHash = hashPassword(password))
            repository.insertUser(newUser)
            _signUpResult.postValue(true)
        } catch (e: Exception) {
            // Likely a unique constraint violation (username already exists)
            _signUpResult.postValue(false)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

data class LoginResult(
    val success: User? = null,
    val error: String? = null
)
