package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Org
import org.springframework.data.jpa.repository.JpaRepository

interface OrgRepository : JpaRepository<Org, Long>
