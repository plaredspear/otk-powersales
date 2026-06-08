import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../../domain/entities/claim_category.dart';
import '../../domain/entities/claim_code.dart';
import '../../domain/entities/product.dart';
import '../providers/claim_register_provider.dart';
import '../widgets/claim/claim_category_selector.dart';
import '../widgets/claim/claim_date_field.dart';
import '../widgets/claim/claim_form_row.dart';
import '../widgets/claim/claim_photo_field.dart';
import '../widgets/claim/claim_product_field.dart';
import '../widgets/claim/claim_purchase_section.dart';
import '../widgets/claim/claim_request_type_field.dart';

/// 클레임 등록 페이지
///
/// 기능:
/// - 거래처 선택
/// - 제품 선택 (바코드 스캔 또는 직접 선택)
/// - 기한 입력 (유통기한/제조일자)
/// - 클레임 종류 선택 (종류1 + 종류2)
/// - 불량 내역 입력
/// - 불량 수량 입력
/// - 불량 사진 첨부
/// - 일부인 사진 첨부
/// - 구매 정보 입력 (선택)
/// - 요청사항 선택 (선택)
/// - 임시저장 / 전송
class ClaimRegisterPage extends ConsumerStatefulWidget {
  /// 진입 시 미리 선택해 둘 제품 코드 (제품검색 결과에서 진입한 경우).
  final String? presetProductCode;

  /// 진입 시 미리 선택해 둘 제품명.
  final String? presetProductName;

  const ClaimRegisterPage({
    super.key,
    this.presetProductCode,
    this.presetProductName,
  });

  @override
  ConsumerState<ClaimRegisterPage> createState() =>
      _ClaimRegisterPageState();
}

class _ClaimRegisterPageState extends ConsumerState<ClaimRegisterPage> {
  @override
  void initState() {
    super.initState();
    // 페이지 로드 시 폼 데이터 로드 + 임시저장 이어쓰기 확인
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialize();
    });
  }

  /// 진입 폼 로드(메타데이터 + 임시저장) → (제품 지정 진입이면 선택) / (아니면 임시저장 이어쓰기 확인)
  Future<void> _initialize() async {
    final notifier = ref.read(claimRegisterProvider.notifier);
    // 진입 1콜: 메타데이터는 state 에 채워지고, 이어쓰기용 임시저장(있으면)이 반환된다.
    final draft = await notifier.loadForm();
    if (!mounted) return;

    // 제품검색 결과에서 전달된 제품이 있으면 미리 선택 (임시저장 복원은 생략)
    final code = widget.presetProductCode;
    final name = widget.presetProductName;
    if (code != null && name != null) {
      notifier.selectProduct(code, name);
      return;
    }

    // 임시저장이 있으면 이어쓰기 여부 확인
    if (draft == null || !mounted) return;

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
      notifier.applyDraft(draft);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(claimRegisterProvider);
    final notifier = ref.read(claimRegisterProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('클레임 등록'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: state.loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 거래처 선택
                  _AccountField(
                    accountName: state.form?.accountName,
                    onTap: () => _showAccountSelector(context),
                  ),

                  // 제품 선택
                  ClaimProductField(
                    productName: state.form?.productName,
                    productCode: state.form?.productCode,
                    onBarcodePressed: () => _handleBarcodeScan(),
                    onProductSelectPressed: () => _showProductSelector(context),
                  ),

                  // 기한 입력
                  ClaimDateField(
                    dateType: state.form?.dateType ?? ClaimDateType.expiryDate,
                    date: state.form?.date ?? DateTime.now(),
                    onDateTypeChanged: notifier.selectDateType,
                    onDateSelected: notifier.selectDate,
                  ),

                  // 클레임 종류 선택
                  ClaimCategorySelector(
                    categories: state.formData?.categories ?? [],
                    selectedCategory: _findSelectedCategory(state),
                    selectedSubcategory: _findSelectedSubcategory(state),
                    onCategorySelected: (category) {
                      if (category != null) {
                        notifier.selectCategory(category.id, category.name);
                      }
                    },
                    onSubcategorySelected: (subcategory) {
                      if (subcategory != null) {
                        notifier.selectSubcategory(
                          subcategory.id,
                          subcategory.name,
                        );
                      }
                    },
                  ),

                  // 불량 내역
                  _DefectDescriptionField(
                    description: state.form?.defectDescription,
                    onChanged: notifier.updateDefectDescription,
                  ),

                  // 불량 수량
                  _DefectQuantityField(
                    quantity: state.form?.defectQuantity,
                    onChanged: (value) {
                      final quantity = int.tryParse(value);
                      if (quantity != null) {
                        notifier.updateDefectQuantity(quantity);
                      }
                    },
                  ),

                  // 불량 사진
                  ClaimPhotoField(
                    label: '불량 사진 (최대 1장)',
                    photo: _getValidPhoto(state.form?.defectPhoto),
                    onPhotoSelected: notifier.attachDefectPhoto,
                    onPhotoRemoved: notifier.removeDefectPhoto,
                    isRequired: true,
                  ),

                  // 일부인 사진
                  ClaimPhotoField(
                    label: '일부인 (최대 1장)',
                    photo: _getValidPhoto(state.form?.labelPhoto),
                    onPhotoSelected: notifier.attachLabelPhoto,
                    onPhotoRemoved: notifier.removeLabelPhoto,
                    isRequired: true,
                  ),

                  // 구매 정보 섹션
                  ClaimPurchaseSection(
                    purchaseAmount: state.form?.purchaseAmount,
                    purchaseMethods: state.formData?.purchaseMethods ?? [],
                    selectedPurchaseMethod: _findSelectedPurchaseMethod(state),
                    receiptPhoto: state.form?.receiptPhoto,
                    onPurchaseAmountChanged: notifier.updatePurchaseAmount,
                    onPurchaseMethodSelected: (method) {
                      if (method != null) {
                        notifier.selectPurchaseMethod(method.code, method.name);
                      } else {
                        notifier.selectPurchaseMethod(null, null);
                      }
                    },
                    onReceiptPhotoSelected: notifier.attachReceiptPhoto,
                    onReceiptPhotoRemoved: notifier.removeReceiptPhoto,
                  ),

                  // 요청사항
                  ClaimRequestTypeField(
                    selectedRequestType: _findSelectedRequestType(state),
                    requestTypes: state.formData?.requestTypes ?? [],
                    onRequestTypeSelected: (requestType) {
                      if (requestType != null) {
                        notifier.selectRequestType(
                          requestType.code,
                          requestType.name,
                        );
                      } else {
                        notifier.selectRequestType(null, null);
                      }
                    },
                  ),

                  const SizedBox(height: 24),
                ],
              ),
            ),
      bottomNavigationBar: _buildBottomButtons(context, state),
    );
  }

  /// 하단 버튼 (임시저장 + 전송) — 레거시처럼 전폭 무여백 2분할
  Widget _buildBottomButtons(
    BuildContext context,
    state,
  ) {
    return SafeArea(
      top: false,
      child: SizedBox(
        height: 60,
        child: Row(
          children: [
            // 임시저장 버튼
            Expanded(
              child: _BottomBarButton(
                label: '임시저장',
                backgroundColor: const Color(0xFF38434F),
                textColor: Colors.white,
                onPressed: state.loading ? null : () => _handleDraftSave(),
              ),
            ),
            // 전송 버튼
            Expanded(
              child: _BottomBarButton(
                label: '전송',
                backgroundColor: const Color(0xFFFFE000),
                textColor: Colors.black,
                onPressed: state.loading ? null : () => _handleSubmit(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 거래처 선택 — 내 거래처 선택 바텀시트에서 고른 거래처를 폼에 반영
  Future<void> _showAccountSelector(BuildContext context) async {
    final account = await AccountSelectorSheet.show(context);
    if (account == null || !mounted) return;
    ref
        .read(claimRegisterProvider.notifier)
        .selectAccount(account.accountId, account.accountName);
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
        .read(claimRegisterProvider.notifier)
        .selectProduct(selected.productCode, selected.productName);
  }

  /// 바코드 스캔
  void _handleBarcodeScan() {
    // TODO: 바코드 스캐너 실행
    // 임시로 샘플 제품 선택
    final notifier = ref.read(claimRegisterProvider.notifier);
    notifier.selectProduct('P002', '진라면 매운맛');

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('바코드 스캔 기능은 추후 구현 예정입니다')),
    );
  }

  /// 임시 저장 — 현재 폼 상태를 서버에 임시저장(upsert)
  Future<void> _handleDraftSave() async {
    final success = await ref.read(claimRegisterProvider.notifier).saveDraft();

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('임시저장되었습니다')),
      );
    } else {
      final errorMessage =
          ref.read(claimRegisterProvider).error ?? '임시저장 실패';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(errorMessage)),
      );
    }
  }

  /// 등록 전송
  Future<void> _handleSubmit() async {
    final success = await ref.read(claimRegisterProvider.notifier).registerClaim();

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('클레임이 등록되었습니다')),
      );
      Navigator.of(context).pop();
    } else {
      final errorMessage = ref.read(claimRegisterProvider).error ?? '등록 실패';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(errorMessage)),
      );
    }
  }

  /// 선택된 카테고리 찾기
  ClaimCategory? _findSelectedCategory(state) {
    final categoryId = state.form?.categoryId;
    if (categoryId == null || categoryId.isEmpty) return null;

    final categories = state.formData?.categories ?? [];
    try {
      return categories.firstWhere((c) => c.id == categoryId);
    } catch (_) {
      return null;
    }
  }

  /// 선택된 서브카테고리 찾기
  ClaimSubcategory? _findSelectedSubcategory(state) {
    final subcategoryId = state.form?.subcategoryId;
    if (subcategoryId == null || subcategoryId.isEmpty) return null;

    final category = _findSelectedCategory(state);
    if (category == null) return null;

    try {
      return category.subcategories.firstWhere((s) => s.id == subcategoryId);
    } catch (_) {
      return null;
    }
  }

  /// 선택된 구매 방법 찾기
  PurchaseMethod? _findSelectedPurchaseMethod(state) {
    final code = state.form?.purchaseMethodCode;
    if (code == null || code.isEmpty) return null;

    final methods = state.formData?.purchaseMethods ?? [];
    try {
      return methods.firstWhere((m) => m.code == code);
    } catch (_) {
      return null;
    }
  }

  /// 선택된 요청사항 찾기
  ClaimRequestType? _findSelectedRequestType(state) {
    final code = state.form?.requestTypeCode;
    if (code == null || code.isEmpty) return null;

    final types = state.formData?.requestTypes ?? [];
    try {
      return types.firstWhere((t) => t.code == code);
    } catch (_) {
      return null;
    }
  }

  /// 유효한 사진 파일 반환 (빈 경로면 null)
  File? _getValidPhoto(File? photo) {
    if (photo == null) return null;
    if (photo.path.isEmpty) return null;
    return photo;
  }
}

/// 하단 바 버튼 (전폭 무여백)
class _BottomBarButton extends StatelessWidget {
  const _BottomBarButton({
    required this.label,
    required this.backgroundColor,
    required this.textColor,
    required this.onPressed,
  });

  final String label;
  final Color backgroundColor;
  final Color textColor;
  final VoidCallback? onPressed;

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
              fontSize: 17,
              fontWeight: FontWeight.w600,
              color: textColor,
            ),
          ),
        ),
      ),
    );
  }
}

/// 거래처 선택 필드
class _AccountField extends StatelessWidget {
  const _AccountField({
    required this.accountName,
    required this.onTap,
  });

  final String? accountName;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '거래처',
      isRequired: true,
      onTap: onTap,
      trailing: const ClaimRowChevron(),
      below: ClaimValueText(
        value: accountName,
        placeholder: '거래처 선택',
      ),
    );
  }
}

/// 불량 내역 입력 필드
class _DefectDescriptionField extends StatelessWidget {
  const _DefectDescriptionField({
    required this.description,
    required this.onChanged,
  });

  final String? description;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '불량 내역',
      isRequired: true,
      below: TextField(
        controller: TextEditingController(text: description ?? ''),
        maxLines: null,
        style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
        decoration: const InputDecoration(
          isCollapsed: true,
          hintText: '내용 입력',
          hintStyle: TextStyle(fontSize: 14, color: ClaimFormColors.placeholder),
          border: InputBorder.none,
        ),
        onChanged: onChanged,
      ),
    );
  }
}

/// 불량 수량 입력 필드
class _DefectQuantityField extends StatelessWidget {
  const _DefectQuantityField({
    required this.quantity,
    required this.onChanged,
  });

  final int? quantity;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '불량 수량',
      isRequired: true,
      trailing: const Text(
        '개',
        style: TextStyle(fontSize: 14, color: ClaimFormColors.unit),
      ),
      below: TextField(
        controller: TextEditingController(
          text: quantity != null && quantity! > 0 ? quantity.toString() : '',
        ),
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
        decoration: const InputDecoration(
          isCollapsed: true,
          hintText: '숫자 입력',
          hintStyle: TextStyle(fontSize: 14, color: ClaimFormColors.placeholder),
          border: InputBorder.none,
        ),
        onChanged: onChanged,
      ),
    );
  }
}
