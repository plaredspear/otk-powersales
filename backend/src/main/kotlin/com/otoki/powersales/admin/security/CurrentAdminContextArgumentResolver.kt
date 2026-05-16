package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.employee.entity.Employee
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [CurrentEmployee] / [CurrentDataScope] 어노테이션이 부착된 controller 메서드 파라미터를
 * [WebAdminContextFilter] 가 request attribute 에 적재한 값으로 채운다.
 *
 * holder (`@RequestScope` 빈) 패턴을 대체. controller 가 ambient context 빈 비인지.
 *
 * WebMvcConfigurer 의 [WebMvcConfig.addArgumentResolvers] 로 등록.
 */
@Component
class CurrentAdminContextArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasCurrentEmployee = parameter.hasParameterAnnotation(CurrentEmployee::class.java)
        val hasCurrentDataScope = parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        if (hasCurrentEmployee && parameter.parameterType != Employee::class.java) return false
        if (hasCurrentDataScope && parameter.parameterType != DataScope::class.java) return false
        return hasCurrentEmployee || hasCurrentDataScope
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        return when {
            parameter.hasParameterAnnotation(CurrentEmployee::class.java) -> {
                webRequest.getAttribute(AdminContextAttributes.EMPLOYEE, NativeWebRequest.SCOPE_REQUEST)
                    ?: throw IllegalStateException(
                        "CurrentEmployee 가 request attribute 에 없습니다 — WebAdminContextFilter 통과 여부 확인"
                    )
            }

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
