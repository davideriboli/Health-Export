package com.healthexport.data.sheets

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All interactions with the Google Sheets API.
 *
 * Every method is a suspend function that executes blocking IO on [Dispatchers.IO].
 * Callers should catch:
 * - [com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException]
 *   → user has not authorised Sheets access; launch `exception.intent` to prompt them.
 * - [com.google.api.client.googleapis.json.GoogleJsonResponseException]
 *   → Sheets API returned an error (rate-limit, permission denied, …).
 * - [java.io.IOException] → network error.
 */
@Singleton
class GoogleSheetsRepository @Inject constructor(
    private val client: GoogleSheetsClient,
) {

    // ── Spreadsheet ───────────────────────────────────────────────────────

    /** Creates a brand-new spreadsheet and returns its ID. */
    suspend fun createSpreadsheet(
        title: String,
        accountEmail: String,
    ): String = withContext(Dispatchers.IO) {
        val spreadsheet = Spreadsheet().setProperties(
            SpreadsheetProperties().setTitle(title)
        )
        val service = client.build(accountEmail)
        service.spreadsheets().create(spreadsheet)
            .setFields("spreadsheetId")
            .execute()
            .spreadsheetId
    }

    /** Returns the list of existing sheet (tab) titles in a spreadsheet. */
    private suspend fun getSheetTitles(
        service: Sheets,
        spreadsheetId: String,
    ): Set<String> = withContext(Dispatchers.IO) {
        service.spreadsheets().get(spreadsheetId)
            .setFields("sheets.properties.title")
            .execute()
            .sheets
            ?.mapNotNull { it.properties?.title }
            ?.toSet()
            ?: emptySet()
    }

    // ── Tab management ────────────────────────────────────────────────────

    /**
     * Ensures a sheet (tab) named [sheetTitle] exists and has [headers] as its first row.
     *
     * - If the sheet does not exist: creates it and writes the header row.
     * - If the sheet exists but has no headers: writes the header row.
     * - If the sheet exists with headers already: does nothing (idempotent).
     */
    suspend fun ensureSheetExists(
        spreadsheetId: String,
        sheetTitle: String,
        headers: List<String>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        val service    = client.build(accountEmail)
        val existing   = getSheetTitles(service, spreadsheetId)

        if (sheetTitle !in existing) {
            // Create the tab
            val addRequest = Request().setAddSheet(
                AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
            )
            service.spreadsheets()
                .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(addRequest)))
                .execute()

            // Write headers
            writeRange(service, spreadsheetId, "$sheetTitle!A1", listOf(headers))
        }
    }

    /** Clears all data in [sheetTitle] (header row included). */
    suspend fun clearSheet(
        spreadsheetId: String,
        sheetTitle: String,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        val service = client.build(accountEmail)
        service.spreadsheets().values()
            .clear(spreadsheetId, sheetTitle, ClearValuesRequest())
            .execute()
    }

    // ── Data writing ──────────────────────────────────────────────────────

    /**
     * Overwrites [sheetTitle] with [headers] + [rows] (clears first, then writes).
     *
     * Use for OVERWRITE export mode.
     */
    suspend fun overwriteSheet(
        spreadsheetId: String,
        sheetTitle: String,
        headers: List<String>,
        rows: List<List<Any?>>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        val service = client.build(accountEmail)
        val existing = getSheetTitles(service, spreadsheetId)

        if (sheetTitle !in existing) {
            val addRequest = Request().setAddSheet(
                AddSheetRequest().setProperties(SheetProperties().setTitle(sheetTitle))
            )
            service.spreadsheets()
                .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(addRequest)))
                .execute()
        } else {
            service.spreadsheets().values()
                .clear(spreadsheetId, sheetTitle, ClearValuesRequest())
                .execute()
        }

        val allRows = listOf(headers) + rows
        writeRange(service, spreadsheetId, "$sheetTitle!A1", allRows)
    }

    /**
     * Appends [rows] after the last occupied row in [sheetTitle].
     *
     * Use for APPEND export mode. Call [ensureSheetExists] first.
     */
    suspend fun appendRows(
        spreadsheetId: String,
        sheetTitle: String,
        rows: List<List<Any?>>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val service = client.build(accountEmail)
        val range   = ValueRange()
            .setRange("$sheetTitle!A1")
            .setValues(rows.map { it.map { v -> v?.toString() } })
        service.spreadsheets().values()
            .append(spreadsheetId, "$sheetTitle!A1", range)
            .setValueInputOption("RAW")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun writeRange(
        service: Sheets,
        spreadsheetId: String,
        range: String,
        rows: List<List<Any?>>,
    ) {
        val body = ValueRange()
            .setRange(range)
            .setValues(rows.map { it.map { v -> v?.toString() } })
        service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }
}
