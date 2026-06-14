package com.otoki.powersales.domain.activity.inspection.service

import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import com.otoki.powersales.domain.activity.inspection.repository.InspectionThemeRepository
import com.otoki.powersales.domain.activity.inspection.repository.SiteActivityRepository
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
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
    private val fileStorageService: FileStorageService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(AdminThemeExcelExporter::class.java)
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val HEADERS = listOf(
            "현장활동번호", "제품구분", "현장유형", "소속", "직위", "사번", "직원",
            "거래처", "거래처코드", "거래처유형", "제품", "제품유형(중분류)", "제품코드",
            "설명", "경쟁사명", "경쟁사상품명", "경쟁사 상품 시식여부", "경쟁사 활동내용",
            "경쟁사 상품 가격", "행사 시 판매수량", "점검날짜", "이미지1", "이미지2",
        )

        // 이미지 컬럼 인덱스(이미지1/이미지2) 및 임베드 표시용 셀 크기.
        private const val IMG_COL_1 = 21
        private const val IMG_COL_2 = 22
        private const val IMG_COL_WIDTH = 18 * 256 // 약 140px
        private const val IMG_ROW_HEIGHT_PT = 90f
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

            // 이미지 컬럼 폭 + 임베드용 drawing 준비. 사진은 private/ 저장이라 presigned URL 은 만료되므로
            // URL 문자열 대신 이미지 바이트를 직접 셀에 임베드한다.
            sheet.setColumnWidth(IMG_COL_1, IMG_COL_WIDTH)
            sheet.setColumnWidth(IMG_COL_2, IMG_COL_WIDTH)
            val drawing = sheet.createDrawingPatriarch()
            val anchorHelper = wb.creationHelper

            // 데이터행
            activities.forEachIndexed { idx, sa ->
                val rowIdx = idx + 2
                val row = sheet.createRow(rowIdx)
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
                )
                values.forEachIndexed { i, v -> row.createCell(i).setCellValue(v ?: "") }

                // 이미지1/이미지2 — 바이트 임베드 (최대 2장)
                val keys = photoKeys(sa)
                if (keys.isNotEmpty()) {
                    row.heightInPoints = IMG_ROW_HEIGHT_PT
                    keys.forEachIndexed { i, key ->
                        embedPhoto(wb, drawing, anchorHelper, key, IMG_COL_1 + i, rowIdx)
                    }
                }
            }

            ByteArrayOutputStream().use { out ->
                wb.write(out)
                out.toByteArray()
            }
        }

        val filename = "현장활동_${theme.name ?: themeId}.xlsx"
        return ExcelResult(bytes = bytes, filename = filename)
    }

    /** 현장점검 첨부 사진 uniqueKey 목록 (생성순, 최대 2장). */
    private fun photoKeys(sa: SiteActivity): List<String> =
        uploadFileRepository
            .findByParentTypeAndParentIdAndIsDeletedFalse(UploadFileParentTypes.SITE_ACTIVITY, sa.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .take(2)
            .map { it.uniqueKey!! }

    /**
     * private/ 객체 바이트를 내려받아 셀(1칸)에 이미지로 임베드한다. 개별 사진 다운로드 실패는
     * export 전체를 막지 않고 로그 후 건너뛴다.
     */
    private fun embedPhoto(
        wb: XSSFWorkbook,
        drawing: XSSFDrawing,
        helper: CreationHelper,
        uniqueKey: String,
        col: Int,
        rowIdx: Int,
    ) {
        val bytes = try {
            fileStorageService.downloadSiteActivityPhoto(uniqueKey)
        } catch (e: Exception) {
            log.warn("현장활동 엑셀 이미지 임베드 실패 — key={}, cause={}", uniqueKey, e.message)
            return
        }
        val pictureType = if (uniqueKey.lowercase().endsWith(".png")) {
            Workbook.PICTURE_TYPE_PNG
        } else {
            Workbook.PICTURE_TYPE_JPEG
        }
        val pictureIdx = wb.addPicture(bytes, pictureType)
        val anchor = helper.createClientAnchor().apply {
            anchorType = ClientAnchor.AnchorType.MOVE_AND_RESIZE
            setCol1(col)
            row1 = rowIdx
            setCol2(col + 1)
            row2 = rowIdx + 1
        }
        drawing.createPicture(anchor, pictureIdx)
    }

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
