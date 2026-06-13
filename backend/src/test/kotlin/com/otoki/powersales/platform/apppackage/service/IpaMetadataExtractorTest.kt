package com.otoki.powersales.platform.apppackage.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@DisplayName("IpaMetadataExtractor 테스트")
class IpaMetadataExtractorTest {

    private val extractor = IpaMetadataExtractor()

    private fun ipaWith(entries: Map<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (path, content) ->
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun infoPlistXml(bundleId: String, shortVer: String = "1.2.0", bundleVer: String = "3") = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0"><dict>
        <key>CFBundleIdentifier</key><string>$bundleId</string>
        <key>CFBundleShortVersionString</key><string>$shortVer</string>
        <key>CFBundleVersion</key><string>$bundleVer</string>
        </dict></plist>
    """.trimIndent()

    @Test
    @DisplayName("Payload/<App>.app/Info.plist 에서 bundle id 와 버전 추출")
    fun extractsBundleIdAndVersions() {
        val ipa = ipaWith(mapOf("Payload/Runner.app/Info.plist" to infoPlistXml("com.otoki.pwrs.mobile")))

        val meta = extractor.extract(ipa)

        assertThat(meta).isNotNull
        assertThat(meta!!.bundleIdentifier).isEqualTo("com.otoki.pwrs.mobile")
        assertThat(meta.shortVersion).isEqualTo("1.2.0")
        assertThat(meta.bundleVersion).isEqualTo("3")
    }

    @Test
    @DisplayName("플러그인/프레임워크 내부 Info.plist 는 무시하고 앱 루트만 사용")
    fun ignoresNestedInfoPlist() {
        val ipa = ipaWith(
            linkedMapOf(
                // 중첩 plist 가 먼저 나와도 앱 루트(.app 직속)만 선택
                "Payload/Runner.app/Frameworks/Flutter.framework/Info.plist" to infoPlistXml("io.flutter.flutter"),
                "Payload/Runner.app/PlugIns/Ext.appex/Info.plist" to infoPlistXml("com.otoki.pwrs.mobile.ext"),
                "Payload/Runner.app/Info.plist" to infoPlistXml("com.otoki.pwrs.mobile"),
            )
        )

        val meta = extractor.extract(ipa)

        assertThat(meta?.bundleIdentifier).isEqualTo("com.otoki.pwrs.mobile")
    }

    @Test
    @DisplayName("Info.plist 가 없으면 null")
    fun missingInfoPlistReturnsNull() {
        val ipa = ipaWith(mapOf("Payload/Runner.app/SomeOther.bin" to "x"))

        assertThat(extractor.extract(ipa)).isNull()
    }

    @Test
    @DisplayName("ZIP 이 아닌 손상 바이트면 null")
    fun corruptBytesReturnNull() {
        assertThat(extractor.extract(ByteArray(16) { it.toByte() })).isNull()
    }
}
