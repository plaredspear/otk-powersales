package com.otoki.powersales.user.service

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
 * `User` 조직 표시 필드(`branch` / `division` / `department` / `title` / `hr_code`) 부팅 catch-up.
 *
 * ## 배경
 * 해당 컬럼들은 SF Stage1 마이그레이션 적재 시점에만 값이 들어왔고, 그 이후 신규 생성되는 User
 * (SAP 사원 마스터 인바운드 → [UserProvisioningService]) 에는 채우는 경로가 아예 없어 계속 비어 있었다.
 * 원인은 SF `AppointmentTriggerHanlder.cls:313-323` 의 조직 필드 대입 블록이 신규 이식 시
 * ProfileId 산출 부분만 옮겨지고 누락된 것.
 *
 * 이식 누락은 [UserOrgDisplayFieldsSynchronizer] 로 해소되지만, 그 경로는 SF 와 동일하게 **발령 수신
 * 시에만** 동작하므로 이미 비어 있는 기존 행은 다음 발령까지 그대로 남는다. 본 Runner 가 그 존량을
 * 따라잡는다.
 *
 * ## 동작
 * `branch` / `division` 이 둘 다 비어 있는 User 의 사번으로 Employee 를 찾아
 * [UserOrgDisplayFieldsSynchronizer.sync] 를 호출한다 — 조직 필드 매핑(SF 정합)을 한 곳에만 두기 위해
 * 로직을 복제하지 않는다. 해당 메서드는 조직 cascade lookup 실패 시 무변경이므로, 조직 마스터 미적재
 * 사번은 자연히 skip 된다.
 *
 * 표시 필드만 건드리고 `profileId` / `isSalesSupport` 재산출은 하지 않는다 — 운영자가 web admin
 * "프로파일 변경" 으로 수동 지정한 Profile 을 catch-up 이 덮어쓰면 안 되기 때문. 권한 파생 캐시
 * 재산출은 발령 후처리
 * ([com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater.updateUserProfileCache])
 * 전용이다.
 *
 * ## 운영 특성
 * - **멱등**: INSERT 없이 기존 User 행 UPDATE 만 한다. 동기화된 행은 `division` 이 채워져 다음 부팅부터
 *   조회 대상에서 빠지고, 정상 상태에서는 빈 결과 SELECT 1회로 끝난다.
 * - **다중 인스턴스**: dev/prod 다중 인스턴스가 동시에 부팅하면 같은 대상을 각자 UPDATE 한다. 값이
 *   동일해 결과는 같고 두 번째부터는 dirty check 에서 걸러지므로 별도 분산 락을 두지 않는다.
 * - **실행 순서**: 다른 sync runner 에 의존하지 않는다 (Organization / Employee 는 SAP·SF 적재분을
 *   읽기만 하고, 미적재면 무변경 후 다음 부팅에 재시도). [Order] 는 그 "의존 없음 / 나중 실행" 의도 표시.
 * - `local` 은 시드 계정만 있어 실행 의미가 없고 시드 순서 의존을 만들지 않기 위해 dev/prod 만 대상으로 한다.
 */
@Component
@Profile("dev | prod")
@Order(100)
class UserOrgFieldsBackfillRunner(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val userOrgDisplayFieldsSynchronizer: UserOrgDisplayFieldsSynchronizer,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val codes = try {
            userRepository.findEmployeeCodesWithoutOrgDisplayFields()
        } catch (e: Exception) {
            // 부팅을 막지 않는다 — 표시 전용 필드라 실패해도 다음 발령/다음 부팅에서 따라잡는다.
            log.warn("User 조직 표시 필드 catch-up 대상 조회 실패: {}", e.message, e)
            return
        }
        if (codes.isEmpty()) return

        var applied = 0
        // 청크 단위 트랜잭션 — 대상이 많아도 단일 트랜잭션에 전량을 물고 있지 않도록 나눈다.
        // 청크별로 예외를 삼켜 한 청크의 실패가 남은 청크의 catch-up 을 막지 않게 한다.
        codes.chunked(CHUNK_SIZE).forEach { chunk ->
            applied += try {
                transactionTemplate.execute { backfillChunk(chunk) } ?: 0
            } catch (e: Exception) {
                log.warn("User 조직 표시 필드 catch-up 청크 실패 — 다음 청크 계속: size={}, error={}", chunk.size, e.message, e)
                0
            }
        }
        log.info("User 조직 표시 필드 catch-up 완료: target={}, applied={}", codes.size, applied)
    }

    /** @return 실제 동기화한 행 수. 사원 미매칭 / 조직 마스터 미적재로 무변경인 건은 제외. */
    private fun backfillChunk(employeeCodes: List<String>): Int {
        val usersByCode = userRepository.findByEmployeeCodeIn(employeeCodes)
            .filter { it.employeeCode != null }
            .associateBy { it.employeeCode!! }
        var applied = 0
        employeeRepository.findByEmployeeCodeIn(employeeCodes).forEach { employee ->
            val user = employee.employeeCode?.let { usersByCode[it] } ?: return@forEach
            if (userOrgDisplayFieldsSynchronizer.sync(user, employee)) applied++
        }
        return applied
    }

    companion object {
        private const val CHUNK_SIZE = 500
    }
}
