package com.otoki.internal.admin.dto

/**
 * 관리자 데이터 조회 범위
 * @param branchCodes 조회 가능한 지점 코드 목록 (costCenterCode 기준)
 * @param isAllBranches true면 전체 조회 (branchCodes 무시)
 */
data class DataScope(
    val branchCodes: List<String>,
    val isAllBranches: Boolean
)
