package com.otoki.powersales.schedule.service.dto

import com.otoki.powersales.schedule.entity.Appointment

/**
 * 인사발령 INSERT 도메인 결과.
 *
 * 부분 실패 시멘틱 — 행 단위 검증 실패는 트랜잭션 롤백 없이 [failures] 누적 후 성공 행만 saveAll.
 *
 * - [savedAppointments] : 적재된 entity 목록. 어댑터가 후처리 트리거 ([com.otoki.powersales.sap.inbound.service.AppointmentUserProfileUpdater])
 *   호출 시 사용한다 (Spec #635 P2-B 어댑터/도메인 분리).
 */
data class AppointmentInsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<AppointmentInsertFailedRow>,
    val savedAppointments: List<Appointment>
)
