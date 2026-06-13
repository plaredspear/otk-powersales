package com.otoki.powersales.domain.support.education.repository

import com.otoki.powersales.domain.support.education.entity.EducationViewHistory
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 조회 이력 Repository
 */
interface EducationViewHistoryRepository : JpaRepository<EducationViewHistory, Long>
