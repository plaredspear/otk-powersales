import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../providers/shelf_life_form_provider.dart';
import '../widgets/shelf_life/shelf_life_add_product_sheet.dart';
import '../widgets/shelf_life/shelf_life_register_form.dart';

/// 유통기한 등록 페이지
///
/// 거래처/제품 선택, 유통기한/알림일 입력, 설명 입력을 통해 유통기한을 등록합니다.
/// 등록 완료 시 이전 화면(관리 화면)으로 복귀합니다.
class ShelfLifeRegisterPage extends ConsumerStatefulWidget {
  const ShelfLifeRegisterPage({super.key});

  @override
  ConsumerState<ShelfLifeRegisterPage> createState() =>
      _ShelfLifeRegisterPageState();
}

class _ShelfLifeRegisterPageState
    extends ConsumerState<ShelfLifeRegisterPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(shelfLifeFormProvider.notifier).initializeForRegister();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(shelfLifeFormProvider);

    // 저장 완료 리스닝
    ref.listen(shelfLifeFormProvider, (previous, next) {
      if (next.isSaved && !(previous?.isSaved ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('유통기한이 등록되었습니다')),
        );
        AppRouter.goBack(context, result: true);
      }
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(shelfLifeFormProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('유통기한 등록'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: state.isLoading && !state.hasStore
          ? const Center(child: CircularProgressIndicator())
          : ShelfLifeRegisterForm(
              stores: state.stores,
              selectedStoreId: state.selectedStoreId,
              selectedProductCode: state.selectedProductCode,
              selectedProductName: state.selectedProductName,
              expiryDate: state.expiryDate,
              alertDate: state.alertDate,
              description: state.description,
              onStoreChanged: (storeId, storeName) {
                ref
                    .read(shelfLifeFormProvider.notifier)
                    .selectStore(storeId, storeName);
              },
              onSelectProduct: () async {
                final result =
                    await ShelfLifeAddProductSheet.show(context);
                if (result != null) {
                  ref.read(shelfLifeFormProvider.notifier).selectProduct(
                        result.productCode,
                        result.productName,
                      );
                }
              },
              onScanBarcode: () {
                // TODO: 바코드 스캔 기능 (P5 이후 구현)
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('바코드 스캔 기능은 준비 중입니다')),
                );
              },
              onExpiryDateChanged: (date) {
                ref
                    .read(shelfLifeFormProvider.notifier)
                    .updateExpiryDate(date);
              },
              onAlertDateChanged: (date) {
                ref
                    .read(shelfLifeFormProvider.notifier)
                    .updateAlertDate(date);
              },
              onDescriptionChanged: (text) {
                ref
                    .read(shelfLifeFormProvider.notifier)
                    .updateDescription(text);
              },
            ),

      // 등록 버튼
      bottomNavigationBar: _buildRegisterButton(state),
    );
  }

  Widget _buildRegisterButton(dynamic state) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: SizedBox(
          width: double.infinity,
          height: AppSpacing.buttonHeight,
          child: ElevatedButton(
            onPressed: state.canSave
                ? () {
                    ref.read(shelfLifeFormProvider.notifier).register();
                  }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.white,
              disabledBackgroundColor: AppColors.divider,
              disabledForegroundColor: AppColors.textTertiary,
              shape: RoundedRectangleBorder(
                borderRadius: AppSpacing.buttonBorderRadius,
              ),
            ),
            child: state.isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.white,
                    ),
                  )
                : const Text('등록'),
          ),
        ),
      ),
    );
  }
}
