package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.PushMessageReceiver
import org.springframework.data.jpa.repository.JpaRepository

interface PushMessageReceiverRepository : JpaRepository<PushMessageReceiver, Int>
