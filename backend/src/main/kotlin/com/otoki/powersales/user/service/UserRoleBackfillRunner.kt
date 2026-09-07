package com.otoki.powersales.user.service

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * `User.user_role_id` 부팅 catch-up.
 *
 * ## 배경
 * `user_role_id` 는 SF Stage1 마이그레이션에서만 적재되고 런타임에 채우는 경로가 없었다. SF 레거시는
 * 발령 트리거(`AppointmentTriggerHanlder.updateUser`)가 조직명+직책으로 UserRole 을 배정하는데,
 * 신규 이식 시 같은 블록의 ProfileId 산출만 옮겨지고 UserRole 배정이 누락됐다
 * (조직 표시 필드 누락과 동일한 원인 — [UserOrgDisplayFieldsSynchronizer] 참조).
 *
 * 결과로 마이그레이션 이후 생성된 사용자는 조직도 밖에 놓여, sharing rule 의 `ROLE` 타겟 매칭과
 * role hierarchy 부여가 모두 죽고 OWD 가 `Private` 인 SObject(Account 등) 조회가 0건이 된다.
 *
 * 발령 경로는 [com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater] 복원으로
 * 해소되지만 다음 발령까지 기존 존량은 그대로다. 본 Runner 가 그 존량을 따라잡는다.
 *
 * ## 대상
 * `user_role_id IS NULL AND is_active AND sfid IS NULL` — 즉 **신규 생성분 중 활성** 사용자.
 * - `sfid IS NULL`: SF 에서도 비어 있던 마이그레이션분은 제외한다. SF 가 배정하지 않은 계정을 신규가
 *   새로 배정하면 레거시 이탈이 되므로 건드리지 않는다 (운영 결정, 2026-09-07).
 * - `is_active`: SF 는 퇴직 시 `UserRoleId = null` 로 지운다(`IF_REST_SAP_EmployeeMaster.cls:269`).
 *   비활성 계정의 null 은 정상 상태다.
 *
 * ## 재실행 안전성
 * 배정에 성공한 행은 `user_role_id` 가 채워져 **다음 부팅부터 조회 대상에서 빠진다** — 이미 갱신한
 * 대상을 다시 읽거나 덮어쓰지 않는다. 운영자가 SQL 로 먼저 채워둔 행도 같은 이유로 제외된다.
 *
 * 이름 매칭에 실패한 행만 다음 부팅에 다시 시도된다. 이는 의도한 동작이다 — `user_role` 마스터가
 * 뒤늦게 적재되거나 사원의 조직·직책이 바뀌면 그때 자동으로 채워진다. 재시도 비용은 대상자 수만큼의
 * 조직 조회(Redis 캐시 hit)이며, 실패 건수는 완료 로그에 남는다.
 *
 * `local` 은 시드 계정만 있어 실행 의미가 없고 시드 순서 의존을 만들지 않기 위해 dev/prod 만 대상으로 한다.
 */
@Component
@Profile("dev | prod")
@Order(101)
class UserRoleBackfillRunner(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val userOrgDisplayFieldsSynchronizer: UserOrgDisplayFieldsSynchronizer,
    private val userRoleAssignmentResolver: UserRoleAssignmentResolver,
    private val adminDataScopeCache: AdminDataScopeCache,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val codes = try {
            userRepository.findEmployeeCodesWithoutUserRole()
        } catch (e: Exception) {
            // 부팅을 막지 않는다 — 다음 발령/다음 부팅에서 따라잡는다.
            log.warn("UserRole catch-up 대상 조회 실패: {}", e.message, e)
            return
        }
        if (codes.isEmpty()) return

        val nameIndex = try {
            userRoleAssignmentResolver.loadNameIndex()
        } catch (e: Exception) {
            log.warn("UserRole 이름 색인 적재 실패 — catch-up 중단: {}", e.message, e)
            return
        }
        if (nameIndex.isEmpty()) {
            log.warn("UserRole 마스터가 비어 있어 catch-up 을 건너뛴다 — SF Stage1 적재 확인 필요")
            return
        }

        var applied = 0
        codes.chunked(CHUNK_SIZE).forEach { chunk ->
            applied += try {
                transactionTemplate.execute { backfillChunk(chunk, nameIndex) } ?: 0
            } catch (e: Exception) {
                log.warn("UserRole catch-up 청크 실패 — 다음 청크 계속: size={}, error={}", chunk.size, e.message, e)
                0
            }
        }
        log.info("UserRole catch-up 완료: target={}, applied={}, unresolved={}", codes.size, applied, codes.size - applied)
    }

    /** @return 실제 배정한 행 수. 사원/조직 미매칭 및 이름 미매칭 건은 제외 (기존 null 유지). */
    private fun backfillChunk(employeeCodes: List<String>, nameIndex: Map<String, Long>): Int {
        val usersByCode = userRepository.findByEmployeeCodeIn(employeeCodes)
            .filter { it.employeeCode != null }
            .associateBy { it.employeeCode!! }
        var applied = 0
        employeeRepository.findByEmployeeCodeIn(employeeCodes).forEach { employee ->
            val user = employee.employeeCode?.let { usersByCode[it] } ?: return@forEach
            // 이미 배정된 행은 조회 단계에서 빠지지만, 동시 부팅 인스턴스가 먼저 채운 경우를 위한 재확인.
            if (user.userRoleId != null) return@forEach
            val org = userOrgDisplayFieldsSynchronizer.resolveOrg(employee) ?: return@forEach
            val roleId = userRoleAssignmentResolver.resolveUserRoleId(employee, org, nameIndex)
            if (roleId == null) {
                log.info(
                    "UserRole 미매칭 — 기존 값 유지: employeeCode={}, orgName={}, jikchak={}",
                    employee.employeeCode, employee.orgName, employee.jikchak,
                )
                return@forEach
            }
            user.userRoleId = roleId
            // 데이터 스코프 산출 입력이라 즉시 invalidate — 안 하면 최대 5분간 이전 스코프가 남는다.
            adminDataScopeCache.invalidate(user.id)
            applied++
        }
        return applied
    }

    companion object {
        private const val CHUNK_SIZE = 500
    }
}
