/*
package com.otoki.internal.repository

import com.otoki.internal.entity.InspectionPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/ **
 * 현장 점검 사진 Repository
 * /
@Repository
interface InspectionPhotoRepository : JpaRepository<InspectionPhoto, Long> {

    / **
     * 특정 점검의 사진 목록 조회
     * /
    fun findByInspectionId(inspectionId: Long): List<InspectionPhoto>
}
*/
