package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.StaffReview
import org.springframework.data.jpa.repository.JpaRepository

interface StaffReviewRepository : JpaRepository<StaffReview, Int>
