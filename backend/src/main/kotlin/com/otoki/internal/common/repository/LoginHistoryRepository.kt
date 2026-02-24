package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.LoginHistory
import com.otoki.internal.common.entity.LoginHistoryId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 로그인 이력 Repository
 *
 * 쓰기 전용 — save()만 사용하며, 조회 메서드는 없다.
 */
interface LoginHistoryRepository : JpaRepository<LoginHistory, LoginHistoryId>
