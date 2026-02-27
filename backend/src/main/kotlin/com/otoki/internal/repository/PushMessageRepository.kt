package com.otoki.internal.repository

import com.otoki.internal.entity.PushMessage
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageRepository : JpaRepository<PushMessage, Int>
