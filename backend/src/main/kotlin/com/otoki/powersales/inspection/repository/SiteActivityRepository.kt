package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.SiteActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SiteActivityRepository :
    JpaRepository<SiteActivity, Long>,
    SiteActivityRepositoryCustom {

    fun findByIdAndIsDeletedFalse(id: Long): SiteActivity?

    /** SF Name AutoNumber(`SA{00000000}`) 채번용 sequence nextval. */
    @Query(value = "SELECT nextval('powersales.site_activity_name_seq')", nativeQuery = true)
    fun getNextNameSeq(): Long
}
