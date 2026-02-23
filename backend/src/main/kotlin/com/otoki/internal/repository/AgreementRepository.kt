package com.otoki.internal.repository

import com.otoki.internal.entity.Agreement
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementRepository : JpaRepository<Agreement, Long>
