import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/inspection_detail.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../providers/inspection_detail_provider.dart';
import '../widgets/inspection/inspection_detail_competitor.dart';
import '../widgets/inspection/inspection_detail_own.dart';

/// 현장점검 상세 페이지
///
/// 점검 ID를 기반으로 점검 상세 정보를 조회하고,
/// 자사/경쟁사에 따라 다른 위젯을 표시합니다.
class InspectionDetailPage extends ConsumerWidget {
  /// 점검 ID (라우트 arguments로 전달)
  final int inspectionId;

  const InspectionDetailPage({
    super.key,
    required this.inspectionId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(inspectionDetailProvider(inspectionId));

    // 에러 메시지 리스닝
    ref.listen(inspectionDetailProvider(inspectionId), (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(inspectionDetailProvider(inspectionId).notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('현장 점검 상세'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: _buildBody(context, ref, state),
    );
  }

  Widget _buildBody(
    BuildContext context,
    WidgetRef ref,
    InspectionDetailState state,
  ) {
    // 로딩 상태
    if (state.isLoading && !state.hasData) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (데이터 없음)
    if (state.errorMessage != null && !state.hasData) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              state.errorMessage!,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref
                    .read(inspectionDetailProvider(inspectionId).notifier)
                    .loadDetail(inspectionId);
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 데이터 없음
    if (!state.hasData) {
      return const Center(child: CircularProgressIndicator());
    }

    final detail = state.detail!;

    return RefreshIndicator(
      onRefresh: () async {
        await ref
            .read(inspectionDetailProvider(inspectionId).notifier)
            .loadDetail(inspectionId);
      },
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 공통 정보 헤더
            _buildInfoHeader(detail),

            const SizedBox(height: AppSpacing.lg),

            // 자사/경쟁사 구분 위젯
            if (state.isOwn)
              InspectionDetailOwnWidget(detail: detail)
            else if (state.isCompetitor)
              InspectionDetailCompetitorWidget(detail: detail),

            const SizedBox(height: AppSpacing.lg),

            // 사진 섹션
            if (detail.photos.isNotEmpty) ...[
              _buildPhotosSection(detail.photos),
              const SizedBox(height: AppSpacing.lg),
            ],

            // 하단 여백
            const SizedBox(height: AppSpacing.xxxl),
          ],
        ),
      ),
    );
  }

  /// 공통 정보 헤더 위젯
  Widget _buildInfoHeader(detail) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 카테고리 배지
          _buildCategoryBadge(detail.category),

          const SizedBox(height: AppSpacing.md),

          // 거래처명
          _buildInfoRow('거래처', detail.storeName),

          const SizedBox(height: AppSpacing.sm),

          // 점검일
          _buildInfoRow('점검일', dateFormat.format(detail.inspectionDate)),

          const SizedBox(height: AppSpacing.sm),

          // 테마
          _buildInfoRow('테마', detail.themeName),

          const SizedBox(height: AppSpacing.sm),

          // 매대
          _buildInfoRow('매대', detail.fieldType),
        ],
      ),
    );
  }

  /// 카테고리 배지
  Widget _buildCategoryBadge(InspectionCategory category) {
    final isOwn = category == InspectionCategory.OWN;
    final label = isOwn ? '자사' : '경쟁사';
    final backgroundColor = isOwn ? AppColors.primary : AppColors.secondary;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: AppTypography.labelSmall.copyWith(
          color: Colors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  /// 정보 행 위젯
  Widget _buildInfoRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80,
          child: Text(
            label,
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Text(
            value,
            style: AppTypography.bodyMedium,
          ),
        ),
      ],
    );
  }

  /// 사진 섹션 위젯
  Widget _buildPhotosSection(List<InspectionPhoto> photos) {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '사진 (${photos.length})',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: AppSpacing.sm,
              mainAxisSpacing: AppSpacing.sm,
            ),
            itemCount: photos.length,
            itemBuilder: (context, index) {
              final photo = photos[index];
              return ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  photo.url,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      color: AppColors.border,
                      child: const Icon(
                        Icons.broken_image,
                        color: AppColors.textTertiary,
                      ),
                    );
                  },
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
