package com.otoki.powersales.user.repository

import com.otoki.powersales.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {

    /**
     * web admin User 관리 화면 목록 조회.
     *
     * - keyword 가 있으면 username / employee_code / name 부분 일치 (case-insensitive)
     * - isActive 가 null 이 아니면 정확 일치
     * - profileId 가 null 이 아니면 정확 일치 (프로파일 필터)
     * - costCenterCodes 가 비어있지 않으면 `cost_center_code IN (...)` (지점 필터)
     * - 정렬: name ASC
     */
    fun findUsers(
        keyword: String?,
        isActive: Boolean?,
        profileId: Long?,
        costCenterCodes: List<String>?,
        pageable: Pageable
    ): Page<User>

    /**
     * Spec #803 — Profile 상세의 부여 사용자 일람.
     *
     * - profileId 정확 일치
     * - keyword 가 있으면 employee_code / name 부분 일치 (case-insensitive)
     * - 정렬: name ASC
     */
    fun findUsersByProfileId(profileId: Long, keyword: String?, pageable: Pageable): Page<User>

    /**
     * Spec #803 — PermissionSet 상세의 부여 사용자 일람.
     *
     * - permission_set_assignment 의 active row 가 있는 user 만
     * - keyword 가 있으면 employee_code / name 부분 일치
     */
    fun findUsersByPermissionSetFlagsId(permissionSetFlagsId: Long, keyword: String?, pageable: Pageable): Page<User>

    /**
     * SF user sfid → 신규 User PK 일괄 매핑 (sfid, id) 쌍 목록.
     *
     * SharingPolicyQueryRepository 가 sharing rule condition 의 audit/owner field value (SF user sfid) 를
     * snapshot 적재 시점에 신규 User.id 로 pre-resolve 하는 용도.
     */
    fun findIdsBySfidIn(sfids: Collection<String>): List<Pair<String, Long>>

    /**
     * 조직 표시 필드가 한 번도 채워지지 않은 사원 매칭 User 의 사번 목록.
     *
     * `branch` / `division` 이 **둘 다** null 인 행만 대상 — `branch` 단독 기준은 발령 이력이 없어
     * `Employee.orgName` 이 null 인 사원을 매 부팅 재조회하게 만든다 (동기화에 성공해도 branch 는
     * 계속 null 이므로). 두 컬럼 모두 [com.otoki.powersales.user.service.UserOrgDisplayFieldsSynchronizer]
     * 만 쓰기 때문에 "미동기화" 마커로 안전하다 — `hrCode` 는 SAP 사원 마스터 인바운드도 쓰므로 부적합.
     *
     * 호출자: [com.otoki.powersales.user.service.UserOrgFieldsBackfillRunner] (부팅 catch-up).
     */
    fun findEmployeeCodesWithoutOrgDisplayFields(): List<String>
}
