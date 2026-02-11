import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../providers/shelf_life_form_provider.dart';
import '../widgets/shelf_life/shelf_life_edit_form.dart';

/// 유통기한 수정 페이지
///
/// 기존 유통기한 항목의 유통기한/알림일/설명을 수정합니다.
/// 거래처/제품은 읽기 전용으로 표시됩니다.
/// 수정 화면에서 단건 삭제도 가능합니다.
class ShelfLifeEditPage extends ConsumerStatefulWidget {
  final ShelfLifeItem item;

  const ShelfLifeEditPage({super.key, required this.item});

  @override
  ConsumerState<ShelfLifeEditPage> createState() =>
      _ShelfLifeEditPageState();
}

class _ShelfLifeEditPageState extends ConsumerState<ShelfLifeEditPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(shelfLifeFormProvider.notifier)
          .initializeForEdit(widget.item);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(shelfLifeFormProvider);

    // 저장/삭제 완료 리스닝
    ref.listen(shelfLifeFormProvider, (previous, next) {
      if (next.isSaved && !(previous?.isSaved ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('유통기한이 수정되었습니다')),
        );
        AppRouter.goBack(context, result: true);
      }
      if (next.isDeleted && !(previous?.isDeleted ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('유통기한이 삭제되었습니다')),
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
        title: const Text('유통기한 수정'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        actions: [
          // 삭제 버튼
          IconButton(
            icon: const Icon(Icons.delete_outline, color: AppColors.error),
            onPressed: state.isLoading
                ? null
                : () => _showDeleteConfirmDialog(context),
          ),
        ],
      ),
      body: ShelfLifeEditForm(
        storeName: state.selectedStoreName ?? widget.item.storeName,
        productName: state.selectedProductName ?? widget.item.productName,
        productCode: state.selectedProductCode ?? widget.item.productCode,
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
        onExpiryDateChanged: (date) {
          ref.read(shelfLifeFormProvider.notifier).updateExpiryDate(date);
        },
        onAlertDateChanged: (date) {
          ref.read(shelfLifeFormProvider.notifier).updateAlertDate(date);
        },
        onDescriptionChanged: (text) {
          ref
              .read(shelfLifeFormProvider.notifier)
              .updateDescription(text);
        },
      ),

      // 저장 버튼
      bottomNavigationBar: _buildSaveButton(state),
    );
  }

  Widget _buildSaveButton(dynamic state) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: SizedBox(
          width: double.infinity,
          height: AppSpacing.buttonHeight,
          child: ElevatedButton(
            onPressed: state.canSave
                ? () {
                    ref.read(shelfLifeFormProvider.notifier).update();
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
                : const Text('저장'),
          ),
        ),
      ),
    );
  }

  void _showDeleteConfirmDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('유통기한 삭제'),
        content: const Text('이 유통기한 항목을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              ref.read(shelfLifeFormProvider.notifier).delete();
            },
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('삭제'),
          ),
        ],
      ),
    );
  }
}
