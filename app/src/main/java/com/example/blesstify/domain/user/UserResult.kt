package com.example.blesstify.domain.user

import com.example.blesstify.core.error.AppError

sealed class UserResult<out T> {
    data class Success<T>(val data: T) : UserResult<T>()
    data class Error(val error: AppError) : UserResult<Nothing>()
}

