package com.otoki.powersales.common.storage

import java.time.Duration

object StorageConstants {
	const val MAX_FILE_BYTES: Long = 20L * 1024 * 1024
	val ALLOWED_CONTENT_TYPES: Set<String> = setOf(
		"image/jpeg",
		"image/png",
		"image/heic",
		"application/pdf",
		"image/jpg",
		"image/gif",
		"image/webp",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"application/vnd.ms-excel"
	)
	val DEFAULT_PRESIGN_TTL: Duration = Duration.ofMinutes(5)
}
