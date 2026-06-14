package com.otoki.powersales.platform.common.storage

data class UploadResult(
	val key: String,
	val contentType: String,
	val originalName: String,
	val sizeBytes: Long
)
