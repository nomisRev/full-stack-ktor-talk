package org.jetbrains.demo.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleTokens(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)

@Serializable
data class UserInfo(
    val email: String,
    val picture: String?
)

@Serializable
data class AuthRequest(
    val idToken: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val user: UserInfo?,
    val message: String?
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)