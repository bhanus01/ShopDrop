package com.example.shopdrop.domain.user_case.auth_user

import com.example.shopdrop.common.Resource
import com.example.shopdrop.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): Resource<FirebaseUser?> =
        authRepository.login(email, password)
}