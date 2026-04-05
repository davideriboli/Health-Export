# CLAUDE.md — HealthExport

## Progetto

HealthExport è un'app Android nativa che esporta i dati di Android Health Connect su Google Sheets. L'app è pensata per uso personale (sideload APK), non per distribuzione su Play Store (almeno per ora).

## Lingua

Tutta la comunicazione con lo sviluppatore avviene in **italiano**. Commenti nel codice e naming di classi/variabili/funzioni restano in **inglese**, come da convenzione Kotlin/Android.

## Profilo dello sviluppatore

Lo sviluppatore (Davide) ha competenza avanzata in Linux, Bash, Python, JavaScript, ma **non è uno sviluppatore Android/Kotlin nativo**. Conosce i concetti ma non ha familiarità profonda con l'ecosistema Android. Spiega le scelte architetturali Android-specifiche quando non sono ovvie, ma non semplificare il codice — mantieni qualità production-grade.

## Stack tecnologico

| Componente | Tecnologia |
|---|---|
| Linguaggio | Kotlin |
| UI | Jetpack Compose + Material 3 (Material You) |
| Design system | Ispirato all'estetica di Claude AI (palette calda, tipografia pulita, spazi generosi, bordi arrotondati, animazioni fluide) |
| Health Connect | `androidx.health.connect:connect-client` |
| Google Sheets | Google Sheets API v4 via `google-api-client-android` |
| Autenticazione Google | Credential Manager |
| Persistenza preferenze | Jetpack DataStore (Preferences) |
| Export periodico | WorkManager con PeriodicWorkRequest |
| Dependency Injection | Hilt |
| Navigazione | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 34 (Android 14) |

## Architettura

MVVM a 3 layer:

```
UI Layer (Compose) → ViewModel Layer → Repository Layer → Services
```

- **UI**: Jetpack Compose, wizard a 4 step con navigazione lineare
- **ViewModel**: stato del wizard, validazione, coordinamento export
- **Repository**: `HealthConnectRepository` (lettura dati), `GoogleSheetsRepository` (scrittura fogli)
- **Services**: WorkManager (scheduling), DataStore (preferenze persistenti)

## Flusso utente (Wizard a 4 step)

1. **Selezione dati**: lista di tutti i tipi di record Health Connect, raggruppati per categoria, con toggle individuali e seleziona/deseleziona tutto. L'ultima selezione viene ricordata tra le sessioni.
2. **Intervallo temporale**: ultime 24h, ultima settimana, ultimo mese, range custom con date picker.
3. **Destinazione e modalità**: scegli/crea Google Sheet, modalità sovrascrittura o append, schedulazione (una tantum / giornaliera / settimanale / mensile).
4. **Riepilogo e conferma**: summary delle scelte, pulsante esporta, progress bar.

## Struttura Google Sheet

- Un singolo Spreadsheet
- Un tab (sheet) per ogni tipo di dato esportato
- Ogni tab ha schema colonnare specifico per il tipo di record
- Colonna `source_app` sempre presente per tracciare la provenienza

## Record Health Connect supportati

Tutti i tipi disponibili in `androidx.health.connect.client.records`, raggruppati in:

- **Attività**: Steps, Distance, Calories, ExerciseSession, FloorsClimbed, ecc.
- **Corpo**: Weight, Height, BodyFat, BoneMass, ecc.
- **Segni vitali**: HeartRate, BloodPressure, OxygenSaturation, RespiratoryRate, ecc.
- **Sonno**: SleepSession (con stages)
- **Nutrizione**: Nutrition, Hydration
- **Riproduzione**: MenstruationFlow, CervicalMucus, ecc.

Ogni tipo è mappato da una sealed class `RecordSchema` che definisce: nome tab, lista colonne, logica di estrazione dal record HC, serializzazione per Sheets API.

## Build e distribuzione

- **No Android Studio richiesto**: il progetto si builda via GitHub Actions
- Il workflow CI: push su `main` → build Gradle → APK firmato come artifact
- Keystore e credenziali Google gestiti come GitHub Secrets
- APK distribuito via sideload

## Convenzioni di codice

- Kotlin idiomatico, coroutine-first (niente callback)
- Compose: stato hoistato nei ViewModel, composable stateless dove possibile
- Naming: PascalCase per classi/composable, camelCase per funzioni/variabili
- Package structure per feature, non per layer:
  ```
  com.healthexport/
  ├── di/              # Hilt modules
  ├── data/
  │   ├── healthconnect/  # HC client, record schemas, repository
  │   └── sheets/         # Sheets API client, repository
  ├── ui/
  │   ├── theme/          # Material 3 theme, colors, typography
  │   ├── wizard/         # Wizard screens (step1..step4)
  │   ├── progress/       # Export progress screen
  │   └── components/     # Shared composable components
  ├── worker/          # WorkManager workers
  └── util/            # Extensions, constants
  ```

## Criticità note

- **Rate limit Sheets API**: 60 req/min. Usare `batchUpdate` e backoff esponenziale.
- **Limite 30 giorni**: nuova installazione può leggere solo ultimi 30 giorni da HC.
- **Celle Google Sheets**: limite 10M celle per spreadsheet. Monitorare con dati ad alta frequenza (es. HeartRate).
- **WorkManager**: configurare `Constraints` (rete, batteria) per export schedulato.

## Piano di sviluppo (moduli incrementali)

1. Scaffold progetto, build.gradle.kts, tema, navigazione wizard
2. Integrazione Health Connect, lettura dati, UI selezione tipi
3. Autenticazione Google + scrittura su Sheets
4. Logica append/sovrascrittura, gestione tab dinamici
5. WorkManager per export schedulato
6. Polish UI, gestione errori, edge case

## Istruzioni per Claude Code

- Procedi un modulo alla volta. Non anticipare moduli successivi.
- Ad ogni modulo, produci codice completo e funzionante, non scheletri.
- Quando crei nuovi file, mostra il path completo nel progetto.
- Dopo ogni modulo, elenca i file creati/modificati e le dipendenze aggiunte.
- Se una decisione di design ha alternative ragionevoli, esponile brevemente e chiedi prima di procedere.