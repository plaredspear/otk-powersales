package com.otoki.internal.repository

import com.otoki.internal.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Int>
