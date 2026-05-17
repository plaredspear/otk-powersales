package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.dto.DataScope
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [CurrentDataScope] 어노테이션이 부착된 controller 메서드 파라미터를 [WebAdminContextFilter] 가
 * request attribute 에 적재한 값으로 채운다.
 *
 * holder (`@RequestScope` 빈) 패턴 대체. 인증 사용자 자체는 `@AuthenticationPrincipal WebUserPrincipal`
 * 로 직접 수신하므로 본 resolver 는 DataScope 만 다룬다.
 *
 * WebMvcConfigurer 의 [WebMvcConfig.addArgumentResolvers] 로 등록.
 */
@Component
class CurrentAdminContextArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasCurrentDataScope = parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        if (hasCurrentDataScope && parameter.parameterType != DataScope::class.java) return false
        return hasCurrentDataScope
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        return when {
            parameter.hasParameterAnnotation(CurrentDataScope::class.java) -> {
                webRequest.getAttribute(AdminContextAttributes.DATA_SCOPE, NativeWebRequest.SCOPE_REQUEST)
                    ?: throw IllegalStateException(
                        "CurrentDataScope 가 request attribute 에 없습니다 — WebAdminContextFilter 통과 여부 확인"
                    )
            }

            else -> throw IllegalStateException("지원하지 않는 파라미터: ${parameter.parameterName}")
        }
    }
}
