package com.example.blesstify.core.error

sealed class AppError : Exception() {
    object Network : AppError()
    object Unauthorized : AppError()
    object NotFound : AppError()
    object PermissionDenied : AppError()
    object InvalidCredentials : AppError()
    object EmailAlreadyInUse : AppError()
    object WeakPassword : AppError()
    object InvalidData : AppError()
    object QuotaExceeded : AppError()
    data class Unknown(override val message: String? = null) : AppError()
}
