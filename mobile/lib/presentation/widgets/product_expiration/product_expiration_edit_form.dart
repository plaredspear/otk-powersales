import 'package:flutter/material.dart';

import '../claim/claim_form_row.dart';
import 'product_expiration_date_row.dart';

/// 소비기한 수정 폼 위젯
///
/// 레거시(otg_PowerSales product/expiration/write.jsp, type='U') UI 기준 정합.
/// 거래처/제품은 읽기 전용(회색 배경), 소비기한/마감 전 푸시 메세지 알림/설명만
/// 수정 가능하다.
class ProductExpirationEditForm extends StatelessWidget {
  /// 거래처명 (읽기 전용)
  final String accountName;

  /// 제품명 (읽기 전용)
  final String productName;

  /// 제품 코드 (읽기 전용)
  final String productCode;

  /// 소비기한
  final DateTime expiryDate;

  /// 마감 전 푸시 메세지 알림 날짜
  final DateTime alertDate;

  /// 설명
  final String description;

  /// 소비기한 변경 콜백
  final ValueChanged<DateTime> onExpiryDateChanged;

  /// 마감 전 푸시 메세지 알림 변경 콜백
  final ValueChanged<DateTime> onAlertDateChanged;

  /// 설명 변경 콜백
  final ValueChanged<String> onDescriptionChanged;

  const ProductExpirationEditForm({
    super.key,
    required this.accountName,
    required this.productName,
    required this.productCode,
    required this.expiryDate,
    required this.alertDate,
    required this.description,
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
          // 거래처 (읽기 전용)
          ClaimFormRow(
            label: '거래처',
            enabled: false,
            below: _ReadOnlyValue(accountName),
          ),

          // 제품 (읽기 전용)
          ClaimFormRow(
            label: '제품',
            enabled: false,
            below: _ReadOnlyValue(
              productCode.isNotEmpty
                  ? '$productName ($productCode)'
                  : productName,
            ),
          ),

          // 소비기한
          ProductExpirationDateRow(
            label: '소비기한',
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

/// 읽기 전용 값 텍스트 (비활성 회색)
class _ReadOnlyValue extends StatelessWidget {
  const _ReadOnlyValue(this.value);

  final String value;

  @override
  Widget build(BuildContext context) {
    return Text(
      value,
      style: const TextStyle(
        fontSize: 14,
        color: ClaimFormColors.labelDisabled,
      ),
    );
  }
}
