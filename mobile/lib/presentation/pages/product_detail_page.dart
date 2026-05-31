import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/product_detail.dart';
import '../providers/product_search_provider.dart';
import '../widgets/common/error_view.dart';
import '../widgets/common/loading_indicator.dart';

/// 제품 상세 화면
///
/// 제품의 이미지·코드·품목 그룹·단위·출시일·유통기한·바코드·박스규격·출고가·
/// 셀링포인트·용도·타겟거래유형·알러지·교차오염을 표시합니다.
/// 레거시 `product/search/detail.jsp` 대응 화면.
class ProductDetailPage extends ConsumerWidget {
  /// 제품코드
  final String productCode;

  const ProductDetailPage({
    super.key,
    required this.productCode,
  });

  static final NumberFormat _numberFormat = NumberFormat('#,###', 'ko_KR');

  String _formatNumber(double? value) =>
      value == null ? '-' : _numberFormat.format(value);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailAsync = ref.watch(productDetailProvider(productCode));

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('제품 상세'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: detailAsync.when(
        loading: () => const LoadingIndicator(message: '제품 정보를 불러오는 중...'),
        error: (error, _) => ErrorView(
          message: '제품 정보를 불러올 수 없습니다',
          description: error.toString(),
          onRetry: () => ref.invalidate(productDetailProvider(productCode)),
        ),
        data: (detail) => _buildContent(detail),
      ),
    );
  }

  Widget _buildContent(ProductDetail detail) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 제품명
          Text(
            detail.productName ?? '-',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.bold,
            ),
          ),
          const Divider(height: AppSpacing.xl, color: AppColors.border),

          // 제품 이미지
          _ImageSection(detail: detail),
          const SizedBox(height: AppSpacing.lg),

          // 기본 정보
          _InfoRow(label: '코드', value: detail.productCode ?? '-'),
          _InfoRow(label: '품목 그룹명', value: detail.categoryPath),
          _InfoRow(label: '단위', value: detail.unit ?? '-'),
          _InfoRow(label: '출시일', value: detail.launchDate ?? '-'),
          _InfoRow(label: '유통기한', value: detail.shelfLifeDisplay),
          _InfoRow(label: '바코드', value: detail.barcode ?? '-'),
          _InfoRow(label: '박스규격', value: _formatNumber(detail.boxReceivingQuantity)),
          _InfoRow(label: '출고가', value: _formatNumber(detail.standardUnitPrice)),

          // 설명형 정보 (멀티라인)
          _MultilineRow(label: '셀링포인트', value: detail.sellingPoint),
          _MultilineRow(label: '용도', value: detail.purpose),
          _MultilineRow(label: '타겟거래유형', value: detail.targetAccountType),
          _MultilineRow(label: '알러지 유발물질', value: detail.allergen),
          _MultilineRow(label: '교차오염', value: detail.crossContamination),
        ],
      ),
    );
  }
}

/// 제품 정면/후면 이미지 영역
class _ImageSection extends StatelessWidget {
  final ProductDetail detail;

  const _ImageSection({required this.detail});

  @override
  Widget build(BuildContext context) {
    if (!detail.hasImages) {
      return Container(
        height: 160,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        ),
        child: Text(
          '제품 이미지 없음',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textTertiary,
          ),
        ),
      );
    }

    final urls = [detail.frontImageUrl, detail.backImageUrl]
        .whereType<String>()
        .toList();

    return SizedBox(
      height: 160,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: urls.length,
        separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.md),
        itemBuilder: (context, index) => ClipRRect(
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          child: Image.network(
            urls[index],
            height: 160,
            fit: BoxFit.contain,
            loadingBuilder: (context, child, progress) {
              if (progress == null) return child;
              return const SizedBox(
                width: 160,
                child: Center(child: CircularProgressIndicator()),
              );
            },
            errorBuilder: (context, error, stackTrace) => Container(
              width: 160,
              color: AppColors.surface,
              child: const Center(
                child: Icon(
                  Icons.broken_image,
                  size: 40,
                  color: AppColors.textTertiary,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// 라벨(좌) + 값(우) 한 줄 정보 행
class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 96,
            child: Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value.isEmpty ? '-' : value,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 라벨(상) + 멀티라인 값(하) — 셀링포인트/용도 등 긴 설명용
class _MultilineRow extends StatelessWidget {
  final String label;
  final String? value;

  const _MultilineRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    if (value == null || value!.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            value!,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textPrimary,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
