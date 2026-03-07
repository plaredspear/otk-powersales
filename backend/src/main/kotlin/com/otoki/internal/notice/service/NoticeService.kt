package com.otoki.internal.notice.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.notice.dto.request.NoticeCreateRequest
import com.otoki.internal.notice.dto.request.NoticeUpdateRequest
import com.otoki.internal.notice.dto.response.NoticeImageResponse
import com.otoki.internal.notice.dto.response.BranchOption
import com.otoki.internal.notice.dto.response.CategoryOption
import com.otoki.internal.notice.dto.response.NoticeFormMetaResponse
import com.otoki.internal.notice.dto.response.NoticeMutationResponse
import com.otoki.internal.notice.dto.response.NoticePostDetailResponse
import com.otoki.internal.notice.dto.response.NoticePostListResponse
import com.otoki.internal.notice.dto.response.NoticePostSummaryResponse
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import com.otoki.internal.notice.exception.BranchRequiredException
import com.otoki.internal.notice.exception.InvalidNoticeCategoryException
import com.otoki.internal.notice.exception.InvalidNoticeIdException
import com.otoki.internal.notice.exception.NoticePostNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.notice.repository.UploadFileRepository
import com.otoki.internal.sap.repository.OrgRepository
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
    private val userRepository: UserRepository,
    private val orgRepository: OrgRepository,
    @Value("\${aws.s3.bucket.name:otoki-bucket}")
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

        val images = if (!notice.sfid.isNullOrBlank()) {
            uploadFileRepository.findByRecordIdAndIsDeletedFalse(notice.sfid!!)
                .filter { !it.uniqueKey.isNullOrBlank() }
                .sortedBy { it.createdDate }
                .mapIndexed { index, file ->
                    NoticeImageResponse(
                        id = file.id,
                        url = "https://${s3BucketName}.s3.ap-northeast-2.amazonaws.com/${file.uniqueKey}",
                        sortOrder = index
                    )
                }
        } else {
            emptyList()
        }

        return NoticePostDetailResponse(
            id = notice.id,
            category = notice.category?.apiCode ?: "",
            categoryName = notice.category?.displayName ?: "",
            title = notice.name ?: "",
            content = notice.contents ?: "",
            branch = notice.branch,
            branchCode = notice.branchCode,
            createdAt = notice.createdDate?.format(DATE_TIME_FORMATTER) ?: "",
            images = images
        )
    }

    fun getPosts(userId: Long, category: String?, search: String?, page: Int, size: Int): NoticePostListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        val branch = user.orgName ?: ""

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
                createdAt = notice.createdDate?.format(DATE_TIME_FORMATTER) ?: ""
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
                createdAt = notice.createdDate?.format(DATE_TIME_FORMATTER) ?: ""
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
    fun createNotice(request: NoticeCreateRequest): NoticeMutationResponse {
        val cat = parseCategory(request.category)
        validateBranch(cat, request.branch, request.branchCode)

        val notice = Notice(
            name = request.title,
            category = cat,
            contents = request.content,
            branch = if (cat == NoticeCategory.BRANCH) request.branch else null,
            branchCode = if (cat == NoticeCategory.BRANCH) request.branchCode else null,
            createdDate = LocalDateTime.now()
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

        val branches = orgRepository.findAll()
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
