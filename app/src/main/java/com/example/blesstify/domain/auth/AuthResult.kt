package com.example.blesstify.domain.auth

import com.example.blesstify.core.error.AppError

sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val userId: String) : AuthResult()
    object SignedOut : AuthResult()
    data class Error(val error: AppError) : AuthResult()
}

