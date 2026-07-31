package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.platform.push.dto.PushTargetEmployee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
interface NoticeRepositoryCustom {

    fun findNotices(
        category: NoticeCategory?,
        search: String?,
        branchCode: String,
        pageable: Pageable
    ): Page<Notice>

    /**
     * web admin 목록 조회 — 발행/임시저장, 전 지점 공지를 모두 포함한다.
     *
     * @param branchCodes 지점 조회 조건. 지정 시 `notice.branchCode` 가 이 집합에 속한 공지만
     *   (지점공지 한정 결과가 된다). 호출부가 선택 지점코드를
     *   [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander] 로 확장해 넘긴다.
     *   null/빈 목록이면 지점 조건 없음.
     */
    fun findAllNotices(
        category: NoticeCategory?,
        search: String?,
        branchCodes: List<String>?,
        pageable: Pageable
    ): Page<Notice>

    fun findRecentNotices(
        branchCode: String
    ): List<Notice>

    /**
     * 공지 push 대상 사원(사원ID + FCM 토큰)을 조회한다.
     *
     * 대상 = 해당 공지가 앱 목록에 노출되는 사용자와 동일 (조회 노출 규칙 정합):
     * - 회사공지(COMPANY)/교육(EDUCATION): FCM 토큰 보유 전 사용자
     * - 지점공지(BRANCH): costCenterCode 가 공지 branchCode 와 일치하는 사용자만
     *
     * 배지(미확인 푸시 건수)는 사원별 값이라 토큰만으로는 계산할 수 없어 사원ID 를 함께 반환한다.
     *
     * @param category 공지 카테고리
     * @param branchCode 지점공지일 때 매칭할 지점코드 (그 외 카테고리는 무시)
     */
    fun findPushTargets(
        category: NoticeCategory,
        branchCode: String?
    ): List<PushTargetEmployee>

    /**
     * 공지 push 발송 대상 사원 수를 조회한다 (발송 전 예상 대상 수 표시용).
     *
     * 선별 규칙은 [findPushTargetTokens] 와 동일 (동일 WHERE 조건의 count).
     * 지점공지인데 branchCode 가 비면 0 (오발송 방지 규칙 정합).
     *
     * @param category 공지 카테고리
     * @param branchCode 지점공지일 때 매칭할 지점코드 (그 외 카테고리는 무시)
     */
    fun countPushTargets(
        category: NoticeCategory,
        branchCode: String?
    ): Long
}
