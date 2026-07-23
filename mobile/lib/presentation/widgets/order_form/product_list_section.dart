import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';
import 'order_product_card.dart';

/// 제품 목록 섹션
class ProductListSection extends StatefulWidget {
  final List<OrderDraftItem> items;
  final Map<String, ValidationError> validationErrors;
  final bool allItemsSelected;
  final ValueChanged<String> onToggleSelection;
  final VoidCallback onToggleSelectAll;
  final VoidCallback onAddProduct;
  final VoidCallback onBarcodeScan;
  final VoidCallback onRemoveSelected;
  final Function(String productCode, double boxes, int pieces) onQuantityChanged;
  final ScrollController? scrollController;

  const ProductListSection({
    super.key,
    required this.items,
    required this.validationErrors,
    required this.allItemsSelected,
    required this.onToggleSelection,
    required this.onToggleSelectAll,
    required this.onAddProduct,
    required this.onBarcodeScan,
    required this.onRemoveSelected,
    required this.onQuantityChanged,
    this.scrollController,
  });

  @override
  State<ProductListSection> createState() => _ProductListSectionState();
}

class _ProductListSectionState extends State<ProductListSection> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final items = widget.items;
    final hasSelectedItems = items.any((item) => item.isSelected);
    // 100개 상한 도달 시 제품 추가 차단 (바코드/추가 버튼 비활성화).
    final canAddMore = items.length < 100;
    // 품목이 1개라도 있으면 검색창을 상시 노출한다 (UX 일관성).
    final showSearch = items.isNotEmpty;

    // 필터링해도 원본 순번(레거시 "N." 표기)은 유지되도록 원래 인덱스를 함께 보관.
    final query = _searchQuery.toLowerCase();
    final filtered = <MapEntry<int, OrderDraftItem>>[];
    for (var i = 0; i < items.length; i++) {
      final item = items[i];
      if (query.isEmpty ||
          item.productName.toLowerCase().contains(query) ||
          item.productCode.toLowerCase().contains(query)) {
        filtered.add(MapEntry(i, item));
      }
    }
    final isSearching = _searchQuery.trim().isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 레거시 write.jsp: "제품 *" 라벨 우측에 바코드 / +추가 버튼 배치.
        Row(
          children: [
            RichText(
              text: TextSpan(
                text: '제품 ',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                ),
                children: [
                  TextSpan(
                    text: '*',
                    style: TextStyle(
                      color: AppColors.error,
                    ),
                  ),
                ],
              ),
            ),
            const Spacer(),
            OutlinedButton.icon(
              onPressed: canAddMore ? widget.onBarcodeScan : null,
              icon: const Icon(Icons.qr_code_scanner, size: 18),
              label: const Text('바코드'),
              style: OutlinedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (폭 min 0), 높이는 44px 탭영역 확보.
                minimumSize: const Size(0, 44),
                foregroundColor: AppColors.textPrimary,
                disabledForegroundColor: AppColors.textTertiary,
                side: BorderSide(color: AppColors.textSecondary),
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            OutlinedButton.icon(
              onPressed: canAddMore ? widget.onAddProduct : null,
              icon: const Icon(Icons.add, size: 18),
              label: const Text('추가'),
              style: OutlinedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (폭 min 0), 높이는 44px 탭영역 확보.
                minimumSize: const Size(0, 44),
                foregroundColor: AppColors.textPrimary,
                disabledForegroundColor: AppColors.textTertiary,
                side: BorderSide(color: AppColors.textSecondary),
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        // 레거시 write.jsp: 100개 권장 안내 (빨강, 2줄).
        Text(
          '품목 추가는 100개 이하로 하시는 것을 권장합니다.',
          style: AppTypography.bodySmall.copyWith(color: AppColors.error),
        ),
        Text(
          '주문 품목이 100개를 초과하는 경우 분할하여 주문요청 부탁드립니다.',
          style: AppTypography.bodySmall.copyWith(color: AppColors.error),
        ),
        const SizedBox(height: AppSpacing.lg),
        // 레거시 write.jsp: 선택 삭제(좌, 빨강 버튼) / 전체 선택(우, 체크박스).
        Row(
          children: [
            ElevatedButton(
              onPressed: hasSelectedItems ? widget.onRemoveSelected : null,
              style: ElevatedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
                minimumSize: Size.zero,
                backgroundColor: AppColors.error,
                foregroundColor: AppColors.white,
                // ignore: deprecated_member_use
                disabledBackgroundColor: AppColors.error.withOpacity(0.4),
                disabledForegroundColor: AppColors.white,
                elevation: 0,
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                ),
                visualDensity: VisualDensity.compact,
              ),
              child: const Text('선택 삭제'),
            ),
            const Spacer(),
            GestureDetector(
              onTap: widget.onToggleSelectAll,
              child: Text(
                '전체 선택',
                style: AppTypography.bodyMedium,
              ),
            ),
            Checkbox(
              value: widget.allItemsSelected,
              onChanged: (value) => widget.onToggleSelectAll(),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.sm),
        // 추가 품목이 많을 때 제품명/코드로 목록을 좁혀 찾을 수 있는 검색창.
        if (showSearch) ...[
          TextField(
            controller: _searchController,
            onChanged: (value) => setState(() => _searchQuery = value),
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              isDense: true,
              hintText: '추가한 제품 검색 (제품명·코드)',
              prefixIcon: const Icon(Icons.search, size: 20),
              suffixIcon: isSearching
                  ? IconButton(
                      icon: const Icon(Icons.clear, size: 20),
                      tooltip: '검색어 지우기',
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _searchQuery = '');
                      },
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.sm,
              ),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              ),
            ),
          ),
          if (isSearching) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              '${filtered.length}개 표시 중 (전체 ${items.length}개)',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.sm),
        ],
        // 검색 결과가 없을 때는 빈 화면 대신 명시적 안내 노출.
        if (isSearching && filtered.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
            child: Center(
              child: Text(
                "'${_searchQuery.trim()}'에 해당하는 제품이 없습니다.",
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          )
        else
          ListView.builder(
            controller: widget.scrollController,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: filtered.length,
            itemBuilder: (context, index) {
              // entry.key = 원본 순번(표시용), entry.value = 아이템.
              final entry = filtered[index];
              final item = entry.value;
              final error = widget.validationErrors[item.productCode];

              return OrderProductCard(
                index: entry.key,
                item: item,
                validationError: error,
                onSelectionChanged: (selected) {
                  widget.onToggleSelection(item.productCode);
                },
                onQuantityChanged: (boxes, pieces) {
                  widget.onQuantityChanged(item.productCode, boxes, pieces);
                },
              );
            },
          ),
      ],
    );
  }
}
