package com.otoki.internal.repository

import com.otoki.internal.entity.HqReview
import org.springframework.data.jpa.repository.JpaRepository

interface HqReviewRepository : JpaRepository<HqReview, Long>
