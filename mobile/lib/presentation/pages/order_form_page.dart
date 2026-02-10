import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../app_router.dart';
import '../providers/order_form_provider.dart';
import '../providers/order_form_state.dart';
import '../widgets/order_form/draft_banner.dart';
import '../widgets/order_form/client_selector.dart';
import '../widgets/order_form/credit_balance_display.dart';
import '../widgets/order_form/delivery_date_picker.dart';
import '../widgets/order_form/product_list_section.dart';
import '../widgets/order_form/total_amount_display.dart';
import '../widgets/order_form/order_form_action_buttons.dart';
import '../widgets/order_form/add_product_bottom_sheet.dart';

class OrderFormPage extends ConsumerStatefulWidget {
  final int? orderId; // null = new order, non-null = edit mode

  const OrderFormPage({super.key, this.orderId});

  @override
  ConsumerState<OrderFormPage> createState() => _OrderFormPageState();
}

class _OrderFormPageState extends ConsumerState<OrderFormPage> {
  late ScrollController _scrollController;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(orderFormProvider.notifier).initialize(orderId: widget.orderId);
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _showDatePicker(
    BuildContext context,
    OrderFormNotifier notifier,
    DateTime? currentDate,
  ) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: currentDate ?? now,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365)),
      locale: const Locale('ko', 'KR'),
    );
    if (picked != null) {
      notifier.setDeliveryDate(picked);
    }
  }

  void _showDeleteConfirmDialog(
    BuildContext context,
    OrderFormNotifier notifier,
  ) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('주문서 삭제'),
        content: const Text('주문서를 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              notifier.deleteOrder();
              AppRouter.navigateToAndReplace(context, AppRouter.orderList);
            },
            child: Text(
              '삭제',
              style: TextStyle(color: AppColors.error),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(orderFormProvider);
    final notifier = ref.read(orderFormProvider.notifier);

    // Listen for success/error messages
    ref.listen<OrderFormState>(orderFormProvider, (prev, next) {
      if (next.successMessage != null &&
          next.successMessage != prev?.successMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.successMessage!)),
        );
        notifier.clearSuccess();
      }
      if (next.errorMessage != null &&
          next.errorMessage != prev?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            backgroundColor: AppColors.error,
          ),
        );
        notifier.clearError();
      }
      // Navigate on submit success
      if (next.submitResult != null && prev?.submitResult == null) {
        AppRouter.navigateToAndReplace(context, AppRouter.orderList);
      }
    });

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        title: Text(state.isEditMode ? '주문서 수정' : '주문서 작성'),
        centerTitle: true,
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              controller: _scrollController,
              padding: AppSpacing.screenAll,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (state.hasDraft)
                    DraftBanner(
                      onLoadDraft: () => notifier.loadDraftOrder(),
                      onNewOrder: () => notifier.initializeNewOrder(),
                    ),
                  if (state.hasDraft) const SizedBox(height: AppSpacing.lg),
                  ClientSelector(
                    clients: state.clients,
                    selectedClientId: state.selectedClientId,
                    onClientSelected: (clientId) {
                      final name = state.clients[clientId] ?? '';
                      notifier.selectClient(clientId, name);
                    },
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  CreditBalanceDisplay(
                    creditBalance: state.creditBalance,
                    isLoading: state.isLoading,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  DeliveryDatePicker(
                    selectedDate: state.deliveryDate,
                    onTap: () =>
                        _showDatePicker(context, notifier, state.deliveryDate),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  ProductListSection(
                    items: state.items,
                    validationErrors: state.validationErrors,
                    allItemsSelected: state.allItemsSelected,
                    onToggleSelection: notifier.toggleProductSelection,
                    onToggleSelectAll: notifier.toggleSelectAllProducts,
                    onAddProduct: () {
                      AddProductBottomSheet.show(context);
                    },
                    onBarcodeScan: () {
                      // TODO: 바코드 스캔 기능 (향후 구현)
                    },
                    onRemoveSelected: notifier.removeSelectedProducts,
                    onQuantityChanged: notifier.updateProductQuantity,
                    scrollController: _scrollController,
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  TotalAmountDisplay(totalAmount: state.totalAmount),
                  const SizedBox(height: AppSpacing.lg),
                  OrderFormActionButtons(
                    onDelete: () =>
                        _showDeleteConfirmDialog(context, notifier),
                    onSaveDraft: () => notifier.saveDraft(),
                    onSubmit: () => notifier.validateAndSubmitOrder(),
                    isSubmitting: state.isSubmitting,
                    hasItems: state.hasItems,
                  ),
                  const SizedBox(height: AppSpacing.xxxl),
                ],
              ),
            ),
    );
  }
}
