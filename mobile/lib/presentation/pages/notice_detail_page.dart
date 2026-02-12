import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../domain/entities/notice_category.dart';
import '../providers/notice_detail_provider.dart';
import '../providers/notice_detail_state.dart';

/// 공지사항 상세 화면
///
/// 공지사항 제목, 분류, 날짜, 본문, 이미지를 표시합니다.
class NoticeDetailPage extends ConsumerStatefulWidget {
  /// 공지사항 ID
  final int noticeId;

  const NoticeDetailPage({
    super.key,
    required this.noticeId,
  });

  @override
  ConsumerState<NoticeDetailPage> createState() => _NoticeDetailPageState();
}

class _NoticeDetailPageState extends ConsumerState<NoticeDetailPage> {
  @override
  void initState() {
    super.initState();
    // 상세 정보 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(noticeDetailProvider.notifier).loadDetail(widget.noticeId);
    });
  }

  /// 날짜를 "yyyy.MM.dd(요일)" 형식으로 변환
  String _formatDate(DateTime date) {
    final formatter = DateFormat('yyyy.MM.dd(E)', 'ko_KR');
    return formatter.format(date);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(noticeDetailProvider);
    final notifier = ref.read(noticeDetailProvider.notifier);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('공지 상세'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: _buildContent(state, notifier),
    );
  }

  Widget _buildContent(NoticeDetailState state, NoticeDetailNotifier notifier) {
    // 로딩 중
    if (state.isLoading) {
      return const Center(
        child: CircularProgressIndicator(),
      );
    }

    // 에러
    if (state.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 48,
              color: AppColors.error,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              state.errorMessage!,
              style: const TextStyle(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            ElevatedButton(
              onPressed: () => notifier.refresh(widget.noticeId),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    // 데이터 없음
    if (!state.hasData || state.detail == null) {
      return const Center(
        child: Text(
          '공지사항을 찾을 수 없습니다',
          style: TextStyle(
            color: AppColors.textSecondary,
          ),
        ),
      );
    }

    final detail = state.detail!;

    return Column(
      children: [
        // 상세 내용
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
                    color: detail.category == NoticeCategory.company
                        ? AppColors.primaryLight
                        : AppColors.secondaryLight,
                    borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
                  ),
                  child: Text(
                    detail.categoryName,
                    style: AppTypography.labelSmall.copyWith(
                      color: detail.category == NoticeCategory.company
                          ? AppColors.primary
                          : AppColors.secondary,
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

                const SizedBox(height: AppSpacing.sm),

                // 등록일
                Text(
                  _formatDate(detail.createdAt),
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),

                const Divider(height: AppSpacing.xl, color: AppColors.border),

                // 본문
                Text(
                  detail.content,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary,
                    height: 1.6,
                  ),
                ),

                // 이미지 목록
                if (detail.images.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.xl),
                  ...detail.images.map((image) {
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
                  }),
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
              top: BorderSide(
                color: AppColors.border,
                width: 1,
              ),
            ),
          ),
          child: SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
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
}
