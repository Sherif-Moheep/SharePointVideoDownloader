package org.example.desktop_app.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.example.desktop_app.data.datasource.LocalDataSource
import org.example.desktop_app.data.datasource.room.AppDatabase
import org.example.desktop_app.data.datasource.room.HistoryDao
import org.example.desktop_app.data.repository.ChromeRepositoryImpl
import org.example.desktop_app.data.repository.HistoryRepositoryImpl
import org.example.desktop_app.data.repository.YtDlpRepositoryImpl
import org.example.desktop_app.domain.models.VideoDownloadRepository
import org.example.desktop_app.domain.repository.ChromeRepository
import org.example.desktop_app.domain.repository.HistoryRepository
import org.example.desktop_app.presentation.MainViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

fun provideRoomDatabase(): AppDatabase {
    val userHome = System.getProperty("user.home")
    val appFolder = File(userHome, ".sharepoint_downloader")
    if (!appFolder.exists()) { appFolder.mkdirs() }
    val dbFile = File(appFolder, "history.db")

    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
//        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

val appModule = module {
    // 1. Room DB & DAO
    single<AppDatabase> { provideRoomDatabase() }
    single<HistoryDao> { get<AppDatabase>().historyDao() }

    // 2. Data Sources
    singleOf(::LocalDataSource)

    // 3. Repositories
    single {
        ChromeRepositoryImpl()
    }.bind<ChromeRepository>()

    single {
        HistoryRepositoryImpl(get())
    }.bind<HistoryRepository>()

    single {
        YtDlpRepositoryImpl()
    }.bind<VideoDownloadRepository>()

    // 4. ViewModels
    factoryOf(::MainViewModel)
}