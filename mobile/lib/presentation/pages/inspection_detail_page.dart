import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/inspection_detail.dart';
import '../providers/inspection_detail_provider.dart';
import '../providers/inspection_detail_state.dart';
import '../widgets/inspection/inspection_detail_competitor.dart';
import '../widgets/inspection/inspection_detail_own.dart';
import '../widgets/inspection/inspection_detail_row.dart';

/// 현장점검 상세 페이지
///
/// 점검 ID를 기반으로 점검 상세 정보를 조회하고,
/// 레거시 view.jsp 정합으로 단일 정보 블록(타이틀 → 필드 → 사진)을 표시합니다.
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
        child: _buildDetailCard(state, detail),
      ),
    );
  }

  /// 레거시 view.jsp 정합 — 단일 정보 블록 (타이틀 → 필드 → 사진)
  Widget _buildDetailCard(InspectionDetailState state, InspectionDetail detail) {
    final dateFormat = DateFormat('yyyy-MM-dd');
    final categoryLabel = detail.isOwn ? '자사' : '경쟁사';

    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 타이틀: [분류] 거래처명 (레거시 .txt01 — 16px, weight 800)
          Text(
            '[$categoryLabel] ${detail.accountName}',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: AppSpacing.md),

          // 공통 필드 (테마 → 점검일 → 현장 유형)
          InspectionDetailRow(label: '테마', value: detail.themeName),
          InspectionDetailRow(
            label: '점검일',
            value: dateFormat.format(detail.inspectionDate),
          ),
          InspectionDetailRow(label: '현장 유형', value: detail.fieldType),

          // 분류별 추가 필드 (섹션 헤더 없이 이어서 표시)
          if (state.isOwn)
            InspectionDetailOwnWidget(detail: detail)
          else if (state.isCompetitor)
            InspectionDetailCompetitorWidget(detail: detail),

          // 사진 (레거시: 세로로 풀폭 적층)
          if (detail.photos.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.lg),
            _buildPhotos(detail.photos),
          ],
        ],
      ),
    );
  }

  /// 사진 영역 — 레거시 .con 정합: 세로 풀폭 적층, 사이 간격 10px
  Widget _buildPhotos(List<InspectionPhoto> photos) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var i = 0; i < photos.length; i++)
          Padding(
            padding: EdgeInsets.only(top: i == 0 ? 0 : 10),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: AspectRatio(
                aspectRatio: 4 / 3,
                child: Image.network(
                  photos[i].url,
                  fit: BoxFit.cover,
                  loadingBuilder: (context, child, progress) {
                    if (progress == null) return child;
                    return Container(
                      color: AppColors.surface,
                      child: const Center(
                        child: SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                      ),
                    );
                  },
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      color: AppColors.surface,
                      alignment: Alignment.center,
                      child: const Icon(
                        Icons.broken_image,
                        color: AppColors.textTertiary,
                        size: 40,
                      ),
                    );
                  },
                ),
              ),
            ),
          ),
      ],
    );
  }
}
