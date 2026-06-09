import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/pos_product.dart';
import '../../providers/pos_sales_provider.dart';

/// POS 매출 조회용 제품명 검색 바텀시트.
///
/// 레거시 `posmain.jsp` 의 제품명 팝업(`productSelectList`) 동등 — 제품명/제품코드로 검색해 여러 제품을
/// 선택하고 [PosProduct] 목록을 반환한다 (취소 시 null/빈 목록).
class PosProductSearchSheet extends ConsumerStatefulWidget {
  const PosProductSearchSheet({super.key});

  /// 바텀시트로 표시하고 선택된 제품 목록을 반환한다.
  static Future<List<PosProduct>?> show(BuildContext context) {
    return showModalBottomSheet<List<PosProduct>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(
        borderRadius:
            BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusXl)),
      ),
      builder: (_) => const PosProductSearchSheet(),
    );
  }

  @override
  ConsumerState<PosProductSearchSheet> createState() =>
      _PosProductSearchSheetState();
}

class _PosProductSearchSheetState extends ConsumerState<PosProductSearchSheet> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _debounce;

  List<PosProduct> _results = [];
  final Set<String> _selected = {};
  final Map<String, PosProduct> _selectedProducts = {};
  bool _loading = false;
  String? _error;
  bool _searched = false;

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () => _search(value));
  }

  Future<void> _search(String query) async {
    final trimmed = query.trim();
    if (trimmed.isEmpty) {
      setState(() {
        _results = [];
        _searched = false;
        _error = null;
      });
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _searched = true;
    });
    try {
      final results =
          await ref.read(posProductUseCaseProvider).searchByText(trimmed);
      if (!mounted) return;
      setState(() {
        _results = results;
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

  void _toggle(PosProduct product) {
    final key = PosProductSheetKey.of(product);
    setState(() {
      if (_selected.contains(key)) {
        _selected.remove(key);
        _selectedProducts.remove(key);
      } else {
        _selected.add(key);
        _selectedProducts[key] = product;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.8,
        child: Column(
          children: [
            const SizedBox(height: AppSpacing.sm),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('제품 검색', style: AppTypography.headlineSmall),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: TextField(
                controller: _searchController,
                autofocus: true,
                onChanged: _onChanged,
                decoration: InputDecoration(
                  hintText: '제품명 또는 제품코드를 입력하세요',
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
            _buildFooter(),
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
          '제품을 검색하세요.',
          style: AppTypography.bodyMedium
              .copyWith(color: AppColors.textSecondary),
        ),
      );
    }
    if (_results.isEmpty) {
      return Center(
        child: Text(
          '검색 결과가 없습니다.',
          style: AppTypography.bodyMedium
              .copyWith(color: AppColors.textSecondary),
        ),
      );
    }
    return ListView.separated(
      itemCount: _results.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        final product = _results[index];
        final key = PosProductSheetKey.of(product);
        final selected = _selected.contains(key);
        return CheckboxListTile(
          value: selected,
          onChanged: (_) => _toggle(product),
          controlAffinity: ListTileControlAffinity.leading,
          title: Text(product.productName, style: AppTypography.bodyLarge),
          subtitle: Text(
            product.barcode.isEmpty
                ? '바코드 없음'
                : '바코드 ${product.barcode}',
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
        );
      },
    );
  }

  Widget _buildFooter() {
    return Container(
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: AppColors.divider)),
      ),
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: SafeArea(
        top: false,
        child: SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _selected.isEmpty
                ? null
                : () => Navigator.of(context)
                    .pop(_selectedProducts.values.toList()),
            style: ElevatedButton.styleFrom(
              minimumSize: const Size(double.infinity, 48),
              backgroundColor: AppColors.otokiRed,
              foregroundColor: AppColors.white,
              disabledBackgroundColor: AppColors.surface,
              disabledForegroundColor: AppColors.textSecondary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              ),
            ),
            child: Text(
              _selected.isEmpty ? '제품 추가' : '제품 추가 (${_selected.length}개)',
            ),
          ),
        ),
      ),
    );
  }
}

/// 시트 내부 선택 식별 키 헬퍼.
class PosProductSheetKey {
  static String of(PosProduct p) => '${p.productCode}|${p.barcode}';
}
