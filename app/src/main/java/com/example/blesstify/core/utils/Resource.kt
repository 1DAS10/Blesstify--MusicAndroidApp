package com.example.blesstify.core.utils

import com.example.blesstify.core.error.AppError

sealed class Resource<T>(val data: T? = null, val error: AppError? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(error: AppError, data: T? = null) : Resource<T>(data, error)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
