package com.otoki.powersales.admin.security

import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionEvaluator
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.ObjectMapper

/**
 * Web Admin 컨텍스트 필터 (spec #801 — SF 권한 모델 전면 적용).
 *
 * `/api/v1/admin/` 하위 경로에서 [WebUserPrincipal] 이 인증된 요청에 대해:
 *  1. [AdminDataScopeService] 로 DataScope 산출 → attribute ([AdminContextAttributes.DATA_SCOPE]) 세팅
 *  2. Principal 에 박혀있는 SF 권한 snapshot (Set<String>) 을 attribute
 *     ([AdminContextAttributes.PERMISSIONS]) 로 노출 + controller handler 의 `@RequiresSfPermission`
 *     어노테이션과 매칭 검사
 *
 * Permission 은 [AdminPermissionCache] 가 lazy lookup (5분 TTL). spec #808 expandAllDataBits 이후
 * permission set 이 365+ key 로 커지면서 JWT claim 운반 시 토큰 byte 가 8KB 초과 → nginx
 * `large_client_header_buffers` 한도 (4 8k) 초과로 `400 Request Header Or Cookie Too Large`
 * 발생한 사례를 해소. cache TTL 동안 permission 변경 반영이 지연될 수 있고, 즉시 반영이 필요하면
 * AdminPermissionCache.invalidate(userId) 명시 호출.
 *
 * Principal 의 `employeeId` snapshot 이 비어있는 사용자 (ADMIN-* 부트스트랩 등) 는 permission 빈 셋.
 * `@RequiresSfPermission` 부착 endpoint 접근 시 403 (단, 아래 시스템 관리자 우회 대상이면 통과),
 * 미부착 endpoint 는 통과.
 *
 * ## 시스템 관리자 우회
 *
 * [SystemAdminProfilePolicy.isSystemAdmin] (`profileName == "시스템 관리자"`) 인 principal 은
 * `@RequiresSfPermission` 검사를 **건너뛴다**. SF 권한 모델(ProfileFlags / PermissionSet) 의 마이그레이션
 * 적재 여부와 무관하게 모든 admin API 에 접근하며, 신규 endpoint 에 가드가 추가되어도 별도 권한 매핑 없이
 * 자동 통과한다.
 *
 * 우회 판정 입력은 `permissions` snapshot 이 아니라 `profileName` 이다. `profileName` 은 `User.profileId`
 * (Employee 아님) 로부터 산출되므로, `employeeId == null` 인 ADMIN-* 부트스트랩 사용자라도 User 가 '시스템
 * 관리자' Profile 을 가리키면 우회 대상이 되어 lock-out 을 회피한다. `profileName` 은 서명 검증된 JWT claim
 * 출처라 클라이언트 위조 불가 (단 로그인 시점 snapshot — Profile 강등은 토큰 만료/재로그인 후 반영).
 *
 * 우회는 본 API 가드에 한정 — 데이터 스코프 / 개별 서비스의 비즈니스 분기는 각자의 profileName 분기를
 * 그대로 따른다.
 *
 * 본 우회는 SF 권한 데이터(특히 '시스템 관리자' Profile 의 `MODIFY_ALL_DATA` 비트 → `expandAllDataBits`
 * 전체 자원 펼침) 미적재 상황에서도 시스템 관리자 lock-out 을 막는 안전망이다. 정식 경로([SfPermissionResolver])
 * 가 정상 동작하는 환경에서도 동등 결과이며, 적재 누락 시 fallback 으로 통과했음을 운영에서 가시화하기 위해
 * WARN 로그를 남긴다.
 */
class WebAdminContextFilter(
    private val adminDataScopeCache: AdminDataScopeCache,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val sfPermissionEvaluator: SfPermissionEvaluator,
    private val adminPermissionCache: AdminPermissionCache,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as? WebUserPrincipal

        if (principal != null) {
            val permissions: Set<String> = if (principal.employeeId != null) {
                request.setAttribute(AdminContextAttributes.DATA_SCOPE, adminDataScopeCache.get(principal))
                val resolved = adminPermissionCache.get(principal.userId)
                request.setAttribute(AdminContextAttributes.PERMISSIONS, resolved)
                resolved
            } else {
                emptySet()
            }

            val handlerMethod = resolveHandlerMethod(request)
            val required = handlerMethod
                ?.getMethodAnnotation(RequiresSfPermission::class.java)

            if (required != null) {
                // 시스템 관리자는 `@RequiresSfPermission` 검사를 우회 — SF 권한 모델(ProfileFlags / PermissionSet)
                // 적재 여부와 무관하게 모든 admin API 에 접근하며, 신규 endpoint 에 `@RequiresSfPermission` 이
                // 추가되어도 별도 권한 매핑 없이 자동 통과한다. profileName 은 데이터 스코프(AdminDataScopeService)
                // 가 이미 시스템 관리자 전체 스코프 분기에 쓰는 식별자와 동일. 우회는 API 가드에 한정하며,
                // 데이터 스코프 / 개별 서비스 비즈니스 분기는 각자의 profileName 분기를 그대로 유지한다.
                val isSystemAdmin = SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)
                val allowedBySfPermission = sfPermissionEvaluator.isAllowed(required, permissions)
                if (!isSystemAdmin && !allowedBySfPermission) {
                    writeErrorResponse(response, "PERMISSION_DENIED", "해당 API에 대한 접근 권한이 없습니다")
                    return
                }
                // 정식 경로(SF 권한 데이터)로는 막혔으나 시스템 관리자 fallback 으로 통과한 경우 — '시스템 관리자'
                // Profile 의 MODIFY_ALL_DATA 비트 미적재 등 데이터 정합 결함 신호. 운영에서 적재 누락 조기 발견용.
                if (isSystemAdmin && !allowedBySfPermission) {
                    log.warn(
                        "[admin-permission] 시스템 관리자 fallback 통과 (SF 권한 데이터 미적재 가능) — userId={} {} {}",
                        principal.userId, request.method, request.requestURI,
                    )
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveHandlerMethod(request: HttpServletRequest): HandlerMethod? {
        return try {
            requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod
        } catch (_: Exception) {
            null
        }
    }

    private fun writeErrorResponse(response: HttpServletResponse, code: String, message: String) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ApiResponse.error<Any>(code, message)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
