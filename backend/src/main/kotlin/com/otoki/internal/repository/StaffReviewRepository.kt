package com.otoki.internal.repository

import com.otoki.internal.entity.StaffReview
import org.springframework.data.jpa.repository.JpaRepository

interface StaffReviewRepository : JpaRepository<StaffReview, Long>
