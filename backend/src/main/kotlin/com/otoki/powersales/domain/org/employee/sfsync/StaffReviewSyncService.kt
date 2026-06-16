package com.otoki.powersales.domain.org.employee.sfsync

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.StaffReview
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.repository.StaffReviewRepository
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SF 사원평가 마스터(`StaffReview__c`) fetch 결과를 신규 DB 에 upsert 하는 서비스.
 *
 * - **매칭 키**: SF 레코드 Id([StaffReviewFetchDto.sfid]). sfid 가 비어 있는 row 는 skip.
 * - 기존 row 존재 → 평가 점수/근무유형/캐시 컬럼 UPDATE. 없으면 INSERT.
 * - 사원(employee FK)은 [StaffReviewFetchDto.employeeSfid](= DKRetail_EmployeeId__c) 로 resolve.
 *   미매칭이면 INSERT 시 employee 미연결로 적재(평가 데이터 자체는 보존), UPDATE 시 기존 FK 보존.
 * - audit FK(createdBy/lastModifiedBy)는 SF User Id buffer(*_sfid)만 채우고 신규 User 매핑은 하지 않는다.
 * - **삭제 미반영**: SF fetch 결과에 없는 기존 row 는 건드리지 않는다 (upsert only).
 *
 * ## 충돌 처리
 * `sfid` 는 unique 제약. 동일 사이클 내 중복은 [LinkedHashMap] dedupe 로 방어하나, 본 sync 와 다른 적재
 * 경로(마이그레이션 / admin)가 동시에 같은 sfid 를 INSERT 하면 `saveAll` commit 시 제약 위반으로
 * 해당 트랜잭션이 전량 롤백된다. 동시 적재 경로가 추가되면 건별 격리 또는 upsert 전략을 검토할 것.
 */
@Service
class StaffReviewSyncService(
    private val fetchClient: StaffReviewFetchClient,
    private val repository: StaffReviewRepository,
    private val employeeRepository: EmployeeRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * SF fetch → upsert 전체 경로 (배치 진입점).
     *
     * @param modDt SF 조회 기준 일자 (YYYYMMDD). 미지정 시 오늘 — 주기 배치가 당일 변경분을 가져온다.
     */
    @Transactional
    fun sync(
        context: ScheduledJobRunContext? = null,
        modDt: String = LocalDate.now().format(MOD_DT_FORMAT),
    ): SyncResult {
        val fetched = fetchClient.fetch(modDt)
        return syncRecords(fetched, context)
    }

    /**
     * fetch 결과 리스트를 받아 upsert 수행 (핵심 경로 — 단위 테스트에서 직접 호출).
     *
     * @return upsert 통계
     */
    @Transactional
    fun syncRecords(
        fetched: List<StaffReviewFetchDto>,
        context: ScheduledJobRunContext? = null,
    ): SyncResult {
        if (fetched.isEmpty()) {
            context?.metadata(mapOf("fetched" to 0, "inserted" to 0, "updated" to 0, "skipped" to 0))
            return SyncResult(fetched = 0, inserted = 0, updated = 0, skipped = 0)
        }

        // 1) sfid(매칭 키) 보유 row 만 통과. 없으면 skip.
        val keyed = fetched.mapNotNull { dto ->
            val sfid = dto.sfid?.takeIf { it.isNotBlank() }
            if (sfid == null) {
                log.warn("[staff-review-sync] sfid(매칭 키) 부재 — skip. name={}", dto.name)
                null
            } else {
                sfid to dto
            }
        }
        val skippedNoKey = fetched.size - keyed.size

        // 2) 사원 SF Id → Employee 일괄 resolve (N+1 회피).
        val employeeSfids = keyed.mapNotNull { it.second.employeeSfid }.filter { it.isNotBlank() }.distinct()
        val employeeBySfid: Map<String, Employee> =
            if (employeeSfids.isEmpty()) emptyMap()
            else employeeRepository.findBySfidIn(employeeSfids)
                .filter { it.sfid != null }
                .associateBy { it.sfid!! }

        // 3) 기존 row 일괄 조회 (sfid IN).
        val sfids = keyed.map { it.first }.distinct()
        val existingBySfid: Map<String, StaffReview> =
            repository.findBySfidIn(sfids)
                .filter { it.sfid != null }
                .associateBy { it.sfid!! }

        var inserted = 0
        var updated = 0
        val toSave = mutableListOf<StaffReview>()

        // 동일 sfid 가 fetch 응답에 중복으로 오는 경우 마지막 값이 이기도록 LinkedHashMap 으로 dedupe.
        val deduped = LinkedHashMap<String, StaffReviewFetchDto>()
        keyed.forEach { (sfid, dto) -> deduped[sfid] = dto }

        for ((sfid, dto) in deduped) {
            val employee = dto.employeeSfid?.let { employeeBySfid[it] }
            val existing = existingBySfid[sfid]
            if (existing != null) {
                applyTo(existing, dto, employee)
                toSave += existing
                updated++
            } else {
                toSave += newEntity(sfid, dto, employee)
                inserted++
            }
        }

        repository.saveAll(toSave)

        val result = SyncResult(
            fetched = fetched.size,
            inserted = inserted,
            updated = updated,
            skipped = skippedNoKey,
        )
        context?.metadata(
            mapOf(
                "fetched" to result.fetched,
                "inserted" to result.inserted,
                "updated" to result.updated,
                "skipped" to result.skipped,
            )
        )
        log.info(
            "[staff-review-sync] 완료 — fetched={}, inserted={}, updated={}, skipped={}",
            result.fetched, result.inserted, result.updated, result.skipped,
        )
        return result
    }

    /** 신규 INSERT 엔티티 생성 — 키(sfid/name) 는 생성자, 나머지는 [applyTo] 로 채운다. */
    private fun newEntity(
        sfid: String,
        dto: StaffReviewFetchDto,
        employee: Employee?,
    ): StaffReview = StaffReview(
        sfid = sfid,
        name = dto.name,
    ).also { applyTo(it, dto, employee) }

    /** 평가 점수/근무유형/캐시 컬럼 + audit sfid 갱신. sfid/name(키) 은 갱신 대상 제외. */
    private fun applyTo(
        entity: StaffReview,
        dto: StaffReviewFetchDto,
        employee: Employee?,
    ) {
        entity.employeeSfid = dto.employeeSfid
        entity.employeeName = dto.employeeName
        entity.employeeCode = dto.employeeCode
        entity.employeeType = dto.employeeType
        entity.entryDate = dto.entryDate
        entity.branchReviewSfid = dto.branchReviewSfid
        entity.employeeTotalScore = dto.employeeTotalScore
        entity.jikwee = dto.jikwee
        entity.jobCode = dto.jobCode
        entity.firstDayOfMonth = dto.firstDayOfMonth
        entity.branch = dto.branch
        entity.costCenterCode = dto.costCenterCode
        entity.workingCategory1 = dto.workingCategory1
        entity.workingCategory2 = dto.workingCategory2
        entity.workingCategory3 = dto.workingCategory3
        entity.displayEventGoalScore = dto.displayEventGoalScore
        entity.priorityItemEventScore = dto.priorityItemEventScore
        entity.productManageCallmentScore = dto.productManageCallmentScore
        entity.instructionDisobedienceScore = dto.instructionDisobedienceScore
        entity.accountPartnershipScore = dto.accountPartnershipScore
        entity.attendanceScore = dto.attendanceScore
        entity.clothesHygieneScore = dto.clothesHygieneScore
        entity.educationEvaluationScore = dto.educationEvaluationScore
        entity.createdBySfid = dto.createdBySfid
        entity.lastModifiedBySfid = dto.lastModifiedBySfid
        // 사원이 resolve 된 경우에만 연결 갱신 — 미매칭(employee=null) 시 기존 FK 를 보존한다.
        // (Employee 적재가 본 sync 보다 지연되는 경우 등에서 매 사이클 기존 연결을 끊지 않도록.)
        employee?.let { entity.employee = it }
    }

    data class SyncResult(
        val fetched: Int,
        val inserted: Int,
        val updated: Int,
        val skipped: Int,
    )

    companion object {
        /** SF Request Body MOD_DT 형식 (YYYYMMDD). */
        private val MOD_DT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
