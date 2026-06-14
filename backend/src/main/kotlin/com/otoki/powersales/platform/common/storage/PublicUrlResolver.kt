package com.otoki.powersales.platform.common.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * UploadFile.uniqueKey (= S3 객체 key) 를 anonymous-accessible 한 완전 URL 로 변환한다.
 *
 * - app.aws.s3.public-url-prefix 가 비어 있으면 (local 환경) key 를 그대로 반환.
 * - prefix 가 설정된 경우 prefix + "/" + key 조립. prefix 가 "/" 로 끝나도 중복 안 됨.
 */
@Component
class PublicUrlResolver(
    @Value("\${app.aws.s3.public-url-prefix:}") private val prefix: String
) {

    fun resolve(uniqueKey: String?): String? {
        if (uniqueKey.isNullOrBlank()) return null
        if (prefix.isBlank()) return uniqueKey
        val base = prefix.trimEnd('/')
        return "$base/$uniqueKey"
    }
}
