package com.healthexport.data.sheets

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory that builds a [Sheets] API service instance bound to a specific Google account.
 *
 * The credential uses [ExponentialBackOff] to automatically retry on transient failures.
 * The first API call may throw [com.google.api.client.googleapis.extensions.android.gms.auth
 * .UserRecoverableAuthIOException] if the user has not yet granted Sheets access — callers
 * must catch this and launch the recovery intent included in the exception.
 */
@Singleton
class GoogleSheetsClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val transport    = NetHttpTransport()
    private val jsonFactory  = GsonFactory.getDefaultInstance()

    fun build(accountEmail: String): Sheets {
        // setSelectedAccountName() in google-api-client-android 2.7.0 sets the String field
        // but getSelectedAccountName() reads from selectedAccount.name (the Account object).
        // getToken() internally uses the Account object → selectedAccountName comes back null.
        // Fix: bypass the string setter and pass the Account object directly.
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(SheetsScopes.SPREADSHEETS))
            .setBackOff(ExponentialBackOff())
            .setSelectedAccount(Account(accountEmail, "com.google"))
        Log.d(TAG, "build() email='$accountEmail' selectedAccountName='${credential.selectedAccountName}'")

        return Sheets.Builder(transport, jsonFactory, credential)
            .setApplicationName("HealthExport")
            .build()
    }

    companion object {
        private const val TAG = "GoogleSheetsClient"
    }
}
