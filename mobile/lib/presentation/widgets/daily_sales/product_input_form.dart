import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/utils/thousands_separator_input_formatter.dart';
import '../common/synced_text_field.dart';

/// 제품 입력 폼 타입
enum ProductType {
  /// 대표제품
  main,

  /// 기타제품
  sub,
}

/// 일매출 등록 제품 입력 폼.
///
/// 레거시(Heroku) `promotion/event/write.jsp` 일 매출 등록 화면과 동일한 구성:
/// - 대표 제품: 행사마스터의 대표제품명 + 제품코드 노출 → 판매 수량 + 총 판매 금액 입력.
///   판매 단가 필드는 없으며 총 판매 금액을 단가×수량으로 자동 계산하지 않는다
///   (2024-01-29 마스터 개선안으로 레거시에서도 주석 처리됨).
/// - 기타 제품: 행사마스터의 기타제품 텍스트 노출 → 행사 대체 제품(최대 30자) +
///   판매 수량 + 총 판매 금액 입력. 셋 중 하나라도 입력되면 나머지도 필수가 되며
///   레거시와 동일하게 라벨에 `*` 가 실시간으로 붙는다.
///
/// 외부 상태(provider)를 단일 소스로 사용하는 stateless 폼이며, 입력은
/// [SyncedTextField] 로 처리한다(인라인 컨트롤러 금지 컨벤션). 숫자 입력은 레거시
/// `numberWithCommas` 정합으로 천단위 콤마를 표시한다.
class ProductInputForm extends StatelessWidget {
  /// 제품 타입
  final ProductType type;

  /// 행사마스터 대표제품명 (대표제품만)
  final String? productName;

  /// 행사마스터 대표제품코드 (대표제품만)
  final String? productCode;

  /// 행사마스터 기타제품 텍스트 (기타제품만)
  final String? masterOtherProduct;

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
    this.productName,
    this.productCode,
    this.masterOtherProduct,
    this.name,
    this.quantity,
    this.amount,
    required this.onChanged,
  });

  bool get _isMain => type == ProductType.main;

  /// 기타제품 3필드 중 하나라도 입력되면 나머지도 필수 (레거시 `onkeyup_event2`).
  bool get _subRequired =>
      !_isMain &&
      ((name != null && name!.isNotEmpty) || quantity != null || amount != null);

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
              _isMain ? '대표 제품' : '기타 제품',
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
            ),

            // 행사마스터가 지정한 제품 정보 (읽기 전용)
            if (_isMain) ...[
              const SizedBox(height: 8),
              _buildMasterProductInfo(),
            ] else ...[
              if (masterOtherProduct != null &&
                  masterOtherProduct!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  masterOtherProduct!,
                  style: const TextStyle(fontSize: 15),
                ),
              ],
              // 안내문 (레거시: "※ 대표제품 외 추가 또는 대체")
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
                required: _subRequired,
                inputFormatters: [LengthLimitingTextInputFormatter(30)],
                onChanged: (v) => onChanged(
                  name: v.isEmpty ? null : v,
                  quantity: quantity,
                  amount: amount,
                ),
              ),
              const SizedBox(height: 12),
            ],

            // 판매 수량
            _buildNumberField(
              value: quantity,
              label: '판매 수량 (개)',
              required: _subRequired,
              onChanged: (v) => onChanged(
                name: name,
                quantity: v,
                amount: amount,
              ),
            ),
            const SizedBox(height: 12),

            // 총 판매 금액 (사용자 직접 입력)
            _buildNumberField(
              value: amount,
              label: '총 판매 금액 (원)',
              required: _subRequired,
              onChanged: (v) => onChanged(
                name: name,
                quantity: quantity,
                amount: v,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 행사마스터 대표제품명 + 제품코드 (레거시 `primaryProductNmTxt`/`primaryProductCdTxt`).
  Widget _buildMasterProductInfo() {
    final hasInfo = (productName != null && productName!.isNotEmpty) ||
        (productCode != null && productCode!.isNotEmpty);
    if (!hasInfo) {
      return Text(
        '행사에 지정된 대표 제품이 없습니다',
        style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (productName != null && productName!.isNotEmpty)
          Text(productName!, style: const TextStyle(fontSize: 15)),
        if (productCode != null && productCode!.isNotEmpty)
          Text(
            productCode!,
            style: TextStyle(fontSize: 13, color: Colors.grey.shade600),
          ),
      ],
    );
  }

  Widget _buildNumberField({
    required int? value,
    required String label,
    required bool required,
    required ValueChanged<int?> onChanged,
  }) {
    return _buildField(
      value: ThousandsSeparatorInputFormatter.format(value),
      label: label,
      required: required,
      keyboardType: TextInputType.number,
      inputFormatters: const [ThousandsSeparatorInputFormatter()],
      onChanged: (v) =>
          onChanged(int.tryParse(ThousandsSeparatorInputFormatter.digitsOf(v))),
    );
  }

  Widget _buildField({
    required String value,
    required String label,
    required bool required,
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
        label: Text.rich(
          TextSpan(
            text: label,
            children: [
              if (required)
                const TextSpan(
                  text: ' *',
                  style: TextStyle(color: Colors.red),
                ),
            ],
          ),
        ),
        border: const OutlineInputBorder(),
      ),
    );
  }
}
