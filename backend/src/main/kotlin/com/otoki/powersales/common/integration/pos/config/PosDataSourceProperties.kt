package com.otoki.powersales.common.integration.pos.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * POS 외부 PostgreSQL DB 직결 DataSource 설정값.
 *
 * 레거시 Heroku `PosMapper.xml`(datasource=`pos`) 가 쓰던 별도 POS DB — 전산매출
 * (`public.live_tot_sales_dh`) · POS매출(`public.live_pos_sales_dh`) 소스.
 * 메인 RDS 와 분리된 read-only 직결. 접속정보는 환경변수(`POS_DB_*`) 주입.
 */
@ConfigurationProperties("app.datasource.pos")
data class PosDataSourceProperties(
	val host: String = "",
	val port: Int = 5432,
	val database: String = "",
	val username: String = "",
	val password: String = "",
	val hikari: Hikari = Hikari(),
) {
	data class Hikari(
		val poolName: String = "posHikariPool",
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
