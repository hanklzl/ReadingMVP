package com.littlemandarin.classics.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AppInfoTest {
    @Test
    fun useCaseReturnsLocaleNeutralAppNameKey() {
        val appInfo = GetAppInfoUseCase().invoke()

        assertEquals(AppInfoResourceKeys.AppName, appInfo.nameResourceKey)
    }

    @Test
    fun repositoryPublishesAppInfo() = runTest {
        val appInfo = DefaultAppInfoRepository().appInfo.first()

        assertEquals(AppInfoResourceKeys.AppName, appInfo.nameResourceKey)
    }
}
