package com.otoki.powersales.platform.common.integration.orora.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.datasource.orora")
data class OroraDataSourceProperties(
	val host: String = "",
	val database: String = "",
	val username: String = "",
	val password: String = "",
	val encrypt: Boolean = false,
	val trustServerCertificate: Boolean = false,
	val hikari: Hikari = Hikari(),
) {
	data class Hikari(
		val poolName: String = "ororaHikariPool",
		val maximumPoolSize: Int = 5,
		val minimumIdle: Int = 1,
		val idleTimeout: Long = 600_000,
		val maxLifetime: Long = 1_500_000,
		val keepaliveTime: Long = 120_000,
		val connectionTestQuery: String = "SELECT 1",
		val connectionTimeout: Long = 5_000,
		val validationTimeout: Long = 3_000,
		val readOnly: Boolean = true,
		val autoCommit: Boolean = true,
		val initializationFailTimeout: Long = -1,
	)
}
