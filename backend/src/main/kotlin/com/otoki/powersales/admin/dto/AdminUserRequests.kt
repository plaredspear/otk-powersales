package com.otoki.powersales.admin.dto

/**
 * User 활성/비활성 토글 요청.
 */
data class UpdateUserActiveStatusRequest(
    val isActive: Boolean
)

/**
 * User 프로파일 수동 변경 요청 — 시스템 관리자 전용.
 *
 * 평시 `User.profileId` 는 SAP 발령 후처리
 * ([com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater.updateUserProfileCache])
 * 가 사원의 조직/직책으로부터 산출해 채운다. 본 요청은 발령을 기다릴 수 없는 예외 상황
 * (발령 예정일 이전 인수인계 등) 에 한해 그 산출값을 수동으로 덮어쓴다.
 */
data class UpdateUserProfileRequest(
    val profileId: Long
)
