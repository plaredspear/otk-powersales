package com.otoki.powersales.domain.org.organization.branchmapping.repository

import com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping
import org.springframework.data.jpa.repository.JpaRepository

interface BranchMappingRepository : JpaRepository<BranchMapping, String>
