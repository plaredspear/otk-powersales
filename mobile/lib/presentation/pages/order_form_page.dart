import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../app_router.dart';
import '../providers/order_form_provider.dart';
import '../providers/order_form_state.dart';
import '../widgets/order_form/client_selector.dart';
import '../widgets/order_form/credit_balance_display.dart';
import '../widgets/order_form/delivery_date_picker.dart';
import '../widgets/order_form/delivery_date_warning_dialog.dart';
import '../widgets/order_form/draft_banner.dart';
import '../widgets/order_form/draft_delete_dialog.dart';
import '../widgets/order_form/draft_restore_dialog.dart';
import '../widgets/order_form/exit_confirm_dialog.dart';
import '../widgets/order_form/product_list_section.dart';
import '../widgets/order_form/total_amount_display.dart';
import '../widgets/order_form/order_form_action_buttons.dart';
import '../widgets/order_form/add_product_bottom_sheet.dart';

class OrderFormPage extends ConsumerStatefulWidget {
  final int? orderId; // null = new order, non-null = edit mode

  /// 제품검색 "주문서 등록"에서 진입 시 미리 담을 제품코드 (신규 주문 전용).
  final String? initialProductCode;

  const OrderFormPage({super.key, this.orderId, this.initialProductCode});

  @override
  ConsumerState<OrderFormPage> createState() => _OrderFormPageState();
}

class _OrderFormPageState extends ConsumerState<OrderFormPage> {
  late ScrollController _scrollController;
  bool _restoreDialogShown = false;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final notifier = ref.read(orderFormProvider.notifier);
      await notifier.initialize(orderId: widget.orderId);
      // 제품검색에서 전달된 제품이 있으면 주문 라인에 미리 추가
      final code = widget.initialProductCode;
      if (code != null && code.isNotEmpty) {
        await notifier.preloadProductByCode(code);
      }
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

  /// 페이지 이탈 시 호출. 라인/거래처 입력 있으면 다이얼로그.
  void _handlePopAttempt(BuildContext context, OrderFormNotifier notifier) {
    ExitConfirmDialog.show(
      context,
      onDiscard: () {
        notifier.discardForm();
        Navigator.of(context).pop();
      },
      onSaveDraft: () async {
        await notifier.saveDraft();
        if (!mounted) return;
        // saveDraft 성공/실패 여부는 successMessage/errorMessage 로 listener 가 SnackBar 표시.
        // 성공 시에는 자동 pop, 실패 시에는 페이지 유지 (사용자 재시도 가능).
        final latest = ref.read(orderFormProvider);
        if (latest.successMessage != null) {
          Navigator.of(context).pop();
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(orderFormProvider);
    final notifier = ref.read(orderFormProvider.notifier);

    // hasDraft 가 true 가 되는 순간 자동 다이얼로그 노출 (1회만).
    ref.listen<bool>(orderFormProvider.select((s) => s.hasDraft),
        (prev, next) {
      if (next == true && !_restoreDialogShown) {
        _restoreDialogShown = true;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) return;
          DraftRestoreDialog.show(
            context,
            onAccept: () => notifier.acceptDraft(),
            onDecline: () => notifier.declineDraft(),
          );
        });
      } else if (next == false) {
        _restoreDialogShown = false;
      }
    });

    // (I) 납기일 +10일 다이얼로그 트리거 (Spec #598 P3-M §2.6).
    ref.listen<bool>(
        orderFormProvider.select((s) => s.requiresDeliveryDateConfirm),
        (prev, next) {
      if (next == true) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) return;
          DeliveryDateWarningDialog.show(
            context,
            onConfirm: () => notifier.confirmDeliveryDateAndSubmit(),
            onCancel: () => notifier.cancelDeliveryDateConfirm(),
          );
        });
      }
    });

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

    final canPopFreely =
        state.orderDraft.items.isEmpty && state.selectedAccountId == null;

    return PopScope(
      canPop: canPopFreely,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        _handlePopAttempt(context, notifier);
      },
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => Navigator.of(context).maybePop(),
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
                        onLoadDraft: () => notifier.acceptDraft(),
                        onNewOrder: () => notifier.declineDraft(),
                      ),
                    if (state.hasDraft) const SizedBox(height: AppSpacing.lg),
                    ClientSelector(
                      clients: state.clients,
                      selectedClientId: state.selectedClientId,
                      onClientSelected: (clientId) {
                        final name = state.clients[clientId] ?? '';
                        final externalKey =
                            state.clientExternalKeys[clientId] ?? '';
                        notifier.selectClient(clientId, name, externalKey);
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
                      onTap: () => _showDatePicker(
                        context,
                        notifier,
                        state.deliveryDate,
                      ),
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
                    TotalAmountDisplay(
                      totalAmount: state.totalAmount,
                      creditBalance: state.creditBalance,
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    OrderFormActionButtons(
                      onDelete: () => DraftDeleteDialog.show(
                        context,
                        onConfirm: () => notifier.deleteDraft(),
                      ),
                      onSaveDraft: () => notifier.saveDraft(),
                      onSubmit: () => notifier.validateAndSubmitOrder(),
                      isSubmitting: state.isSubmitting,
                      hasItems: state.hasItems,
                    ),
                    const SizedBox(height: AppSpacing.xxxl),
                  ],
                ),
              ),
      ),
    );
  }
}
