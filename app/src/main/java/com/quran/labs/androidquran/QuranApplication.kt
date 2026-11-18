package com.quran.labs.androidquran

import android.app.Application
import androidx.core.os.UserManagerCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.quran.labs.androidquran.core.worker.QuranWorkerFactory
import com.quran.labs.androidquran.di.component.application.ApplicationComponent
import com.quran.labs.androidquran.util.BundledTranslationsBootstrapper
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.RecordingLogTree
import com.quran.labs.androidquran.util.ThemeUtil
import com.quran.labs.androidquran.widget.BookmarksWidgetSubscriber
import com.quran.mobile.di.QuranApplicationComponent
import com.quran.mobile.di.QuranApplicationComponentProvider
import com.quran.mobile.feature.sync.QuranSyncManager
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@HasMemberInjections
open class QuranApplication : Application(), QuranApplicationComponentProvider, Configuration.Provider {
  lateinit var applicationComponent: ApplicationComponent

  @Inject lateinit var quranWorkerFactory: QuranWorkerFactory
  @Inject lateinit var bookmarksWidgetSubscriber: BookmarksWidgetSubscriber
  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var quranSyncManager: QuranSyncManager
  @Inject lateinit var bundledTranslationsBootstrapper: BundledTranslationsBootstrapper

  override fun provideQuranApplicationComponent(): QuranApplicationComponent {
    return applicationComponent
  }

  override fun onCreate() {
    super.onCreate()
    setupTimber()
    applicationComponent = initializeInjector()
    applicationComponent.inject(this)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      bundledTranslationsBootstrapper.ensureBundledTranslations()
    }
    // Sync/Firebase disabled for Qur'an Mobile fork
    // quranSyncManager.startForegroundSync()
    initializeWorkManager()
    bookmarksWidgetSubscriber.subscribeBookmarksWidgetIfNecessary()

    // theme setup
    val theme = quranSettings.currentTheme()
    ThemeUtil.setTheme(theme)
  }

  open fun setupTimber() {
    Timber.plant(RecordingLogTree())
  }

  open fun initializeInjector(): ApplicationComponent {
    return createGraphFactory<ApplicationComponent.Factory>()
      .generate(this)
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setWorkerFactory(quranWorkerFactory)
      .build()

  open fun initializeWorkManager() {
    if (UserManagerCompat.isUserUnlocked(this)) {
      WorkManager.initialize(this, workManagerConfiguration)
    }
  }
}
