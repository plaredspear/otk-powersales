package com.otoki.powersales._migration.sf.service

import com.otoki.powersales.platform.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS
import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed
import com.otoki.powersales.platform.auth.permission.SystemAdminGrantList
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.policy.TemporaryPasswordPolicy
import com.otoki.powersales.domain.org.organization.branchmapping.BranchMappingSupplement
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SF 데이터 마이그레이션 Stage 2 — Logical 변환 (1회성 cut-over, 런칭 후 폐기).
 *
 * 본 클래스는 scripts/sf-data-migration/migrate-stage2.main.kts 의 backend 흡수본이다.
 * 운영 서버에서 실행되어 RDS 와의 latency 를 단축한다.
 *
 * 구현 substep:
 * - 2-B picklist : User.cost_center_code derived 캐시 동기화만 (Employee.role enum 변환은 spec #807 폐기)
 * - 2-C password : BCrypt password hash (sfid IS NOT NULL AND password NULL 인 user).
 *                  초기 평문은 사번 기반 `"{사번}@pwrs"` ([TemporaryPasswordPolicy]) — 최초 로그인 시 변경 강제.
 *
 * 2-A FK resolve 는 별도 클래스 (SfMigrationStage2FkService) 로 분리.
 * 2-D permission 은 spec #801 SF 권한 모델 전면 적용으로 폐기 — user_permission 테이블 자체 폐기.
 * 2-B user.profile_type substep 은 spec #806 의 ProfileType destructive 폐기로 제거.
 * 2-B employee.role substep 은 spec #807 의 UserRoleEnum destructive 폐기로 제거 —
 * SF AppAuthority picklist value (한글) 가 곧 저장값이라 변환 substep 자체가 불필요.
 */
@Service
class SfMigrationStage2Service(
    @PersistenceContext private val em: EntityManager,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        /**
         * `leader-profile-flags` substep 의 적용 대상 profile.name.
         *
         * [LeaderProfileFlagsSeed.SEEDS] 는 조장 계열 2종(`6.조장` / `7.영업사원 + 조장`) 을 정의하나,
         * 본 substep 은 그중 `6.조장` 만 적용한다 (사용자 결정). `7.영업사원 + 조장` 은 web admin
         * 권한 편집으로 수동 처리. 대상 확대 시 본 집합에 이름을 추가하면 된다.
         */
        val LEADER_FLAGS_TARGET_PROFILE_NAMES = setOf("6.조장")

        /**
         * `leader-erp-org-revoke` substep 이 조장 `object_permissions` 에서 **제거**할 SF object key.
         *
         * [com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed] 의 `6.조장` SoT 에서도
         * 동일하게 제외되어 있다 — 본 집합은 **이미 운영 DB 에 적재된 권한을 회수**하는 축이다
         * (SoT 수정만으로는 dirty row 가 갱신되지 않으므로).
         *
         * - `ERP_Order__c` / `ERP_OrderProduct__c` → 가드 entity `erp_order` (ERP주문 목록/상세)
         * - `Org__c` → 가드 entity `organization` (조직마스터 조회)
         * - `DKRetail__CommuteLog__c` → 가드 entity `attendance_log` (근무 등록현황 목록/상세)
         * - `DKRetail__AlternativeHoliday__c` → 가드 entity `alternative_holiday` (대체휴무)
         * - `AttendInfo__c` → 가드 entity `attend_info` (기준정보 > HR 적재 근무기간)
         * - `DailySalesHistory__c` → 가드 entity `daily_sales_history` (기준정보 > ORORA 일매출)
         * - `MonthlySalesHistory__c` → 가드 entity `monthly_sales_history` (기준정보 > ORORA 월매출)
         *
         * 대체휴무는 SoT 에 기재된 적이 없지만 운영 DB 의 web admin 편집분(dirty row)에 남아 있을 수 있어
         * 회수 대상에 포함한다 — 없으면 jsonb `-` 가 no-op 이라 무해하다.
         *
         * `AttendInfo__c` 회수는 기준정보 > HR 적재 근무기간 화면**만** 닫는다. 인사/근무 > 근무기간
         * 조회는 `work_history` 가상 자원(AdminWorkHistoryController, custom_permissions 경로) 으로
         * 분리되어 조장에게 READ 가 부여되므로 조회 화면과 지점/사원 셀렉터는 계속 동작한다
         * — 조장은 근무 실적 조회는 하되 SAP HR 적재 마스터는 보지 않는다 (사용자 결정).
         *
         * `DailySalesHistory__c` 회수는 기준정보 > ORORA 일매출 화면**만** 닫는다 — 이 키를 쓰는
         * endpoint 가 그 화면의 목록/거래처 lookup 2건뿐이다.
         *
         * `MonthlySalesHistory__c` 회수 파급은 **기준정보 > ORORA 월매출 1화면**이다 (전용 거래처
         * 셀렉터 `/accounts/lookup-for-monthly-sales` 포함). 이 키는 원래 5화면을 함께 여닫아 화면 단위
         * 통제가 불가능했으나, 두 차례 자원 분리로 나머지 4화면이 빠져나갔고 모두
         * [GRANTED_LEADER_CUSTOM_PERMISSIONS] 로 별도 부여되어 본 회수의 영향을 받지 않는다:
         *
         * - 매출/실적 대시보드 3화면(물류배부/전산실적/POS) → `sales_dashboard`
         * - 월별 진열사원 투입적합성 / 진열사원 배치 적합성 → `display_employee_adequacy`
         *   (각 화면 전용 지점 셀렉터 포함)
         *
         * 자원 분리가 없었다면 이 회수가 그 4화면까지 닫았을 것이다 — 조장에게 적합성 2화면을 열어주려면
         * ORORA 월매출도 함께 열 수밖에 없었다.
         */
        val REVOKED_LEADER_OBJECT_KEYS = listOf(
            "ERP_Order__c",
            "ERP_OrderProduct__c",
            "Org__c",
            "DKRetail__CommuteLog__c",
            "DKRetail__AlternativeHoliday__c",
            "AttendInfo__c",
            "DailySalesHistory__c",
            "MonthlySalesHistory__c",
        )

        /**
         * `leader-sales-dashboard-grant` substep 이 조장 `custom_permissions` 에 **병합**할 가상 자원 권한.
         *
         * [REVOKED_LEADER_OBJECT_KEYS] 의 정반대 축 — 이쪽은 **이미 운영 DB 에 적재된 권한에 키를 더한다**
         * (SoT 수정만으로는 dirty row 가 갱신되지 않으므로).
         *
         * - `sales_dashboard` → 매출/실적 > 월 매출(물류배부) / 월 매출(전산실적) / POS매출 3화면
         *   (`com.otoki.powersales.admin.controller.SALES_DASHBOARD_RESOURCE`, @PermissionResource).
         *   3화면이 적재 테이블 entity `monthly_sales_history` 를 공유하던 것을 화면 전용 가상 자원으로
         *   분리하면서, 기존에 `MonthlySalesHistory__c` READ 로 3화면을 보던 조장이 그대로 보려면 신규
         *   자원을 다시 부여해야 한다 (사용자 결정). 3화면 모두 조회 전용이라 READ 단독.
         * - `display_employee_adequacy` → 월별 진열사원 투입적합성 / 진열사원 배치 적합성 2화면
         *   (`com.otoki.powersales.admin.controller.DISPLAY_EMPLOYEE_ADEQUACY_RESOURCE`, @PermissionResource).
         *   같은 `monthly_sales_history` 에서 같은 이유로 분리했다 — 그 키를 조장에게 주면 기준정보 >
         *   ORORA 월매출까지 함께 열려 화면 단위 통제가 불가능했다. 조장은 적합성 2화면만 조회하고
         *   ORORA 월매출은 계속 닫는다 (사용자 결정). 각 화면 전용 지점 셀렉터
         *   (`/sales/input-adequacy/branches` · `/sales/deployment/branches`) 도 같은 자원으로 가드되어
         *   함께 열린다. 2화면 모두 조회 전용이라 READ 단독.
         *
         * SF object 가 아닌 가상 자원이라 `object_permissions` 가 아니라 `custom_permissions` 경로다.
         * 추가 부여가 필요하면 본 JSON 에 키를 더하면 된다 — jsonb `||` 가 top-level 키 단위로 병합하므로
         * 기재된 키만 덮어쓰고 나머지 운영 편집분은 보존된다.
         *
         * [com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed] 의 `6.조장` SoT 에도
         * 동일 키가 기재되어 있다 — 신규 환경(clean row)은 `leader-profile-flags` 가, 기존 환경(dirty row)은
         * 본 substep 이 담당해 양쪽 결과가 같아진다.
         */
        const val GRANTED_LEADER_CUSTOM_PERMISSIONS =
            """{"sales_dashboard": {"allowRead": true}, "display_employee_adequacy": {"allowRead": true}}"""

        /**
         * `leader-password-reset` substep 의 profile 무관 초기화 대상 사번 (사용자 지정).
         *
         * 조장 profile 이 아니지만 cut-over 시 비밀번호 초기화가 필요한 계정을 명시 열거한다.
         * 조장 대상과 합집합으로 처리되며 중복은 자동 제거된다. 대상 변경 시 본 집합만 수정.
         */
        val MANUAL_PASSWORD_RESET_EMPLOYEE_CODES = setOf(
            "20000531",
            "20020553",
            "20190075",
            "20210359",
            "20210360",
            "20240208",
            "20070066",
            "20050269",
        )
    }

    /**
     * Stage 2-B picklist — User.cost_center_code derived 캐시 동기화만.
     */
    @Transactional
    fun runPicklistMapping(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        results += runUserCostCenterCodeSync().results
        results += runPptMasterBranchCodeSync().results

        return SfMigrationStage2Response(
            substep = "picklist",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * Stage 2-B (user.cost_center_code) — Employee.cost_center_code → User.cost_center_code derived 캐시 동기화.
     *
     * 상관 서브쿼리 형태 — H2 / PostgreSQL 양쪽 모두 표준 SQL 로 동작.
     */
    @Transactional
    fun runUserCostCenterCodeSync(): SfMigrationStage2Response {
        val rows = em.createNativeQuery(
            """
            UPDATE powersales."user"
            SET cost_center_code = (
                SELECT e.cost_center_code FROM powersales.employee e
                WHERE e.employee_code = powersales."user".employee_code
            )
            WHERE employee_code IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM powersales.employee e
                WHERE e.employee_code = powersales."user".employee_code
              )
            """.trimIndent()
        ).executeUpdate()
        return singleResultResponse(
            substep = "picklist.user_cost_center_code",
            label = "User.cost_center_code (sync from Employee)",
            rows = rows,
        )
    }

    /**
     * Stage 2-B (professional_promotion_team_master.branch_code) —
     * Employee.cost_center_code → ProfessionalPromotionTeamMaster.branch_code derived 동기화.
     *
     * branch_code 는 SF `CostCenterCode__c`(라벨 "조직유형") 에 매핑되어 있으나, 해당 SF 필드는
     * 운영에서 한 번도 채워지지 않은 dead field (extract 전 행 빈값) 라 SF 적재분의 branch_code 가
     * 전부 NULL 이다. 신규 등록 로직(AdminPPTMasterService.createMaster)은 사원의 cost_center_code 로
     * branch_code 를 채우므로, 마이그레이션 적재분도 동일 출처(employee_id → Employee.cost_center_code)
     * 로 동기화해 정합을 맞춘다.
     *
     * 상관 서브쿼리 형태 — H2 / PostgreSQL 양쪽 모두 표준 SQL 로 동작.
     * 멱등: branch_code IS NULL 한정이라 이미 채워진 row (신규 등록분 포함) 는 건드리지 않는다.
     */
    @Transactional
    fun runPptMasterBranchCodeSync(): SfMigrationStage2Response {
        val rows = em.createNativeQuery(
            """
            UPDATE powersales.professional_promotion_team_master ppt
            SET branch_code = (
                SELECT e.cost_center_code FROM powersales.employee e
                WHERE e.employee_id = ppt.employee_id
            )
            WHERE ppt.branch_code IS NULL
              AND EXISTS (
                SELECT 1 FROM powersales.employee e
                WHERE e.employee_id = ppt.employee_id
                  AND e.cost_center_code IS NOT NULL
              )
            """.trimIndent()
        ).executeUpdate()
        return singleResultResponse(
            substep = "picklist.ppt_master_branch_code",
            label = "ProfessionalPromotionTeamMaster.branch_code (sync from Employee)",
            rows = rows,
        )
    }

    private fun singleResultResponse(substep: String, label: String, rows: Int): SfMigrationStage2Response =
        SfMigrationStage2Response(
            substep = substep,
            results = listOf(SubstepResult(label = label, rowsAffected = rows)),
            totalRowsAffected = rows,
        )

    /**
     * Stage 2-A (polymorphic parent) — UploadFile 의 record_sfid 로 부모 entity 를 찾아
     * parent_type (엔티티명) + parent_id (Long FK) 를 동시에 채운다.
     *
     * **record_sfid 직접 조인 방식**: SF Object__c (object_type) 는 모바일 등록 경로(claim /
     * site_activity)에서 미설정(NULL)이라 신뢰할 수 없다. 대신 record_sfid (부모 SObject Id)
     * 를 각 부모 테이블의 sfid 와 조인해 **실제 매칭되는 테이블**을 부모로 확정한다. SF Id 는
     * 전역 유니크라 한 record_sfid 는 최대 한 부모 테이블에만 매칭된다 (충돌 없음).
     * object_type 은 SF 원본 보존용으로만 유지하며 본 resolve 의 분기 키로 쓰지 않는다.
     *
     * 일반 FK resolve (`SfMigrationStage2FkService`) 는 `*_sfid` → `*_id` 1:1 패턴만 처리하므로,
     * UploadFile 처럼 한 `record_sfid` 컬럼이 여러 entity 를 가리키는 polymorphic 케이스는 본
     * substep 이 별도 처리한다.
     *
     * 매핑 표는 [UPLOAD_FILE_POLYMORPHIC_PARENTS] (SoT) — entityName → (refTable, refIdColumn).
     * 한 entry 당 한 UPDATE 실행하며, 재호출 시 `parent_id IS NULL` 조건으로 멱등성 확보.
     */
    @Transactional
    fun runUploadFilePolymorphicParent(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        // record_sfid ↔ 부모 sfid 조인으로 parent_type (엔티티명) + parent_id 동시 설정.
        // object_type 무관. parent_id IS NULL 한정 (멱등) — 이미 연결된 row 는 건드리지 않는다.
        for ((entityName, spec) in UPLOAD_FILE_POLYMORPHIC_PARENTS) {
            val rows = em.createNativeQuery(
                """
                UPDATE powersales.upload_file uf
                SET parent_type = :entityName,
                    parent_id = c.${spec.refIdColumn}
                FROM powersales.${spec.refTable} c
                WHERE uf.record_sfid = c.sfid
                  AND uf.parent_id IS NULL
                """.trimIndent()
            )
                .setParameter("entityName", entityName)
                .executeUpdate()
            results += SubstepResult(
                label = "upload_file ($entityName ← record_sfid = ${spec.refTable}.sfid)",
                rowsAffected = rows,
            )
        }
        return SfMigrationStage2Response(
            substep = "uploadFilePolymorphicParent",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * 2-C password — SF 마이그레이션 대상 user 의 초기 비밀번호를 사번 기반
     * `"{사번}@pwrs"` ([TemporaryPasswordPolicy]) 의 BCrypt hash 로 채운다.
     *
     * 대상: `sfid IS NOT NULL AND (password IS NULL OR password = '')`. 멱등 (이미 채워진 row skip).
     * `password_change_required = TRUE` 로 최초 로그인 시 강제 변경 유도.
     *
     * 평문이 사번마다 다르므로 row 별로 encode 한다.
     */
    @Transactional
    fun runPasswordHash(): SfMigrationStage2Response {
        val codesQuery = em.createNativeQuery(
            "SELECT employee_code FROM powersales.\"user\" " +
                "WHERE sfid IS NOT NULL AND (password IS NULL OR password = '') " +
                "AND employee_code IS NOT NULL"
        )
        @Suppress("UNCHECKED_CAST")
        val codes = codesQuery.resultList as List<String>

        var totalUpdated = 0
        for (code in codes) {
            val hash = passwordEncoder.encode(TemporaryPasswordPolicy.forEmployeeCode(code))
            val updated = em.createNativeQuery(
                "UPDATE powersales.\"user\" SET password = :hash, password_change_required = TRUE " +
                    "WHERE employee_code = :code AND (password IS NULL OR password = '')"
            )
                .setParameter("hash", hash)
                .setParameter("code", code)
                .executeUpdate()
            totalUpdated += updated
        }

        return SfMigrationStage2Response(
            substep = "password",
            results = listOf(SubstepResult(label = "User.password (BCrypt)", rowsAffected = totalUpdated)),
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * 2-C leader/manual password reset — 조장 계열 + 지정 사번 user 의 비밀번호를 **강제 초기화**한다.
     *
     * ## [runPasswordHash] 와의 차이
     * [runPasswordHash] 는 `password IS NULL OR password = ''` 가드가 있어 **미설정 row 만** 채우는
     * 적재용 substep 이다. 본 메소드는 "초기화" 가 목적이라 **이미 설정된 비밀번호도 덮어쓴다**
     * (가드 없음). 따라서 재실행하면 대상자가 스스로 바꾼 비밀번호도 매번 초기값으로 되돌아간다 —
     * cut-over 시점에 1회 실행하는 운영 도구로 취급한다.
     *
     * ## 대상
     * - profile.name = [LEADER_FLAGS_TARGET_PROFILE_NAMES] (`6.조장`) 인 user.
     *   `7.영업사원 + 조장` 은 leader-profile-flags substep 과 동일하게 제외 (web admin 수동 처리).
     * - [MANUAL_PASSWORD_RESET_EMPLOYEE_CODES] 의 사번을 가진 user.
     *
     * 두 집합의 합집합이며 중복은 자동 제거된다(사번 기준 DISTINCT). SF 마이그레이션 대상이 아닌
     * (sfid 가 없는) user 도 사번이 일치하면 초기화한다 — 지정 사번은 명시 요청이므로 sfid 로 좁히지 않는다.
     *
     * 초기 평문은 [runPasswordHash] 와 동일하게 사번 기반 `"{사번}@pwrs"` ([TemporaryPasswordPolicy]) 이고,
     * `password_change_required = TRUE` 로 최초 로그인 시 변경을 강제한다. 평문은 응답에 담지 않는다
     * (화면이 사번으로 동일 규칙을 재조립해 안내한다 — [TemporaryPasswordPolicy] 정책).
     */
    @Transactional
    fun runLeaderPasswordReset(): SfMigrationStage2Response {
        val leaderCodes = selectEmployeeCodes(
            "SELECT u.employee_code FROM powersales.\"user\" u " +
                "JOIN powersales.profile p ON p.profile_id = u.profile_id " +
                "WHERE p.name IN (:names) AND u.employee_code IS NOT NULL",
        ) { it.setParameter("names", LEADER_FLAGS_TARGET_PROFILE_NAMES) }

        val manualCodes = selectEmployeeCodes(
            "SELECT u.employee_code FROM powersales.\"user\" u " +
                "WHERE u.employee_code IN (:codes)",
        ) { it.setParameter("codes", MANUAL_PASSWORD_RESET_EMPLOYEE_CODES) }

        val leaderUpdated = resetPasswords(leaderCodes)
        // 조장에도 포함된 사번은 이미 초기화되었으므로 카운트 중복을 피해 차집합만 별도 집계한다.
        val manualOnly = manualCodes - leaderCodes
        val manualUpdated = resetPasswords(manualOnly)

        val missingCodes = MANUAL_PASSWORD_RESET_EMPLOYEE_CODES - manualCodes
        val results = buildList {
            add(SubstepResult(label = "조장(6.조장) user.password 초기화", rowsAffected = leaderUpdated))
            add(SubstepResult(label = "지정 사번 user.password 초기화", rowsAffected = manualUpdated))
            if (missingCodes.isNotEmpty()) {
                // 사번이 신규 DB 에 없으면 초기화 대상이 없다 — 조용히 넘기지 않고 화면에 노출한다.
                add(
                    SubstepResult(
                        label = "미존재 사번 (초기화 안 됨): ${missingCodes.sorted().joinToString(", ")}",
                        rowsAffected = 0,
                    )
                )
            }
        }

        return SfMigrationStage2Response(
            substep = "leader-password-reset",
            results = results,
            totalRowsAffected = leaderUpdated + manualUpdated,
        )
    }

    /** 사번 목록 조회 헬퍼 — native query 결과를 non-null String 집합으로 정규화한다. */
    private fun selectEmployeeCodes(
        sql: String,
        bind: (jakarta.persistence.Query) -> Unit,
    ): Set<String> {
        val query = em.createNativeQuery(sql)
        bind(query)
        @Suppress("UNCHECKED_CAST")
        return (query.resultList as List<String?>).filterNotNull().toSet()
    }

    /**
     * [codes] 사번 user 의 비밀번호를 `"{사번}@pwrs"` BCrypt hash 로 **무조건 덮어쓴다**.
     * 평문이 사번마다 다르므로 row 별로 encode 한다(BCrypt salt 도 매번 랜덤).
     */
    private fun resetPasswords(codes: Collection<String>): Int {
        var updated = 0
        for (code in codes) {
            val hash = passwordEncoder.encode(TemporaryPasswordPolicy.forEmployeeCode(code))
            updated += em.createNativeQuery(
                "UPDATE powersales.\"user\" SET password = :hash, password_change_required = TRUE " +
                    "WHERE employee_code = :code"
            )
                .setParameter("hash", hash)
                .setParameter("code", code)
                .executeUpdate()
        }
        return updated
    }

    /**
     * Stage 2 (user.profile_id reconcile) — SF User.ProfileId(=user.profile_sfid) 를 profile_id 의 **최종 권위**로 강제 정합.
     *
     * ## 배경 (정합 사고)
     * 일반 FK Resolve([SfMigrationStage2FkService])는 `profile_id = COALESCE(t.profile_id, ...)`
     * + `WHERE (t.profile_id IS NULL ...)` 가드라 **profile_id 가 이미 채워져 있으면 덮어쓰지 않는다**.
     * 그런데 SAP 인바운드 provisioning([com.otoki.powersales.user.service.UserProvisioningService.profileIdFor])이
     * `employee.role`(SF AppAuthority picklist) 기반 fallback(`else -> "5.영업사원"`)으로 user 를 먼저 INSERT 하면,
     * FK Resolve 실행 시점에 profile_id 가 이미 `5.영업사원`/`9. Staff` 로 선점되어 SF 실제 Profile(예: `6.조장`)
     * 로 갱신되지 못한다. 즉 provisioning 과 마이그레이션이 같은 profile_id 컬럼을 두고 경쟁하고, `IS NULL`
     * 가드 탓에 "먼저 채운 쪽이 이기는" 구조가 되어 SF 권위가 뒤집힌다.
     *
     * ## 정합 원칙
     * SF 마이그레이션이 있는 사원(`profile_sfid` 보유)은 **SF User.Profile 이 최종 권위**다. 본 substep 은
     * `profile_sfid → profile.sfid` 조인으로 SF 정답 profile_id 를 산출해 `COALESCE` 없이 **무조건 override** 한다.
     * FK Resolve 를 재실행해도 provisioning 선점이 재발할 수 있으므로, cut-over 시점 fk substep **직후 1회**
     * 실행하여 profile_id 를 SF 정답으로 수렴시킨다. 멱등 (이미 일치하는 row 는 IS DISTINCT FROM 조건으로 skip).
     *
     * ## 시스템 관리자 격상 보존 + 지정 사번 강제 격상
     * 운영에서 SF Profile(`9. Staff` 등)보다 높게 격상된 `시스템 관리자` 계정 은 SF 정답(9.Staff)으로
     * 되돌리면 관리자 권한을 박탈하게 된다. 따라서 두 가지로 방어한다:
     * 1. **현재 profile 이 '시스템 관리자'면 override 대상에서 제외** (이미 격상된 계정 보존 — 예:
     *    [com.otoki.powersales.platform.common.config.ProdAdminBootstrapInitializer] 부트스트랩 계정).
     * 2. **[SystemAdminGrantList] 사번은 override 대상에서 제외 + 별도로 '시스템 관리자' 로 강제 upsert**.
     *    이 사번들은 SF 상 `9. Staff` (비관리자) 라 override 하면 관리자에서 탈락하고, DB reset 직후에는
     *    현재 profile 이 아직 관리자가 아니라 1번 가드도 안 걸린다. 따라서 "현재 상태 보존" 이 아니라
     *    "지정 사번 강제 격상" 이 필요하며, 그 지정 출처가 [SystemAdminGrantList] SoT 다. reset 후에도
     *    본 substep 이 멱등 재현한다.
     */
    @Transactional
    fun runUserProfileSfidReconcile(): SfMigrationStage2Response {
        val grantCodes = SystemAdminGrantList.EMPLOYEE_CODES

        // ① SF override — profile_sfid → SF 정답 profile_id 로 무조건 정합.
        //    단 (a) 이미 시스템 관리자로 격상된 계정, (b) SystemAdminGrantList 지정 사번 은 제외.
        val overrideRows = em.createNativeQuery(
            """
            UPDATE powersales."user" u
            SET profile_id = p_sf.profile_id
            FROM powersales.profile p_sf
            WHERE p_sf.sfid = u.profile_sfid
              AND u.profile_sfid IS NOT NULL
              AND u.profile_id IS DISTINCT FROM p_sf.profile_id
              AND u.employee_code NOT IN (:grantCodes)
              AND NOT EXISTS (
                SELECT 1 FROM powersales.profile p_now
                WHERE p_now.profile_id = u.profile_id
                  AND p_now.name = :sysAdminName
              )
            """.trimIndent()
        )
            .setParameter("grantCodes", grantCodes)
            .setParameter("sysAdminName", SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
            .executeUpdate()

        // ② 지정 사번 강제 격상 — SystemAdminGrantList 사번의 profile_id 를 '시스템 관리자' 로 upsert.
        //    이미 시스템 관리자면 IS DISTINCT FROM 으로 skip (멱등).
        val grantRows = em.createNativeQuery(
            """
            UPDATE powersales."user" u
            SET profile_id = p_admin.profile_id
            FROM powersales.profile p_admin
            WHERE p_admin.name = :sysAdminName
              AND u.employee_code IN (:grantCodes)
              AND u.profile_id IS DISTINCT FROM p_admin.profile_id
            """.trimIndent()
        )
            .setParameter("sysAdminName", SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
            .setParameter("grantCodes", grantCodes)
            .executeUpdate()

        return SfMigrationStage2Response(
            substep = "userProfileSfidReconcile",
            results = listOf(
                SubstepResult(
                    label = "User.profile_id (SF profile_sfid override — 관리자/지정 사번 제외)",
                    rowsAffected = overrideRows,
                ),
                SubstepResult(
                    label = "User.profile_id (시스템 관리자 지정 사번 강제 격상 — SystemAdminGrantList)",
                    rowsAffected = grantRows,
                ),
            ),
            totalRowsAffected = overrideRows + grantRows,
        )
    }

    /**
     * 조장 Profile 의 ProfileFlags 초기 권한 적용 — [LeaderProfileFlagsSeed] SoT 기준.
     *
     * ## 왜 부팅 Runner 가 아니라 Stage 2 substep 인가
     * 과거 `LeaderProfileFlagsSyncRunner` 가 부팅 시 동일 sync 를 수행했으나, Stage 1 적재 **이전**에
     * 실행되어 `findByProfileId` 가 SF row 를 못 찾고 (profile_name=NULL, profile_id=존재) row 를
     * 별도 create → Stage 1 의 (profile_name=존재, profile_id=NULL) row 와 공존 →
     * Stage 2 FK Resolve 가 profile_id 를 채우는 순간 `profile_flags_profile_id_key` UNIQUE 위반이
     * 발생했다 (운영 관측). 그래서 Runner 는 비활성화(@Component 미부착)되어 있다.
     *
     * 본 substep 은 그 sync 를 **사용자가 순서를 통제하는 Stage 2 시점**으로 옮긴 것이다. 호출 시점에는
     * 이미 Stage 1 적재 + `fk-natural-key` 가 profile_flags.profile_id 를 채워둔 상태라 create 분기가
     * 불필요하며, 실제로 **create 를 하지 않는다** (아래 skip 정책).
     *
     * ## 적용 규칙
     * - **row 부재 시 create 하지 않고 skip** — UNIQUE 충돌 재발 방지. row 는 Stage 1 SF 적재분이
     *   유일 출처이며, 부재는 "Stage 1/fk-natural-key 미완료" 를 뜻하므로 조용히 만들지 않고 보고한다.
     * - **`is_locally_modified = TRUE` (web admin 편집분) 은 skip** — 운영 편집 자율성 보존
     *   (SF 재적재 dirty-skip 정책과 동일).
     * - 그 외에는 SoT 값으로 update + `is_locally_modified = FALSE` 유지.
     *
     * ## 적용 대상
     * [LeaderProfileFlagsSeed.SEEDS] 중 **`6.조장` 단건**. `7.영업사원 + 조장` 은 web admin 수동 편집
     * 대상으로 남긴다 (사용자 결정). 대상 확대가 필요하면 [LEADER_FLAGS_TARGET_PROFILE_NAMES] 에 추가.
     *
     * ## 실행 순서
     * `fk` → `fk-natural-key` **이후**에 호출해야 한다. 그 전에는 profile_flags.profile_id 가 NULL 이라
     * profile 조인이 전건 미해소되어 skip 만 보고된다.
     *
     * 멱등 — 동일 값 재적용은 rowsAffected 0.
     */
    @Transactional
    fun runLeaderProfileFlags(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        for (seed in LeaderProfileFlagsSeed.SEEDS) {
            if (seed.profileName !in LEADER_FLAGS_TARGET_PROFILE_NAMES) continue

            // profile.name → profile_flags row 조회. Stage 1 적재 + fk-natural-key 로 profile_id 가
            // 채워져 있어야 매칭된다 (create 분기 없음 — 부재 시 skip).
            val existing = em.createNativeQuery(
                """
                SELECT pf.is_locally_modified
                FROM powersales.profile_flags pf
                JOIN powersales.profile p ON p.profile_id = pf.profile_id
                WHERE p.name = :profileName
                """.trimIndent()
            )
                .setParameter("profileName", seed.profileName)
                .resultList
                .firstOrNull() as? Boolean

            if (existing == null) {
                results += SubstepResult(
                    label = "profile_flags['${seed.profileName}'] skip — row 부재 " +
                        "(Stage 1 적재 / fk-natural-key 선행 필요)",
                    rowsAffected = 0,
                )
                continue
            }
            if (existing) {
                results += SubstepResult(
                    label = "profile_flags['${seed.profileName}'] skip — web admin 편집분 보존 " +
                        "(is_locally_modified=TRUE)",
                    rowsAffected = 0,
                )
                continue
            }

            val updated = em.createNativeQuery(
                """
                UPDATE powersales.profile_flags pf
                SET permissions_view_all_data = :viewAllData,
                    permissions_modify_all_data = :modifyAllData,
                    permissions_view_all_users = :viewAllUsers,
                    permissions_manage_users = :manageUsers,
                    permissions_api_enabled = :apiEnabled,
                    object_permissions = CAST(:objectPermissions AS jsonb),
                    custom_permissions = CAST(:customPermissions AS jsonb)
                FROM powersales.profile p
                WHERE p.profile_id = pf.profile_id
                  AND p.name = :profileName
                  AND pf.is_locally_modified = FALSE
                """.trimIndent()
            )
                .setParameter("viewAllData", seed.viewAllData)
                .setParameter("modifyAllData", seed.modifyAllData)
                .setParameter("viewAllUsers", seed.viewAllUsers)
                .setParameter("manageUsers", seed.manageUsers)
                .setParameter("apiEnabled", seed.apiEnabled)
                .setParameter("objectPermissions", seed.objectPermissionsJson)
                .setParameter("customPermissions", seed.customPermissionsJson)
                .setParameter("profileName", seed.profileName)
                .executeUpdate()

            results += SubstepResult(
                label = "profile_flags['${seed.profileName}'] SoT 적용 (LeaderProfileFlagsSeed)",
                rowsAffected = updated,
            )
        }

        return SfMigrationStage2Response(
            substep = "leaderProfileFlags",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * 조장(`6.조장`) 의 ERP주문 / 조직마스터 권한 회수 — **`is_locally_modified` 무시하고 강제 적용**.
     *
     * ## 왜 leader-profile-flags 와 분리하는가
     * [runLeaderProfileFlags] 는 `object_permissions` **전체를 SoT JSON 으로 덮어쓰므로**,
     * `is_locally_modified=TRUE` 인 web admin 편집분에 적용하면 운영에서 조정한 다른 권한까지
     * 함께 되돌아간다. 그래서 그쪽은 dirty row 를 skip 하는 정책을 유지한다.
     *
     * 본 substep 은 [REVOKED_LEADER_OBJECT_KEYS] **키만 JSON 에서 제거**한다 (jsonb `-` 연산자).
     * 나머지 키는 손대지 않으므로 dirty row 에 적용해도 운영 편집분이 보존된다 — 그래서 가드 없이
     * 강제 적용해도 안전하다 (사용자 결정).
     *
     * ## 대상 키
     * - `ERP_Order__c` / `ERP_OrderProduct__c` — ERP주문 (가드 entity `erp_order`)
     * - `Org__c` — 조직마스터 (가드 entity `organization`)
     * - `DKRetail__CommuteLog__c` — 근무 등록현황 (가드 entity `attendance_log`)
     * - `DKRetail__AlternativeHoliday__c` — 대체휴무 (가드 entity `alternative_holiday`)
     * - `AttendInfo__c` — 기준정보 > HR 적재 근무기간 (가드 entity `attend_info`)
     * - `DailySalesHistory__c` — 기준정보 > ORORA 일매출 (가드 entity `daily_sales_history`)
     * - `MonthlySalesHistory__c` — 기준정보 > ORORA 월매출 + 월별 진열사원 투입적합성 +
     *   진열사원 배치 적합성 (가드 entity `monthly_sales_history` — 3화면 공유 키라 함께 닫힌다)
     *
     * 매출/실적 대시보드 3화면(물류배부/전산실적/POS)은 `sales_dashboard` 가상 자원이라 본 회수와 무관하며,
     * [runLeaderSalesDashboardGrant] 로 별도 부여된다.
     *
     * 공휴일(`HolidayMaster__c`) / 영업일(`WorkingDayMaster__c`) 은 애초에 SoT 에 없어 회수 대상이 아니다.
     *
     * 멱등 — 이미 제거된 키는 jsonb `-` 가 no-op 이라 재실행해도 결과가 같다. 적용 후 권한 캐시는
     * 컨트롤러가 invalidate 한다 ([runLeaderProfileFlags] 와 동일 정책).
     */
    @Transactional
    fun runLeaderErpOrgPermissionRevoke(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        for (profileName in LEADER_FLAGS_TARGET_PROFILE_NAMES) {
            // jsonb `-` (key 제거) 를 키마다 연쇄 적용한다. 존재하지 않는 키는 no-op 이라 멱등.
            // is_locally_modified 가드 없음 — 키 단위 제거라 다른 편집분을 건드리지 않는다.
            //
            // SQL 작성 제약 2가지:
            // 1. jsonb 의 `?|` (key-exists-any) 를 쓰지 않는다 — Hibernate native query 파서가 `?` 를 JDBC
            //    ordinal 파라미터로 오인해 named 파라미터와 혼용 오류(ParameterRecognitionException).
            //    대신 "제거 결과가 원본과 다른가"(IS DISTINCT FROM) 로 동등하게 변경 대상을 판별한다.
            // 2. `- CAST(:keys AS text[])` 배열 형태를 쓰지 않는다 — H2(MODE=PostgreSQL) 가 text[] 캐스팅을
            //    파싱하지 못해 통합 테스트가 깨진다. 키를 개별 named 파라미터로 연쇄 제거해 양쪽 호환.
            val removalExpr = REVOKED_LEADER_OBJECT_KEYS.indices
                .fold("pf.object_permissions") { expr, i -> "($expr - :key$i)" }
            val query = em.createNativeQuery(
                """
                UPDATE powersales.profile_flags pf
                SET object_permissions = $removalExpr
                FROM powersales.profile p
                WHERE p.profile_id = pf.profile_id
                  AND p.name = :profileName
                  AND pf.object_permissions IS NOT NULL
                  AND $removalExpr IS DISTINCT FROM pf.object_permissions
                """.trimIndent()
            )
            REVOKED_LEADER_OBJECT_KEYS.forEachIndexed { i, key -> query.setParameter("key$i", key) }
            val updated = query
                .setParameter("profileName", profileName)
                .executeUpdate()

            results += SubstepResult(
                label = "profile_flags['$profileName'] ERP주문/조직마스터 권한 회수 " +
                    "(${REVOKED_LEADER_OBJECT_KEYS.joinToString(", ")})",
                rowsAffected = updated,
            )
        }

        return SfMigrationStage2Response(
            substep = "leaderErpOrgPermissionRevoke",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * 조장(`6.조장`) 에게 매출/실적 대시보드 권한(`sales_dashboard` READ) **부여** —
     * `is_locally_modified` 무시하고 강제 적용.
     *
     * ## 왜 leader-profile-flags 와 분리하는가
     * [runLeaderProfileFlags] 는 `object_permissions` / `custom_permissions` **전체를 SoT JSON 으로
     * 덮어쓰므로** dirty row 를 skip 한다. 운영 DB 의 조장 row 는 web admin 편집으로 이미
     * `is_locally_modified = TRUE` 일 가능성이 높아, SoT 에 키를 더하는 것만으로는 실제 부여가 되지 않는다.
     *
     * 본 substep 은 [GRANTED_LEADER_CUSTOM_PERMISSIONS] **키만 병합**한다 (jsonb `||`). 나머지 키는
     * 손대지 않으므로 dirty row 에 적용해도 운영 편집분이 보존된다 — [runLeaderErpOrgPermissionRevoke]
     * 가 키만 제거하기에 강제 적용이 안전한 것과 정확히 대칭이다.
     *
     * ## 배경
     * 적재 테이블 entity `monthly_sales_history` 가드를 공유하던 화면들을 두 차례에 걸쳐 화면 전용
     * 가상 자원으로 분리했고, 분리는 모두 승계 마이그레이션 없이 배포되므로(사용자 결정) 기존에 그
     * 화면들을 보던 조장은 본 substep 으로만 권한이 복구된다:
     *
     * - 매출/실적 3화면(물류배부 / 전산실적 / POS매출) → `sales_dashboard`
     * - 진열사원 적합성 2화면(월별 투입적합성 / 배치 적합성) → `display_employee_adequacy`
     *
     * 기준정보 > ORORA 월매출(`MonthlySalesHistory__c`) 은 분리 대상이 아니라 계속 회수 상태다 —
     * 조장은 적합성 2화면은 조회하되 ORORA 월매출은 보지 않는다.
     *
     * 부여 대상 자원이 늘어도 본 메소드는 그대로다 — [GRANTED_LEADER_CUSTOM_PERMISSIONS] JSON 을
     * 통째로 병합하므로 상수에 키를 더하면 된다 (substep 이름은 최초 도입 자원을 따라 유지).
     *
     * ## 실행 순서
     * `fk` → `fk-natural-key` **이후** (profile_flags.profile_id 가 채워진 뒤). 그 전에는 profile 조인이
     * 전건 미해소되어 0 row 로 보고된다.
     *
     * 멱등 — 이미 병합된 값은 `IS DISTINCT FROM` 가드에 걸려 재실행 시 0 row. 적용 후 권한 캐시는
     * 컨트롤러가 invalidate 한다 ([runLeaderProfileFlags] 와 동일 정책).
     */
    @Transactional
    fun runLeaderSalesDashboardGrant(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        for (profileName in LEADER_FLAGS_TARGET_PROFILE_NAMES) {
            // jsonb `||` (top-level 병합) — 기재된 키만 덮어쓰고 나머지 custom_permissions 는 보존한다.
            // custom_permissions 가 NULL 인 row 도 대상이라 COALESCE 로 빈 객체를 깔고 병합한다.
            // is_locally_modified 가드 없음 — 키 단위 병합이라 다른 편집분을 건드리지 않는다.
            //
            // revoke substep 과 동일한 SQL 제약을 따른다: jsonb `?`(key-exists) 계열을 쓰지 않고
            // "병합 결과가 원본과 다른가"(IS DISTINCT FROM) 로 변경 대상을 판별해 Hibernate 의
            // `?` → JDBC ordinal 파라미터 오인을 피한다.
            val mergedExpr = "(COALESCE(pf.custom_permissions, CAST('{}' AS jsonb)) || CAST(:grant AS jsonb))"
            val updated = em.createNativeQuery(
                """
                UPDATE powersales.profile_flags pf
                SET custom_permissions = $mergedExpr
                FROM powersales.profile p
                WHERE p.profile_id = pf.profile_id
                  AND p.name = :profileName
                  AND $mergedExpr IS DISTINCT FROM pf.custom_permissions
                """.trimIndent()
            )
                .setParameter("grant", GRANTED_LEADER_CUSTOM_PERMISSIONS)
                .setParameter("profileName", profileName)
                .executeUpdate()

            results += SubstepResult(
                label = "profile_flags['$profileName'] 매출/실적 대시보드 + 진열사원 적합성 권한 부여 " +
                    "(custom_permissions ← $GRANTED_LEADER_CUSTOM_PERMISSIONS)",
                rowsAffected = updated,
            )
        }

        return SfMigrationStage2Response(
            substep = "leaderSalesDashboardGrant",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * `branch_mapping` 누락 행 보정 적재 — SoT 는 [BranchMappingSupplement.ROWS].
     *
     * SF `BranchMapping__mdt` 원본 자체에 빠져 있어 Stage1 CSV 로는 들어올 수 없는 행을 채운다
     * (현재 `E5694` CVS전략팀 1건 — 근거는 [BranchMappingSupplement] KDoc).
     *
     * ## 실행 순서 / 멱등
     * Stage1 `BranchMapping` 적재와 순서 무관. Stage1 이 같은 테이블을 PK 충돌 `DO NOTHING` 으로
     * 멱등 적재하므로([com.otoki.powersales._migration.sf.stage1.Stage1Targets] BRANCH_MAPPING),
     * 본 substep 이 먼저 돌아도 Stage1 재적재가 보정 행을 덮거나 지우지 않는다. 반대로 본 substep 도
     * 이미 있는 행은 건드리지 않는다 — 운영자가 값을 손봤다면 그 값이 보존된다.
     *
     * ## 캐시
     * [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander] 는 부팅 1회
     * 메모리 캐시라 적재만으로는 반영되지 않는다. 컨트롤러가 실행 직후 `reload()` 한다
     * (Stage1 의 `affectsBranchCodeCache` 분기와 동일 정책).
     */
    @Transactional
    fun runBranchMappingSupplement(): SfMigrationStage2Response {
        val results = BranchMappingSupplement.ROWS.map { row ->
            // 효과는 Stage1 BRANCH_MAPPING 적재의 `ON CONFLICT (branch_code) DO NOTHING` 과 동일하나,
            // native query 는 dialect 문법을 그대로 넘기므로 (H2 는 ON CONFLICT 미지원) 존재 확인 +
            // INSERT 로 표현한다. 1회성 운영 도구 + 행 수 한 자릿수라 2 statement 비용은 무의미하다.
            val existing = (
                em.createNativeQuery(
                    "SELECT COUNT(*) FROM powersales.branch_mapping WHERE branch_code = :branchCode"
                )
                    .setParameter("branchCode", row.branchCode)
                    .singleResult as Number
                ).toLong()

            val inserted = if (existing > 0L) {
                0
            } else {
                em.createNativeQuery(
                    """
                    INSERT INTO powersales.branch_mapping (branch_code, included_branch_codes, label)
                    VALUES (:branchCode, :includedBranchCodes, :label)
                    """.trimIndent()
                )
                    .setParameter("branchCode", row.branchCode)
                    .setParameter("includedBranchCodes", row.includedBranchCodes)
                    .setParameter("label", row.label)
                    .executeUpdate()
            }

            SubstepResult(
                label = "branch_mapping['${row.branchCode}'] ← ${row.includedBranchCodes} (${row.label})",
                rowsAffected = inserted,
            )
        }

        return SfMigrationStage2Response(
            substep = "branchMappingSupplement",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }
}
