package com.example.blesstify.data.remote

sealed class NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>()

    data class Success<T>(val data: T) : NetworkResult<T>()

    /** Network / unexpected exception */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : NetworkResult<Nothing>()

    /** Non-2xx response */
    data class HttpError(
        val code: Int,
        val message: String,
        val body: String? = null
    ) : NetworkResult<Nothing>()
}
