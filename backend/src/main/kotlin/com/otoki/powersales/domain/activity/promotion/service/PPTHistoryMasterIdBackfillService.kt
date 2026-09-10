package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.repository.PPTHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 전문행사조 이력의 원인 마스터 FK(`master_id`) 백필 도구 (개발자 도구 전용).
 *
 * SF 이력 오브젝트(`ProfessionalPromotionTeamHistory__c`) 에는 마스터 참조 필드가 없어 이관된 이력은
 * 이 FK 가 전부 비어 있다. 신규 시스템이 만든 이력만 [AdminPPTMasterService.updateEmployeeTeam] 에서
 * 채운다. 마스터 삭제 / 핵심필드 수정 가드는 이 FK 로 "사원에 반영된 마스터인가" 를 판정하므로,
 * 백필 전에는 **운영 중 유효 마스터의 대다수인 이관분이 가드를 통과해 삭제**된다 (2026-09 장애 재발 경로).
 *
 * - [preview]: 채울 수 있는 건수 / 후보가 둘 이상이라 건너뛸 건수만 조회 (변경 없음).
 * - [backfill]: 후보가 정확히 1건인 이력만 이력 id 오래된 순으로 최대 limit 건 갱신. 이미 채워진 행은
 *   대상에서 빠지므로 **재실행해도 결과가 같다**(멱등). 대상이 많으면 잔여가 0 이 될 때까지 반복한다.
 *
 * 매칭 규칙은 추정이다 — 같은 사원 + 같은 전문행사조 + 변경 시각(KST)이 마스터 기간 안.
 * 그래서 부팅 자동 실행이 아니라 사람이 preview 수치를 보고 판단하는 수동 도구로 둔다.
 */
@Service
class PPTHistoryMasterIdBackfillService(
    private val pptHistoryRepository: PPTHistoryRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 백필 대상 건수 — backfillable(단일 매칭) / ambiguous(후보 다중, 건드리지 않음). */
    @Transactional(readOnly = true)
    fun preview(): BackfillPreview {
        val counts = pptHistoryRepository.countMasterIdBackfillTargets()
        return BackfillPreview(backfillable = counts.backfillable, ambiguous = counts.ambiguous)
    }

    /**
     * 단일 매칭 이력의 `master_id` 를 채운다 (이력 id 오래된 순, 최대 limit 건).
     * @return 갱신 건수 + 실행 후 잔여 대상 건수.
     */
    @Transactional
    fun backfill(limit: Int): BackfillResult {
        val safeLimit = limit.coerceIn(1, MAX_LIMIT_PER_RUN)
        val updated = pptHistoryRepository.backfillMasterIds(safeLimit)
        val after = pptHistoryRepository.countMasterIdBackfillTargets()
        log.info(
            "전문행사조 이력 master_id 백필: 상한 {}건, {}건 갱신, 잔여 {}건 (모호 {}건)",
            safeLimit, updated, after.backfillable, after.ambiguous,
        )
        return BackfillResult(
            updated = updated,
            remaining = after.backfillable,
            ambiguous = after.ambiguous,
        )
    }

    data class BackfillPreview(
        /** 후보가 정확히 1건이라 채울 수 있는 이력 수. */
        val backfillable: Long,
        /** 후보가 둘 이상이라 대상에서 제외되는 이력 수 — 크면 매칭 규칙을 좁혀야 한다는 신호. */
        val ambiguous: Long,
    )

    data class BackfillResult(
        /** 이번 실행에서 실제로 `master_id` 가 채워진 건수. */
        val updated: Int,
        /** 실행 후에도 남은 단일 매칭 대상 건수 (0 이면 완료). */
        val remaining: Long,
        /** 후보 다중이라 계속 남는 건수 — 재실행해도 줄지 않는다. */
        val ambiguous: Long,
    )

    companion object {
        /** 한 번 실행에서 처리하는 상한 (과도한 UPDATE / 락 시간 방지). 초과분은 재실행으로 소진. */
        const val MAX_LIMIT_PER_RUN = 5000
        const val DEFAULT_LIMIT = 1000
    }
}
