package com.otoki.powersales.platform.common.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StorageConfig {

	@Bean
	@ConditionalOnMissingBean(StorageService::class)
	fun localStorageService(): StorageService = LocalStorageService()
}
