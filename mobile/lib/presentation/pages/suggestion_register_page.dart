import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/suggestion_register_provider.dart';
import '../providers/suggestion_register_state.dart';
import '../widgets/suggestion/suggestion_category_selector.dart';
import '../widgets/suggestion/suggestion_photo_field.dart';
import '../widgets/suggestion/suggestion_product_field.dart';

/// 제안하기 등록 페이지
///
/// 기능:
/// - 분류 선택 (신제품 제안 / 기존제품 상품가치향상)
/// - 제품 선택 (기존제품일 때만, 바코드 스캔 또는 직접 선택)
/// - 제목 입력
/// - 내용 입력
/// - 사진 첨부 (최대 2장)
/// - 제출
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

    // 에러 메시지 표시
    ref.listen<SuggestionRegisterState>(
      suggestionRegisterProvider,
      (previous, next) {
        if (next.errorMessage != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next.errorMessage!)),
          );
        }
        if (next.successMessage != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next.successMessage!)),
          );
          // 성공 시 페이지 닫기
          Navigator.of(context).pop();
        }
      },
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('제안하기'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 분류 선택
                  SuggestionCategorySelector(
                    selectedCategory: state.form.category,
                    onCategoryChanged: notifier.changeCategory,
                  ),
                  const SizedBox(height: 16),

                  // 제품 선택
                  SuggestionProductField(
                    enabled: state.form.isExistingProduct,
                    productName: state.form.productName,
                    productCode: state.form.productCode,
                    onBarcodePressed: _handleBarcodeScan,
                    onSelectPressed: () => _showProductSelector(context),
                  ),
                  const SizedBox(height: 16),

                  // 제목 입력
                  _TitleField(
                    controller: _titleController,
                    onChanged: notifier.updateTitle,
                  ),
                  const SizedBox(height: 16),

                  // 내용 입력
                  _ContentField(
                    controller: _contentController,
                    onChanged: notifier.updateContent,
                  ),
                  const SizedBox(height: 16),

                  // 사진 첨부
                  SuggestionPhotoField(
                    photos: state.form.photos,
                    onAddPhoto: _handleAddPhoto,
                    onRemovePhoto: notifier.removePhoto,
                  ),

                  const SizedBox(height: 80), // 하단 버튼 공간
                ],
              ),
            ),
      bottomNavigationBar: _buildBottomButton(context, state, notifier),
    );
  }

  /// 하단 제출 버튼
  Widget _buildBottomButton(
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
      child: ElevatedButton(
        onPressed: state.isSubmitting ? null : () => _handleSubmit(notifier),
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
                '제출',
                style: TextStyle(fontSize: 16, color: Colors.black),
              ),
      ),
    );
  }

  /// 바코드 스캔
  Future<void> _handleBarcodeScan() async {
    // TODO: 바코드 스캐너 실행
    // 임시로 샘플 제품 선택
    final notifier = ref.read(suggestionRegisterProvider.notifier);
    notifier.selectProduct('P001', '진라면');

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('바코드 스캔 기능은 추후 구현 예정입니다')),
      );
    }
  }

  /// 제품 선택 다이얼로그
  Future<void> _showProductSelector(BuildContext context) async {
    // TODO: 제품 검색 화면으로 이동
    // 임시로 샘플 제품 선택
    final notifier = ref.read(suggestionRegisterProvider.notifier);
    notifier.selectProduct('P002', '진라면 매운맛');

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('제품 선택 기능은 추후 구현 예정입니다')),
      );
    }
  }

  /// 사진 추가
  Future<void> _handleAddPhoto() async {
    // TODO: 이미지 선택 구현 (image_picker 패키지 사용)
    // 임시로 스낵바만 표시
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('사진 선택 기능은 추후 구현 예정입니다')),
      );
    }
  }

  /// 제출
  Future<void> _handleSubmit(SuggestionRegisterNotifier notifier) async {
    await notifier.submit();
  }
}

/// 제목 입력 필드
class _TitleField extends StatelessWidget {
  const _TitleField({
    required this.controller,
    required this.onChanged,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '제목 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLength: 100,
          decoration: const InputDecoration(
            hintText: '제목을 입력하세요',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.all(12),
            counterText: '', // 글자 수 카운터 숨김
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }
}

/// 내용 입력 필드
class _ContentField extends StatelessWidget {
  const _ContentField({
    required this.controller,
    required this.onChanged,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '내용 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          maxLines: 5,
          maxLength: 500,
          decoration: const InputDecoration(
            hintText: '제안 내용을 상세하게 입력하세요',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.all(12),
          ),
          onChanged: onChanged,
        ),
      ],
    );
  }
}
