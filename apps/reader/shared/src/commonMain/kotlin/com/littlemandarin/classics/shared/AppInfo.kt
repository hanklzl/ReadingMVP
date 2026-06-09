package com.littlemandarin.classics.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

object AppInfoResourceKeys {
    const val AppName: String = "app_name"
}

@Serializable
data class AppInfo(
    val nameResourceKey: String,
)

interface AppInfoRepository {
    val appInfo: Flow<AppInfo>
}

class DefaultAppInfoRepository : AppInfoRepository {
    override val appInfo: Flow<AppInfo> = flowOf(GetAppInfoUseCase().invoke())
}

class GetAppInfoUseCase {
    operator fun invoke(): AppInfo = AppInfo(
        nameResourceKey = AppInfoResourceKeys.AppName,
    )
}
