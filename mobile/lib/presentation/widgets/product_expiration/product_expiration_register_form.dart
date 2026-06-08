import 'package:flutter/material.dart';

import '../claim/claim_form_row.dart';
import '../claim/claim_product_field.dart';
import 'product_expiration_date_row.dart';

/// 유통기한 등록 폼 위젯
///
/// 레거시(otg_PowerSales product/expiration/write.jsp) UI 기준 정합.
/// 테두리 박스 없이 라벨 + 하단 값/플레이스홀더 행으로 구성하고 행마다 얇은
/// 구분선을 둔다. 거래처/제품/유통기한/마감 전 푸시 메세지 알림/설명 순.
class ProductExpirationRegisterForm extends StatelessWidget {
  /// 선택된 거래처명
  final String? accountName;

  /// 선택된 제품 코드
  final String? productCode;

  /// 선택된 제품명
  final String? productName;

  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 푸시 메세지 알림 날짜
  final DateTime alertDate;

  /// 설명
  final String description;

  /// 거래처 선택 버튼/행 콜백
  final VoidCallback onSelectAccount;

  /// 제품 선택 버튼 콜백
  final VoidCallback onSelectProduct;

  /// 바코드 스캔 버튼 콜백
  final VoidCallback onScanBarcode;

  /// 유통기한 변경 콜백
  final ValueChanged<DateTime> onExpiryDateChanged;

  /// 마감 전 푸시 메세지 알림 변경 콜백
  final ValueChanged<DateTime> onAlertDateChanged;

  /// 설명 변경 콜백
  final ValueChanged<String> onDescriptionChanged;

  const ProductExpirationRegisterForm({
    super.key,
    required this.accountName,
    required this.productCode,
    required this.productName,
    required this.expiryDate,
    required this.alertDate,
    required this.description,
    required this.onSelectAccount,
    required this.onSelectProduct,
    required this.onScanBarcode,
    required this.onExpiryDateChanged,
    required this.onAlertDateChanged,
    required this.onDescriptionChanged,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 거래처
          ClaimFormRow(
            label: '거래처',
            isRequired: true,
            onTap: onSelectAccount,
            trailing: const ClaimRowChevron(),
            below: ClaimValueText(
              value: accountName,
              placeholder: '거래처 선택',
            ),
          ),

          // 제품
          ClaimProductField(
            productName: productName,
            productCode: productCode,
            onBarcodePressed: onScanBarcode,
            onProductSelectPressed: onSelectProduct,
          ),

          // 유통기한
          ProductExpirationDateRow(
            label: '유통기한',
            date: expiryDate,
            onDateChanged: onExpiryDateChanged,
          ),

          // 마감 전 푸시 메세지 알림
          ProductExpirationDateRow(
            label: '마감 전 푸시 메세지 알림',
            date: alertDate,
            onDateChanged: onAlertDateChanged,
          ),

          // 설명
          ClaimFormRow(
            label: '설명',
            showDivider: false,
            below: TextFormField(
              initialValue: description,
              style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
              decoration: const InputDecoration(
                isCollapsed: true,
                hintText: '설명 입력',
                hintStyle:
                    TextStyle(fontSize: 14, color: ClaimFormColors.placeholder),
                border: InputBorder.none,
              ),
              onChanged: onDescriptionChanged,
            ),
          ),

          const SizedBox(height: 24),
        ],
      ),
    );
  }
}
