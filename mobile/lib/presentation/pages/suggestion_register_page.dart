import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../domain/entities/suggestion_draft.dart';
import '../../domain/entities/suggestion_form.dart';
import '../providers/pos_sales_provider.dart';
import '../providers/suggestion_register_provider.dart';
import '../providers/suggestion_register_state.dart';
import '../screens/barcode_scanner_screen.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/order_form/add_product_bottom_sheet.dart';
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
  const SuggestionRegisterPage({
    super.key,
    this.entryCategory = SuggestionCategory.logisticsClaim,
  });

  /// 진입 기본 분류.
  /// - 물류 클레임 등록: [SuggestionCategory.logisticsClaim]
  /// - 제안하기(신제품 제안 등): [SuggestionCategory.newProduct]
  final SuggestionCategory entryCategory;

  /// 제안하기(신제품 제안/기존제품 상품가치향상) 진입 여부.
  bool get isProposalEntry =>
      entryCategory != SuggestionCategory.logisticsClaim;

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
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // 전역 provider 라 이전 등록 성공(successMessage) 등 종료 상태가 그대로
      // 남아 있다. 재진입 시 첫 상태 변화(임시저장 조회 등)에 listen 이 묵은
      // 성공 메시지를 감지해 곧바로 pop 되는 문제를 막기 위해 먼저 초기화한다.
      ref
          .read(suggestionRegisterProvider.notifier)
          .reset(category: widget.entryCategory);
      // 진입 후 첫 프레임에서 임시저장 이어쓰기 확인 (레거시 nullChk='N' 흐름 대응)
      _checkDraft();
    });
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  /// 임시저장이 있으면 이어쓰기 여부를 확인한다.
  Future<void> _checkDraft() async {
    final notifier = ref.read(suggestionRegisterProvider.notifier);
    final draft = await notifier.loadDraftIfExists();
    if (draft == null || !mounted) return;

    final resume = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('임시저장 불러오기'),
        content: const Text('이전에 작성 중인 내용이 있습니다.\n이어서 작성하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('새로 작성'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('이어서 작성'),
          ),
        ],
      ),
    );

    if (resume == true && mounted) {
      notifier.applyDraft(draft);
      _syncTextControllers(draft);
    }
  }

  /// 제목/내용은 페이지 보유 plain 컨트롤러를 쓰므로 prefill 시 직접 동기화한다.
  void _syncTextControllers(SuggestionDraft draft) {
    _titleController.text = draft.title ?? '';
    _contentController.text = draft.content ?? '';
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
        title: Text(
          widget.isProposalEntry ? '제안하기' : '물류 클레임 등록',
          style: const TextStyle(
            fontWeight: FontWeight.w700,
            color: Colors.black,
          ),
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
                      categories: widget.isProposalEntry
                          ? const [
                              SuggestionCategory.newProduct,
                              SuggestionCategory.existingProduct,
                            ]
                          : const [SuggestionCategory.logisticsClaim],
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
            // 임시저장 — 현재 폼 상태를 검증 없이 서버에 upsert (이어쓰기 지원)
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
                onPressed: state.isSubmitting
                    ? null
                    : () => _handleSubmit(notifier),
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

  /// 대표 제품 선택 — 공용 제품검색 모달(단건 선택)에서 1건 선택.
  Future<void> _showProductSelector(BuildContext context) async {
    final selected = await AddProductBottomSheet.show(
      context,
      title: '제품 선택',
      multiSelect: false,
      showCategoryFilter: true,
    );
    if (selected == null || selected.isEmpty || !mounted) return;
    final product = selected.first;
    ref
        .read(suggestionRegisterProvider.notifier)
        .selectProduct(product.productCode, product.productName);
  }

  /// 거래처 선택 — 내 거래처 목록(field scope) 바텀시트에서 1건 선택.
  ///
  /// 물류 클레임은 현장 활동 계열이므로 [MyAccountScope.field](기본값) 사용.
  /// 선택 결과의 `accountCode`(거래처/외부 코드)를 backend `sapAccountCode` 로 전달한다.
  Future<void> _showAccountSelector() async {
    final account = await AccountSelectorSheet.show(context);
    if (account == null || !mounted) return;
    ref.read(suggestionRegisterProvider.notifier).selectAccount(
          accountId: account.accountId,
          accountName: account.accountName,
          sapAccountCode: account.accountCode,
        );
  }

  /// 임시저장 — 현재 폼 상태를 검증 없이 서버에 upsert.
  Future<void> _handleTempSave() async {
    final notifier = ref.read(suggestionRegisterProvider.notifier);
    final ok = await notifier.saveDraft();
    if (!mounted || !ok) return;
    // 성공 메시지만 직접 노출 (실패 메시지는 ref.listen 의 errorMessage 가 처리).
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('임시저장되었습니다')));
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
    final Color effectiveBackground =
        onPressed == null ? background.withValues(alpha: 0.6) : background;
    final Color effectiveForeground = foreground;

    return Material(
      color: effectiveBackground,
      child: InkWell(
        onTap: onPressed,
        child: Center(
          child: loading
              ? SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: effectiveForeground,
                  ),
                )
              : Text(
                  label,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: effectiveForeground,
                  ),
                ),
        ),
      ),
    );
  }
}
