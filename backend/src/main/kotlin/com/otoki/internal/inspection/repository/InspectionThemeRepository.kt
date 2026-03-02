package com.otoki.internal.inspection.repository

import com.otoki.internal.inspection.entity.InspectionTheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 현장 점검 테마 Repository
 */
@Repository
interface InspectionThemeRepository : JpaRepository<InspectionTheme, Long>, InspectionThemeRepositoryCustom
