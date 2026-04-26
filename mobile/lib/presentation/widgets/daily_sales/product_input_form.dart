import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// 제품 입력 폼 타입
enum ProductType {
  /// 대표제품
  main,

  /// 기타제품
  sub,
}

/// 일매출 등록 제품 입력 폼
///
/// 대표제품 또는 기타제품의 정보를 입력받습니다.
class ProductInputForm extends StatefulWidget {
  /// 제품 타입
  final ProductType type;

  /// 판매단가 초기값 (대표제품만)
  final int? initialPrice;

  /// 판매수량 초기값
  final int? initialQuantity;

  /// 총 판매금액 초기값
  final int? initialAmount;

  /// 기타제품 코드 초기값 (기타제품만)
  final String? initialCode;

  /// 기타제품명 초기값 (기타제품만)
  final String? initialName;

  /// 값 변경 콜백
  final Function({
    int? price,
    int? quantity,
    int? amount,
    String? code,
    String? name,
  }) onChanged;

  const ProductInputForm({
    super.key,
    required this.type,
    this.initialPrice,
    this.initialQuantity,
    this.initialAmount,
    this.initialCode,
    this.initialName,
    required this.onChanged,
  });

  @override
  State<ProductInputForm> createState() => _ProductInputFormState();
}

class _ProductInputFormState extends State<ProductInputForm> {
  late final TextEditingController _priceController;
  late final TextEditingController _quantityController;
  late final TextEditingController _amountController;
  late final TextEditingController _codeController;
  late final TextEditingController _nameController;

  @override
  void initState() {
    super.initState();
    _priceController = TextEditingController(
      text: widget.initialPrice?.toString() ?? '',
    );
    _quantityController = TextEditingController(
      text: widget.initialQuantity?.toString() ?? '',
    );
    _amountController = TextEditingController(
      text: widget.initialAmount?.toString() ?? '',
    );
    _codeController = TextEditingController(
      text: widget.initialCode ?? '',
    );
    _nameController = TextEditingController(
      text: widget.initialName ?? '',
    );

    // 대표제품의 경우 단가/수량 변경 시 금액 자동 계산
    if (widget.type == ProductType.main) {
      _priceController.addListener(_calculateMainProductAmount);
      _quantityController.addListener(_calculateMainProductAmount);
    }
  }

  @override
  void dispose() {
    _priceController.dispose();
    _quantityController.dispose();
    _amountController.dispose();
    _codeController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  /// 대표제품 총 판매금액 자동 계산
  void _calculateMainProductAmount() {
    final price = int.tryParse(_priceController.text);
    final quantity = int.tryParse(_quantityController.text);

    if (price != null && quantity != null) {
      final amount = price * quantity;
      _amountController.text = amount.toString();
      _notifyChanged();
    }
  }

  /// 값 변경 알림
  void _notifyChanged() {
    if (widget.type == ProductType.main) {
      widget.onChanged(
        price: int.tryParse(_priceController.text),
        quantity: int.tryParse(_quantityController.text),
        amount: int.tryParse(_amountController.text),
      );
    } else {
      widget.onChanged(
        code: _codeController.text.isEmpty ? null : _codeController.text,
        name: _nameController.text.isEmpty ? null : _nameController.text,
        quantity: int.tryParse(_quantityController.text),
        amount: int.tryParse(_amountController.text),
      );
    }
  }

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
              widget.type == ProductType.main ? '대표제품' : '기타제품',
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 16),

            // 대표제품 입력 필드
            if (widget.type == ProductType.main) ...[
              _buildTextField(
                controller: _priceController,
                label: '판매단가 (원)',
                keyboardType: TextInputType.number,
                onChanged: (_) => _notifyChanged(),
              ),
              const SizedBox(height: 12),
              _buildTextField(
                controller: _quantityController,
                label: '판매수량 (개)',
                keyboardType: TextInputType.number,
                onChanged: (_) => _notifyChanged(),
              ),
              const SizedBox(height: 12),
              _buildTextField(
                controller: _amountController,
                label: '총 판매금액 (원)',
                keyboardType: TextInputType.number,
                enabled: false,
              ),
            ],

            // 기타제품 입력 필드
            if (widget.type == ProductType.sub) ...[
              _buildTextField(
                controller: _codeController,
                label: '제품 코드',
                onChanged: (_) => _notifyChanged(),
              ),
              const SizedBox(height: 12),
              _buildTextField(
                controller: _nameController,
                label: '제품명',
                onChanged: (_) => _notifyChanged(),
              ),
              const SizedBox(height: 12),
              _buildTextField(
                controller: _quantityController,
                label: '판매수량 (개)',
                keyboardType: TextInputType.number,
                onChanged: (_) => _notifyChanged(),
              ),
              const SizedBox(height: 12),
              _buildTextField(
                controller: _amountController,
                label: '총 판매금액 (원)',
                keyboardType: TextInputType.number,
                onChanged: (_) => _notifyChanged(),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// 텍스트 필드 위젯 빌더
  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    TextInputType? keyboardType,
    bool enabled = true,
    ValueChanged<String>? onChanged,
  }) {
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      enabled: enabled,
      onChanged: onChanged,
      inputFormatters: keyboardType == TextInputType.number
          ? [FilteringTextInputFormatter.digitsOnly]
          : null,
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        filled: !enabled,
        fillColor: enabled ? null : Colors.grey.shade100,
      ),
    );
  }
}
