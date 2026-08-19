import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/product_for_order.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
import 'favorite_products_tab.dart';
import 'search_products_tab.dart';
import 'order_history_tab.dart';

/// 제품 선택 BottomSheet (공용)
///
/// 즐겨찾기/제품검색/주문이력 3개 탭으로 구성된 Full Screen BottomSheet.
/// 주문서 작성뿐 아니라 클레임·현장점검·제안·유통기한·매출조회 등
/// 제품을 고르는 모든 화면에서 공용으로 사용한다.
///
/// 주문 도메인에 결합되지 않으며, 선택 결과를 `List<ProductForOrder>`로
/// [Navigator.pop] 반환한다. 호출 화면이 결과를 각자 모델로 매핑해 쓴다.
class AddProductBottomSheet extends ConsumerStatefulWidget {
  /// 헤더 제목
  final String title;

  /// 다건 선택 여부. false 면 단건 선택.
  final bool multiSelect;

  /// 검색 탭에 중/소분류 필터를 노출할지 여부(전산매출 등 분류검색용).
  final bool showCategoryFilter;

  /// 바코드가 없는 제품 선택을 차단할지 여부(POS/전산 등 바코드 필터용).
  final bool requireBarcode;

  /// 전용상품 선택을 차단할지 여부. 주문서 작성에서만 true(주문 불가 룰),
  /// 그 외 화면(클레임/점검/제안/유통기한/매출조회)은 전용상품도 선택 가능.
  final bool blockExclusive;

  /// 주문이력 탭 조회용 거래처 내부 ID(Account.id). 주문서처럼 거래처가
  /// 선택된 화면에서만 넘기며, 없으면 주문이력 탭은 비어 있다.
  final int? orderHistoryAccountId;

  /// 호출 화면에 **이미 담겨 있는** 제품 수. null 이면 표시하지 않는다.
  ///
  /// 이 시트는 목록만 보여줄 뿐 "지금까지 몇 개를 담았는지" 를 알 수 없어, 사용자가
  /// 시트를 닫고 뒤 화면을 확인해야만 누적 개수를 알 수 있었다. 호출 화면이 자기 기준
  /// 개수를 넘기면 헤더에 함께 보여준다.
  final int? addedCount;

  /// [addedCount] 를 경고색으로 강조할지 여부. 상한 판정은 도메인마다 다르므로
  /// (주문서는 100개) 이 시트가 아니라 호출 화면이 정한다.
  final bool highlightAddedCount;

  const AddProductBottomSheet({
    super.key,
    this.title = '제품 추가',
    this.multiSelect = true,
    this.showCategoryFilter = false,
    this.requireBarcode = false,
    this.blockExclusive = false,
    this.orderHistoryAccountId,
    this.addedCount,
    this.highlightAddedCount = false,
  });

  /// BottomSheet 표시 — 선택된 제품 목록을 반환(취소 시 null).
  static Future<List<ProductForOrder>?> show(
    BuildContext context, {
    String title = '제품 추가',
    bool multiSelect = true,
    bool showCategoryFilter = false,
    bool requireBarcode = false,
    bool blockExclusive = false,
    int? orderHistoryAccountId,
    int? addedCount,
    bool highlightAddedCount = false,
  }) {
    return showModalBottomSheet<List<ProductForOrder>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (sheetContext) => AddProductBottomSheet(
        title: title,
        multiSelect: multiSelect,
        showCategoryFilter: showCategoryFilter,
        requireBarcode: requireBarcode,
        blockExclusive: blockExclusive,
        orderHistoryAccountId: orderHistoryAccountId,
        addedCount: addedCount,
        highlightAddedCount: highlightAddedCount,
      ),
    );
  }

  @override
  ConsumerState<AddProductBottomSheet> createState() =>
      _AddProductBottomSheetState();
}

class _AddProductBottomSheetState extends ConsumerState<AddProductBottomSheet>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: AddProductTab.values.length,
      vsync: this,
    );
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        final tab = AddProductTab.values[_tabController.index];
        ref.read(addProductProvider.notifier).changeTab(tab);
      }
    });

    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(addProductProvider.notifier).initialize(
            multiSelect: widget.multiSelect,
            orderHistoryAccountId: widget.orderHistoryAccountId,
          );
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  /// 선택 확정 — 선택된 제품 목록을 반환하고 닫는다.
  /// 반환값 처리(주문 담기/폼 반영/조회 필터 등)는 호출 화면이 담당한다.
  void _onConfirm() {
    final selected = ref.read(addProductProvider.notifier).getSelectedProducts();
    Navigator.of(context).pop(selected);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(addProductProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.9,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      expand: false,
      builder: (context, scrollController) {
        return Column(
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
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: AppSpacing.sm,
              ),
              child: Row(
                children: [
                  Text(
                    widget.title,
                    style: AppTypography.headlineMedium,
                  ),
                  // 이미 담긴 개수 — 시트 안에서 누적 개수를 알 수 있게 한다.
                  if (widget.addedCount != null) ...[
                    const SizedBox(width: AppSpacing.sm),
                    Flexible(
                      child: Text(
                        '이미 추가됨 ${widget.addedCount}개',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySmall.copyWith(
                          color: widget.highlightAddedCount
                              ? AppColors.error
                              : AppColors.textSecondary,
                        ),
                      ),
                    ),
                  ],
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            // 탭바
            TabBar(
              controller: _tabController,
              tabs: AddProductTab.values
                  .map((tab) => Tab(text: tab.label))
                  .toList(),
              labelColor: AppColors.otokiBlue,
              unselectedLabelColor: AppColors.textTertiary,
              indicatorColor: AppColors.otokiBlue,
              indicatorWeight: AppSpacing.tabIndicatorWeight,
            ),
            // 탭 콘텐츠
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: [
                  FavoriteProductsTab(
                    scrollController: scrollController,
                    requireBarcode: widget.requireBarcode,
                    blockExclusive: widget.blockExclusive,
                  ),
                  SearchProductsTab(
                    // 무한스크롤 하단 감지를 위해 자체 controller 를 쓴다(공유 불가).
                    showCategoryFilter: widget.showCategoryFilter,
                    requireBarcode: widget.requireBarcode,
                    blockExclusive: widget.blockExclusive,
                  ),
                  OrderHistoryTab(
                    scrollController: scrollController,
                    requireBarcode: widget.requireBarcode,
                    blockExclusive: widget.blockExclusive,
                  ),
                ],
              ),
            ),
            // 하단 버튼
            Container(
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: AppColors.divider, width: 1),
                ),
              ),
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: SafeArea(
                top: false,
                child: SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: state.hasSelection ? _onConfirm : null,
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 48),
                      backgroundColor: AppColors.primary,
                      foregroundColor: AppColors.onPrimary,
                      disabledBackgroundColor: AppColors.surface,
                      disabledForegroundColor: AppColors.textSecondary,
                      shape: RoundedRectangleBorder(
                        borderRadius:
                            BorderRadius.circular(AppSpacing.radiusMd),
                      ),
                    ),
                    child: Text(
                      widget.multiSelect
                          ? (state.hasSelection
                              ? '제품 추가 (${state.selectedCount}개)'
                              : '제품 추가')
                          : '선택',
                    ),
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
