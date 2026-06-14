package com.otoki.powersales.domain.org.organization.repository.dto

import com.otoki.powersales.domain.org.organization.entity.Organization

/**
 * Organization cascade lookup 결과 캐시 전용 DTO.
 *
 * ## 도입 배경
 *
 * Organization entity 는 `@ManyToOne(LAZY) User × 3 + Group × 1` 관계를 보유하므로 그대로
 * Redis 직렬화 시 Hibernate proxy 처리 / `LazyInitializationException` 위험이 있다.
 * 캐시 round-trip 후 JPA managed 상태가 사라져 lazy 접근도 어차피 의미 없음.
 *
 * 따라서 cascade lookup 의 호출자가 실제로 사용하는 필드만 추출한 가벼운 immutable DTO 를
 * 캐시 값으로 사용한다.
 *
 * ## 포함 필드
 *
 * - `orgCodeLevel3` — [com.otoki.powersales.user.service.EmployeeProfileResolver] 의 Profile.name 분기
 * - `orgNameLevel4` / `orgNameLevel3` — [com.otoki.powersales.user.service.UserRoleResolver] 의 영업지원/영업본부 매칭
 * - `costCenterLevel3` — [com.otoki.powersales.domain.activity.schedule.service.AdminScheduleService] 의 영업지원실 다중 지점 expand
 *
 * 신규 호출자가 다른 필드를 필요로 하면 본 DTO 에 필드 추가 + Repository 의 [from] 매핑 갱신.
 * cache value 형식 변경 시 기존 Redis entry 호환성 — `GenericJacksonJsonRedisSerializer` 의
 * default typing 이 typed JSON 을 저장하므로 시그니처 변경 시 역직렬화 실패 가능 → 배포 시
 * 캐시 전체 evict 권장 (24h TTL 이라 자연 만료도 가능).
 */
data class OrganizationCacheDto(
    val orgCodeLevel3: String?,
    val orgNameLevel3: String?,
    val orgNameLevel4: String?,
    val costCenterLevel3: String?,
) {
    companion object {
        fun from(org: Organization): OrganizationCacheDto = OrganizationCacheDto(
            orgCodeLevel3 = org.orgCodeLevel3,
            orgNameLevel3 = org.orgNameLevel3,
            orgNameLevel4 = org.orgNameLevel4,
            costCenterLevel3 = org.costCenterLevel3,
        )
    }
}
