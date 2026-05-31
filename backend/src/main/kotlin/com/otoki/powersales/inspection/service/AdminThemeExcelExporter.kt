package com.otoki.powersales.inspection.service

import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.inspection.repository.SiteActivityRepository
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 테마 단위 현장점검 결과 엑셀 export.
 *
 * 레거시 매핑: SF `VisualforceToExcelController` + `SiteActivityToExcel` QuickAction.
 * 동작: 테마 헤더(`{테마번호} : {테마이름} 시작일 ~ 종료일`) + 하위 SiteActivity 전체를 22컬럼 행으로 출력.
 *       이미지 컬럼 2개는 SiteActivity 첨부 사진(UploadFile)의 S3 URL.
 */
@Service
@Transactional(readOnly = true)
class AdminThemeExcelExporter(
    private val inspectionThemeRepository: InspectionThemeRepository,
    private val siteActivityRepository: SiteActivityRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val themeService: AdminInspectionThemeService,
) {

    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val HEADERS = listOf(
            "현장활동번호", "제품구분", "현장유형", "소속", "직위", "사번", "직원",
            "거래처", "거래처코드", "거래처유형", "제품", "제품유형(중분류)", "제품코드",
            "설명", "경쟁사명", "경쟁사상품명", "경쟁사 상품 시식여부", "경쟁사 활동내용",
            "경쟁사 상품 가격", "행사 시 판매수량", "점검날짜", "이미지1", "이미지2",
        )
    }

    fun export(themeId: Long): ExcelResult {
        val theme = inspectionThemeRepository.findById(themeId).orElseThrow {
            IllegalArgumentException("테마를 찾을 수 없습니다")
        }
        val activities = siteActivityRepository.findByInspectionThemeIdForAdmin(themeId)

        val workbook = XSSFWorkbook()
        val bytes = workbook.use { wb ->
            val sheet = wb.createSheet("현장활동")

            // 상단 제목행 — "{테마번호} : {테마이름} 시작일 ~ 종료일"
            val titleText = buildString {
                append(theme.name ?: "")
                append(" : ")
                append(theme.title ?: "")
                if (theme.startDate != null && theme.endDate != null) {
                    append(" ")
                    append(theme.startDate!!.format(DATE_FMT))
                    append(" ~ ")
                    append(theme.endDate!!.format(DATE_FMT))
                }
            }
            sheet.createRow(0).createCell(0).setCellValue(titleText)

            // 헤더행
            val headerStyle = wb.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            val headerRow = sheet.createRow(1)
            HEADERS.forEachIndexed { i, h ->
                headerRow.createCell(i).apply {
                    setCellValue(h)
                    cellStyle = headerStyle
                }
            }

            // 데이터행
            activities.forEachIndexed { idx, sa ->
                val row = sheet.createRow(idx + 2)
                val images = imageUrls(sa)
                val values = listOf(
                    sa.name,
                    sa.productType,
                    sa.category,
                    sa.employee?.orgName,
                    sa.employee?.jikwee,
                    sa.employee?.employeeCode,
                    sa.employee?.name,
                    sa.account?.name,
                    sa.account?.externalKey,
                    sa.account?.accountType?.displayName,
                    sa.product?.name,
                    sa.product?.productCategory2,
                    sa.product?.productCode,
                    sa.description,
                    sa.competitorName,
                    sa.competitorProductName,
                    sa.sampleTastFlag,
                    sa.competitorActivityDescription,
                    sa.competitorProudctPrice?.toPlainString(),
                    sa.salesQuantity?.toPlainString(),
                    sa.activityDate?.toString(),
                    images.getOrNull(0),
                    images.getOrNull(1),
                )
                values.forEachIndexed { i, v -> row.createCell(i).setCellValue(v ?: "") }
            }

            ByteArrayOutputStream().use { out ->
                wb.write(out)
                out.toByteArray()
            }
        }

        val filename = "현장활동_${theme.name ?: themeId}.xlsx"
        return ExcelResult(bytes = bytes, filename = filename)
    }

    private fun imageUrls(sa: SiteActivity): List<String> =
        uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, sa.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .take(2)
            .map { themeService.composeS3Url(it.uniqueKey!!) }

    data class ExcelResult(
        val bytes: ByteArray,
        val filename: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ExcelResult
            return bytes.contentEquals(other.bytes) && filename == other.filename
        }

        override fun hashCode(): Int = bytes.contentHashCode() * 31 + filename.hashCode()
    }
}
