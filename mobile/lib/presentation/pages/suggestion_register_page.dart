import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../providers/pos_sales_provider.dart';
import '../providers/suggestion_register_provider.dart';
import '../providers/suggestion_register_state.dart';
import '../screens/barcode_scanner_screen.dart';
import '../widgets/suggestion/suggestion_category_selector.dart';
import '../widgets/suggestion/suggestion_logistics_claim_fields.dart';
import '../widgets/suggestion/suggestion_photo_field.dart';
import '../widgets/suggestion/suggestion_product_field.dart';

const int _maxPhotoSizeBytes = 20 * 1024 * 1024;

/// 제안하기 등록 페이지 (레거시 suggestWrite.jsp 정합)
///
/// 기능:
/// - 분류 선택 (물류 클레임 / 신제품 제안 / 기존제품 상품가치향상, 기본 물류 클레임)
/// - 카테고리 분기 입력
///   - 물류 클레임: 거래처 → 대표 제품 → 제목 → 클레임 항목 → 발생일자 →
///     클레임 상세 내용 → 차량 번호 → 사진(필수)
///   - 신제품/기존제품: 제품 → 제목 → 제안 내용 → 사진
/// - 사진 첨부 (최대 2장, 20MB 가드)
/// - 임시저장(별 스펙 — 안내) / 전송
class SuggestionRegisterPage extends ConsumerStatefulWidget {
  const SuggestionRegisterPage({super.key});

  @override
  ConsumerState<SuggestionRegisterPage> createState() =>
      _SuggestionRegisterPageState();
}

class _SuggestionRegisterPageState
    extends ConsumerState<SuggestionRegisterPage> {
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  final ImagePicker _imagePicker = ImagePicker();

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(suggestionRegisterProvider);
    final notifier = ref.read(suggestionRegisterProvider.notifier);

    ref.listen<SuggestionRegisterState>(suggestionRegisterProvider, (
      previous,
      next,
    ) {
      if (next.errorMessage != null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.errorMessage!)));
      }
      if (next.successMessage != null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(next.successMessage!)));
        Navigator.of(context).pop();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('제안하기'),
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
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SuggestionCategorySelector(
                      selectedCategory: state.form.category,
                      onCategoryChanged: notifier.changeCategory,
                    ),
                    const SizedBox(height: 16),
                    ..._buildCategoryFields(state, notifier),
                    const SizedBox(height: 80),
                  ],
                ),
              ),
            ),
      bottomNavigationBar: _buildBottomBar(context, state, notifier),
    );
  }

  /// 카테고리별 분기 필드 — 레거시 필드 순서 정합
  List<Widget> _buildCategoryFields(
    SuggestionRegisterState state,
    SuggestionRegisterNotifier notifier,
  ) {
    if (state.isLogisticsClaim) {
      return [
        SuggestionAccountField(
          accountName: state.form.accountName,
          onSelect: _showAccountSelector,
        ),
        const SizedBox(height: 16),
        SuggestionProductField(
          enabled: true,
          label: '대표 제품',
          required: true,
          guideText: '제안내용과 관련된 당사 유사제품을 선택해주세요.',
          productName: state.form.productName,
          productCode: state.form.productCode,
          onBarcodePressed: _handleBarcodeScan,
          onSelectPressed: () => _showProductSelector(context),
        ),
        const SizedBox(height: 16),
        _TitleField(
          controller: _titleController,
          onChanged: notifier.updateTitle,
        ),
        const SizedBox(height: 16),
        SuggestionClaimTypeField(
          value: state.form.claimType,
          onChanged: notifier.updateClaimType,
        ),
        const SizedBox(height: 16),
        SuggestionClaimDateField(
          value: state.form.claimDate,
          onChanged: notifier.updateClaimDate,
        ),
        const SizedBox(height: 16),
        _ContentField(
          controller: _contentController,
          label: '클레임 상세 내용',
          hint: '클레임 내용을 상세하게 입력하세요',
          onChanged: notifier.updateContent,
        ),
        const SizedBox(height: 16),
        SuggestionCarNumberField(
          value: state.form.carNumber,
          onChanged: notifier.updateCarNumber,
        ),
        const SizedBox(height: 16),
        SuggestionPhotoField(
          photos: state.form.photos,
          required: true,
          onAddPhoto: _handleAddPhoto,
          onRemovePhoto: notifier.removePhoto,
        ),
      ];
    }

    // 신제품 제안 / 기존제품 상품가치향상
    return [
      SuggestionProductField(
        enabled: state.isExistingProduct,
        required: state.isExistingProduct,
        productName: state.form.productName,
        productCode: state.form.productCode,
        onBarcodePressed: _handleBarcodeScan,
        onSelectPressed: () => _showProductSelector(context),
      ),
      const SizedBox(height: 16),
      _TitleField(
        controller: _titleController,
        onChanged: notifier.updateTitle,
      ),
      const SizedBox(height: 16),
      _ContentField(
        controller: _contentController,
        label: '제안 내용',
        hint: '제안 내용을 상세하게 입력하세요',
        onChanged: notifier.updateContent,
      ),
      const SizedBox(height: 16),
      SuggestionPhotoField(
        photos: state.form.photos,
        onAddPhoto: _handleAddPhoto,
        onRemovePhoto: notifier.removePhoto,
      ),
    ];
  }

  /// 하단 버튼 — 레거시 정합 (임시저장 | 전송)
  Widget _buildBottomBar(
    BuildContext context,
    SuggestionRegisterState state,
    SuggestionRegisterNotifier notifier,
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
          // 임시저장 — 신규 모바일 백엔드 미지원(별 스펙), 안내만 노출
          Expanded(
            child: OutlinedButton(
              onPressed: state.isSubmitting ? null : _handleTempSave,
              style: OutlinedButton.styleFrom(
                foregroundColor: Colors.grey.shade700,
                side: BorderSide(color: Colors.grey.shade400),
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: const Text('임시저장', style: TextStyle(fontSize: 16)),
            ),
          ),
          const SizedBox(width: 12),
          // 전송
          Expanded(
            child: ElevatedButton(
              onPressed: state.isSubmitting
                  ? null
                  : () => _handleSubmit(notifier),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.yellow[700],
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
              child: state.isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      '전송',
                      style: TextStyle(fontSize: 16, color: Colors.black),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  /// 바코드 스캔 — 카메라로 제품 바코드를 스캔해 대표 제품을 선택한다.
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
          .read(suggestionRegisterProvider.notifier)
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

  Future<void> _showProductSelector(BuildContext context) async {
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('제품 선택 화면은 별 스펙에서 구현됩니다')));
    }
  }

  Future<void> _showAccountSelector() async {
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('거래처 선택 화면은 별 스펙에서 구현됩니다')));
    }
  }

  void _handleTempSave() {
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('임시저장은 추후 지원 예정입니다')));
    }
  }

  Future<void> _handleAddPhoto() async {
    final notifier = ref.read(suggestionRegisterProvider.notifier);
    final XFile? picked = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 80,
      maxWidth: 1920,
    );
    if (picked == null) return;

    final file = File(picked.path);
    final size = await file.length();
    if (size > _maxPhotoSizeBytes) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('사진 1장은 최대 20MB 까지 첨부 가능합니다')),
        );
      }
      return;
    }

    notifier.addPhoto(file);
  }

  Future<void> _handleSubmit(SuggestionRegisterNotifier notifier) async {
    // 유효성 선검증 — 통과 시에만 전송 확인 (레거시 confirm 정합)
    final errors = ref.read(suggestionRegisterProvider).form.validate();
    if (errors.isNotEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(errors.first)));
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: const Text('제안 내용을 전송하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('전송'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    await notifier.submit();
  }
}

/// 제목 입력 필드 (max 250 — backend `@Size(max=250)` 정합)
class _TitleField extends StatelessWidget {
  const _TitleField({required this.controller, required this.onChanged});

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SuggestionFieldLabel(text: '제목', required: true),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLength: 250,
          decoration: const InputDecoration(
            hintText: '제목을 입력하세요',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.all(12),
            counterText: '',
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }
}

class _ContentField extends StatelessWidget {
  const _ContentField({
    required this.controller,
    required this.label,
    required this.hint,
    required this.onChanged,
  });

  final TextEditingController controller;
  final String label;
  final String hint;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SuggestionFieldLabel(text: label, required: true),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLines: 5,
          maxLength: 2000,
          decoration: InputDecoration(
            hintText: hint,
            border: const OutlineInputBorder(),
            contentPadding: const EdgeInsets.all(12),
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }
}
