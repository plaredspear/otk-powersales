import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../common/synced_text_field.dart';

/// 제품 입력 폼 타입
enum ProductType {
  /// 대표제품
  main,

  /// 기타제품
  sub,
}

/// 일매출 마감 제품 입력 폼.
///
/// 레거시(Heroku) 일 매출 등록 화면과 동일한 구성:
/// - 대표제품: 판매수량 + 총 판매금액(사용자 직접 입력). 판매단가 필드는 없으며
///   총 판매금액을 단가×수량으로 자동 계산하지 않는다(2024-01-29 마스터 개선안).
/// - 기타제품: 행사 대체 제품(최대 30자) + 판매수량 + 총 판매금액.
///
/// 외부 상태(provider)를 단일 소스로 사용하는 stateless 폼이며, 입력은
/// [SyncedTextField] 로 처리한다(인라인 컨트롤러 금지 컨벤션).
class ProductInputForm extends StatelessWidget {
  /// 제품 타입
  final ProductType type;

  /// 행사 대체 제품명 (기타제품만)
  final String? name;

  /// 판매수량
  final int? quantity;

  /// 총 판매금액
  final int? amount;

  /// 값 변경 콜백 (변경된 필드 포함 풀 스냅샷 전달)
  final void Function({String? name, int? quantity, int? amount}) onChanged;

  const ProductInputForm({
    super.key,
    required this.type,
    this.name,
    this.quantity,
    this.amount,
    required this.onChanged,
  });

  bool get _isMain => type == ProductType.main;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 제목
            Text(
              _isMain ? '대표제품' : '기타제품',
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),

            // 기타제품 안내문 (레거시: "※ 대표제품 외 추가 또는 대체")
            if (!_isMain) ...[
              const SizedBox(height: 4),
              Text(
                '※ 대표제품 외 추가 또는 대체',
                style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
              ),
            ],
            const SizedBox(height: 16),

            // 기타제품: 행사 대체 제품명 (최대 30자)
            if (!_isMain) ...[
              _buildField(
                value: name ?? '',
                label: '행사 대체 제품',
                inputFormatters: [LengthLimitingTextInputFormatter(30)],
                onChanged: (v) => onChanged(
                  name: v.isEmpty ? null : v,
                  quantity: quantity,
                  amount: amount,
                ),
              ),
              const SizedBox(height: 12),
            ],

            // 판매수량
            _buildField(
              value: quantity?.toString() ?? '',
              label: '판매수량 (개)',
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onChanged: (v) => onChanged(
                name: name,
                quantity: int.tryParse(v),
                amount: amount,
              ),
            ),
            const SizedBox(height: 12),

            // 총 판매금액 (사용자 직접 입력)
            _buildField(
              value: amount?.toString() ?? '',
              label: '총 판매금액 (원)',
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              onChanged: (v) => onChanged(
                name: name,
                quantity: quantity,
                amount: int.tryParse(v),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildField({
    required String value,
    required String label,
    TextInputType? keyboardType,
    List<TextInputFormatter>? inputFormatters,
    required ValueChanged<String> onChanged,
  }) {
    return SyncedTextField(
      value: value,
      keyboardType: keyboardType,
      inputFormatters: inputFormatters,
      onChanged: onChanged,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
      ),
    );
  }
}
