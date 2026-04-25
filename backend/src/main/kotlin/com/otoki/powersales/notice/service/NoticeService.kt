package com.otoki.powersales.notice.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.notice.dto.response.BranchOption
import com.otoki.powersales.notice.dto.response.CategoryOption
import com.otoki.powersales.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.notice.entity.NoticeCategory
import com.otoki.powersales.notice.exception.BranchRequiredException
import com.otoki.powersales.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.notice.repository.NoticeRepository
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.sap.repository.OrganizationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
    private val uploadFileRepository: UploadFileRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    @Value("\${app.aws.s3.bucket:otoki-bucket}")
    private val s3BucketName: String
) {

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    fun getNoticeDetail(noticeId: Long): NoticePostDetailResponse {
        val notice = noticeRepository.findById(noticeId)
            .filter { it.isDeleted != true }
            .orElseThrow { NoticePostNotFoundException() }

        val images = uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("NOTICE", notice.id)
            .filter { !it.uniqueKey.isNullOrBlank() }
            .sortedBy { it.createdAt }
            .mapIndexed { index, file ->
                NoticeImageResponse(
                    id = file.id,
                    url = "https://${s3BucketName}.s3.ap-northeast-2.amazonaws.com/${file.uniqueKey}",
                    sortOrder = index
                )
            }

        return NoticePostDetailResponse(
            id = notice.id,
            category = notice.category?.apiCode ?: "",
            categoryName = notice.category?.displayName ?: "",
            title = notice.name ?: "",
            content = notice.contents ?: "",
            branch = notice.branch,
            branchCode = notice.branchCode,
            createdAt = notice.createdAt?.format(DATE_TIME_FORMATTER) ?: "",
            images = images
        )
    }

    fun getPosts(userId: Long, category: String?, search: String?, page: Int, size: Int): NoticePostListResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val branch = employee.orgName ?: ""

        val parsedCategory = if (category != null) parseCategory(category) else null

        val truncatedSearch = search?.take(100)?.ifBlank { null }
        val pageable = PageRequest.of(page - 1, size)
        val noticePage = noticeRepository.findNotices(parsedCategory, truncatedSearch, branch, pageable)

        val content = noticePage.content.map { notice ->
            NoticePostSummaryResponse(
                id = notice.id,
                category = notice.category?.apiCode ?: "",
                categoryName = notice.category?.displayName ?: "",
                title = notice.name ?: "",
                createdAt = notice.createdAt?.format(DATE_TIME_FORMATTER) ?: ""
            )
        }

        return NoticePostListResponse(
            content = content,
            totalCount = noticePage.totalElements,
            totalPages = noticePage.totalPages,
            currentPage = page,
            size = size
        )
    }

    fun getPostsForAdmin(category: String?, search: String?, page: Int, size: Int): NoticePostListResponse {
        val parsedCategory = if (category != null) parseCategory(category) else null

        val truncatedSearch = search?.take(100)?.ifBlank { null }
        val pageable = PageRequest.of(page - 1, size)
        val noticePage = noticeRepository.findAllNotices(parsedCategory, truncatedSearch, pageable)

        val content = noticePage.content.map { notice ->
            NoticePostSummaryResponse(
                id = notice.id,
                category = notice.category?.apiCode ?: "",
                categoryName = notice.category?.displayName ?: "",
                title = notice.name ?: "",
                createdAt = notice.createdAt?.format(DATE_TIME_FORMATTER) ?: ""
            )
        }

        return NoticePostListResponse(
            content = content,
            totalCount = noticePage.totalElements,
            totalPages = noticePage.totalPages,
            currentPage = page,
            size = size
        )
    }

    @Transactional
    fun createNotice(request: NoticeCreateRequest, creatorId: Long): NoticeMutationResponse {
        val cat = parseCategory(request.category)
        validateBranch(cat, request.branch, request.branchCode)

        val notice = Notice(
            name = request.title,
            category = cat,
            contents = request.content,
            branch = if (cat == NoticeCategory.BRANCH) request.branch else null,
            branchCode = if (cat == NoticeCategory.BRANCH) request.branchCode else null,
            employee = employeeRepository.getReferenceById(creatorId)
        )
        val saved = noticeRepository.save(notice)
        return NoticeMutationResponse.from(saved)
    }

    @Transactional
    fun updateNotice(noticeId: Long, request: NoticeUpdateRequest): NoticeMutationResponse {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)

        val cat = parseCategory(request.category)
        validateBranch(cat, request.branch, request.branchCode)

        notice.name = request.title
        notice.category = cat
        notice.contents = request.content
        notice.branch = if (cat == NoticeCategory.BRANCH) request.branch else null
        notice.branchCode = if (cat == NoticeCategory.BRANCH) request.branchCode else null

        val saved = noticeRepository.save(notice)
        return NoticeMutationResponse.from(saved)
    }

    @Transactional
    fun deleteNotice(noticeId: Long) {
        if (noticeId <= 0) throw InvalidNoticeIdException()
        val notice = findActiveNotice(noticeId)
        notice.isDeleted = true
        noticeRepository.save(notice)
    }

    fun getNoticeFormMeta(): NoticeFormMetaResponse {
        val categories = NoticeCategory.entries.map {
            CategoryOption(code = it.apiCode, name = it.displayName)
        }

        val branches = organizationRepository.findAll()
            .filter { !it.orgNameLevel3.isNullOrBlank() && !it.orgNameLevel4.isNullOrBlank() }
            .map { org ->
                val branchName = if (org.orgNameLevel5.isNullOrBlank()) {
                    "[${org.orgNameLevel3}] ${org.orgNameLevel4}"
                } else {
                    "[${org.orgNameLevel3}] ${org.orgNameLevel4}-${org.orgNameLevel5}"
                }
                val branchCode = if (!org.costCenterLevel5.isNullOrBlank()) {
                    org.costCenterLevel5!!
                } else {
                    org.costCenterLevel4 ?: ""
                }
                BranchOption(branchCode = branchCode, branchName = branchName)
            }
            .distinctBy { it.branchCode }
            .sortedBy { it.branchName }

        return NoticeFormMetaResponse(categories = categories, branches = branches)
    }

    private fun findActiveNotice(noticeId: Long): Notice {
        val notice = noticeRepository.findById(noticeId)
            .orElseThrow { NoticePostNotFoundException() }
        if (notice.isDeleted == true) throw NoticePostNotFoundException()
        return notice
    }

    private fun parseCategory(category: String): NoticeCategory {
        return try {
            NoticeCategory.fromApiCode(category)
        } catch (_: IllegalArgumentException) {
            throw InvalidNoticeCategoryException()
        }
    }

    private fun validateBranch(category: NoticeCategory, branch: String?, branchCode: String?) {
        if (category == NoticeCategory.BRANCH) {
            if (branch.isNullOrBlank() || branchCode.isNullOrBlank()) {
                throw BranchRequiredException()
            }
        }
    }
}
