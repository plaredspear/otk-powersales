import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../domain/entities/suggestion_form.dart';
import '../providers/suggestion_register_provider.dart';
import '../providers/suggestion_register_state.dart';
import '../widgets/suggestion/suggestion_category_selector.dart';
import '../widgets/suggestion/suggestion_logistics_claim_fields.dart';
import '../widgets/suggestion/suggestion_photo_field.dart';
import '../widgets/suggestion/suggestion_product_field.dart';

const int _maxPhotoSizeBytes = 20 * 1024 * 1024;

/// 제안하기 등록 페이지
///
/// 기능:
/// - 분류 선택 (신제품 제안 / 기존제품 상품가치향상 / 물류 클레임)
/// - 카테고리 분기 입력 (제품 선택 또는 물류 클레임 6 필드)
/// - 제목 입력
/// - 내용 입력
/// - 사진 첨부 (최대 2장, 20MB 가드)
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
                  SuggestionCategorySelector(
                    selectedCategory: state.form.category,
                    onCategoryChanged: notifier.changeCategory,
                  ),
                  const SizedBox(height: 16),

                  if (state.category == SuggestionCategory.logisticsClaim)
                    SuggestionLogisticsClaimFields(
                      accountName: state.form.accountName,
                      claimType: state.form.claimType,
                      claimDate: state.form.claimDate,
                      carNumber: state.form.carNumber,
                      logisticsResponsibility:
                          state.form.logisticsResponsibility,
                      duplicateProposalNum: state.form.duplicateProposalNum,
                      onSelectAccount: _showAccountSelector,
                      onClaimTypeChanged: notifier.updateClaimType,
                      onClaimDateChanged: notifier.updateClaimDate,
                      onCarNumberChanged: notifier.updateCarNumber,
                      onLogisticsResponsibilityChanged:
                          notifier.updateLogisticsResponsibility,
                      onDuplicateProposalNumChanged:
                          notifier.updateDuplicateProposalNum,
                    )
                  else
                    SuggestionProductField(
                      enabled: state.form.isExistingProduct,
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
                    onChanged: notifier.updateContent,
                  ),
                  const SizedBox(height: 16),

                  SuggestionPhotoField(
                    photos: state.form.photos,
                    onAddPhoto: _handleAddPhoto,
                    onRemovePhoto: notifier.removePhoto,
                  ),

                  const SizedBox(height: 80),
                ],
              ),
            ),
      bottomNavigationBar: _buildBottomButton(context, state, notifier),
    );
  }

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

  Future<void> _handleBarcodeScan() async {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('바코드 스캔 기능은 별 스펙에서 구현됩니다')),
      );
    }
  }

  Future<void> _showProductSelector(BuildContext context) async {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('제품 선택 화면은 별 스펙에서 구현됩니다')),
      );
    }
  }

  Future<void> _showAccountSelector() async {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('거래처 선택 화면은 별 스펙에서 구현됩니다')),
      );
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
    await notifier.submit();
  }
}

/// 제목 입력 필드 (max 250 — backend `@Size(max=250)` 정합)
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
          maxLength: 2000,
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
