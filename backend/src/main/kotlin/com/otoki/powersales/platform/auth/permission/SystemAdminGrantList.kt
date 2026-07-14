package com.otoki.powersales.platform.auth.permission

/**
 * 사번 기준 시스템 관리자 지정 목록 SoT (declarative reference data).
 *
 * "어떤 사번이 시스템 관리자여야 하는가" 의 단일 진실은 SF 원본 Profile 이 아니라 본 코드 상수다.
 * 여기 등재된 사번은 SF User.Profile (`profile_sfid`) 이 `9. Staff` 등 비관리자 Profile 이더라도,
 * 신규 운영 정책상 시스템 관리자로 격상하기로 결정된 계정이다.
 * CLAUDE.md §4 reference data 정책 정합 (Flyway INSERT 금지 / Kotlin SoT + 부팅/마이그레이션 sync 패턴).
 * `LeaderProfileFlagsSeed` / `SystemAdminProfilePolicy` 와 동일 접근.
 *
 * ## 왜 SF override 로는 안 되고 SoT 인가
 * SF 마이그레이션 reconcile ([com.otoki.powersales._migration.sf.service.SfMigrationStage2Service.runUserProfileSfidReconcile])
 * 은 `profile_sfid → profile.sfid` 조인으로 SF User.Profile 을 profile_id 최종 권위로 override 한다.
 * 그러나 본 목록의 사번들은 SF 상 `9. Staff` (비관리자) 라, override 를 그대로 적용하면 관리자 권한이
 * 박탈된다. reset 직후에는 현재 profile_id 가 아직 `시스템 관리자` 가 아니므로 reconcile 의 "현재
 * 관리자면 보존" 가드도 걸리지 않는다. 따라서 "현재 상태 보존" 이 아니라 "지정 사번 강제 격상" 이
 * 필요하며, 그 지정 출처가 본 SoT 다. reconcile substep 이 override 시 본 목록을 제외하고,
 * 이후 본 목록을 `시스템 관리자` profile_id 로 upsert 하여 reset 후에도 멱등 재현한다.
 *
 * ## 운영 편집과의 관계
 * web admin 에서 개별 사용자의 profile_id 를 바꾸는 화면은 (현재) 존재하지 않으므로 dirty-skip 개념이
 * 없다. 신규 관리자 추가/제외는 본 목록을 갱신하는 것으로 관리한다.
 */
object SystemAdminGrantList {

    /**
     * 시스템 관리자로 강제 지정할 사번 (employee_code) 집합.
     *
     * SF 마이그레이션으로 이관되는 실사번 계정 중, SF Profile 과 무관하게 신규 운영에서 시스템 관리자로
     * 운영하기로 결정된 사원. (부트스트랩 `ADMIN-` 계정은 [com.otoki.powersales.platform.common.config.ProdAdminBootstrapInitializer]
     * 가 별도로 생성하므로 여기 포함하지 않는다.)
     */
    val EMPLOYEE_CODES: Set<String> = setOf(
        "20000531", // 강현배
        "20020553", // 황도연
        "20050269", // 박민경
        "20070066", // 강기원
        "20190075", // 유인철
        "20210359", // 하헌준
        "20210360", // 한수진
        "20240208", // 장윤아
    )
}
