package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PPTHistoryRepository : JpaRepository<ProfessionalPromotionTeamHistory, Long>, PPTHistoryRepositoryCustom {

    /**
     * name(전문행사조 이력 번호) 채번 — SF AutoNumber(displayFormat PH{0000000}) 재현.
     * 시퀀스 nextval 단독. 마스터(PM) 채번과 동일 패턴.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * 전문행사조 sync 배치(매일 01:00)가 사원마다 본 채번을 호출하므로 hot path 다.
     */
    @Query(
        value = "SELECT nextval('powersales.professional_promotion_team_history_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_ppt_history_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.professional_promotion_team_history_name_seq',
                GREATEST(
                    nextval('powersales.professional_promotion_team_history_name_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.professional_promotion_team_history
                          WHERE name ~ '^PH[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long

    /**
     * 해당 마스터로 인해 사원 전문행사조가 실제로 바뀐 적이 있는지 — 삭제 / 핵심필드 수정 가드의 판정 기준.
     *
     * 이력은 사원 값이 바뀔 때만 쌓이므로 true 면 "운영에 반영된 마스터" 다. SF 이관 이력은 원본 오브젝트에
     * 마스터 참조 필드 자체가 없어 `master_id` 가 비어 있으므로, 백필 전에는 이관분이 판정되지 않는다.
     */
    fun existsByMasterId(masterId: Long): Boolean

    /**
     * 주어진 마스터 중 반영 이력이 있는 id 만 추린다 — 목록 응답의 `applied` 플래그용.
     * 행마다 [existsByMasterId] 를 호출하지 않도록 페이지 단위로 1회 조회한다.
     */
    @Query("SELECT DISTINCT h.masterId FROM ProfessionalPromotionTeamHistory h WHERE h.masterId IN :masterIds")
    fun findAppliedMasterIds(@Param("masterIds") masterIds: Collection<Long>): List<Long>

    /**
     * 이관 이력의 원인 마스터 FK 백필 대상 건수 — 개발자 도구 preview 용 (변경 없음).
     *
     * SF 이력 오브젝트에는 마스터 참조 필드가 없어 이관분은 `master_id` 가 비어 있다. 같은 사원 +
     * 같은 전문행사조 + 변경 시각(KST)이 마스터 기간 안이면 그 마스터가 원인이라고 본다.
     * 후보가 둘 이상인 이력([PPTHistoryBackfillCounts.ambiguous])은 오연결 방지를 위해 대상에서 뺀다.
     */
    @Query(value = BACKFILL_CANDIDATE_SQL_PREVIEW, nativeQuery = true)
    fun countMasterIdBackfillTargets(): PPTHistoryBackfillCounts

    /**
     * 백필 실행 — 후보가 정확히 1건인 이력만 `master_id` 를 채운다 (이력 id 오래된 순 최대 limit 건).
     *
     * 이미 채워진 행은 조건(`master_id IS NULL`)에서 빠지므로 재실행해도 결과가 같다(멱등).
     * @return 실제 갱신 row 수.
     */
    @Modifying
    @Query(value = BACKFILL_UPDATE_SQL, nativeQuery = true)
    fun backfillMasterIds(@Param("limit") limit: Int): Int

    companion object {
        /**
         * 이력 → 원인 마스터 후보 매칭 SQL 조각.
         *
         * `카레세일조` 는 표시명이 바뀐 유형이라 DB 에 이전 표시명(`카레행사조`)으로 적재된 행이 공존한다
         * ([com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType.legacyAliases])
         * — 두 문자열을 같은 값으로 취급해야 매칭이 새지 않는다.
         * `changed_at` 은 timestamptz 이고 마스터 기간은 date 라 KST 로 변환해 비교한다
         * (반영 배치가 시작일 당일 01:00 KST 에 도는 것 정합).
         */
        private const val BACKFILL_CANDIDATE_SQL = """
            SELECT h.professional_promotion_team_history_id AS history_id,
                   m.professional_promotion_team_master_id  AS master_id
              FROM powersales.professional_promotion_team_history h
              JOIN powersales.professional_promotion_team_master m
                ON m.employee_id = h.employee_id
               AND ( m.team_type = h.new_value
                  OR (m.team_type IN ('카레세일조', '카레행사조')
                      AND h.new_value IN ('카레세일조', '카레행사조')) )
               AND (h.changed_at AT TIME ZONE 'Asia/Seoul')::date >= m.start_date
               AND (m.end_date IS NULL OR (h.changed_at AT TIME ZONE 'Asia/Seoul')::date <= m.end_date)
             WHERE h.professional_promotion_team_master_id IS NULL
               AND h.new_value IS NOT NULL
        """

        const val BACKFILL_CANDIDATE_SQL_PREVIEW = """
            SELECT count(*) FILTER (WHERE cnt = 1) AS backfillable,
                   count(*) FILTER (WHERE cnt > 1) AS ambiguous
              FROM (
                    SELECT history_id, count(*) AS cnt
                      FROM ( $BACKFILL_CANDIDATE_SQL ) c
                     GROUP BY history_id
                   ) t
        """

        const val BACKFILL_UPDATE_SQL = """
            WITH uniq AS (
                SELECT history_id, min(master_id) AS master_id
                  FROM ( $BACKFILL_CANDIDATE_SQL ) c
                 GROUP BY history_id
                HAVING count(*) = 1
                 ORDER BY history_id
                 LIMIT :limit
            )
            UPDATE powersales.professional_promotion_team_history h
               SET professional_promotion_team_master_id = u.master_id,
                   updated_at = now()
              FROM uniq u
             WHERE h.professional_promotion_team_history_id = u.history_id
        """
    }
}

/** [PPTHistoryRepository.countMasterIdBackfillTargets] 결과 projection. */
interface PPTHistoryBackfillCounts {
    /** 후보가 정확히 1건이라 이번 실행으로 채울 수 있는 이력 수. */
    val backfillable: Long

    /** 후보가 둘 이상이라 건드리지 않는 이력 수 — 많으면 매칭 규칙을 좁혀야 한다는 신호. */
    val ambiguous: Long
}
