package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Long> {

    fun findByRecordIdAndIsDeletedFalse(recordId: String): List<UploadFile>

    fun findByParentTypeAndParentIdAndIsDeletedFalse(parentType: String, parentId: Long): List<UploadFile>
}
