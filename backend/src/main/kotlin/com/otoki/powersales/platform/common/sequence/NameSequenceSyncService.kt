package com.otoki.powersales.platform.common.sequence

import com.otoki.powersales.domain.activity.inspection.repository.SiteActivityRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionProductRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * SF AutoNumber 재현 name/번호 시퀀스 8종을 "기존 데이터 최대 번호 위" 로 끌어올리는 일괄 보정.
 *
 * ## 왜 채번 시점이 아니라 여기인가
 * 과거에는 채번 쿼리 자체가 매번 `GREATEST(nextval, MAX(...)+1)` 로 보정했다. 시점 의존이 없다는
 * 장점이 있었지만, MAX 대상이 표현식이라 **채번 1회가 대상 테이블 전건 스캔**이었고
 * (team_member_schedule 은 1.76M row / 2.9GB), 특히 아래 경로들은 건별 채번을 루프에서 반복해
 * 스캔이 N 배로 증폭됐다 — 행사 확정 지연의 주원인.
 *
 * 조사 결과 **상시 운영 중 앱 밖에서 이 번호들을 주입하는 경로는 없다**:
 * SAP 인사발령 인바운드·스케줄 배치 20여 종 모두 앱 자체 채번(nextval)을 경유하고,
 * 상시 동작하는 SF→RDS 역방향 sync 도 없다. 번호가 외부에서 들어오는 시점은 **SF 마이그레이션
 * (Stage1 COPY — SF 원본 Name 을 그대로 적재)** 뿐이다.
 *
 * 따라서 채번은 `nextval` 단독으로 두고, MAX 보정은 아래 두 시점에만 수행한다:
 *  1. **부팅 1회** ([NameSequenceSyncRunner]) — 앱이 내려간 사이의 이관 / DB 복원 대비
 *  2. **SF Stage1 적재 직후** (`Stage1CopyController`) — 실제 주입 지점.
 *     Stage1 은 기동 중인 앱의 관리자 엔드포인트로 실행되므로 부팅 sync 만으로는 구멍이 남는다.
 *
 * ## 실패 격리
 * 시퀀스별로 **독립 트랜잭션**에서 실행한다. `setval` 은 롤백되지 않지만, 한 문장이 실패하면
 * 같은 트랜잭션의 후속 문장이 전부 `current transaction is aborted` 로 죽기 때문이다.
 * 개별 실패는 warn 로그만 남기고 나머지 시퀀스 보정을 계속한다.
 *
 * ## 멱등성
 * 각 보정 쿼리는 `GREATEST(nextval, MAX+1)` 이라 이미 시퀀스가 앞서 있으면 값이 그대로 유지되고
 * nextval 1개만 소모한다(번호 gap — SF AutoNumber 도 동일하게 gap 이 생기므로 정합).
 */
@Service
class NameSequenceSyncService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val promotionRepository: PromotionRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val pptMasterRepository: PPTMasterRepository,
    private val pptHistoryRepository: PPTHistoryRepository,
    private val appointmentRepository: AppointmentRepository,
    private val siteActivityRepository: SiteActivityRepository,
    private val transactionTemplate: TransactionTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 8종 시퀀스를 일괄 보정한다.
     *
     * @param reason 로그용 실행 계기 (예: "boot", "stage1-copy:TeamMemberSchedule").
     * @return 시퀀스 라벨 → 보정 후 값. 실패한 시퀀스는 맵에 포함되지 않는다.
     */
    fun syncAll(reason: String): Map<String, Long> {
        val synced = LinkedHashMap<String, Long>()
        for ((label, sync) in targets()) {
            try {
                val value = transactionTemplate.execute { sync() }
                if (value != null) synced[label] = value
            } catch (e: Exception) {
                log.warn("[name-seq-sync] {} 보정 실패 (reason={}): {}", label, reason, e.message)
            }
        }
        log.info("[name-seq-sync] reason={} 완료 — {}", reason, synced)
        return synced
    }

    /** 보정 대상 (라벨 = 번호 공간 식별용 `테이블.컬럼`). */
    private fun targets(): List<Pair<String, () -> Long>> = listOf(
        "team_member_schedule.name" to teamMemberScheduleRepository::syncNameSeq,
        "promotion_employee.name" to promotionEmployeeRepository::syncNameSeq,
        "promotion.promotion_number" to promotionRepository::syncPromotionNumberSeq,
        "promotion_product.name" to promotionProductRepository::syncNameSeq,
        "professional_promotion_team_master.name" to pptMasterRepository::syncNameSeq,
        "professional_promotion_team_history.name" to pptHistoryRepository::syncNameSeq,
        "appointment.name" to appointmentRepository::syncNameSeq,
        "site_activity.name" to siteActivityRepository::syncNameSeq,
    )
}
