package com.otoki.powersales.agreement.service

import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * GPS 동의문구(AgreementWord) 6개월 재동의 강제 cycle 처리 서비스 (스펙 #654).
 *
 * ## 레거시 매핑
 * - SF Apex: `AgreementWordBatch.cls` (start / execute / finish)
 * - SF Apex: `AgreementWordSchedule.cls` (cron Schedulable, `0 0 9 * * ? *`)
 * - flow-legacy: `docs/specs/completed/651-agreement-gps-consent-cycle-batch-audit/flow-legacy-cycle-batch.yaml`
 * - origin spec: #654 (#651 audit 후속 인계)
 *
 * ## 레거시 동작 요약
 * 1. 입력: 매일 09:00 cron 발화 (Schedulable). 실시간 system date.
 * 2. SOQL: `Active__c = True OR (ActiveDate__c = null AND AfterActiveDate__c = today)` — 활성 약관 + 오늘 도래 후보.
 * 3. 분류: `Active=true` → key 1 / `AfterActiveDate=today` → key 2 (Map<Integer, AgreementWord>).
 * 4. 분기:
 *    - **rotation** (size > 1, key 1+2 모두 존재): oldObj.Active=False + AfterActiveDate=null / newObj.Active=True + ActiveDate=AfterActiveDate + AfterActiveDate=+6개월. cascade reset 발화.
 *    - **new-only** (size==1, key 2 만 존재 + AfterActiveDate=today): newObj 활성화 + 6개월 갱신. cascade reset 발화.
 *    - **self-renewal** (size==1, key 1 만 존재 + AfterActiveDate=today): 활성 유지하면서 ActiveDate=AfterActiveDate + AfterActiveDate=+6개월. cascade reset 발화. (운영상 매우 드문 케이스 — 새 약관이 등록되지 않은 fallback)
 *    - **no-op** (size==1, key 1 만 + AfterActiveDate≠today): DML 0건. cascade reset 미발화.
 * 5. cascade reset (`isReset=true`): 전 사원 SOQL → `if (emp.AgreementFlag__c) emp.AgreementFlag__c = false;` → empList 추가 → `update empList`.
 *    - 부수 효과: 다음 로그인 시 WebInterceptor 가 `/home/gps` redirect 게이트를 통과시켜 GPS 재동의 화면 강제.
 *
 * ## 신규 차이
 * - **Q2 cascade 좁히기 (옵션 1 권고)**: legacy 의 전 사원 SOQL → `WHERE agreement_flag=true` 좁히기 (QueryDSL bulk update). legacy `cls:70` 들여쓰기 버그 (변경 안 된 사원도 empList 에 추가) 자연 회피 + DML rows 절감. 결과 동등. 참조: `legacy-deviation.md` 미등록 (legacy 동등 결과).
 * - **Q3 all-or-nothing (옵션 2)**: `@Transactional` 단일 트랜잭션. 1건 fail 시 전체 rollback. retry queue / 알림 인프라 불요. legacy 동등 (legacy `update empList` allOrNone default true).
 * - **Q5 Active 단일성 강제 (옵션 3)**: DB partial unique index 미도입. 운영 데이터 신뢰. legacy 동등 (DB constraint 부재).
 * - **Q6 hardcoded 6개월 (옵션 3)**: `today.plusMonths(6)` 코드 박제. legacy 동등 (`AgreementWordBatch.cls:41,56`). 외부화 도입 부담 회피.
 * - **Q8 cascade 단일 컬럼 (옵션 3)**: `agreement_flag` 단일 컬럼만 reset. `EmployeeInfo.lastAgreementNumber` 는 의미 무관 (사용자 마지막 동의 약관 번호 낙찰 필드 — GPS 동의 비트와 무관).
 */
@Service
class AgreementWordCycleService(
    private val agreementWordRepository: AgreementWordRepository,
    private val employeeRepository: EmployeeRepository
) {
    private val log = LoggerFactory.getLogger(AgreementWordCycleService::class.java)

    /**
     * cycle batch 1회 실행.
     *
     * @return 실행 결과 통계 (분기 / cascade row 수). cron batch finish 모니터링 (#548 ScheduledJobRunner metadata) 용.
     */
    @Transactional
    fun runCycle(): Result {
        val today = LocalDate.now(TimeZones.SEOUL_ZONE)
        val candidates = agreementWordRepository.findActiveOrDueCandidates(today)

        val (oldActive, dueCandidate) = classifyCandidates(candidates, today)
        val branch = applyRotation(oldActive, dueCandidate, today)

        val resetCount: Long = if (branch.cascadeReset) {
            employeeRepository.resetAgreementFlagForActiveConsents()
        } else {
            0L
        }

        val result = Result(branch = branch, resetCount = resetCount)
        log.info(
            "AGREEMENT_WORD_CYCLE_BATCH today={} branch={} resetCount={}",
            today, result.branch.name, result.resetCount
        )
        return result
    }

    /**
     * 후보를 활성(`Active=true`) / 도래(`AfterActiveDate=today` AND `ActiveDate=null`) 두 분류로 분리한다.
     *
     * 운영상 활성 1건 가정 (#654 Q5). 다건이 들어오면 첫 번째 활성을 사용하고 나머지는 무시 (legacy 의 마지막 덮어쓰기와 다르나 운영상 발생 불가 가정).
     */
    private fun classifyCandidates(
        candidates: List<AgreementWord>,
        today: LocalDate
    ): Pair<AgreementWord?, AgreementWord?> {
        val oldActive = candidates.firstOrNull { it.active == true }
        val dueCandidate = candidates.firstOrNull {
            it.active != true && it.activeDate == null && it.afterActiveDate == today
        }
        return oldActive to dueCandidate
    }

    /**
     * 분기 적용 + cascade reset 여부 결정. 영속 entity 의 도메인 메서드만 호출 (dirty checking 신뢰).
     *
     * - **rotation**: 활성 + 도래 둘 다 존재. oldActive deactivate + dueCandidate activate.
     * - **new-only**: 도래만 존재. dueCandidate activate.
     * - **self-renewal**: 활성만 존재 + AfterActiveDate=today. oldActive 를 그대로 다시 activate (= 활성 유지 + ActiveDate 갱신 + AfterActiveDate+6M).
     * - **no-op**: 그 외. 변경 없음 + cascade 미발화.
     */
    private fun applyRotation(
        oldActive: AgreementWord?,
        dueCandidate: AgreementWord?,
        today: LocalDate
    ): Branch {
        return when {
            oldActive != null && dueCandidate != null -> {
                oldActive.deactivate()
                dueCandidate.activate(today)
                Branch.ROTATION
            }
            oldActive == null && dueCandidate != null -> {
                dueCandidate.activate(today)
                Branch.NEW_ONLY
            }
            oldActive != null && oldActive.afterActiveDate == today -> {
                oldActive.activate(today)
                Branch.SELF_RENEWAL
            }
            else -> Branch.NO_OP
        }
    }

    enum class Branch(val cascadeReset: Boolean) {
        ROTATION(true),
        NEW_ONLY(true),
        SELF_RENEWAL(true),
        NO_OP(false)
    }

    data class Result(val branch: Branch, val resetCount: Long)
}
