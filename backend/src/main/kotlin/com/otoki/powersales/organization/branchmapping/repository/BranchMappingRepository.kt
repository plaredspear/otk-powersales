package com.otoki.powersales.organization.branchmapping.repository

import com.otoki.powersales.organization.branchmapping.entity.BranchMapping
import org.springframework.data.jpa.repository.JpaRepository

interface BranchMappingRepository : JpaRepository<BranchMapping, String>
