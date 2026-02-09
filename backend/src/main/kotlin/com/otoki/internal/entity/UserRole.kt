package com.otoki.internal.entity

/**
 * 사용자 권한 Enum
 */
enum class UserRole {
    USER,   // 일반 사용자 (영업사원)
    LEADER, // 조장 (팀장)
    ADMIN   // 관리자
}
