package app.mininote.mininote

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.mininote.mininote.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MiniNoteApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Периодический бэкстоп (15 мин) на случай пропущенного constraint-триггера WorkManager;
        // оппортунистическая/connectivity-триггерная синхронизация не требует отдельного
        // наблюдателя — enqueueOneTime() с NetworkType.CONNECTED сам ждёт сеть, если её сейчас нет.
        syncScheduler.schedulePeriodic()
    }
}
