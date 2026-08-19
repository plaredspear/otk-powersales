import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/order_deadline.dart';
import '../../app_router.dart';
import '../providers/order_form_provider.dart';
import '../providers/order_form_state.dart';
import '../../domain/entities/order_draft.dart';
import '../screens/barcode_scanner_screen.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../widgets/account/account_selector_field.dart';
import '../widgets/common/info_notice_banner.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/common/single_date_picker_sheet.dart';
import '../widgets/order_form/credit_balance_display.dart';
import '../widgets/order_form/delivery_date_picker.dart';
import '../widgets/order_form/delivery_date_warning_dialog.dart';
import '../widgets/order_form/draft_banner.dart';
import '../widgets/order_form/draft_delete_dialog.dart';
import '../widgets/order_form/draft_restore_dialog.dart';
import '../widgets/order_form/exit_confirm_dialog.dart';
import '../widgets/order_form/product_list_section.dart';
import '../widgets/order_form/submit_confirm_dialog.dart';
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

  /// 제품 목록의 검색 해제를 호출하기 위한 키 (수량 미입력 줄로 이동 시).
  final GlobalKey<ProductListSectionState> _productListKey =
      GlobalKey<ProductListSectionState>();

  /// 현재 강조 중인 제품코드 — 수량 미입력 줄로 이동한 직후 잠시 유지된다.
  String? _highlightedProductCode;

  /// 강조 해제 타이머 — 재진입/중복 탭 시 이전 타이머를 취소하기 위해 보관한다.
  Timer? _highlightTimer;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();

    WidgetsBinding.instance.addPostFrameCallback((_) => _initialize());
  }

  /// 비활성 상태의 승인요청 버튼 탭 — 막힌 사유를 토스트로 알린다.
  ///
  /// 사유 문구는 제출 검증([OrderFormNotifier.submitBlockReason])을 그대로 쓴다. 버튼 라벨은
  /// 폭이 좁아 사유를 한 단어로만 말하므로(예: `제품 100개 초과`), "무엇을 해야 하는가" 는
  /// 여기서 채운다. 수량 미입력이면 안내와 함께 첫 미입력 줄로 이동시킨다(종전 동작).
  void _handleDisabledSubmitTap(
    OrderFormState state,
    OrderFormNotifier notifier,
  ) {
    final reason = notifier.submitBlockReason ?? '승인요청에 필요한 항목을 확인해 주세요';
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(reason)));
    if (state.zeroQuantityLineCount > 0) {
      _scrollToFirstZeroQuantity(state);
    }
  }

  /// 수량이 0 인 첫 줄로 스크롤 이동 + 강조.
  ///
  /// 목록은 [SliverList.builder] 라 화면 밖 카드는 트리에 없다. 따라서 대상 카드의 키를
  /// 바로 [Scrollable.ensureVisible] 할 수 없고, 조금씩 내려가며 대상이 build 되기를 기다린 뒤
  /// 정밀 정렬한다. 카드 높이가 제품명 줄수/에러 유무로 달라 오프셋 산술 계산은 쓰지 않는다.
  Future<void> _scrollToFirstZeroQuantity(OrderFormState state) async {
    final target = state.orderDraft.items
        .cast<OrderDraftItem?>()
        .firstWhere((e) => (e?.totalPieces ?? 1) <= 0, orElse: () => null);
    if (target == null) return;
    final productCode = target.productCode;

    // 검색 중이면 대상이 필터에서 빠져 있을 수 있으므로 먼저 해제한다.
    _productListKey.currentState?.clearSearch();
    await WidgetsBinding.instance.endOfFrame;
    if (!mounted) return;

    final targetKey = ValueKey('order-product-$productCode');
    // 최대 60프레임(≈1초)까지 내려가며 대상이 트리에 올라오길 기다린다.
    for (var attempt = 0; attempt < 60; attempt++) {
      // 직전 프레임에서 트리를 훑어 찾은 context 이므로 이 시점엔 유효하다
      // (mounted 로 한 번 더 확인해 lint 의 async-gap 경고 조건도 만족시킨다).
      final targetContext = _findCardContext(targetKey);
      if (targetContext != null && targetContext.mounted) {
        await Scrollable.ensureVisible(
          targetContext,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeInOut,
          // 화면 상단(툴바 아래)이 아니라 중앙 부근에 두어 앞뒤 맥락이 함께 보이게 한다.
          alignment: 0.3,
        );
        break;
      }
      if (!_scrollController.hasClients) return;
      final position = _scrollController.position;
      if (position.pixels >= position.maxScrollExtent) break;
      // 한 화면씩 내려가며 lazy build 를 유도한다.
      _scrollController.jumpTo(
        (position.pixels + position.viewportDimension * 0.8)
            .clamp(0.0, position.maxScrollExtent),
      );
      await WidgetsBinding.instance.endOfFrame;
      if (!mounted) return;
    }

    if (!mounted) return;
    _highlightTimer?.cancel();
    setState(() => _highlightedProductCode = productCode);
    _highlightTimer = Timer(const Duration(seconds: 3), () {
      if (!mounted) return;
      setState(() => _highlightedProductCode = null);
    });
  }

  /// 렌더 트리에서 [key] 를 가진 제품 카드의 BuildContext 를 찾는다 (없으면 null).
  BuildContext? _findCardContext(ValueKey<String> key) {
    BuildContext? found;
    void visit(Element element) {
      if (found != null) return;
      if (element.widget.key == key) {
        found = element;
        return;
      }
      element.visitChildren(visit);
    }

    final rootElement = context as Element;
    rootElement.visitChildren(visit);
    return found;
  }

  /// 진입 초기화. 폼 로드 → (제품검색 진입이면 제품 preload) / (아니면 임시저장 복원 확인).
  ///
  /// 임시저장 복원 팝업은 build 의 리스너(값 전이 감지)가 아니라 여기서 진입 시
  /// 명령형으로 띄운다. Provider 가 재진입 후에도 살아있어(non-autoDispose) hasDraft 가
  /// 이미 true 인 상태로 재진입하면 리스너의 false→true 전이가 발생하지 않아 팝업이
  /// 누락되던 문제를 방지하기 위함. State 는 매 진입마다 새로 생성되므로 initState 는
  /// 항상 재실행되어 이 흐름이 매번 보장된다.
  Future<void> _initialize() async {
    final notifier = ref.read(orderFormProvider.notifier);
    await notifier.initialize(orderId: widget.orderId);
    if (!mounted) return;

    // 제품검색에서 전달된 제품이 있으면 주문 라인에 미리 추가하고 임시저장 복원은 생략.
    final code = widget.initialProductCode;
    if (code != null && code.isNotEmpty) {
      await notifier.preloadProductByCode(code);
      return;
    }

    // 임시저장이 있으면 이어쓰기 여부를 묻는다.
    if (ref.read(orderFormProvider).hasDraft && mounted) {
      DraftRestoreDialog.show(
        context,
        onAccept: () => notifier.acceptDraft(),
        onDecline: () => notifier.declineDraft(),
      );
    }
  }

  @override
  void dispose() {
    _highlightTimer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _showDatePicker(
    BuildContext context,
    OrderFormNotifier notifier,
    DateTime? currentDate,
  ) async {
    final now = DateTime.now();
    // 마감(납기일 전일 13:50)이 이미 지난 날짜는 고르는 순간 주문 불가라, 선택 자체를 막는다.
    // 레거시 write.jsp:21 의 `min = 오늘+1일` 정합 (13:50 이후면 모레부터).
    final firstDate = OrderDeadline.earliestDeliveryDate(now: now);
    final picked = await SingleDatePickerSheet.show(
      context,
      // 복원된 임시저장 납기일이 이미 마감을 넘겼으면 선택 가능한 최초일로 되돌린다.
      initialDate: currentDate ?? firstDate,
      firstDate: firstDate,
      lastDate: now.add(const Duration(days: 365)),
      title: '납기일 선택',
    );
    if (picked != null) {
      notifier.setDeliveryDate(picked);
    }
  }

  /// 바코드 스캔 — 카메라로 제품 바코드를 스캔해 주문 라인에 추가한다.
  ///
  /// 스캐너에서 받은 바코드를 [OrderFormNotifier.addProductByBarcode] 로 넘긴다.
  /// 추가 성공/실패 메시지는 build 의 success/error listener 가 SnackBar 로 노출한다.
  Future<void> _handleBarcodeScan(OrderFormNotifier notifier) async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || !mounted) return;
    await notifier.addProductByBarcode(barcode);
  }

  /// 제품 추가 모달(공용)에서 선택한 제품들을 주문 라인에 담는다.
  /// 담기에는 라인 수 상한이 없다 — 100개 초과는 승인요청 단계에서만 막힌다.
  /// 중복 무시/추가 결과 안내는 여기(호출부)에서 처리한다.
  Future<void> _handleAddProduct(OrderFormNotifier notifier) async {
    final messenger = ScaffoldMessenger.of(context);
    // 주문서 작성만 전용상품 추가를 막는다(주문 불가 룰). 그 외 화면은 선택 가능.
    // 주문이력 탭은 현재 선택된 거래처(Account.id) 기준으로 본인 주문이력을 조회한다.
    final selected = await AddProductBottomSheet.show(
      context,
      showCategoryFilter: true,
      blockExclusive: true,
      orderHistoryAccountId: ref.read(orderFormProvider).selectedAccountId,
    );
    if (selected == null || selected.isEmpty || !mounted) return;

    int addedCount = 0;
    for (final product in selected) {
      // addProductToOrder 는 중복을 무시한다.
      final beforeCount = ref.read(orderFormProvider).items.length;
      notifier.addProductToOrder(
        OrderDraftItem(
          productCode: product.productCode,
          productName: product.productName,
          quantityBoxes: 0,
          quantityPieces: 0,
          unitPrice: product.unitPrice,
          boxSize: product.boxSize,
          totalPrice: 0,
        ),
      );
      if (ref.read(orderFormProvider).items.length > beforeCount) {
        addedCount++;
      }
    }

    if (addedCount > 0) {
      messenger.showSnackBar(
        SnackBar(content: Text('$addedCount개 제품이 추가되었습니다.')),
      );
    } else {
      messenger.showSnackBar(
        const SnackBar(content: Text('이미 추가된 제품입니다.')),
      );
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
        // 성공/실패 안내 SnackBar 는 listener 가 표시한다.
        // 성공 여부는 반환값으로 판정 (state 의 successMessage 는 listener 가
        // 이미 소비했을 수 있어 신뢰할 수 없음). 성공 시에만 화면을 벗어나고,
        // 실패 시에는 페이지를 유지해 사용자가 재시도할 수 있게 한다.
        final saved = await notifier.saveDraft();
        if (!mounted) return;
        if (saved) {
          Navigator.of(context).pop();
        }
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(orderFormProvider);
    final notifier = ref.read(orderFormProvider.notifier);

    // 임시저장 복원 팝업은 진입 시 _initialize() 에서 명령형으로 띄운다.
    // (리스너의 값 전이 감지 방식은 non-autoDispose provider 재진입 시 누락되므로 사용 안 함)

    // 승인요청 확인 다이얼로그 트리거 (검증 통과 후 전송 직전).
    ref.listen<bool>(
      orderFormProvider.select((s) => s.requiresSubmitConfirm),
      (prev, next) {
        if (next == true) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (!mounted) return;
            SubmitConfirmDialog.show(
              context,
              onConfirm: () => notifier.confirmSubmit(),
              onCancel: () => notifier.cancelSubmitConfirm(),
            );
          });
        }
      },
    );

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
      },
    );

    // Listen for success/error messages
    ref.listen<OrderFormState>(orderFormProvider, (prev, next) {
      if (next.successMessage != null &&
          next.successMessage != prev?.successMessage) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.successMessage!)));
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

    // 잃을 내용이 없으면 확인 없이 바로 나간다.
    // 폼이 비어 있거나, 마지막 임시저장 이후 고친 것이 없는 경우가 여기 해당한다
    // (임시저장 직후 back → 팝업 재노출 방지).
    final canPopFreely = !state.isDirty;

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
        // 숫자 키패드는 iOS 에 '완료' 버튼이 없어 빈 영역 탭 / 스크롤로 키보드를 닫는다.
        body: state.isLoading
            ? const LoadingIndicator()
            : GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => FocusScope.of(context).unfocus(),
                // 제품 목록 툴바(선택 삭제/전체 선택/검색)를 고정하기 위해
                // 단일 스크롤뷰가 아닌 sliver 구성으로 그린다.
                child: CustomScrollView(
                  controller: _scrollController,
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  slivers: [
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(
                        AppSpacing.lg,
                        AppSpacing.lg,
                        AppSpacing.lg,
                        0,
                      ),
                      sliver: SliverToBoxAdapter(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (state.hasDraft)
                              DraftBanner(
                                onLoadDraft: () => notifier.acceptDraft(),
                                onNewOrder: () => notifier.declineDraft(),
                              ),
                            if (state.hasDraft)
                              const SizedBox(height: AppSpacing.lg),
                            // 거래처 선택 (월매출과 동일한 거래처 선택 바텀시트 재사용)
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text.rich(
                                  TextSpan(
                                    text: '거래처 ',
                                    style: TextStyle(
                                      color: AppColors.textPrimary,
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
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
                                const SizedBox(height: AppSpacing.sm),
                                DecoratedBox(
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(
                                      AppSpacing.radiusMd,
                                    ),
                                    border: Border.all(
                                      color: AppColors.border,
                                    ),
                                  ),
                                  child: AccountSelectorField(
                                    selectedName: state.selectedClientName,
                                    scope: MyAccountScope.order,
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: AppSpacing.md,
                                      vertical: AppSpacing.md,
                                    ),
                                    onSelected: (account) =>
                                        notifier.selectClient(
                                      account.accountId,
                                      account.accountName,
                                      account.accountCode,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: AppSpacing.lg),
                            CreditBalanceDisplay(
                              creditBalance: state.creditBalance,
                              isLoading: state.isLoanInquiryLoading,
                              isFailed: state.isLoanInquiryFailed,
                              onRetry: notifier.retryLoanInquiry,
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
                          ],
                        ),
                      ),
                    ),
                    // 제품 목록(툴바 고정 포함)은 자체 sliver 구성으로 그린다.
                    ProductListSection(
                      key: _productListKey,
                      items: state.items,
                      validationErrors: state.validationErrors,
                      allItemsSelected: state.allItemsSelected,
                      highlightedProductCode: _highlightedProductCode,
                      onToggleSelection: notifier.toggleProductSelection,
                      onToggleSelectAll: notifier.toggleSelectAllProducts,
                      onAddProduct: () => _handleAddProduct(notifier),
                      onBarcodeScan: () => _handleBarcodeScan(notifier),
                      onRemoveSelected: notifier.removeSelectedProducts,
                      onQuantityChanged: notifier.updateProductQuantity,
                    ),
                    const SliverToBoxAdapter(
                      child: SizedBox(height: AppSpacing.xxxl),
                    ),
                  ],
                ),
              ),
        // 레거시 write.jsp: 총 주문금액 + 삭제/임시저장/승인요청 하단 고정 바.
        bottomNavigationBar: state.isLoading
            ? null
            : Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const InfoNoticeBanner(
                    message: '승인처리에 최대 5분이 걸릴 수 있습니다.',
                  ),
                  TotalAmountDisplay(
                    totalAmount: state.totalAmount,
                    creditBalance: state.creditBalance,
                  ),
                  OrderFormActionButtons(
                    onDelete: () => DraftDeleteDialog.show(
                      context,
                      onConfirm: () => notifier.deleteDraft(),
                    ),
                    onSaveDraft: () => notifier.saveDraft(),
                    onSubmit: () => notifier.validateAndSubmitOrder(),
                    isSubmitting: state.isSubmitting,
                    requiredFieldsFilled: state.isReadyForApproval,
                    loanExceeded: state.isLoanExceeded,
                    lineLimitExceeded: state.isLineLimitExceeded,
                    pastDeadline: state.isPastDeadline,
                    zeroQuantityLineCount: state.zeroQuantityLineCount,
                    onDisabledTap: () =>
                        _handleDisabledSubmitTap(state, notifier),
                  ),
                ],
              ),
      ),
    );
  }
}
