package com.otoki.powersales.apppackage.service

import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * iOS .ipa(ZIP) 내부 `Payload/<App>.app/Info.plist` 에서 메타데이터를 추출한다.
 *
 * .ipa 는 ZIP 컨테이너이고, 실제 서명된 앱의 bundle identifier 는 Info.plist 의
 * `CFBundleIdentifier` 에 들어 있다. 업로더가 손으로 입력하면 오타로 OTA manifest 가
 * 불일치(설치 실패)할 수 있어, 서명된 IPA 의 실측 값을 직접 읽어 사용한다.
 *
 * Info.plist 는 일반적으로 binary plist 이므로 [PropertyListParser] 로 파싱한다.
 */
@Component
class IpaMetadataExtractor {

    data class IpaMetadata(
        val bundleIdentifier: String,
        val shortVersion: String?,
        val bundleVersion: String?,
    )

    /**
     * @return Info.plist 를 찾아 파싱한 메타데이터. 찾지 못하거나 CFBundleIdentifier 가
     *         없으면 null (호출 측에서 입력 누락 예외로 전환).
     */
    fun extract(ipaBytes: ByteArray): IpaMetadata? {
        val infoPlistBytes = readAppInfoPlist(ipaBytes) ?: return null
        return runCatching {
            val root = PropertyListParser.parse(infoPlistBytes) as? NSDictionary ?: return null
            val bundleId = root["CFBundleIdentifier"]?.toJavaObject()?.toString()?.takeIf { it.isNotBlank() }
                ?: return null
            IpaMetadata(
                bundleIdentifier = bundleId,
                shortVersion = root["CFBundleShortVersionString"]?.toJavaObject()?.toString()?.takeIf { it.isNotBlank() },
                bundleVersion = root["CFBundleVersion"]?.toJavaObject()?.toString()?.takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }

    /**
     * ZIP 엔트리 중 `Payload/<App>.app/Info.plist`(최상위 .app 직속) 의 바이트를 찾는다.
     * 플러그인/프레임워크 내부의 Info.plist 와 구분하기 위해 경로 segment 수로 필터링한다.
     */
    private fun readAppInfoPlist(ipaBytes: ByteArray): ByteArray? {
        ZipInputStream(ByteArrayInputStream(ipaBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && isAppRootInfoPlist(name)) {
                    return zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    /** `Payload/<App>.app/Info.plist` 형태인지 (segment 3개, .app 직속) 판별. */
    private fun isAppRootInfoPlist(entryName: String): Boolean {
        val parts = entryName.split("/")
        return parts.size == 3 &&
            parts[0] == "Payload" &&
            parts[1].endsWith(".app") &&
            parts[2] == "Info.plist"
    }
}
