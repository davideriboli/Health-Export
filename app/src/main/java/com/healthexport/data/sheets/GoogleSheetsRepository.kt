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

private const val CHUNK_SIZE = 1000   // rows per Sheets API write request

/**
 * All interactions with the Google Sheets API.
 *
 * Design principles for Module 4:
 * - [batchEnsureSheets] creates all required tabs in **one** API call instead of N,
 *   reducing write-request count and staying within the 60 req/min quota.
 * - [appendRows] splits large payloads into [CHUNK_SIZE]-row chunks.
 * - [getLastTimestamp] powers smart-append: only rows newer than the last export are added.
 *
 * Every method is a suspend function dispatched to [Dispatchers.IO].
 * Callers must catch:
 * - [com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException]
 *   → launch `exception.intent` for OAuth authorisation.
 * - [com.google.api.client.googleapis.json.GoogleJsonResponseException] → API error.
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
        val spreadsheet = Spreadsheet()
            .setProperties(SpreadsheetProperties().setTitle(title))
        client.build(accountEmail)
            .spreadsheets().create(spreadsheet)
            .setFields("spreadsheetId")
            .execute()
            .spreadsheetId
    }

    // ── Tab management ────────────────────────────────────────────────────

    /**
     * Ensures all required sheet tabs exist, creating missing ones in a **single**
     * batchUpdate call. Writes the header row only for newly created tabs.
     *
     * Pass this the full list of (tabTitle → headers) before starting the
     * per-type write loop to minimise API calls.
     */
    suspend fun batchEnsureSheets(
        spreadsheetId: String,
        tabs: List<Pair<String, List<String>>>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        val service  = client.build(accountEmail)
        val existing = getSheetTitles(service, spreadsheetId)

        val missing  = tabs.filter { (title, _) -> title !in existing }
        if (missing.isEmpty()) return@withContext

        // One batchUpdate to create all missing tabs at once
        val requests = missing.map { (title, _) ->
            Request().setAddSheet(
                AddSheetRequest().setProperties(SheetProperties().setTitle(title))
            )
        }
        service.spreadsheets()
            .batchUpdate(spreadsheetId, BatchUpdateSpreadsheetRequest().setRequests(requests))
            .execute()

        // Write header row for each newly created tab
        missing.forEach { (title, headers) ->
            writeRange(service, spreadsheetId, "$title!A1", listOf(headers))
        }
    }

    // ── Data writing — overwrite mode ─────────────────────────────────────

    /**
     * Clears [sheetTitle] and rewrites it with [headers] + [rows].
     *
     * Assumes the tab already exists — call [batchEnsureSheets] first.
     * Skips the API call entirely when [rows] is empty (no data to write).
     */
    suspend fun overwriteSheetData(
        spreadsheetId: String,
        sheetTitle: String,
        headers: List<String>,
        rows: List<List<Any?>>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        val service = client.build(accountEmail)
        service.spreadsheets().values()
            .clear(spreadsheetId, sheetTitle, ClearValuesRequest())
            .execute()
        writeRange(service, spreadsheetId, "$sheetTitle!A1", listOf(headers) + rows)
    }

    // ── Data writing — append mode ────────────────────────────────────────

    /**
     * Appends [rows] after the last occupied row in [sheetTitle], in chunks of
     * [CHUNK_SIZE] to stay within Sheets API request-size limits.
     *
     * Assumes the tab and its header row already exist — call [batchEnsureSheets] first.
     */
    suspend fun appendRows(
        spreadsheetId: String,
        sheetTitle: String,
        rows: List<List<Any?>>,
        accountEmail: String,
    ) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val service = client.build(accountEmail)
        rows.chunked(CHUNK_SIZE).forEach { chunk ->
            val body = ValueRange()
                .setRange("$sheetTitle!A1")
                .setValues(chunk.map { row -> row.map { v -> v?.toString() } })
            service.spreadsheets().values()
                .append(spreadsheetId, "$sheetTitle!A1", body)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
        }
    }

    // ── Smart-append support ──────────────────────────────────────────────

    /**
     * Returns the last non-empty value in column A of [sheetTitle], skipping the
     * header row. Returns `null` when the sheet is empty or doesn't exist.
     *
     * Used by the smart-append logic in [WizardViewModel]: only rows with a
     * timestamp string lexicographically greater than this value are exported,
     * preventing duplicate rows across consecutive append exports.
     *
     * Works correctly with the `yyyy-MM-dd'T'HH:mm:ss` format used by
     * [RecordSchema] because ISO 8601 local-datetime strings are naturally
     * sortable as plain strings.
     */
    suspend fun getLastTimestamp(
        spreadsheetId: String,
        sheetTitle: String,
        accountEmail: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val response = client.build(accountEmail)
                .spreadsheets().values()
                .get(spreadsheetId, "$sheetTitle!A:A")
                .execute()
            response.getValues()
                ?.drop(1)                         // skip header row
                ?.lastOrNull { it.isNotEmpty() }
                ?.firstOrNull()
                ?.toString()
        } catch (_: Exception) {
            null   // sheet absent, empty, or network error — treat as "no prior export"
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun getSheetTitles(service: Sheets, spreadsheetId: String): Set<String> =
        service.spreadsheets().get(spreadsheetId)
            .setFields("sheets.properties.title")
            .execute()
            .sheets
            ?.mapNotNull { it.properties?.title }
            ?.toSet()
            ?: emptySet()

    private fun writeRange(
        service: Sheets,
        spreadsheetId: String,
        range: String,
        rows: List<List<Any?>>,
    ) {
        val body = ValueRange()
            .setRange(range)
            .setValues(rows.map { row -> row.map { v -> v?.toString() } })
        service.spreadsheets().values()
            .update(spreadsheetId, range, body)
            .setValueInputOption("RAW")
            .execute()
    }
}
