package com.otoki.powersales.platform.common.config

import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Spring Boot 4 의 기본 ObjectMapper 가 Jackson 3 (`tools.jackson.*`) 이며 Jackson 3 의 KotlinModule
// 및 JavaTime 지원이 ServiceLoader 로 자동 등록되므로 별도 ObjectMapper 빈 구성은 필요 없다.
// wire format 은 Jackson 기본 (LOWER_CAMEL_CASE) 을 사용한다 (Spec #580 P1-B). SAP 인바운드
// 응답의 RESULT_DETAIL 내부는 각 Detail DTO 의 `@JsonNaming(SnakeCaseStrategy)` 으로 SnakeCase 유지.
//
// === 미정의(unknown) JSON 필드 정책 — lenient 유지 (의사결정) ===
// `application.yml` 에 `spring.jackson.deserialization.fail-on-unknown-properties` 를 두지 않아
// Spring Boot 기본값(=비활성, FAIL_ON_UNKNOWN_PROPERTIES=false)이 적용된다. 즉 요청 JSON 에
// DTO 에 없는 필드가 와도 무시(lenient)하고 역직렬화를 계속한다. 전역 단일 정책이라 도메인별 분기 없음.
//
// 이는 레거시 SF Apex 의 `JSON.deserializeStrict` (미정의 필드 1개라도 있으면 전체 배치 예외 실패)
// 와 동작 방향이 정반대다. 전체 SAP 인바운드(조직/사원/제품/바코드/시스템코드/거래처/카테고리/ERP주문/
// 발령/근태) 에 lenient 가 일률 적용되며, 레거시 strict 동등을 의도적으로 채택하지 않는다.
//   사유 (유지보수 우선):
//   1) SAP 가 페이로드에 필드를 추가해도 신규는 무중단 처리 — strict 였다면 필드 1개 추가에 전체
//      마스터 동기화가 일괄 중단되는 fragile 한 운영 리스크를 신규에 이식하게 된다.
//   2) 레거시 strict 는 의도적 입력 검증이 아니라 Apex JSON.deserialize 계열의 부수 효과다. 실제
//      데이터 무결성 검증은 각 도메인 UpsertService 의 명시적 필수값 검증이 더 견고하게 담당한다.
// 특정 인바운드만 strict 가 필요해지면 전역 설정이 아니라 해당 DTO 에 국소 적용(@JsonIgnoreProperties
// (ignoreUnknown=false) + reject 패턴)으로 격리한다. 역직렬화 자체가 실패할 경우의 응답 경로는
// SapInboundExceptionHandler.handleUnreadable() (400 + CODE_INVALID_PAYLOAD) 가 담당한다.
//
// 본 설정은 argument resolver 명시 등록을 함께 담당한다:
//  - [AuthenticationPrincipalArgumentResolver] — Spring Security 7 + @WebMvcTest(addFilters=false)
//    환경에서 SecurityFilterChain 비활성으로 자동 등록되지 않는 `@AuthenticationPrincipal` 지원
//  - [CurrentAdminContextArgumentResolver] — admin controller 의 `@CurrentDataScope`
//    파라미터 주입 (holder 빈 대체). 인증 사용자 본인은 `@AuthenticationPrincipal WebUserPrincipal`
//    로 직접 수신.
@Configuration
class JacksonConfig(
    private val currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AuthenticationPrincipalArgumentResolver())
        resolvers.add(currentAdminContextArgumentResolver)
    }
}
