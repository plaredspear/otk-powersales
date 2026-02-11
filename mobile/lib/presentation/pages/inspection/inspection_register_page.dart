import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/entities/inspection_field_type.dart';
import '../../../domain/entities/inspection_list_item.dart';
import '../../../domain/entities/inspection_theme.dart';
import '../../providers/inspection_register_provider.dart';
import '../../providers/inspection_register_state.dart';
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
      ref.read(inspectionRegisterProvider.notifier).initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(inspectionRegisterProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('점검 등록'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              child: Column(
                children: [
                  // 공통 필드 폼
                  InspectionCommonForm(
                    selectedTheme: state.selectedTheme,
                    category: state.category,
                    selectedStoreName: state.selectedStoreName,
                    inspectionDate: state.form?.inspectionDate ?? DateTime.now(),
                    selectedFieldType: state.selectedFieldType,
                    onThemeTap: () => _showThemeSelector(context),
                    onCategoryChanged: (category) {
                      ref
                          .read(inspectionRegisterProvider.notifier)
                          .changeCategory(category);
                    },
                    onStoreTap: () => _showStoreSelector(context),
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
                      competitorProductName: state.form?.competitorProductName,
                      competitorProductPrice: state.form?.competitorProductPrice,
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

                  const Divider(height: 32, thickness: 8),

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
      bottomNavigationBar: _buildBottomButtons(context, state),
    );
  }

  /// 하단 버튼 (임시저장 + 전송)
  Widget _buildBottomButtons(
    BuildContext context,
    InspectionRegisterState state,
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
              onPressed: state.isLoading ? null : () => _handleDraftSave(),
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
              onPressed: state.isLoading ? null : () => _handleSubmit(),
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

  /// 테마 선택 다이얼로그
  void _showThemeSelector(BuildContext context) {
    final themes = ref.read(inspectionRegisterProvider).themes;
    if (themes.isEmpty) return;

    showModalBottomSheet(
      context: context,
      builder: (context) => ListView.builder(
        itemCount: themes.length,
        itemBuilder: (context, index) {
          final theme = themes[index];
          return ListTile(
            title: Text(theme.name),
            onTap: () {
              ref
                  .read(inspectionRegisterProvider.notifier)
                  .selectTheme(theme);
              Navigator.pop(context);
            },
          );
        },
      ),
    );
  }

  /// 거래처 선택 다이얼로그
  void _showStoreSelector(BuildContext context) {
    final stores = ref.read(inspectionRegisterProvider).stores;
    if (stores.isEmpty) return;

    showModalBottomSheet(
      context: context,
      builder: (context) => ListView(
        children: stores.entries.map((entry) {
          return ListTile(
            title: Text(entry.value),
            onTap: () {
              ref
                  .read(inspectionRegisterProvider.notifier)
                  .selectStore(entry.key, entry.value);
              Navigator.pop(context);
            },
          );
        }).toList(),
      ),
    );
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

  /// 제품 선택 (임시 구현)
  void _showProductSelector(BuildContext context) {
    // TODO: 제품 검색 화면으로 이동
    // 임시로 샘플 제품 선택
    ref
        .read(inspectionRegisterProvider.notifier)
        .selectProduct('P001', '진라면');
  }

  /// 바코드 스캔 (임시 구현)
  void _handleBarcodeScan() {
    // TODO: 바코드 스캐너 실행
    // 임시로 샘플 제품 선택
    ref
        .read(inspectionRegisterProvider.notifier)
        .selectProduct('P002', '진라면 매운맛');
  }

  /// 사진 추가 (임시 구현)
  void _handleAddPhoto() {
    // TODO: ImagePicker로 카메라/갤러리 선택
    // 임시로 더미 파일 추가
    ref
        .read(inspectionRegisterProvider.notifier)
        .addPhoto(File('test_photo.jpg'));
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
    final success =
        await ref.read(inspectionRegisterProvider.notifier).submit();

    if (!mounted) return;

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('점검이 등록되었습니다')),
      );
      Navigator.of(context).pop();
    } else {
      final errorMessage =
          ref.read(inspectionRegisterProvider).errorMessage ?? '등록 실패';
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(errorMessage)),
      );
      ref.read(inspectionRegisterProvider.notifier).clearError();
    }
  }
}
