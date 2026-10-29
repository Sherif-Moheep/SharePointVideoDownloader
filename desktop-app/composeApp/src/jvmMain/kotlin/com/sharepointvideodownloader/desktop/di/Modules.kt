package com.sharepointvideodownloader.desktop.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import com.sharepointvideodownloader.desktop.data.datasource.LocalDataSource
import com.sharepointvideodownloader.desktop.data.datasource.room.AppDatabase
import com.sharepointvideodownloader.desktop.data.datasource.room.HistoryDao
import com.sharepointvideodownloader.desktop.data.repository.ChromeRepositoryImpl
import com.sharepointvideodownloader.desktop.data.repository.HistoryRepositoryImpl
import com.sharepointvideodownloader.desktop.data.repository.YtDlpRepositoryImpl
import com.sharepointvideodownloader.desktop.domain.repository.VideoDownloadRepository
import com.sharepointvideodownloader.desktop.domain.repository.ChromeRepository
import com.sharepointvideodownloader.desktop.domain.repository.HistoryRepository
import com.sharepointvideodownloader.desktop.domain.usecases.DownloadVideoUseCase
import com.sharepointvideodownloader.desktop.domain.usecases.GetDownloadHistoryUseCase
import com.sharepointvideodownloader.desktop.domain.usecases.MainUseCases
import com.sharepointvideodownloader.desktop.domain.usecases.OpenFileUseCase
import com.sharepointvideodownloader.desktop.domain.usecases.ProcessNewVideoJsonUseCase
import com.sharepointvideodownloader.desktop.domain.usecases.ResetStuckDownloadsUseCase
import com.sharepointvideodownloader.desktop.presentation.mainScreen.MainViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
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
    viewModelOf(::MainViewModel)

    // 5. UseCases
    factoryOf(::DownloadVideoUseCase)
    factoryOf(::GetDownloadHistoryUseCase)
    factoryOf(::OpenFileUseCase)
    factoryOf(::ProcessNewVideoJsonUseCase)
    factoryOf(::ResetStuckDownloadsUseCase)
    factoryOf(::MainUseCases)
}