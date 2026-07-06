package com.otoki.powersales.external.sap.auth.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.SecureRandom
import java.util.Base64

/**
 * SAP inbound OAuth client_secret 생성/해시 유틸.
 *
 * 런타임의 SapTokenService 가 client_secret 을 검증할 때 쓰는 [BCryptPasswordEncoder] 를
 * 그대로 재사용하므로, 여기서 만든 해시는 실제 검증 로직과 100% 정합한다 ($2a$, strength=10).
 *
 * 실행 (backend/ 에서):
 *   - 새 secret 원문 + 해시 생성:   ./gradlew genSapClientSecret
 *   - 기존 원문으로 해시만 재생성:   ./gradlew genSapClientSecret --args='<plain-secret>'
 *
 * 출력:
 *   - PLAIN : SAP 연동 담당자에게 전달할 원문 (client_secret)
 *   - HASH  : 배포 환경변수 SAP_CLIENT_SECRET_HASH 에 설정할 BCrypt 해시
 *
 * 주의: 해시에는 salt 가 포함돼 매 실행마다 값이 달라진다. 출력된 PLAIN 과 HASH 는 반드시 쌍으로
 * 사용한다. PLAIN 은 안전한 경로(비밀번호 관리자 등)로만 전달하고 로그/이력에 남기지 않는다.
 */
object SapClientSecretTool {

    /** 원문 secret 바이트 길이 (256-bit). URL-safe base64 로 인코딩되어 43자 문자열이 된다. */
    private const val SECRET_BYTES = 32

    private val encoder = BCryptPasswordEncoder()

    @JvmStatic
    fun main(args: Array<String>) {
        val plain = args.firstOrNull()?.takeIf { it.isNotBlank() } ?: generateSecret()
        val hash = encoder.encode(plain)

        require(encoder.matches(plain, hash)) { "self-check 실패: 생성한 해시가 원문과 매칭되지 않음" }

        println("PLAIN  : $plain")
        println("HASH   : $hash")
        println("VERIFY : OK (BCryptPasswordEncoder.matches passed)")
        println()
        println("SAP_CLIENT_SECRET_HASH 에 위 HASH 값을 설정하고, PLAIN 은 SAP 측에 전달하세요.")
    }

    /** 256-bit CSPRNG 랜덤을 URL-safe(padding 없는) base64 문자열로 생성. */
    private fun generateSecret(): String {
        val bytes = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
