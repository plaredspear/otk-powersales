import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../domain/entities/product_expiration_item.dart';
import '../providers/product_expiration_form_provider.dart';
import '../widgets/product_expiration/product_expiration_edit_form.dart';

/// 유통기한 수정 페이지
///
/// 기존 유통기한 항목의 유통기한/알림일/설명을 수정합니다.
/// 거래처/제품은 읽기 전용으로 표시됩니다.
/// 수정 화면에서 단건 삭제도 가능합니다.
class ProductExpirationEditPage extends ConsumerStatefulWidget {
  final ProductExpirationItem item;

  const ProductExpirationEditPage({super.key, required this.item});

  @override
  ConsumerState<ProductExpirationEditPage> createState() =>
      _ProductExpirationEditPageState();
}

class _ProductExpirationEditPageState extends ConsumerState<ProductExpirationEditPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(productExpirationFormProvider.notifier)
          .initializeForEdit(widget.item);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productExpirationFormProvider);

    // 저장/삭제 완료 리스닝
    ref.listen(productExpirationFormProvider, (previous, next) {
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
        ref.read(productExpirationFormProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('유통기한 수정'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: ProductExpirationEditForm(
        accountName: state.selectedAccountName ?? widget.item.accountName,
        productName: state.selectedProductName ?? widget.item.productName,
        productCode: state.selectedProductCode ?? widget.item.productCode,
        expiryDate: state.expiryDate,
        alertDate: state.alertDate,
        description: state.description,
        onExpiryDateChanged: (date) {
          ref.read(productExpirationFormProvider.notifier).updateExpiryDate(date);
        },
        onAlertDateChanged: (date) {
          ref.read(productExpirationFormProvider.notifier).updateAlertDate(date);
        },
        onDescriptionChanged: (text) {
          ref
              .read(productExpirationFormProvider.notifier)
              .updateDescription(text);
        },
      ),

      // 하단 버튼 (삭제 + 저장) — 레거시(fix_bottom_wrap btn_red/btn_yellow) 전폭 2분할
      bottomNavigationBar: _buildBottomButtons(state),
    );
  }

  Widget _buildBottomButtons(dynamic state) {
    final loading = state.isLoading as bool;
    final canSave = state.canSave as bool;
    return SafeArea(
      top: false,
      child: SizedBox(
        height: 56,
        child: Row(
          children: [
            // 삭제 버튼
            Expanded(
              child: _BottomBarButton(
                label: '삭제',
                backgroundColor: AppColors.otokiRed,
                textColor: AppColors.white,
                onPressed: loading ? null : () => _showDeleteConfirmDialog(context),
              ),
            ),
            // 저장 버튼
            Expanded(
              child: _BottomBarButton(
                label: '저장',
                backgroundColor: AppColors.legacyYellow,
                textColor: AppColors.textPrimary,
                disabledBackgroundColor: AppColors.divider,
                disabledTextColor: AppColors.textTertiary,
                isLoading: loading,
                onPressed: canSave
                    ? () => ref.read(productExpirationFormProvider.notifier).update()
                    : null,
              ),
            ),
          ],
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
              ref.read(productExpirationFormProvider.notifier).delete();
            },
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('삭제'),
          ),
        ],
      ),
    );
  }
}

/// 하단 바 버튼 (전폭 무여백)
class _BottomBarButton extends StatelessWidget {
  const _BottomBarButton({
    required this.label,
    required this.backgroundColor,
    required this.textColor,
    required this.onPressed,
    this.disabledBackgroundColor,
    this.disabledTextColor,
    this.isLoading = false,
  });

  final String label;
  final Color backgroundColor;
  final Color textColor;
  final Color? disabledBackgroundColor;
  final Color? disabledTextColor;
  final bool isLoading;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null;
    return Material(
      color: enabled
          ? backgroundColor
          : (disabledBackgroundColor ?? backgroundColor),
      child: InkWell(
        onTap: onPressed,
        child: Center(
          child: isLoading
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.textPrimary,
                  ),
                )
              : Text(
                  label,
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w600,
                    color: enabled ? textColor : (disabledTextColor ?? textColor),
                  ),
                ),
        ),
      ),
    );
  }
}
