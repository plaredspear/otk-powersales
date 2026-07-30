package com.otoki.powersales.domain.org.employee.policy

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.FemaleStaffJobCode

/**
 * 사원 계정 잠금 ↔ 앱 로그인 활성 축의 저장 직전 부수 효과 — SF `EmployeeTrigger`
 * (before insert / before update) 동등.
 *
 * SF 에서는 `DKRetail__Employee__c` 의 **모든 DML 에 트리거가 걸리므로** 어느 경로로 저장하든
 * 아래 두 규칙이 항상 적용된다. 신규 시스템에는 전역 트리거가 없어 각 저장 경로가 본 정책을
 * 직접 호출해야 한다 — 경로별로 따로 구현하면 규칙이 갈라지므로 여기 한 곳에만 둔다.
 *
 * ## 적용 순서 (SF 코드 순서 그대로)
 *
 * 1. `EmployeeTriggerHandler.upsertPhoneNumber` (cls:40) — `LockingFlag = true` 면
 *    `APPLoginActive = false`. 잠긴 계정의 앱 로그인 차단.
 * 2. `EmployeeTriggerHandler.lockingFlagException` (cls:45-53) — 현장 여사원 직군
 *    (판촉직 / 레이디직 / OSC직) 이면서 재직(≠ 퇴직) 이고 앱 권한이 여사원 / 조장 이면
 *    `LockingFlag = false`, `APPLoginActive = true` 로 **강제 복원**.
 *
 * 2번이 1번보다 뒤에 오므로 **보호 대상은 잠금 자체가 성립하지 않는다** — SAP 인사 데이터 오류나
 * 관리자 오조작으로 현장 인력의 앱 로그인이 끊기지 않게 막는 안전장치다. 순서를 뒤집으면 보호가
 * 무력화되므로 호출부에서 두 규칙을 분리 적용하지 말 것.
 *
 * ## 판정 시점
 *
 * 판정 입력은 **저장될 최종 값** (요청/인입 반영이 끝난 entity) 이다 — SF 트리거가 merge 된
 * 레코드를 보는 것과 정합. 따라서 호출부는 필드 반영을 모두 마친 뒤 마지막에 호출한다.
 *
 * ## 범위 밖
 *
 * SF 트리거의 나머지 부수 효과(전화번호 미러링 `Phone__c = HomePhone__c`, 전문행사조 허용값 검증)
 * 는 경로별로 이미 개별 구현되어 있어 본 정책에 포함하지 않는다. 잠금 축만 다룬다.
 */
object EmployeeLockingPolicy {

    /** SF `lockingFlagException` 보호 직군. jobCode 는 H10060 라벨 변환 후 한글값. */
    private val PROTECTED_JOB_CODES: Set<String> = FemaleStaffJobCode.ALL_CODES

    /** SF `lockingFlagException` 보호 권한. role 은 AppAuthority picklist 한글 raw value. */
    private val PROTECTED_APP_AUTHORITIES: Set<String> = setOf(AppAuthority.WOMAN, AppAuthority.LEADER)

    private const val STATUS_RETIRED = "퇴직"

    /**
     * [employee] 에 잠금 축 부수 효과를 적용한다. 필드 반영이 끝난 뒤, 저장 직전에 호출한다.
     */
    fun applyBeforeSave(employee: Employee) {
        // SF cls:40 — 잠금 ON → 앱 로그인 OFF
        if (employee.lockingFlag == true) {
            employee.appLoginActive = false
        }
        // SF cls:45-53 — 현장 여사원 직군 보호 (잠금 해제 + 앱 로그인 복원)
        if (isProtected(employee)) {
            employee.lockingFlag = false
            employee.appLoginActive = true
        }
    }

    /** SF `lockingFlagException` 의 3중 AND 조건. */
    private fun isProtected(employee: Employee): Boolean =
        employee.jobCode in PROTECTED_JOB_CODES &&
            employee.status != STATUS_RETIRED &&
            employee.role in PROTECTED_APP_AUTHORITIES
}
