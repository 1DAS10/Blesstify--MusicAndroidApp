package com.example.blesstify.data.di

import com.example.blesstify.domain.auth.AuthRepository
import com.example.blesstify.data.repository.FirebaseAuthRepository
import com.example.blesstify.domain.usecase.*
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository =
        FirebaseAuthRepository(auth)

    @Provides
    fun provideSignInUseCase(authRepository: AuthRepository): SignInUseCase =
        SignInUseCase(authRepository)

    @Provides
    fun provideSignInWithGoogleUseCase(authRepository: AuthRepository): SignInWithGoogleUseCase =
        SignInWithGoogleUseCase(authRepository)

    @Provides
    fun provideSignInWithFacebookUseCase(authRepository: AuthRepository): SignInWithFacebookUseCase =
        SignInWithFacebookUseCase(authRepository)

    @Provides
    fun provideSignUpUseCase(authRepository: AuthRepository): SignUpUseCase =
        SignUpUseCase(authRepository)

    @Provides
    fun provideSignOutUseCase(authRepository: AuthRepository): SignOutUseCase =
        SignOutUseCase(authRepository)

    @Provides
    fun provideObserveAuthStateUseCase(authRepository: AuthRepository): ObserveAuthStateUseCase =
        ObserveAuthStateUseCase(authRepository)

    @Provides
    fun provideGetCurrentUserUseCase(authRepository: AuthRepository): GetCurrentUserUseCase =
        GetCurrentUserUseCase(authRepository)
}
