import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_provider.dart';
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
class ShelfLifeAddProductSheet extends ConsumerStatefulWidget {
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
  ConsumerState<ShelfLifeAddProductSheet> createState() =>
      _ShelfLifeAddProductSheetState();
}

class _ShelfLifeAddProductSheetState
    extends ConsumerState<ShelfLifeAddProductSheet> {
  final _searchController = TextEditingController();
  Timer? _debounceTimer;
  List<ProductSelection> _searchResults = [];
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
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      _performSearch(query);
    });
  }

  Future<void> _performSearch(String query) async {
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

    try {
      final dio = ref.read(dioProvider);
      final response = await dio.get(
        '/api/v1/products/search',
        queryParameters: {
          'query': query,
          'type': 'text',
          'size': 20,
        },
      );

      if (!mounted) return;

      final data = response.data['data'] as Map<String, dynamic>;
      final content = data['content'] as List<dynamic>;
      final results = content.map((e) {
        final item = e as Map<String, dynamic>;
        return ProductSelection(
          productCode: item['product_code'] as String,
          productName: item['product_name'] as String,
        );
      }).toList();

      setState(() {
        _searchResults = results;
        _isLoading = false;
        _hasSearched = true;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _searchResults = [];
        _isLoading = false;
        _hasSearched = true;
      });
    }
  }

  void _onProductTap(ProductSelection product) {
    Navigator.of(context).pop(product);
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
      separatorBuilder: (_, __) => const Divider(height: 1),
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
