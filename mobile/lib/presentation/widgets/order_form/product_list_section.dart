import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_draft.dart';
import '../../../domain/entities/validation_error.dart';
import 'order_product_card.dart';

/// 제품 목록 섹션
///
/// CustomScrollView 안에 놓이는 **sliver** 위젯이다. 선택 삭제/전체 선택/검색 바는
/// 제품이 많아도 위아래로 스크롤하지 않게 목록 위에 고정(pinned)한다.
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

  /// 강조 표시할 제품코드 (승인요청 → "수량 미입력" 탭으로 이동해 온 대상).
  final String? highlightedProductCode;

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
    this.highlightedProductCode,
  });

  @override
  State<ProductListSection> createState() => ProductListSectionState();
}

class ProductListSectionState extends State<ProductListSection> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';

  /// 검색으로 목록이 좁혀져 있으면 대상이 필터에서 빠져 스크롤할 수 없다.
  /// 이동 전에 검색을 해제해 전체 목록을 복원한다.
  void clearSearch() {
    if (_searchQuery.isEmpty) return;
    _searchController.clear();
    setState(() => _searchQuery = '');
  }

  /// 툴바 앞(상단 폼 + 제품 헤더)의 스크롤 길이 = 툴바가 상단에 고정되는 지점.
  /// 검색으로 목록이 짧아져도 그 지점을 유지하도록 채움 높이 계산에 쓴다.
  /// 레이아웃 중 sliver 순서대로 갱신되므로 setState 대상이 아니다.
  double _toolbarScrollExtent = 0;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  /// 검색으로 목록이 줄었을 때 첫 결과가 툴바에 가려지지 않도록,
  /// 툴바가 상단에 막 붙는 지점까지만 스크롤을 되돌린다(그 위로는 움직이지 않음).
  void _alignToPinnedToolbar() {
    if (_searchQuery.trim().isEmpty) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final position = Scrollable.maybeOf(context)?.position;
      if (position == null || !position.hasContentDimensions) return;
      final target = _toolbarScrollExtent.clamp(0.0, position.maxScrollExtent);
      if (position.pixels > target) position.jumpTo(target);
    });
  }

  @override
  Widget build(BuildContext context) {
    final items = widget.items;
    final hasSelectedItems = items.any((item) => item.isSelected);
    // 담기에는 라인 수 상한이 없다 — 100개를 넘겨 담을 수 있고, 초과 시 승인요청 버튼만
    // 비활성화된다(하단 고정 바). 그래서 바코드/추가 버튼은 항상 활성 상태로 둔다.
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

    final stickyToolbar = Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        0,
        AppSpacing.lg,
        AppSpacing.sm,
      ),
      child: _buildStickyToolbar(
        hasSelectedItems: hasSelectedItems,
        showSearch: showSearch,
        isSearching: isSearching,
        filteredCount: filtered.length,
        totalCount: items.length,
      ),
    );

    return SliverMainAxisGroup(
      slivers: [
        // ── 스크롤되는 헤더 (제품 라벨 + 바코드/추가 버튼 + 100개 안내) ──
        SliverToBoxAdapter(
          child: Padding(
            padding: AppSpacing.screenHorizontal,
            child: _buildHeader(),
          ),
        ),
        // ── 목록 위에 고정되는 툴바 (선택 삭제 / 전체 선택 / 검색) ──
        SliverLayoutBuilder(
          builder: (context, constraints) {
            // scrollOffset > 0 = 툴바가 실제로 상단에 붙어 아래로 제품 카드가
            // 지나가는 상태. 이때만 그림자로 층을 구분한다.
            final isPinned = constraints.scrollOffset > 0;
            _toolbarScrollExtent = constraints.precedingScrollExtent;
            return PinnedHeaderSliver(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: AppColors.background,
                  boxShadow: isPinned
                      ? [
                          BoxShadow(
                            color: AppColors.black.withValues(alpha: 0.08),
                            blurRadius: 6,
                            offset: const Offset(0, 3),
                          ),
                        ]
                      : null,
                ),
                // 스크롤마다 재빌드되는 builder 안에서 같은 위젯 인스턴스를 넘겨
                // 툴바 본문(TextField 포함) 서브트리는 재빌드되지 않게 한다.
                child: stickyToolbar,
              ),
            );
          },
        ),
        // ── 제품 카드 목록 ──
        if (isSearching && filtered.isEmpty)
          SliverToBoxAdapter(
            // 검색 결과가 없을 때는 빈 화면 대신 명시적 안내 노출.
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
              child: Center(
                child: Text(
                  "'${_searchQuery.trim()}'에 해당하는 제품이 없습니다.",
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ),
          )
        else
          SliverPadding(
            padding: AppSpacing.screenHorizontal,
            sliver: SliverList.builder(
              itemCount: filtered.length,
              itemBuilder: (context, index) {
                // entry.key = 원본 순번(표시용), entry.value = 아이템.
                final entry = filtered[index];
                final item = entry.value;
                final error = widget.validationErrors[item.productCode];

                final isHighlighted =
                    widget.highlightedProductCode == item.productCode;

                return OrderProductCard(
                  // 스크롤 이동 대상을 찾을 수 있도록 제품코드 기반 고정 키를 부여한다.
                  key: ValueKey('order-product-${item.productCode}'),
                  index: entry.key,
                  item: item,
                  validationError: error,
                  highlighted: isHighlighted,
                  onSelectionChanged: (selected) {
                    widget.onToggleSelection(item.productCode);
                  },
                  onQuantityChanged: (boxes, pieces) {
                    widget.onQuantityChanged(item.productCode, boxes, pieces);
                  },
                );
              },
            ),
          ),
        // 검색으로 목록이 짧아지면 스크롤 범위가 줄어 화면이 상단(거래처/납기일)으로
        // 튕겨 올라가던 문제 방지 — 툴바가 상단에 붙은 위치를 유지할 만큼 여백을 채운다.
        if (isSearching)
          SliverLayoutBuilder(
            builder: (context, constraints) {
              // 툴바가 시작된 지점부터 여기까지 차지한 길이(툴바 + 카드 목록).
              final belowToolbar =
                  constraints.precedingScrollExtent - _toolbarScrollExtent;
              final fill = (constraints.viewportMainAxisExtent - belowToolbar)
                  .clamp(0.0, double.infinity);
              return SliverToBoxAdapter(child: SizedBox(height: fill));
            },
          ),
      ],
    );
  }

  /// 스크롤되는 헤더 — 제품 라벨 + 바코드/추가 버튼 + 100개 권장 안내
  Widget _buildHeader() {
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
              onPressed: widget.onBarcodeScan,
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
              onPressed: widget.onAddProduct,
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
      ],
    );
  }

  /// 목록 위에 고정되는 툴바 — 선택 삭제 / 전체 선택 / 추가 품목 검색
  Widget _buildStickyToolbar({
    required bool hasSelectedItems,
    required bool showSearch,
    required bool isSearching,
    required int filteredCount,
    required int totalCount,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 레거시 write.jsp: 선택 삭제(좌, 빨강 버튼) / 전체 선택(우, 체크박스).
        Row(
          children: [
            ElevatedButton(
              onPressed: hasSelectedItems ? widget.onRemoveSelected : null,
              style: ElevatedButton.styleFrom(
                // Row 안 무한폭 제약 크래시 방지 (폭 min 0). 툴바 안 보조 버튼이라
                // 높이는 바코드/추가(44)보다 낮은 36 으로 둔다.
                minimumSize: const Size(0, 36),
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
              // 기본 48px 탭영역이 Row 높이를 부풀려 검색창과 간격이 벌어지던 것을 축소
              // (라벨 "전체 선택" 도 탭 가능해 조작성은 유지).
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              visualDensity: VisualDensity.compact,
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.xs),
        // 추가 품목이 많을 때 제품명/코드로 목록을 좁혀 찾을 수 있는 검색창.
        if (showSearch) ...[
          TextField(
            controller: _searchController,
            onChanged: (value) {
              setState(() => _searchQuery = value);
              _alignToPinnedToolbar();
            },
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
              '$filteredCount개 표시 중 (전체 $totalCount개)',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ],
      ],
    );
  }
}
