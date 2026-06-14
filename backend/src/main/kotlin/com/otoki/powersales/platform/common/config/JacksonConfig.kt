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
