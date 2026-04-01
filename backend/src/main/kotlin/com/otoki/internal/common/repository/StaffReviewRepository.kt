package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.StaffReview
import org.springframework.data.jpa.repository.JpaRepository

interface StaffReviewRepository : JpaRepository<StaffReview, Int>
