import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/product_add_provider.dart';
import '../../providers/product_add_state.dart';
import '../common/single_select_sheet.dart';

/// 전산매출 "제품명" 선택 팝업.
///
/// 레거시 `abcmain.jsp` 의 제품 검색 팝업(`productPop.jsp`, type=order) 정합 —
/// 중분류/소분류 + 제품명 + 바코드 검색으로 제품을 찾아 1건을 선택한다.
/// 선택 결과로 바코드를 포함한 [ProductAddItem] 을 반환한다(전산매출 조회는 `UPC_CD` 기준).
class ElectronicProductPickerSheet extends ConsumerStatefulWidget {
  const ElectronicProductPickerSheet({super.key});

  /// 화면 표시 (선택된 제품을 반환, 취소 시 null).
  static Future<ProductAddItem?> show(BuildContext context) {
    return Navigator.of(context).push<ProductAddItem>(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (_) => const ElectronicProductPickerSheet(),
      ),
    );
  }

  @override
  ConsumerState<ElectronicProductPickerSheet> createState() =>
      _ElectronicProductPickerSheetState();
}

class _ElectronicProductPickerSheetState
    extends ConsumerState<ElectronicProductPickerSheet> {
  final _nameController = TextEditingController();
  final _barcodeController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(productAddProvider.notifier).loadCategories();
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _barcodeController.dispose();
    super.dispose();
  }

  void _onSearch() {
    FocusScope.of(context).unfocus();
    ref.read(productAddProvider.notifier).search(
          productName: _nameController.text.trim(),
          barcode: _barcodeController.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productAddProvider);
    final notifier = ref.read(productAddProvider.notifier);

    final middles = state.categories.map((c) => c.middle).toList();
    final subs = state.subsForSelectedMiddle;

    return Scaffold(
      appBar: AppBar(title: const Text('제품명 선택')),
      body: Column(
        children: [
          // 중분류 / 소분류 드롭다운
          Row(
            children: [
              Expanded(
                child: _FilterField(
                  title: '중분류 선택',
                  hint: '중분류 전체',
                  searchHint: '분류 검색',
                  value: state.selectedMiddle,
                  items: middles,
                  onChanged: notifier.selectMiddle,
                ),
              ),
              Container(width: 1, height: 52, color: AppColors.divider),
              Expanded(
                child: _FilterField(
                  title: '소분류 선택',
                  hint: '소분류 전체',
                  value: state.selectedSub,
                  items: subs,
                  onChanged: notifier.selectSub,
                ),
              ),
            ],
          ),
          const Divider(height: 1, color: AppColors.divider),
          // 제품명 입력
          _SearchTextField(
            controller: _nameController,
            hint: '제품명 입력',
            onSubmitted: (_) => _onSearch(),
          ),
          const Divider(height: 1, color: AppColors.divider),
          // 제품바코드 입력 + 검색 버튼
          Row(
            children: [
              Expanded(
                child: _SearchTextField(
                  controller: _barcodeController,
                  hint: '제품바코드 입력',
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  onSubmitted: (_) => _onSearch(),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(right: AppSpacing.lg),
                child: _SearchButton(onPressed: _onSearch),
              ),
            ],
          ),
          Container(height: 8, color: AppColors.surface),
          // 제품 (N) 카운트
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.md,
            ),
            child: Row(
              children: [
                Text('제품 ', style: AppTypography.bodyLarge),
                Text(
                  '(${state.resultCount})',
                  style: AppTypography.bodyLarge
                      .copyWith(color: AppColors.legacyDanger),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: AppColors.divider),
          // 결과 목록
          Expanded(
            child: _ProductResultList(
              state: state,
              onSelect: (item) => Navigator.of(context).pop(item),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProductResultList extends StatelessWidget {
  final ProductAddState state;
  final ValueChanged<ProductAddItem> onSelect;

  const _ProductResultList({required this.state, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (!state.hasSearched) {
      return const _EmptyMessage(
        icon: Icons.search,
        message: '검색 조건을 입력하고 검색하세요',
      );
    }
    if (state.searchResults.isEmpty) {
      return const _EmptyMessage(
        icon: Icons.search_off,
        message: '검색 결과가 없습니다',
      );
    }
    return ListView.separated(
      itemCount: state.searchResults.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) => _ProductRow(
        product: state.searchResults[index],
        onSelect: onSelect,
      ),
    );
  }
}

/// 제품 1건 행 — 제품명(bold) + 상세(코드/바코드) + 선택 버튼.
class _ProductRow extends StatelessWidget {
  final ProductAddItem product;
  final ValueChanged<ProductAddItem> onSelect;

  const _ProductRow({required this.product, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    final hasBarcode = product.barcode != null && product.barcode!.isNotEmpty;
    final details = <String>[
      '제품코드 : ${product.productCode}',
      if (hasBarcode) '바코드 : ${product.barcode}',
    ];

    return InkWell(
      onTap: hasBarcode ? () => onSelect(product) : null,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.productName,
                    style: AppTypography.bodyLarge
                        .copyWith(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  ...details.map(
                    (d) => Padding(
                      padding: const EdgeInsets.only(top: AppSpacing.xxs),
                      child: Text(
                        d,
                        style: AppTypography.bodySmall
                            .copyWith(color: AppColors.textSecondary),
                      ),
                    ),
                  ),
                  if (!hasBarcode)
                    Padding(
                      padding: const EdgeInsets.only(top: AppSpacing.xxs),
                      child: Text(
                        '바코드가 없어 매출 조회에 추가할 수 없습니다',
                        style: AppTypography.bodySmall
                            .copyWith(color: AppColors.error),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            _SelectButton(
              onPressed: hasBarcode ? () => onSelect(product) : null,
            ),
          ],
        ),
      ),
    );
  }
}

/// 필터 — 탭하면 바텀시트(SingleSelectSheet)로 단일 선택.
///
/// 중분류는 항목이 수십 개라 네이티브 드롭다운 오버레이 대신 바텀시트로 표시한다.
/// 첫 항목에 "전체"(value=null) sentinel 을 두어 필터 해제를 지원한다.
class _FilterField extends StatelessWidget {
  final String title;
  final String hint;
  final String? searchHint;
  final String? value;
  final List<String> items;
  final ValueChanged<String?> onChanged;

  const _FilterField({
    required this.title,
    required this.hint,
    required this.value,
    required this.items,
    required this.onChanged,
    this.searchHint,
  });

  Future<void> _open(BuildContext context) async {
    final result = await SingleSelectSheet.show<String?>(
      context,
      title: title,
      selectedValue: value,
      searchHint: searchHint,
      options: [
        SingleSelectOption<String?>(value: null, label: hint),
        ...items.map((e) => SingleSelectOption<String?>(value: e, label: e)),
      ],
    );
    if (result == null) return;
    onChanged(result.value);
  }

  @override
  Widget build(BuildContext context) {
    final hasValue = value != null;
    return InkWell(
      onTap: () => _open(context),
      child: Container(
        height: 52,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
        alignment: Alignment.center,
        child: Row(
          children: [
            Expanded(
              child: Text(
                value ?? hint,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.bodyMedium.copyWith(
                  color:
                      hasValue ? AppColors.textPrimary : AppColors.textTertiary,
                ),
              ),
            ),
            const Icon(
              Icons.keyboard_arrow_down,
              size: 22,
              color: AppColors.textSecondary,
            ),
          ],
        ),
      ),
    );
  }
}

class _SearchTextField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final ValueChanged<String>? onSubmitted;

  const _SearchTextField({
    required this.controller,
    required this.hint,
    this.keyboardType,
    this.inputFormatters,
    this.onSubmitted,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: TextField(
        controller: controller,
        keyboardType: keyboardType,
        inputFormatters: inputFormatters,
        textInputAction: TextInputAction.search,
        onSubmitted: onSubmitted,
        decoration: InputDecoration(
          hintText: hint,
          border: InputBorder.none,
          isDense: true,
          hintStyle: AppTypography.bodyMedium
              .copyWith(color: AppColors.textTertiary),
        ),
      ),
    );
  }
}

class _SearchButton extends StatelessWidget {
  final VoidCallback onPressed;

  const _SearchButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.secondary,
        foregroundColor: AppColors.onSecondary,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.sm,
        ),
      ),
      child: const Text('검색'),
    );
  }
}

class _SelectButton extends StatelessWidget {
  final VoidCallback? onPressed;

  const _SelectButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.secondary,
        side: const BorderSide(color: AppColors.secondary),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.xs,
        ),
      ),
      child: const Text('선택'),
    );
  }
}

class _EmptyMessage extends StatelessWidget {
  final IconData icon;
  final String message;

  const _EmptyMessage({required this.icon, required this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 40, color: AppColors.textTertiary),
          const SizedBox(height: AppSpacing.md),
          Text(
            message,
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }
}
