package com.otoki.powersales.sales.dto.response

import com.otoki.powersales.sales.entity.WorkingDayMaster
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 영업일관리마스터 목록 행 (SF `WorkingDayMaster__c` 조회 동등).
 *
 * `workingDateCheck = 1` 이 영업일, 0 이 휴일(주말/공휴일). 운영 관리 화면은 조회 전용이다.
 */
data class WorkingDayMasterListItem(
    val id: Long,
    val name: String?,
    val workingDate: LocalDate?,
    val workingDateCheck: Double?,
    /** `workingDateCheck == 1.0` 여부 — 화면 표기(영업일/휴일) 편의용 파생 값. */
    val isWorkingDay: Boolean,
    val createdByName: String?,
    val lastModifiedByName: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: WorkingDayMaster): WorkingDayMasterListItem =
            WorkingDayMasterListItem(
                id = entity.id,
                name = entity.name,
                workingDate = entity.workingDate,
                workingDateCheck = entity.workingDateCheck,
                isWorkingDay = entity.isWorkingDay(),
                createdByName = entity.createdBy?.name,
                lastModifiedByName = entity.lastModifiedBy?.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

/**
 * 영업일관리마스터 목록 응답 — 조회 구간(연-월)별 일자 오름차순 행 + 영업일/휴일 집계.
 */
data class WorkingDayMasterListResponse(
    val content: List<WorkingDayMasterListItem>,
    /** 조회 구간 내 영업일 수 (`workingDateCheck = 1`). */
    val workingDayCount: Int,
    /** 조회 구간 내 휴일 수 (영업일 아닌 일자). */
    val holidayCount: Int,
) {
    companion object {
        fun from(entities: List<WorkingDayMaster>): WorkingDayMasterListResponse {
            val items = entities.map { WorkingDayMasterListItem.from(it) }
            val workingDayCount = items.count { it.isWorkingDay }
            return WorkingDayMasterListResponse(
                content = items,
                workingDayCount = workingDayCount,
                holidayCount = items.size - workingDayCount,
            )
        }
    }
}
