package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.HqReview
import org.springframework.data.jpa.repository.JpaRepository

interface HqReviewRepository : JpaRepository<HqReview, Int>
