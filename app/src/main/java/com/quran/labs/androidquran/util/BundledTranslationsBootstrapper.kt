package com.quran.labs.androidquran.util

import android.content.Context
import com.quran.labs.androidquran.dao.translation.Translation
import com.quran.labs.androidquran.dao.translation.TranslationItem
import com.quran.labs.androidquran.database.DatabaseHandler.Companion.getDatabaseHandler
import com.quran.labs.androidquran.database.TranslationsDBAdapter
import com.quran.mobile.di.qualifier.ApplicationContext
import com.quran.mobile.translation.model.LocalTranslation
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class BundledTranslationsBootstrapper @Inject constructor(
  @ApplicationContext private val appContext: Context,
  private val quranFileUtils: QuranFileUtils,
  private val translationsDBAdapter: TranslationsDBAdapter,
  private val quranSettings: QuranSettings
) {

  suspend fun ensureBundledTranslations() = withContext(Dispatchers.IO) {
    try {
      val dbDir = quranFileUtils.getQuranDatabaseDirectory()
      if (!dbDir.exists()) {
        dbDir.mkdirs()
      }

      copyBundledDatabaseIfMissing(dbDir, KEMENAG_FILENAME)
      copyBundledDatabaseIfMissing(dbDir, MUKHTASAR_FILENAME)

      seedBundledCatalogEntries(dbDir)
      ensureDefaultActiveTranslation()
    } catch (e: Exception) {
      Timber.e(e, "Failed to initialize bundled translations")
    }
  }

  private fun copyBundledDatabaseIfMissing(dbDir: File, filename: String) {
    val target = File(dbDir, filename)
    if (target.exists() && target.length() > 0) return

    try {
      appContext.assets.open("databases/$filename").use { input ->
        FileOutputStream(target).use { output ->
          input.copyTo(output)
        }
      }
      Timber.d("Copied bundled translation %s", filename)
    } catch (e: Exception) {
      // asset may not exist (e.g. build without optional bundled file)
      Timber.d(e, "Bundled translation asset not available: %s", filename)
      if (target.exists() && target.length() == 0L) {
        target.delete()
      }
    }
  }

  private suspend fun seedBundledCatalogEntries(dbDir: File) {
    val existing = translationsDBAdapter.translationsHash()

    val updates = mutableListOf<TranslationItem>()

    // Kemenag (id 8) - canonical metadata; if file exists and not in catalog, seed it.
    val kemenagFile = File(dbDir, KEMENAG_FILENAME)
    if (kemenagFile.exists() && existing.get(KEMENAG_ID) == null) {
      val (textVersion, schemaVersion) = getVersionFromDatabase(KEMENAG_FILENAME)
      val translation = Translation(
        id = KEMENAG_ID,
        minimumVersion = if (schemaVersion > 0) schemaVersion else 2,
        currentVersion = if (textVersion > 0) textVersion else 9,
        displayName = "Terjemahan Bahasa Indonesia (Kemenag)",
        downloadType = "translation",
        fileName = KEMENAG_FILENAME,
        fileUrl = "",
        saveTo = "db",
        languageCode = "id",
        translator = "Kementerian Agama RI",
        translatorNameLocalized = "Kemenag"
      )
      updates.add(
        TranslationItem(
          translation = translation,
          localVersion = if (textVersion > 0) textVersion else 9,
          displayOrder = 1
        )
      )
    }

    // Tafsir Al-Mukhtasar (Indonesian) - custom bundled entry (not in upstream server list).
    val mukhtasarFile = File(dbDir, MUKHTASAR_FILENAME)
    if (mukhtasarFile.exists() && existing.get(MUKHTASAR_ID) == null) {
      val (textVersion, schemaVersion) = getVersionFromDatabase(MUKHTASAR_FILENAME)
      val translation = Translation(
        id = MUKHTASAR_ID,
        minimumVersion = if (schemaVersion > 0) schemaVersion else 2,
        currentVersion = if (textVersion > 0) textVersion else 1,
        displayName = "Tafsir Al-Mukhtashar (Bahasa Indonesia)",
        downloadType = "translation",
        fileName = MUKHTASAR_FILENAME,
        fileUrl = "",
        saveTo = "db",
        languageCode = "id",
        translator = "Kementerian Urusan Islam, Wakaf, Dakwah dan Penyuluhan Arab Saudi",
        translatorNameLocalized = "Al-Mukhtashar"
      )
      updates.add(
        TranslationItem(
          translation = translation,
          localVersion = if (textVersion > 0) textVersion else 1,
          displayOrder = 2
        )
      )
    }

    if (updates.isNotEmpty()) {
      translationsDBAdapter.writeTranslationUpdates(updates)
    }
  }

  private fun ensureDefaultActiveTranslation() {
    val prefs = appContext.getSharedPreferences(PREFS_PER_INSTALLATION, Context.MODE_PRIVATE)
    if (!prefs.contains(PREF_ACTIVE_TRANSLATIONS)) {
      val kemenagExists = File(quranFileUtils.getQuranDatabaseDirectory(), KEMENAG_FILENAME).exists()
      if (kemenagExists) {
        quranSettings.setActiveTranslations(linkedSetOf(KEMENAG_FILENAME))
      }
    }
  }

  private fun getVersionFromDatabase(filename: String): Pair<Int, Int> {
    return try {
      val handler = getDatabaseHandler(appContext, filename, quranFileUtils)
      if (handler.validDatabase()) {
        Pair(handler.getTextVersion(), handler.getSchemaVersion())
      } else {
        Pair(0, 0)
      }
    } catch (e: Exception) {
      Timber.d(e, "exception reading bundled db: %s", filename)
      Pair(0, 0)
    }
  }

  companion object {
    const val KEMENAG_ID = 8
    const val KEMENAG_FILENAME = "quran.id.db"

    // Custom ID for bundled Indonesian Al-Mukhtasar tafsir (source: tafsir_api/QUL id 260)
    const val MUKHTASAR_ID = 999260
    const val MUKHTASAR_FILENAME = "ar.mukhtasar.id.db"

    // must match Constants
    private const val PREFS_PER_INSTALLATION = "quran_per_installation"
    private const val PREF_ACTIVE_TRANSLATIONS = "activeTranslations"
  }
}
