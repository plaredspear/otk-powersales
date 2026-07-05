package com.otoki.powersales.admin.dto

/**
 * 사용자 관리 화면(`/users`)의 프로파일 필터 드롭다운 옵션.
 *
 * 프로파일 관리 화면의 상세 목록(`/permissions/profiles`, `profile` READ 가드) 을 빌려쓰면
 * `user` 권한만 가진 관리자는 필터 로딩에서 403 이 난다. 사용자 관리 화면이 필요로 하는
 * 경량 lookup 이므로 화면 게이팅 권한(`user`)과 동일하게 가드한 전용 응답으로 분리한다
 * (id/name 만 노출 — 프로파일 권한 상세는 포함하지 않는다).
 */
data class AdminUserProfileOption(
    val id: Long,
    val name: String,
)
