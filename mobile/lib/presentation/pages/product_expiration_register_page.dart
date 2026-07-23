import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../providers/pos_sales_provider.dart';
import '../providers/product_expiration_form_provider.dart';
import '../screens/barcode_scanner_screen.dart';
import '../widgets/order_form/add_product_bottom_sheet.dart';
import '../widgets/product_expiration/product_expiration_register_form.dart';

/// 소비기한 등록 페이지
///
/// 거래처/제품 선택, 소비기한/알림일 입력, 설명 입력을 통해 소비기한을 등록합니다.
/// 등록 완료 시 이전 화면(관리 화면)으로 복귀합니다.
class ProductExpirationRegisterPage extends ConsumerStatefulWidget {
  const ProductExpirationRegisterPage({super.key});

  @override
  ConsumerState<ProductExpirationRegisterPage> createState() =>
      _ProductExpirationRegisterPageState();
}

class _ProductExpirationRegisterPageState
    extends ConsumerState<ProductExpirationRegisterPage> {
  @override
  void initState() {
    super.initState();
    // provider 수정은 build 단계(라우트 마운트 포함)에서 금지되므로 post-frame
    // 으로 미룬다. 낡은 값이 첫 build 에 노출되는 문제는 설명 필드(SyncedTextField)
    // 가 state 값과 동기화해 해소한다.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(productExpirationFormProvider.notifier).initializeForRegister();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productExpirationFormProvider);

    // 저장 완료 리스닝
    ref.listen(productExpirationFormProvider, (previous, next) {
      if (next.isSaved && !(previous?.isSaved ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('소비기한이 등록되었습니다')),
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
        title: const Text('소비기한 등록'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: state.isLoading && !state.hasAccount
          ? const Center(child: CircularProgressIndicator())
          : ProductExpirationRegisterForm(
              accountName: state.selectedAccountName,
              productCode: state.selectedProductCode,
              productName: state.selectedProductName,
              expiryDate: state.expiryDate,
              alertDate: state.alertDate,
              description: state.description,
              onSelectAccount: () async {
                final account = await AccountSelectorSheet.show(context);
                if (account != null) {
                  ref
                      .read(productExpirationFormProvider.notifier)
                      .selectAccount(account.accountCode, account.accountName);
                }
              },
              onSelectProduct: () async {
                final selected = await AddProductBottomSheet.show(
                  context,
                  title: '제품 선택',
                  multiSelect: false,
                  showCategoryFilter: true,
                );
                if (selected == null || selected.isEmpty) return;
                final product = selected.first;
                ref.read(productExpirationFormProvider.notifier).selectProduct(
                      product.productCode,
                      product.productName,
                    );
              },
              onScanBarcode: _handleBarcodeScan,
              onExpiryDateChanged: (date) {
                ref
                    .read(productExpirationFormProvider.notifier)
                    .updateExpiryDate(date);
              },
              onAlertDateChanged: (date) {
                ref
                    .read(productExpirationFormProvider.notifier)
                    .updateAlertDate(date);
              },
              onDescriptionChanged: (text) {
                ref
                    .read(productExpirationFormProvider.notifier)
                    .updateDescription(text);
              },
            ),

      // 등록 버튼
      bottomNavigationBar: _buildRegisterButton(state),
    );
  }

  /// 바코드 스캔 — 카메라로 제품 바코드를 스캔해 소비기한 등록 대상 제품을 선택한다.
  Future<void> _handleBarcodeScan() async {
    final barcode = await BarcodeScannerScreen.show(context);
    if (barcode == null || !mounted) return;

    try {
      final product =
          await ref.read(posProductUseCaseProvider).findByBarcode(barcode);
      if (!mounted) return;
      if (product == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('해당 제품이 없습니다')),
        );
        return;
      }
      ref
          .read(productExpirationFormProvider.notifier)
          .selectProduct(product.productCode, product.productName);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${product.productName} 선택됨')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('제품 조회에 실패했습니다')),
      );
    }
  }

  /// 하단 등록 버튼 — 레거시(fix_bottom_wrap btn_yellow)처럼 전폭 무여백.
  Widget _buildRegisterButton(dynamic state) {
    final enabled = state.canSave as bool;
    return SafeArea(
      top: false,
      child: SizedBox(
        height: 56,
        child: Material(
          color: enabled ? AppColors.legacyYellow : AppColors.divider,
          child: InkWell(
            onTap: enabled
                ? () => ref.read(productExpirationFormProvider.notifier).register()
                : null,
            child: Center(
              child: state.isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AppColors.textPrimary,
                      ),
                    )
                  : Text(
                      '등록',
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.w700,
                        color: enabled
                            ? AppColors.textPrimary
                            : AppColors.textTertiary,
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }
}
