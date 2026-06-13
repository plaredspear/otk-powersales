package com.otoki.powersales.platform.apppackage.service

import net.dongliu.apk.parser.ByteArrayApkFile
import org.springframework.stereotype.Component

/**
 * Android .apk 내부 `AndroidManifest.xml`(바이너리 AXML) 에서 메타데이터를 추출한다.
 *
 * .apk 는 ZIP 컨테이너이고 메타데이터는 AOSP 전용 바이너리 XML 포맷이라 순수 JDK 로는
 * 파싱이 어렵다. apk-parser 라이브러리로 ApkMeta 를 읽어 packageName / versionName /
 * versionCode 를 추출한다. (iOS [IpaMetadataExtractor] 와 동일한 자동화 목적)
 */
@Component
class ApkMetadataExtractor {

    data class ApkMetadata(
        val packageName: String?,
        val versionName: String?,
        val versionCode: Long?,
    )

    /**
     * @return AndroidManifest 파싱 결과. 손상/비APK 면 null (호출 측에서 입력 누락 예외로 전환).
     */
    fun extract(apkBytes: ByteArray): ApkMetadata? = runCatching {
        ByteArrayApkFile(apkBytes).use { apk ->
            val meta = apk.apkMeta
            ApkMetadata(
                packageName = meta.packageName?.takeIf { it.isNotBlank() },
                versionName = meta.versionName?.takeIf { it.isNotBlank() },
                versionCode = meta.versionCode,
            )
        }
    }.getOrNull()
}
