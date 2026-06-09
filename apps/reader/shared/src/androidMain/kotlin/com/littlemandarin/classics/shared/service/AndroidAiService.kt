package com.littlemandarin.classics.shared.service

actual fun createPlatformAiService(
    config: AiServiceConfig,
): AiService = createAiService(config = config)
