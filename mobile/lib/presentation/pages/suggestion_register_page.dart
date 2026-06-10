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
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text(
          '제안하기',
          style: TextStyle(fontWeight: FontWeight.w700, color: Colors.black),
        ),
        centerTitle: true,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
        scrolledUnderElevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 20),
          onPressed: () => Navigator.of(context).pop(),
        ),
        bottom: const PreferredSize(
          preferredSize: Size.fromHeight(1),
          child: Divider(height: 1, thickness: 1, color: Color(0xFFE5E5E5)),
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
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SuggestionCategorySelector(
                      selectedCategory: state.form.category,
                      onCategoryChanged: notifier.changeCategory,
                    ),
                    ..._buildCategoryFields(state, notifier),
                    const SizedBox(height: 24),
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
        _TitleField(
          controller: _titleController,
          onChanged: notifier.updateTitle,
        ),
        SuggestionClaimTypeField(
          value: state.form.claimType,
          onChanged: notifier.updateClaimType,
        ),
        SuggestionClaimDateField(
          value: state.form.claimDate,
          onChanged: notifier.updateClaimDate,
        ),
        _ContentField(
          controller: _contentController,
          label: '클레임 상세 내용',
          hint: '내용 입력',
          onChanged: notifier.updateContent,
        ),
        SuggestionCarNumberField(
          value: state.form.carNumber,
          onChanged: notifier.updateCarNumber,
        ),
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
      _TitleField(
        controller: _titleController,
        onChanged: notifier.updateTitle,
      ),
      _ContentField(
        controller: _contentController,
        label: '제안 내용',
        hint: '내용 입력',
        onChanged: notifier.updateContent,
      ),
      SuggestionPhotoField(
        photos: state.form.photos,
        onAddPhoto: _handleAddPhoto,
        onRemovePhoto: notifier.removePhoto,
      ),
    ];
  }

  /// 하단 버튼 — 레거시 정합 (임시저장 | 전송, 풀폭 분할·여백 없음)
  Widget _buildBottomBar(
    BuildContext context,
    SuggestionRegisterState state,
    SuggestionRegisterNotifier notifier,
  ) {
    return SafeArea(
      top: false,
      child: SizedBox(
        height: 56,
        child: Row(
          children: [
            // 임시저장 — 신규 모바일 백엔드 미지원(별 스펙), 안내만 노출
            Expanded(
              child: _BottomAction(
                label: '임시저장',
                background: const Color(0xFF3C3C3C),
                foreground: Colors.white,
                onPressed: state.isSubmitting ? null : _handleTempSave,
              ),
            ),
            // 전송
            Expanded(
              child: _BottomAction(
                label: '전송',
                background: const Color(0xFFFFD400),
                foreground: Colors.black,
                loading: state.isSubmitting,
                onPressed:
                    state.isSubmitting ? null : () => _handleSubmit(notifier),
              ),
            ),
          ],
        ),
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
    return SuggestionFieldRow(
      label: '제목',
      required: true,
      child: SuggestionBorderlessField(
        controller: controller,
        hint: '제목 입력',
        maxLength: 250,
        onChanged: onChanged,
      ),
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
    return SuggestionFieldRow(
      label: label,
      required: true,
      child: SuggestionBorderlessField(
        controller: controller,
        hint: hint,
        maxLength: 2000,
        maxLines: 5,
        onChanged: onChanged,
      ),
    );
  }
}

/// 하단 분할 버튼(임시저장/전송) — 풀폭, 모서리·여백 없음 (레거시 정합)
class _BottomAction extends StatelessWidget {
  const _BottomAction({
    required this.label,
    required this.background,
    required this.foreground,
    required this.onPressed,
    this.loading = false,
  });

  final String label;
  final Color background;
  final Color foreground;
  final VoidCallback? onPressed;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: onPressed == null ? background.withValues(alpha: 0.6) : background,
      child: InkWell(
        onTap: onPressed,
        child: Center(
          child: loading
              ? SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: foreground,
                  ),
                )
              : Text(
                  label,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: foreground,
                  ),
                ),
        ),
      ),
    );
  }
}
