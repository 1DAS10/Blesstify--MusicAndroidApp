package com.example.blesstify.domain.usecase

import com.example.blesstify.domain.auth.AuthRepository

class SignInWithFacebookUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(accessToken: String) = authRepository.signInWithFacebook(accessToken)
}
