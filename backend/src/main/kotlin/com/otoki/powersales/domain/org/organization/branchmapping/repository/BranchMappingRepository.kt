package com.otoki.powersales.domain.org.organization.branchmapping.repository

import com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping
import org.springframework.data.jpa.repository.JpaRepository

interface BranchMappingRepository : JpaRepository<BranchMapping, String> {

    /** 지점 코드 맵핑 조회 화면용 — 지점코드 오름차순 전건. 운영 74건 규모라 페이징 없음. */
    fun findAllByOrderByBranchCodeAsc(): List<BranchMapping>
}
