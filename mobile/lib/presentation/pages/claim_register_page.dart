import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/claim_category.dart';
import '../../domain/entities/claim_code.dart';
import '../providers/claim_register_provider.dart';
import '../widgets/claim/claim_category_selector.dart';
import '../widgets/claim/claim_date_field.dart';
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
  const ClaimRegisterPage({super.key});

  @override
  ConsumerState<ClaimRegisterPage> createState() =>
      _ClaimRegisterPageState();
}

class _ClaimRegisterPageState extends ConsumerState<ClaimRegisterPage> {
  @override
  void initState() {
    super.initState();
    // 페이지 로드 시 폼 데이터 로드
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(claimRegisterProvider.notifier).loadFormData();
    });
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
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 거래처 선택
                  _StoreField(
                    storeName: state.form?.storeName,
                    onTap: () => _showStoreSelector(context),
                  ),
                  const SizedBox(height: 16),

                  // 제품 선택
                  ClaimProductField(
                    productName: state.form?.productName,
                    productCode: state.form?.productCode,
                    onBarcodePressed: () => _handleBarcodeScan(),
                    onProductSelectPressed: () => _showProductSelector(context),
                  ),
                  const SizedBox(height: 16),

                  // 기한 입력
                  ClaimDateField(
                    dateType: state.form?.dateType ?? ClaimDateType.expiryDate,
                    date: state.form?.date ?? DateTime.now(),
                    onDateTypeChanged: notifier.selectDateType,
                    onDateSelected: notifier.selectDate,
                  ),
                  const SizedBox(height: 16),

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
                  const SizedBox(height: 16),

                  // 불량 내역
                  _DefectDescriptionField(
                    description: state.form?.defectDescription,
                    onChanged: notifier.updateDefectDescription,
                  ),
                  const SizedBox(height: 16),

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
                  const SizedBox(height: 16),

                  // 불량 사진
                  ClaimPhotoField(
                    label: '불량 사진',
                    photo: _getValidPhoto(state.form?.defectPhoto),
                    onPhotoSelected: notifier.attachDefectPhoto,
                    onPhotoRemoved: notifier.removeDefectPhoto,
                    isRequired: true,
                  ),
                  const SizedBox(height: 16),

                  // 일부인 사진
                  ClaimPhotoField(
                    label: '일부인 사진',
                    photo: _getValidPhoto(state.form?.labelPhoto),
                    onPhotoSelected: notifier.attachLabelPhoto,
                    onPhotoRemoved: notifier.removeLabelPhoto,
                    isRequired: true,
                  ),
                  const SizedBox(height: 16),

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
                  const SizedBox(height: 16),

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

                  const SizedBox(height: 80), // 하단 버튼 공간
                ],
              ),
            ),
      bottomNavigationBar: _buildBottomButtons(context, state),
    );
  }

  /// 하단 버튼 (임시저장 + 전송)
  Widget _buildBottomButtons(
    BuildContext context,
    state,
  ) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          // 임시저장 버튼
          Expanded(
            child: ElevatedButton(
              onPressed: state.loading ? null : () => _handleDraftSave(),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.grey,
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: const Text(
                '임시저장',
                style: TextStyle(fontSize: 16),
              ),
            ),
          ),
          const SizedBox(width: 16),
          // 전송 버튼
          Expanded(
            flex: 2,
            child: ElevatedButton(
              onPressed: state.loading ? null : () => _handleSubmit(),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.yellow[700],
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: const Text(
                '전송',
                style: TextStyle(fontSize: 16, color: Colors.black),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 거래처 선택 다이얼로그
  void _showStoreSelector(BuildContext context) {
    // TODO: 실제 거래처 목록 조회 및 선택
    // 임시로 샘플 데이터 사용
    final notifier = ref.read(claimRegisterProvider.notifier);
    notifier.selectStore(1, '샘플 거래처');

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('거래처 선택 기능은 추후 구현 예정입니다')),
    );
  }

  /// 제품 선택 다이얼로그
  void _showProductSelector(BuildContext context) {
    // TODO: 제품 검색 화면으로 이동
    // 임시로 샘플 제품 선택
    final notifier = ref.read(claimRegisterProvider.notifier);
    notifier.selectProduct('P001', '진라면');

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('제품 선택 기능은 추후 구현 예정입니다')),
    );
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

  /// 임시 저장
  void _handleDraftSave() {
    // TODO: 로컬 저장 구현
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('임시 저장되었습니다')),
    );
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
    if (categoryId == null || categoryId <= 0) return null;

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
    if (subcategoryId == null || subcategoryId <= 0) return null;

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

/// 거래처 선택 필드
class _StoreField extends StatelessWidget {
  const _StoreField({
    required this.storeName,
    required this.onTap,
  });

  final String? storeName;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '거래처 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(4),
            side: BorderSide(color: Colors.grey.shade300),
          ),
          title: Text(
            storeName ?? '거래처 선택',
            style: TextStyle(
              fontSize: 14,
              color: storeName == null ? Colors.grey.shade600 : Colors.black87,
            ),
          ),
          trailing: const Icon(Icons.arrow_forward_ios, size: 16),
          onTap: onTap,
        ),
      ],
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '불량 내역 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: TextEditingController(text: description ?? ''),
          maxLines: 3,
          decoration: const InputDecoration(
            hintText: '불량 내역을 입력하세요',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.all(12),
          ),
          onChanged: onChanged,
        ),
      ],
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '불량 수량 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: TextEditingController(
            text: quantity != null && quantity! > 0 ? quantity.toString() : '',
          ),
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(
            hintText: '수량 입력',
            suffixText: '개',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }
}
