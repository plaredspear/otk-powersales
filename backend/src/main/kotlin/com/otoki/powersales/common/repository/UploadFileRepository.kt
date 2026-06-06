package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.UploadFile
import org.springframework.data.jpa.repository.JpaRepository

interface UploadFileRepository : JpaRepository<UploadFile, Long> {

    fun findByRecordSfidAndIsDeletedFalse(recordSfid: String): List<UploadFile>

    fun findByParentTypeAndParentIdAndIsDeletedFalse(parentType: String, parentId: Long): List<UploadFile>

    fun findByParentTypeAndParentIdInAndIsDeletedFalse(
        parentType: String,
        parentIds: List<Long>
    ): List<UploadFile>

    fun findByIdAndParentTypeAndParentIdAndIsDeletedFalse(
        id: Long,
        parentType: String,
        parentId: Long
    ): UploadFile?
}
