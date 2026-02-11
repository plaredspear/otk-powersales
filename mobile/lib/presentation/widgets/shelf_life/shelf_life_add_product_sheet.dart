import 'dart:async';

import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 제품 선택 결과
class ProductSelection {
  final String productCode;
  final String productName;

  const ProductSelection({
    required this.productCode,
    required this.productName,
  });
}

/// 유통기한 제품 추가 BottomSheet
///
/// 단일 제품 선택 전용입니다.
/// 제품 검색 후 탭하면 즉시 선택되어 등록 화면에 반영됩니다.
class ShelfLifeAddProductSheet extends StatefulWidget {
  const ShelfLifeAddProductSheet({super.key});

  /// BottomSheet 표시 (선택된 제품을 반환)
  static Future<ProductSelection?> show(BuildContext context) {
    return showModalBottomSheet<ProductSelection>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (sheetContext) => const ShelfLifeAddProductSheet(),
    );
  }

  @override
  State<ShelfLifeAddProductSheet> createState() =>
      _ShelfLifeAddProductSheetState();
}

class _ShelfLifeAddProductSheetState extends State<ShelfLifeAddProductSheet> {
  final _searchController = TextEditingController();
  Timer? _debounceTimer;
  List<_MockProduct> _searchResults = [];
  bool _isLoading = false;
  bool _hasSearched = false;

  @override
  void dispose() {
    _searchController.dispose();
    _debounceTimer?.cancel();
    super.dispose();
  }

  void _onSearchChanged(String query) {
    _debounceTimer?.cancel();
    _debounceTimer = Timer(const Duration(milliseconds: 400), () {
      _performSearch(query);
    });
  }

  void _performSearch(String query) {
    if (query.length < 2) {
      setState(() {
        _searchResults = [];
        _hasSearched = false;
      });
      return;
    }

    setState(() {
      _isLoading = true;
    });

    // Mock 검색 (실제로는 API 호출)
    Future.delayed(const Duration(milliseconds: 200), () {
      if (!mounted) return;

      final results = _mockProducts.where((p) {
        final q = query.toLowerCase();
        return p.productName.toLowerCase().contains(q) ||
            p.productCode.contains(q);
      }).toList();

      setState(() {
        _searchResults = results;
        _isLoading = false;
        _hasSearched = true;
      });
    });
  }

  void _onProductTap(_MockProduct product) {
    Navigator.of(context).pop(
      ProductSelection(
        productCode: product.productCode,
        productName: product.productName,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.75,
      child: Column(
        children: [
          // 핸들 바
          Center(
            child: Container(
              margin: const EdgeInsets.only(top: AppSpacing.sm),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),

          // 헤더
          Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('제품 선택', style: AppTypography.headlineMedium),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),

          // 검색 바
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              decoration: InputDecoration(
                hintText: '제품명 또는 제품코드를 입력하세요 (2자 이상)',
                hintStyle: AppTypography.bodyMedium
                    .copyWith(color: AppColors.textTertiary),
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchController.clear();
                          setState(() {
                            _searchResults = [];
                            _hasSearched = false;
                          });
                        },
                      )
                    : null,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.md,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(color: AppColors.divider),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(color: AppColors.divider),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(color: AppColors.primary, width: 2),
                ),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.md),

          // 검색 결과
          Expanded(child: _buildSearchResults()),
        ],
      ),
    );
  }

  Widget _buildSearchResults() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (!_hasSearched) {
      return Center(
        child: Text(
          '제품명 또는 제품코드로 검색하세요',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textTertiary,
          ),
        ),
      );
    }

    if (_searchResults.isEmpty) {
      return Center(
        child: Text(
          '검색 결과가 없습니다',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textTertiary,
          ),
        ),
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      itemCount: _searchResults.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final product = _searchResults[index];
        return ListTile(
          contentPadding: const EdgeInsets.symmetric(
            vertical: AppSpacing.xs,
          ),
          title: Text(
            product.productName,
            style: AppTypography.bodyMedium,
          ),
          subtitle: Text(
            product.productCode,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          trailing: const Icon(
            Icons.chevron_right,
            color: AppColors.textTertiary,
          ),
          onTap: () => _onProductTap(product),
        );
      },
    );
  }
}

// ──────────────────────────────────────────────────────────────────
// Mock Product Data
// ──────────────────────────────────────────────────────────────────

class _MockProduct {
  final String productCode;
  final String productName;

  const _MockProduct(this.productCode, this.productName);
}

const _mockProducts = [
  _MockProduct('30310009', '고등어김치&무조림(캔)280G'),
  _MockProduct('11110015', '카레케찹280G'),
  _MockProduct('11610028', '고깃집소스(트레이더스)830G'),
  _MockProduct('18410022', '미니뿌셔_불고기맛(55GX4)'),
  _MockProduct('10210005', '진라면(순한맛)5입'),
  _MockProduct('10210006', '진라면(매운맛)5입'),
  _MockProduct('11310012', '오뚜기카레약간매운맛100G'),
  _MockProduct('12110008', '3분짜장'),
  _MockProduct('12110009', '3분카레'),
  _MockProduct('12110010', '3분짜장덮밥'),
  _MockProduct('10110001', '오뚜기밥'),
  _MockProduct('10110002', '맛있는오뚜기밥210G'),
  _MockProduct('15110001', '참기름160ML'),
  _MockProduct('15110002', '방앗간참기름320ML'),
  _MockProduct('16110001', '오뚜기케첩300G'),
  _MockProduct('16110002', '토마토케첩500G'),
];
