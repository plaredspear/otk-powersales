package com.otoki.powersales.common.storage

interface StorageService {
	fun upload(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult

	fun download(key: String): ByteArray

	fun getUrl(key: String, expiresInSeconds: Int): String

	fun delete(key: String)
}
