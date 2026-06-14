package com.otoki.powersales.domain.activity.schedule.service.dto

import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo

/**
 * 출근 정보 INSERT 도메인 결과 (단일 청크 단위).
 *
 * - [savedAttendInfos] : 적재된 entity 목록. 어댑터가 후처리 (Schedule 변환) 호출 시 사용.
 *
 * 청크 분할 / 청크 commit 실패 / Schedule 변환 트리거 / audit 분기는 모두 어댑터 책임.
 */
data class AttendInfoInsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<AttendInfoInsertFailedRow>,
    val savedAttendInfos: List<AttendInfo>
)
