package com.otoki.internal.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.response.BranchDto
import com.otoki.internal.admin.dto.response.ScheduleConfirmResultDto
import com.otoki.internal.admin.dto.response.ScheduleUploadResultDto
import com.otoki.internal.admin.exception.*
import com.otoki.internal.common.exception.BusinessException
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.OrganizationRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class AdminScheduleService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val templateGenerator: ScheduleTemplateGenerator,
    private val excelParser: ScheduleExcelParser,
    private val uploadValidator: ScheduleUploadValidator,
    private val scheduleRepository: DisplayWorkScheduleRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val REDIS_KEY_PREFIX = "schedule:upload:"
        private const val REDIS_TTL_MINUTES = 30L
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_ROWS = 500
    }

    fun getBranches(): List<BranchDto> {
        return userRepository.findDistinctBranches()
            .filter { it.branchCode.isNotBlank() && it.branchName.isNotBlank() }
            .map { BranchDto(costCenterCode = it.branchCode, branchName = it.branchName) }
    }

    fun generateTemplate(costCenterCode: String): TemplateResult {
        organizationRepository.findFirstByCostCenterLevel5(costCenterCode)
            ?: organizationRepository.findFirstByCostCenterLevel4(costCenterCode)
            ?: throw OrganizationNotFoundException()

        val employees = userRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
            costCenterCode, "재직"
        ).sortedWith(compareBy({ it.orgName }, { it.employeeId }))

        val excelBytes = templateGenerator.generate(employees)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val filename = "진열스케줄_양식_${costCenterCode}_${timestamp}.xlsx"

        return TemplateResult(excelBytes, filename)
    }

    fun uploadAndValidate(file: MultipartFile): ScheduleUploadResultDto {
        // 파일 검증
        validateFile(file)

        // Excel 파싱
        val parseResult = excelParser.parse(file.inputStream)

        if (parseResult.totalRows == 0) {
            throw ScheduleEmptyFileException()
        }
        if (parseResult.totalRows > MAX_ROWS) {
            throw ScheduleRowLimitExceededException()
        }

        // 사원번호/거래처코드 일괄 조회
        val employeeCodes = parseResult.rows.mapNotNull { it.employeeCode }.distinct()
        val accountCodes = parseResult.rows.mapNotNull { it.accountCode }.distinct()

        val usersByEmployeeId = if (employeeCodes.isNotEmpty()) {
            userRepository.findByEmployeeIdIn(employeeCodes).associateBy { it.employeeId }
        } else {
            emptyMap()
        }

        val accountsByExternalKey = if (accountCodes.isNotEmpty()) {
            accountRepository.findByExternalKeyIn(accountCodes)
                .filter { it.externalKey != null }
                .associateBy { it.externalKey!! }
        } else {
            emptyMap()
        }

        // 기존 스케줄 조회 (중복 검증용)
        val userSfids = usersByEmployeeId.values.mapNotNull { it.sfid }
        val existingSchedules = if (userSfids.isNotEmpty()) {
            scheduleRepository.findByFullNameInAndNotDeleted(userSfids)
        } else {
            emptyList()
        }

        // 검증
        val validationResult = uploadValidator.validate(
            parseResult.rows, usersByEmployeeId, accountsByExternalKey, existingSchedules
        )

        // UUID 생성 + Redis 저장
        val uploadId = UUID.randomUUID().toString()
        val cacheData = UploadCacheData(
            validRows = validationResult.validRows,
            errorCount = validationResult.errors.size
        )
        val json = objectMapper.writeValueAsString(cacheData)
        redisTemplate.opsForValue().set(
            "$REDIS_KEY_PREFIX$uploadId", json, REDIS_TTL_MINUTES, TimeUnit.MINUTES
        )

        return ScheduleUploadResultDto(
            uploadId = uploadId,
            totalRows = parseResult.totalRows,
            successRows = validationResult.validRows.size,
            errorRows = validationResult.errors.size,
            errors = validationResult.errors,
            previews = validationResult.previews
        )
    }

    @Transactional
    fun confirmUpload(uploadId: String): ScheduleConfirmResultDto {
        val redisKey = "$REDIS_KEY_PREFIX$uploadId"
        val json = redisTemplate.opsForValue().get(redisKey)
            ?: throw ScheduleUploadNotFoundException()

        val cacheData = objectMapper.readValue(json, UploadCacheData::class.java)

        if (cacheData.errorCount > 0) {
            throw ScheduleHasValidationErrorsException()
        }

        val entities = cacheData.validRows.map { row ->
            DisplayWorkSchedule(
                fullName = row.userSfid,
                account = row.accountSfid,
                typeOfWork1 = "진열",
                typeOfWork3 = row.typeOfWork3,
                typeOfWork5 = row.typeOfWork5,
                startDate = row.startDate,
                endDate = row.endDate,
                confirmed = false
            )
        }

        scheduleRepository.saveAll(entities)
        redisTemplate.delete(redisKey)

        return ScheduleConfirmResultDto(insertedCount = entities.size)
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw ScheduleFileRequiredException()
        }

        val originalFilename = file.originalFilename ?: ""
        if (!originalFilename.lowercase().endsWith(".xlsx")) {
            throw ScheduleInvalidFileTypeException()
        }

        if (file.size > MAX_FILE_SIZE) {
            throw ScheduleFileTooLargeException()
        }
    }

    data class TemplateResult(
        val bytes: ByteArray,
        val filename: String
    )

    data class UploadCacheData(
        val validRows: List<ScheduleUploadValidator.ValidatedRow>,
        val errorCount: Int
    )
}

class OrganizationNotFoundException : BusinessException(
    errorCode = "ORGANIZATION_NOT_FOUND",
    message = "존재하지 않는 지점 코드입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class MissingCostCenterCodeException : BusinessException(
    errorCode = "MISSING_PARAMETER",
    message = "cost_center_code는 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
