import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/product_add_provider.dart';
import '../../providers/product_add_state.dart';
import '../common/single_select_sheet.dart';

/// 제품 선택 결과
class ProductSelection {
  final String productCode;
  final String productName;

  const ProductSelection({
    required this.productCode,
    required this.productName,
  });
}

/// 유통기한 "제품 추가" 화면
///
/// 레거시(Heroku productPop.jsp / poplayer.js, type=product) 정합 — 풀스크린 2탭
/// (제품 검색 / 주문 이력) 구성. 단일 제품 선택 전용으로, 제품을 선택하면 즉시
/// 등록 화면으로 반환된다.
class ProductExpirationAddProductSheet extends ConsumerStatefulWidget {
  /// 주문 이력 조회용 거래처 코드 (미선택 시 null).
  final String? accountCode;

  const ProductExpirationAddProductSheet({super.key, this.accountCode});

  /// 화면 표시 (선택된 제품을 반환).
  static Future<ProductSelection?> show(
    BuildContext context, {
    String? accountCode,
  }) {
    return Navigator.of(context).push<ProductSelection>(
      MaterialPageRoute(
        fullscreenDialog: true,
        builder: (_) =>
            ProductExpirationAddProductSheet(accountCode: accountCode),
      ),
    );
  }

  @override
  ConsumerState<ProductExpirationAddProductSheet> createState() =>
      _ProductExpirationAddProductSheetState();
}

class _ProductExpirationAddProductSheetState
    extends ConsumerState<ProductExpirationAddProductSheet>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: ProductAddTab.values.length, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(productAddProvider.notifier).loadCategories();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _onSelect(ProductAddItem product) {
    Navigator.of(context).pop(
      ProductSelection(
        productCode: product.productCode,
        productName: product.productName,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        backgroundColor: AppColors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.close, color: AppColors.textPrimary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text('제품 추가', style: AppTypography.headlineMedium),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Container(
            decoration: const BoxDecoration(
              border: Border(
                top: BorderSide(color: AppColors.divider, width: 1),
              ),
            ),
            child: TabBar(
              controller: _tabController,
              tabs: ProductAddTab.values
                  .map((t) => Tab(text: t.label))
                  .toList(),
              labelColor: AppColors.otokiBlue,
              unselectedLabelColor: AppColors.textSecondary,
              labelStyle: AppTypography.bodyLarge
                  .copyWith(fontWeight: FontWeight.w700),
              unselectedLabelStyle: AppTypography.bodyLarge,
              indicatorColor: AppColors.otokiBlue,
              indicatorWeight: AppSpacing.tabIndicatorWeight,
            ),
          ),
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _ProductSearchTab(onSelect: _onSelect),
          _OrderHistoryTab(
            accountCode: widget.accountCode,
            onSelect: _onSelect,
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────
// 제품 검색 탭
// ─────────────────────────────────────────────────────────────────────

class _ProductSearchTab extends ConsumerStatefulWidget {
  final ValueChanged<ProductAddItem> onSelect;

  const _ProductSearchTab({required this.onSelect});

  @override
  ConsumerState<_ProductSearchTab> createState() => _ProductSearchTabState();
}

class _ProductSearchTabState extends ConsumerState<_ProductSearchTab> {
  final _nameController = TextEditingController();
  final _barcodeController = TextEditingController();

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

    return Column(
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
                enabled: state.selectedMiddle != null && subs.isNotEmpty,
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
            onSelect: widget.onSelect,
          ),
        ),
      ],
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

/// 제품 1건 행 — 제품명(bold) + 상세(코드/바코드/보관/유통기한) + 선택 버튼.
class _ProductRow extends StatelessWidget {
  final ProductAddItem product;
  final ValueChanged<ProductAddItem> onSelect;

  const _ProductRow({required this.product, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    final details = <String>[
      '제품코드 : ${product.productCode}',
      if (product.barcode != null && product.barcode!.isNotEmpty)
        '바코드 : ${product.barcode}',
      if (product.storageCondition != null &&
          product.storageCondition!.isNotEmpty)
        '보관방법 : ${product.storageCondition}',
      if (product.shelfLifeText != null) '유통기한 : ${product.shelfLifeText}',
    ];

    return InkWell(
      onTap: () => onSelect(product),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
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
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            _SelectButton(onPressed: () => onSelect(product)),
          ],
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────
// 주문 이력 탭
// ─────────────────────────────────────────────────────────────────────

class _OrderHistoryTab extends ConsumerStatefulWidget {
  final String? accountCode;
  final ValueChanged<ProductAddItem> onSelect;

  const _OrderHistoryTab({required this.accountCode, required this.onSelect});

  @override
  ConsumerState<_OrderHistoryTab> createState() => _OrderHistoryTabState();
}

class _OrderHistoryTabState extends ConsumerState<_OrderHistoryTab> {
  bool _autoSearched = false;

  @override
  void initState() {
    super.initState();
    // 거래처가 선택돼 있으면 첫 진입 시 자동 조회 (레거시: 탭 클릭 시 조회).
    if (widget.accountCode != null && widget.accountCode!.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!_autoSearched && mounted) {
          _autoSearched = true;
          ref.read(productAddProvider.notifier).searchOrderHistory(
                widget.accountCode!,
              );
        }
      });
    }
  }

  static String _fmt(DateTime d) {
    final m = d.month.toString().padLeft(2, '0');
    final day = d.day.toString().padLeft(2, '0');
    return '${d.year}-$m-$day';
  }

  Future<void> _pickFrom(ProductAddState state) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: state.historyFrom,
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now,
    );
    if (picked == null) return;
    // 최대 3일 범위 제약 (레거시 daterangepicker maxSpan: 3일).
    var to = state.historyTo;
    if (to.isBefore(picked)) to = picked;
    if (to.difference(picked).inDays > 3) {
      to = picked.add(const Duration(days: 3));
    }
    if (to.isAfter(now)) to = DateTime(now.year, now.month, now.day);
    ref.read(productAddProvider.notifier).setHistoryRange(picked, to);
  }

  Future<void> _pickTo(ProductAddState state) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: state.historyTo,
      firstDate: state.historyFrom,
      lastDate: now,
    );
    if (picked == null) return;
    var from = state.historyFrom;
    if (picked.difference(from).inDays > 3) {
      from = picked.subtract(const Duration(days: 3));
    }
    ref.read(productAddProvider.notifier).setHistoryRange(from, picked);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productAddProvider);
    final hasAccount =
        widget.accountCode != null && widget.accountCode!.isNotEmpty;

    if (!hasAccount) {
      return const _EmptyMessage(
        icon: Icons.store_outlined,
        message: '거래처를 먼저 선택해주세요',
      );
    }

    return Column(
      children: [
        // 날짜 범위 + 검색 버튼
        Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Row(
            children: [
              Expanded(
                child: _DateField(
                  label: _fmt(state.historyFrom),
                  onTap: () => _pickFrom(state),
                ),
              ),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                child: Text('~'),
              ),
              Expanded(
                child: _DateField(
                  label: _fmt(state.historyTo),
                  onTap: () => _pickTo(state),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              _SearchButton(
                onPressed: () => ref
                    .read(productAddProvider.notifier)
                    .searchOrderHistory(widget.accountCode!),
              ),
            ],
          ),
        ),
        Container(height: 8, color: AppColors.surface),
        Expanded(
          child: _OrderHistoryList(state: state, onSelect: widget.onSelect),
        ),
      ],
    );
  }
}

class _OrderHistoryList extends StatelessWidget {
  final ProductAddState state;
  final ValueChanged<ProductAddItem> onSelect;

  const _OrderHistoryList({required this.state, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (!state.hasSearchedHistory) {
      return const _EmptyMessage(
        icon: Icons.history,
        message: '조회 기간을 선택하고 검색하세요',
      );
    }
    if (state.orderHistoryGroups.isEmpty) {
      return const _EmptyMessage(
        icon: Icons.history,
        message: '주문 이력이 없습니다',
      );
    }

    return ListView.builder(
      itemCount: state.orderHistoryGroups.length,
      itemBuilder: (context, index) {
        final group = state.orderHistoryGroups[index];
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 날짜 그룹 헤더 ("YYYY-MM-DD 주문")
            Container(
              color: AppColors.surface,
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: AppSpacing.sm,
              ),
              child: Text(
                '${group.orderDate} 주문',
                style: AppTypography.labelLarge
                    .copyWith(color: AppColors.textPrimary),
              ),
            ),
            ...group.products.map(
              (p) => Column(
                children: [
                  _ProductRow(product: p, onSelect: onSelect),
                  const Divider(height: 1, color: AppColors.divider),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}

// ─────────────────────────────────────────────────────────────────────
// 공용 위젯
// ─────────────────────────────────────────────────────────────────────

/// 레거시 필터 — 탭하면 바텀시트(SingleSelectSheet)로 단일 선택 (하단 보더 + 셰브론).
///
/// 중분류는 항목이 수십 개라 네이티브 드롭다운 오버레이 대신 바텀시트로 표시한다.
/// 첫 항목에 "전체"(value=null) sentinel 을 두어 필터 해제를 지원한다.
class _FilterField extends StatelessWidget {
  final String title;
  final String hint;
  final String? searchHint;
  final String? value;
  final List<String> items;
  final bool enabled;
  final ValueChanged<String?> onChanged;

  const _FilterField({
    required this.title,
    required this.hint,
    required this.value,
    required this.items,
    required this.onChanged,
    this.searchHint,
    this.enabled = true,
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
    return InkWell(
      onTap: enabled ? () => _open(context) : null,
      child: Container(
        height: 52,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
        alignment: Alignment.center,
        child: Row(
          children: [
            Expanded(
              child: Text(
                value ?? hint,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.bodyLarge.copyWith(
                  color:
                      enabled ? AppColors.textPrimary : AppColors.textTertiary,
                ),
              ),
            ),
            Icon(
              Icons.keyboard_arrow_down,
              color: enabled ? AppColors.textSecondary : AppColors.textTertiary,
            ),
          ],
        ),
      ),
    );
  }
}

/// 레거시 입력 필드 (보더 없는 라인 인풋).
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
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: TextField(
        controller: controller,
        keyboardType: keyboardType,
        inputFormatters: inputFormatters,
        textInputAction: TextInputAction.search,
        onSubmitted: onSubmitted,
        style: AppTypography.bodyLarge,
        decoration: InputDecoration(
          isCollapsed: true,
          contentPadding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
          hintText: hint,
          hintStyle: AppTypography.bodyLarge
              .copyWith(color: AppColors.legacyPlaceholder),
          border: InputBorder.none,
        ),
      ),
    );
  }
}

/// 노란 검색 버튼 (레거시 btn 노란색).
class _SearchButton extends StatelessWidget {
  final VoidCallback onPressed;

  const _SearchButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.legacyYellow,
      borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
        onTap: onPressed,
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.sm,
          ),
          child: Text(
            '검색',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ),
    );
  }
}

/// 제품 행의 회색 "선택" 버튼.
class _SelectButton extends StatelessWidget {
  final VoidCallback onPressed;

  const _SelectButton({required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.textPrimary,
        side: const BorderSide(color: AppColors.divider),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.xs,
        ),
        minimumSize: const Size(0, 36),
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
      ),
      child: Text('선택', style: AppTypography.bodyMedium),
    );
  }
}

/// 날짜 선택 필드.
class _DateField extends StatelessWidget {
  final String label;
  final VoidCallback onTap;

  const _DateField({required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onTap,
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.textPrimary,
        side: const BorderSide(color: AppColors.divider),
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.calendar_today, size: 14),
          const SizedBox(width: AppSpacing.xs),
          Text(label, style: AppTypography.bodySmall),
        ],
      ),
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
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 48, color: AppColors.textTertiary),
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
