package com.otoki.internal.repository

import com.otoki.internal.entity.PushMessageReceiver
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageReceiverRepository : JpaRepository<PushMessageReceiver, Int>
