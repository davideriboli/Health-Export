package com.healthexport.data.google

sealed class GoogleSignInResult {
    data class Success(
        val email: String,
        val displayName: String?,
    ) : GoogleSignInResult()

    data class Error(val message: String) : GoogleSignInResult()

    /** User dismissed the account picker without selecting an account. */
    data object Cancelled : GoogleSignInResult()
}
