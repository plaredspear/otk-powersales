package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.SiteActivity
import org.springframework.data.jpa.repository.JpaRepository

interface SiteActivityRepository :
    JpaRepository<SiteActivity, Long>,
    SiteActivityRepositoryCustom {

    fun findByIdAndIsDeletedFalse(id: Long): SiteActivity?
}
