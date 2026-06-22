import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/image_picker_helper.dart';
import '../../../domain/entities/product.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../../providers/inspection_register_provider.dart';
import '../../providers/inspection_register_state.dart';
import '../../providers/pos_sales_provider.dart';
import '../../screens/barcode_scanner_screen.dart';
import '../../widgets/account/account_selector_sheet.dart';
import '../../widgets/inspection/inspection_common_form.dart';
import '../../widgets/inspection/inspection_competitor_form.dart';
import '../../widgets/inspection/inspection_own_form.dart';
import '../../widgets/inspection/inspection_photo_picker.dart';

/// 현장 점검 등록 페이지
///
/// 기능:
/// - 공통 필드 입력 (테마, 분류, 거래처, 점검일, 현장 유형)
/// - 분류별 활동 정보 입력 (자사/경쟁사)
/// - 사진 선택 (최대 2장)
/// - 임시 저장
/// - 등록 전송
class InspectionRegisterPage extends ConsumerStatefulWidget {
  const InspectionRegisterPage({super.key});

  @override
  ConsumerState<InspectionRegisterPage> createState() =>
      _InspectionRegisterPageState();
}

class _InspectionRegisterPageState
    extends ConsumerState<InspectionRegisterPage> {
  @override
  void initState() {
    super.initState();
    // 페이지 로드 시 초기화
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialize();
    });
  }

  /// 초기화(테마/현장유형 로드) 후, 임시저장이 있으면 "이어서 작성?" 을 묻는다.
  Future<void> _initialize() async {
    final draft = await ref
        .read(inspectionRegisterProvider.notifier)
        .initialize();
    if (!mounted || draft == null) return;

    final resume = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('임시저장 불러오기'),
        content: const Text('이전에 작성 중인 내용이 있습니다.\n이어서 작성하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('새로 작성'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('이어서 작성'),
          ),
        ],
      ),
    );

    if (resume == true && mounted) {
      ref.read(inspectionRegisterProvider.notifier).applyDraft(draft);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(inspectionRegisterProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('현장 점검 등록'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      // 숫자 키패드는 iOS 에 '완료' 버튼이 없어 빈 영역 탭 / 스크롤로 키보드를 닫는다.
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => FocusScope.of(context).unfocus(),
              child: SingleChildScrollView(
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                child: Column(
                  children: [
                    // 공통 필드 폼
                    InspectionCommonForm(
                      selectedTheme: state.selectedTheme,
                      category: state.category,
                      selectedAccountName: state.selectedAccountName,
                      inspectionDate:
                          state.form?.inspectionDate ?? DateTime.now(),
                      selectedFieldType: state.selectedFieldType,
                      onThemeTap: () => _showThemeSelector(context),
                      onCategoryChanged: (category) {
                        ref
                            .read(inspectionRegisterProvider.notifier)
                            .changeCategory(category);
                      },
                      onAccountTap: () => _showAccountSelector(context),
                      onDateChanged: (date) {
                        ref
                            .read(inspectionRegisterProvider.notifier)
                            .updateInspectionDate(date);
                      },
                      onFieldTypeTap: () => _showFieldTypeSelector(context),
                    ),

                    const Divider(height: 32, thickness: 8),

                    // 활동 정보 폼 (분류에 따라 변경)
                    if (state.isOwn)
                      InspectionOwnForm(
                        selectedProductName: state.selectedProductName,
                        description: state.form?.description,
                        onDescriptionChanged: (desc) {
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateDescription(desc);
                        },
                        onBarcodeScan: () => _handleBarcodeScan(),
                        onProductSelect: () => _showProductSelector(context),
                      )
                    else
                      InspectionCompetitorForm(
                        competitorName: state.form?.competitorName,
                        competitorActivity: state.form?.competitorActivity,
                        competitorTasting: state.form?.competitorTasting,
                        competitorProductName:
                            state.form?.competitorProductName,
                        competitorProductPrice:
                            state.form?.competitorProductPrice,
                        competitorSalesQuantity:
                            state.form?.competitorSalesQuantity,
                        onCompetitorNameChanged: (name) {
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorName(name);
                        },
                        onCompetitorActivityChanged: (activity) {
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorActivity(activity);
                        },
                        onCompetitorTastingChanged: (tasting) {
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorTasting(tasting);
                        },
                        onCompetitorProductNameChanged: (name) {
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorProductName(name);
                        },
                        onCompetitorProductPriceChanged: (price) {
                          final priceInt = int.tryParse(price) ?? 0;
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorProductPrice(priceInt);
                        },
                        onCompetitorSalesQuantityChanged: (quantity) {
                          final quantityInt = int.tryParse(quantity) ?? 0;
                          ref
                              .read(inspectionRegisterProvider.notifier)
                              .updateCompetitorSalesQuantity(quantityInt);
                        },
                      ),

                    // 사진 선택
                    InspectionPhotoPicker(
                      photos: state.form?.photos ?? [],
                      onAddPhoto: () => _handleAddPhoto(),
                      onRemovePhoto: (index) {
                        ref
                            .read(inspectionRegisterProvider.notifier)
                            .removePhoto(index);
                      },
                    ),

                    const SizedBox(height: 80), // 하단 버튼 공간
                  ],
                ),
              ),
            ),
      bottomNavigationBar: _buildBottomButtons(context, state),
    );
  }

  /// 하단 버튼 (임시저장 + 전송) — 레거시 정합: 풀폭, 어두운 슬레이트 + 순노랑
  Widget _buildBottomButtons(
    BuildContext context,
    InspectionRegisterState state,
  ) {
    final disabled = state.isLoading;
    return SafeArea(
      top: false,
      child: SizedBox(
        height: 56,
        child: Row(
          children: [
            // 임시저장 버튼 (어두운 슬레이트)
            Expanded(
              child: _BottomBarButton(
                label: '임시저장',
                backgroundColor: const Color(0xFF3F4859),
                foregroundColor: Colors.white,
                onPressed: disabled ? null : _handleDraftSave,
              ),
            ),
            // 전송 버튼 (오뚜기 노랑)
            Expanded(
              child: _BottomBarButton(
                label: '전송',
                backgroundColor: AppColors.otokiYellow,
                foregroundColor: Colors.black,
                onPressed: disabled ? null : _handleSubmit,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 테마 선택 다이얼로그
  void _showThemeSelector(BuildContext context) {
    final themes = ref.read(inspectionRegisterProvider).themes;
    if (themes.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('진행 중인 테마가 없습니다')));
      return;
    }

    showModalBottomSheet(
      context: context,
      builder: (context) => ListView.builder(
        itemCount: themes.length,
        itemBuilder: (context, index) {
          final theme = themes[index];
          return ListTile(
            title: Text(theme.name),
            onTap: () {
              ref.read(inspectionRegisterProvider.notifier).selectTheme(theme);
              Navigator.pop(context);
            },
          );
        },
      ),
    );
  }

  /// 거래처 선택 — 공용 [AccountSelectorSheet] 바텀시트 재사용 (현장 scope=field, 필수 선택).
  Future<void> _showAccountSelector(BuildContext context) async {
    final account = await AccountSelectorSheet.show(
      context,
      scope: MyAccountScope.field,
    );
    if (account == null || !mounted) return;
    ref
        .read(inspectionRegisterProvider.notifier)
        .selectAccount(account.accountId, account.accountName);
  }

  /// 현장 유형 선택 다이얼로그
  void _showFieldTypeSelector(BuildContext context) {
    final fieldTypes = ref.read(inspectionRegisterProvider).fieldTypes;
    if (fieldTypes.isEmpty) return;

    showModalBottomSheet(
      context: context,
      builder: (context) => ListView.builder(
        itemCount: fieldTypes.length,
        itemBuilder: (context, index) {
          final fieldType = fieldTypes[index];
          return ListTile(
            title: Text(fieldType.name),
            onTap: () {
              ref
                  .read(inspectionRegisterProvider.notifier)
                  .selectFieldType(fieldType);
              Navigator.pop(context);
            },
          );
        },
      ),
    );
  }

  /// 제품 선택 — 제품검색(선택 모드)으로 이동 후 고른 제품을 폼에 반영
  Future<void> _showProductSelector(BuildContext context) async {
    final selected = await AppRouter.navigateTo<Product>(
      context,
      AppRouter.productSearch,
      arguments: true,
    );
    if (selected == null || !mounted) return;
    ref
        .read(inspectionRegisterProvider.notifier)
        .selectProduct(selected.productCode, selected.productName);
  }

  /// 바코드 스캔 — 카메라로 제품 바코드를 스캔해 자사 제품을 선택한다.
  ///
  /// 스캐너에서 받은 바코드로 POS 제품 검색(`findByBarcode`)을 호출해 제품을 찾고,
  /// 찾으면 자사 활동 폼의 선택 제품으로 반영한다.
  Future<void> _handleBarcodeScan() async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || !mounted) return;

    try {
      final product = await ref
          .read(posProductUseCaseProvider)
          .findByBarcode(barcode);
      if (!mounted) return;
      if (product == null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('해당 제품이 없습니다')));
        return;
      }
      ref
          .read(inspectionRegisterProvider.notifier)
          .selectProduct(product.productCode, product.productName);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('${product.productName} 선택됨')));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('제품 조회에 실패했습니다')));
    }
  }

  /// 사진 추가 — 카메라/갤러리에서 선택한 이미지를 첨부
  Future<void> _handleAddPhoto() async {
    final file = await pickImageWithSourceSheet(context);
    if (file == null || !mounted) return;
    ref.read(inspectionRegisterProvider.notifier).addPhoto(file);
  }

  /// 임시 저장 — 검증 없이 현재 폼 상태를 서버에 저장(레거시 tempFieldChkProc 정합).
  Future<void> _handleDraftSave() async {
    final success = await ref
        .read(inspectionRegisterProvider.notifier)
        .saveDraft();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(success ? '임시저장되었습니다' : '임시저장에 실패했습니다')),
    );
  }

  /// 등록 전송
  Future<void> _handleSubmit() async {
    final success = await ref
        .read(inspectionRegisterProvider.notifier)
        .submit();

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('점검이 등록되었습니다')));
      Navigator.of(context).pop();
    } else {
      final errorMessage =
          ref.read(inspectionRegisterProvider).errorMessage ?? '등록 실패';
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(errorMessage)));
      ref.read(inspectionRegisterProvider.notifier).clearError();
    }
  }
}

/// 하단 풀폭 버튼 (레거시 정합 — 모서리 각짐, 간격 없음)
class _BottomBarButton extends StatelessWidget {
  final String label;
  final Color backgroundColor;
  final Color foregroundColor;
  final VoidCallback? onPressed;

  const _BottomBarButton({
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: backgroundColor,
      child: InkWell(
        onTap: onPressed,
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: foregroundColor,
            ),
          ),
        ),
      ),
    );
  }
}
