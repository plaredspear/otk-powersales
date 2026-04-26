import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
import '../../providers/order_form_provider.dart';
import 'favorite_products_tab.dart';
import 'search_products_tab.dart';
import 'order_history_tab.dart';

/// 제품 추가 BottomSheet
///
/// 3개 탭 (즐겨찾기/제품검색/주문이력)으로 구성된 Full Screen BottomSheet입니다.
class AddProductBottomSheet extends ConsumerStatefulWidget {
  const AddProductBottomSheet({super.key});

  /// BottomSheet 표시
  static Future<void> show(BuildContext context) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (sheetContext) => const AddProductBottomSheet(),
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
      ref.read(addProductProvider.notifier).initialize();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _onAddProducts() {
    final notifier = ref.read(addProductProvider.notifier);
    final orderFormNotifier = ref.read(orderFormProvider.notifier);

    final selectedItems = notifier.getSelectedOrderDraftItems();

    int addedCount = 0;
    for (final item in selectedItems) {
      // addProductToOrder ignores duplicates
      final beforeCount = ref.read(orderFormProvider).items.length;
      orderFormNotifier.addProductToOrder(item);
      final afterCount = ref.read(orderFormProvider).items.length;
      if (afterCount > beforeCount) {
        addedCount++;
      }
    }

    if (addedCount > 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$addedCount개 제품이 추가되었습니다.')),
      );
    } else if (selectedItems.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('이미 추가된 제품입니다.')),
      );
    }

    Navigator.of(context).pop();
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
                    '제품 추가',
                    style: AppTypography.headlineMedium,
                  ),
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
                  ),
                  SearchProductsTab(
                    scrollController: scrollController,
                  ),
                  OrderHistoryTab(
                    scrollController: scrollController,
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
                    onPressed: state.hasSelection ? _onAddProducts : null,
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
                      state.hasSelection
                          ? '제품 추가 (${state.selectedCount}개)'
                          : '제품 추가',
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
