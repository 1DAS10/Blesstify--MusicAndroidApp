# Blesstify - Firebase Repository Layer

This module adds a Firebase-backed repository layer and use-case wrappers for the Music App.

## Structure

- `app/src/main/java/com/example/blesstify/data/repository`: repository interfaces (data layer)
- `app/src/main/java/com/example/blesstify/data/repository/firebase`: Firebase implementations
- `app/src/main/java/com/example/blesstify/domain/auth`: auth repository contract (domain)
- `app/src/main/java/com/example/blesstify/domain/usecase`: use cases
- `app/src/test/java/com/example/blesstify/domain/usecase`: unit tests

## Auth Module

- Hilt DI provides `FirebaseAuth` and `AuthRepository` via `data/di/AuthModule.kt`.
- Auth flows expose `Flow<AuthResult>` and `Flow<AuthState>` via `domain/auth`.
- UI consumes auth state via `presentation/auth/AuthViewModel.kt`.

## Quick Test

```bash
./gradlew :app:testDebugUnitTest
```
