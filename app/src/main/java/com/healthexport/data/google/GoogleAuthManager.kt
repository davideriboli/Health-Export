package com.healthexport.data.google

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.healthexport.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GoogleAuthManager"

/**
 * Handles Google Sign-In via Credential Manager (modern replacement for GoogleSignIn API).
 *
 * Requires [BuildConfig.GOOGLE_WEB_CLIENT_ID] — the OAuth 2.0 Web Client ID created
 * in Google Cloud Console for this project.
 *
 * The sign-in result only provides identity (email, display name). The OAuth2 access
 * token for the Sheets API is obtained separately by [GoogleSheetsClient] via
 * [com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential].
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Shows the Google account picker and returns the result.
     *
     * Must be called from a coroutine on the main thread; [activityContext] must be
     * an Activity context so Credential Manager can anchor the bottom sheet.
     */
    suspend fun signIn(activityContext: Context): GoogleSignInResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            Log.e(TAG, "GOOGLE_WEB_CLIENT_ID is empty — set it in local.properties or GitHub Secrets")
            return GoogleSignInResult.Error("GOOGLE_WEB_CLIENT_ID non configurato. Controlla local.properties o i GitHub Secrets.")
        }
        Log.d(TAG, "Starting sign-in with clientId=${clientId.take(20)}…")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false) // show all device accounts, not only pre-authorised
            .setAutoSelectEnabled(false)           // always show the picker
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(
                    email       = googleId.id,
                    displayName = googleId.displayName,
                )
            } else {
                GoogleSignInResult.Error("Tipo di credenziale non supportato")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "NoCredentialException: ${e.message}", e)
            GoogleSignInResult.Error(
                "Nessun account Google trovato. Verifica che:\n" +
                "• Il dispositivo abbia un account Google configurato\n" +
                "• L'SHA-1 del keystore sia registrato in Google Cloud Console\n" +
                "• Il package name nel Cloud Console corrisponda a com.healthexport"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e::class.simpleName}: ${e.message}", e)
            GoogleSignInResult.Error("${e::class.simpleName}: ${e.message}")
        }
    }
}
