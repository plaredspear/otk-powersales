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
    final entries = <_InfoEntry>[
      _InfoEntry('코드', detail.productCode ?? '-'),
      _InfoEntry('품목 그룹명', detail.categoryPath),
      _InfoEntry('단위', detail.unit ?? '-'),
      _InfoEntry('출시일', detail.launchDate ?? '-'),
      _InfoEntry('유통기한', detail.shelfLifeDisplay),
      _InfoEntry('바코드', detail.barcode ?? '-'),
      _InfoEntry('박스규격', _formatNumber(detail.boxReceivingQuantity)),
      _InfoEntry('출고가', _formatNumber(detail.standardUnitPrice)),
      // 설명형 정보 — 값이 있을 때만 노출
      if (detail.sellingPoint?.isNotEmpty ?? false)
        _InfoEntry('셀링포인트', detail.sellingPoint!),
      if (detail.purpose?.isNotEmpty ?? false)
        _InfoEntry('용도', detail.purpose!),
      if (detail.targetAccountType?.isNotEmpty ?? false)
        _InfoEntry('타겟거래유형', detail.targetAccountType!),
      if (detail.allergen?.isNotEmpty ?? false)
        _InfoEntry('알러지 유발물질', detail.allergen!),
      if (detail.crossContamination?.isNotEmpty ?? false)
        _InfoEntry('교차오염', detail.crossContamination!),
    ];

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

          // 기본 정보 — 보더 테이블
          _InfoTable(entries: entries),
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

/// 정보 테이블 한 행의 라벨·값 데이터
class _InfoEntry {
  final String label;
  final String value;

  const _InfoEntry(this.label, this.value);
}

/// 라벨(좌) + 값(우) 셀을 보더로 구분한 정보 테이블
///
/// 외곽 보더 + 행/열 구분선으로 각 항목의 경계를 명확히 하여 시인성을 높인다.
/// 값이 길면 우측 셀 안에서 자동 줄바꿈된다.
class _InfoTable extends StatelessWidget {
  static const double _labelWidth = 110;

  final List<_InfoEntry> entries;

  const _InfoTable({required this.entries});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: AppColors.divider),
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Column(
          children: [
            for (var i = 0; i < entries.length; i++) ...[
              if (i > 0)
                const Divider(
                  height: 1,
                  thickness: 1,
                  color: AppColors.border,
                ),
              IntrinsicHeight(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // 라벨 셀
                    Container(
                      width: _labelWidth,
                      color: AppColors.surface,
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppSpacing.md,
                        vertical: AppSpacing.sm + 2,
                      ),
                      alignment: Alignment.centerLeft,
                      child: Text(
                        entries[i].label,
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.textSecondary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const VerticalDivider(
                      width: 1,
                      thickness: 1,
                      color: AppColors.border,
                    ),
                    // 값 셀
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: AppSpacing.md,
                          vertical: AppSpacing.sm + 2,
                        ),
                        alignment: Alignment.centerLeft,
                        child: Text(
                          entries[i].value.isEmpty ? '-' : entries[i].value,
                          style: AppTypography.bodyMedium.copyWith(
                            color: AppColors.textPrimary,
                            height: 1.4,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
