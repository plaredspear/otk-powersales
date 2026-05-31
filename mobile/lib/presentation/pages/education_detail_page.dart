import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/education_post_detail.dart';
import '../providers/education_post_detail_provider.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 교육 자료 상세 화면
///
/// 교육 게시물의 제목, 분류, 등록일, 본문, 이미지, 첨부 문서를 표시합니다.
/// 레거시 `community/edu/view.jsp` 대응 화면.
class EducationDetailPage extends ConsumerWidget {
  /// 교육 게시물 ID (백엔드 eduId 문자열)
  final String postId;

  const EducationDetailPage({
    super.key,
    required this.postId,
  });

  /// 날짜를 "yyyy.MM.dd(요일)" 형식으로 변환
  String _formatDate(DateTime date) {
    return DateFormat('yyyy.MM.dd(E)', 'ko_KR').format(date);
  }

  /// 첨부 문서 열기 (외부 브라우저)
  Future<void> _openAttachment(BuildContext context, String url) async {
    final uri = Uri.tryParse(url);
    if (uri == null) {
      _showSnackBar(context, '잘못된 파일 주소입니다');
      return;
    }
    final launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!launched && context.mounted) {
      _showSnackBar(context, '파일을 열 수 없습니다');
    }
  }

  void _showSnackBar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(educationPostDetailProvider(postId));

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('교육 상세'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: detailAsync.when(
        loading: () => const LoadingIndicator(
          message: '교육 자료를 불러오는 중...',
        ),
        error: (error, _) => ErrorView(
          message: '교육 자료를 불러올 수 없습니다',
          description: error.toString(),
          onRetry: () => ref.invalidate(educationPostDetailProvider(postId)),
        ),
        data: (detail) => _buildContent(context, detail),
      ),
    );
  }

  Widget _buildContent(BuildContext context, EducationPostDetail detail) {
    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 분류 태그
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                    vertical: AppSpacing.xs,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
                  ),
                  child: Text(
                    detail.categoryName,
                    style: AppTypography.labelSmall.copyWith(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),

                const SizedBox(height: AppSpacing.md),

                // 제목
                Text(
                  detail.title,
                  style: AppTypography.headlineSmall.copyWith(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.bold,
                  ),
                ),

                // 등록일 (누락 시 epoch 센티넬을 노출하지 않도록 가드)
                if (detail.hasCreatedAt) ...[
                  const SizedBox(height: AppSpacing.sm),
                  Text(
                    _formatDate(detail.createdAt),
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],

                const Divider(height: AppSpacing.xl, color: AppColors.border),

                // 본문
                if (detail.content.isNotEmpty)
                  Text(
                    detail.content,
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textPrimary,
                      height: 1.6,
                    ),
                  ),

                // 첨부 문서
                if (detail.hasAttachments) ...[
                  const SizedBox(height: AppSpacing.xl),
                  Text(
                    '첨부 문서',
                    style: AppTypography.labelMedium.copyWith(
                      color: AppColors.textSecondary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  ...detail.attachments.map(
                    (attachment) => _buildAttachmentItem(context, attachment),
                  ),
                ],

                // 이미지 목록
                if (detail.hasImages) ...[
                  const SizedBox(height: AppSpacing.xl),
                  ...detail.images.map((image) => _buildImage(image)),
                ],
              ],
            ),
          ),
        ),

        // 목록으로 버튼
        Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: const BoxDecoration(
            color: AppColors.white,
            border: Border(
              top: BorderSide(color: AppColors.border, width: 1),
            ),
          ),
          child: SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: AppColors.white,
                padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                ),
              ),
              child: const Text('목록으로'),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAttachmentItem(
    BuildContext context,
    EducationAttachment attachment,
  ) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.xs),
      child: InkWell(
        onTap: () => _openAttachment(context, attachment.fileUrl),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
          child: Row(
            children: [
              const Icon(
                Icons.description_outlined,
                size: 20,
                color: AppColors.secondary,
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  attachment.fileName,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.secondary,
                    decoration: TextDecoration.underline,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Text(
                attachment.fileSizeFormatted,
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildImage(EducationImage image) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.md),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Image.network(
          image.url,
          fit: BoxFit.cover,
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Center(
              child: CircularProgressIndicator(
                value: loadingProgress.expectedTotalBytes != null
                    ? loadingProgress.cumulativeBytesLoaded /
                        loadingProgress.expectedTotalBytes!
                    : null,
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return Container(
              height: 200,
              color: AppColors.surface,
              child: const Center(
                child: Icon(
                  Icons.broken_image,
                  size: 48,
                  color: AppColors.textTertiary,
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}
