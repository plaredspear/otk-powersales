package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `User` 의 조직 표시 필드(`division` / `department` / `title` / `hrCode` / `branch`) 동기화 도메인 서비스.
 *
 * 레거시 매핑: SF `AppointmentTriggerHanlder.cls:313-323` (updateUser future 의 조직 필드 대입 블록).
 * 동작 요약 (입력 / 분기 / 외부 호출 / 부수 효과):
 *  - 입력: `Employee.costCenterCode` + `Employee.orgName` + `Employee.jikwee`
 *  - 외부 호출: `OrganizationRepository.findFirstByOrgCodeCascade()` — Level5/4/3 cascade
 *    ([EmployeeProfileResolver] / [UserRoleResolver] 와 동일 lookup, Redis 24h 캐시)
 *  - 분기: SF `if (orgInfoTmp != null)` 가드 1개 — 조직 cascade lookup 실패 시 **전 필드 무변경**
 *  - 부수 효과: 전달받은 [User] 의 5개 필드 대입 (dirty checking)
 *
 * lookup 실패 시 기존 값을 남기는 것이 SF 동작이며, 조직 마스터 미적재 시점에 표시값을 null 로
 * 밀어버리지 않는 안전장치이기도 하다. (`profileId` 는 [EmployeeProfileResolver] 가 lookup 실패를
 * `Staff` 디폴트로 흡수하는 별도 정책 — Spec #759 의 의도적 이탈.)
 *
 * ## 호출 경로
 * - 발령 후처리: [com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater.updateUserProfileCache]
 *   — SF 와 동일하게 **발령 수신 시에만** 동작한다.
 * - 부팅 catch-up: [UserOrgFieldsBackfillRunner] — 위 경로가 없던 시기에 생성된 존량 보정.
 *
 * ## 레거시 이탈 1건 (미재현)
 * SF 는 `updateUser` 의 User SOQL 에 `AND isActive = true` 가 있어(`cls:287`) 비활성 User 를 갱신하지
 * 않는다. 신규는 활성 여부를 보지 않는다 — 표시 전용 컬럼이라 비활성 User 를 갱신해도 무해하고,
 * 오히려 퇴직자 재입사로 재활성될 때 값이 최신이라 정합이다.
 */
@Service
class UserOrgDisplayFieldsSynchronizer(
    private val organizationRepository: OrganizationRepository,
) {

    /**
     * 조직 표시 필드 갱신 (SF cls:313-323).
     *
     * `branch` 는 발령 반영 후의 `Employee.orgName` (유통총괄1·2부 prefix 포함,
     * [com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater.resolveOrgName]
     * 결과) — SF 도 `@future` 재조회 시점의 커밋된 `OrgName__c` 를 읽으므로 동일하다. `orgName` 은
     * 발령 수신으로만 채워지므로, 발령 이력이 없는 사원은 나머지 4개만 채워지고 `branch` 는 null 로 남는다.
     *
     * 클래스 레벨 `readOnly` 서비스에서 호출되더라도 쓰기가 유실되지 않도록 `@Transactional` 을 명시한다
     * (트랜잭션 없는 호출 시 `readOnly` 전파로 FlushMode.MANUAL 이 되면 UPDATE 가 무예외 유실).
     *
     * @return 조직 cascade lookup 성공 → 갱신 수행 여부
     */
    @Transactional
    fun sync(user: User, employee: Employee): Boolean {
        val org = employee.costCenterCode
            ?.takeIf { it.isNotBlank() }
            ?.let { organizationRepository.findFirstByOrgCodeCascade(it) }
            ?: return false

        user.division = org.orgNameLevel3
        user.department = joinDepartment(org.orgNameLevel3, org.orgNameLevel4)
        user.title = employee.jikwee
        user.hrCode = employee.costCenterCode
        user.branch = employee.orgName
        return true
    }

    /**
     * `User.department` 조립 — SF cls:316 `OrgNameLevel3__c + '_' + OrgNameLevel4__c` 정합.
     *
     * 결과 정합 이탈 1건: Apex 문자열 연결은 null 을 문자열 `"null"` 로 렌더하므로 레벨명이 비면
     * `"null_영업1팀"` 같은 값이 저장된다. 표시 전용 컬럼에 리터럴 "null" 을 적재하는 것은
     * 재현 가치가 없는 결함이라, 신규는 비어있는 레벨을 빼고 조립한다 (양쪽 다 비면 null).
     */
    internal fun joinDepartment(orgNameLevel3: String?, orgNameLevel4: String?): String? =
        listOfNotNull(orgNameLevel3, orgNameLevel4)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("_")
}
