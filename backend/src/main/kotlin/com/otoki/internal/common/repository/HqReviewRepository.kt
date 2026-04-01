package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.HqReview
import org.springframework.data.jpa.repository.JpaRepository

interface HqReviewRepository : JpaRepository<HqReview, Int>
