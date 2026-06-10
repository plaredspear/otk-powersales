import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/pos_product.dart';
import '../../providers/pos_sales_provider.dart';

/// 제품 선택 바텀시트
///
/// 제품명/코드 검색(`GET /api/v1/mobile/products/search`, [PosProductUseCase.searchByText])으로
/// 제품을 찾아 선택된 [PosProduct]를 반환한다. 제안하기 "대표 제품" 선택 등 폼의 제품 선택에서
/// 공용으로 사용한다. 디자인은 거래처 선택 바텀시트(AccountSelectorSheet)와 정합.
class ProductSelectorSheet extends ConsumerStatefulWidget {
  const ProductSelectorSheet({super.key});

  /// 바텀시트로 표시하고 선택된 제품을 반환한다 (취소 시 null).
  static Future<PosProduct?> show(BuildContext context) {
    return showModalBottomSheet<PosProduct>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (_) => const ProductSelectorSheet(),
    );
  }

  @override
  ConsumerState<ProductSelectorSheet> createState() =>
      _ProductSelectorSheetState();
}

class _ProductSelectorSheetState extends ConsumerState<ProductSelectorSheet> {
  final TextEditingController _searchController = TextEditingController();
  List<PosProduct> _products = [];
  bool _loading = false;
  bool _searched = false;
  String? _error;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _onSearch(String value) async {
    final keyword = value.trim();
    if (keyword.isEmpty) return;
    setState(() {
      _loading = true;
      _searched = true;
      _error = null;
    });
    try {
      final result =
          await ref.read(posProductUseCaseProvider).searchByText(keyword);
      if (!mounted) return;
      setState(() {
        _products = result;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = '제품을 불러오지 못했습니다';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.7,
        child: Column(
          children: [
            const SizedBox(height: AppSpacing.sm),
            // 핸들 바
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            // 제목
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('제품 선택', style: AppTypography.headlineSmall),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            // 검색
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: TextField(
                controller: _searchController,
                textInputAction: TextInputAction.search,
                onSubmitted: _onSearch,
                decoration: InputDecoration(
                  hintText: '제품명 / 코드 검색',
                  prefixIcon: const Icon(Icons.search),
                  isDense: true,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  ),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Expanded(child: _buildList()),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text(_error!));
    }
    if (!_searched) {
      return Center(
        child: Text(
          '제품명 또는 코드를 검색하세요',
          style:
              AppTypography.bodyMedium.copyWith(color: AppColors.textSecondary),
        ),
      );
    }
    if (_products.isEmpty) {
      return const Center(child: Text('검색 결과가 없습니다'));
    }
    return ListView.separated(
      itemCount: _products.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        final product = _products[index];
        return ListTile(
          title: Text(product.productName, style: AppTypography.bodyLarge),
          subtitle: Text(
            product.productCode,
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
          onTap: () => Navigator.of(context).pop(product),
        );
      },
    );
  }
}
