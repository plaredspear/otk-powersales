import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../domain/entities/claim_code.dart';
import 'claim_form_row.dart';
import 'claim_photo_field.dart';

/// 클레임 구매 정보 섹션
class ClaimPurchaseSection extends StatelessWidget {
  const ClaimPurchaseSection({
    super.key,
    required this.purchaseAmount,
    required this.purchaseMethods,
    required this.selectedPurchaseMethod,
    required this.receiptPhoto,
    required this.onPurchaseAmountChanged,
    required this.onPurchaseMethodSelected,
    required this.onReceiptPhotoSelected,
    required this.onReceiptPhotoRemoved,
  });

  final int? purchaseAmount;
  final List<PurchaseMethod> purchaseMethods;
  final PurchaseMethod? selectedPurchaseMethod;
  final File? receiptPhoto;
  final ValueChanged<int?> onPurchaseAmountChanged;
  final ValueChanged<PurchaseMethod?> onPurchaseMethodSelected;
  final ValueChanged<File> onReceiptPhotoSelected;
  final VoidCallback onReceiptPhotoRemoved;

  @override
  Widget build(BuildContext context) {
    // 레거시 write.jsp:353 게이트 정합 — 금액/방법/영수증 중 하나라도 채우면
    // 구매 그룹이 활성화되어 금액·방법이 필수(*)로 승격된다.
    final hasPurchaseInfo = (purchaseAmount != null && purchaseAmount! > 0) ||
        selectedPurchaseMethod != null ||
        receiptPhoto != null;
    // 영수증은 개인카드(B)/현금(C) 일 때만 필수 (법인카드는 면제).
    final receiptRequired = hasPurchaseInfo &&
        (selectedPurchaseMethod?.code == 'B' ||
            selectedPurchaseMethod?.code == 'C');

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 구매 금액 입력 (구매 그룹 활성화 시 필수)
        _PurchaseAmountField(
          amount: purchaseAmount,
          isRequired: hasPurchaseInfo,
          onChanged: onPurchaseAmountChanged,
        ),

        // 구매 방법 선택 (구매 그룹 활성화 시 필수)
        _PurchaseMethodField(
          isRequired: hasPurchaseInfo,
          methods: purchaseMethods,
          selectedMethod: selectedPurchaseMethod,
          onSelected: onPurchaseMethodSelected,
        ),

        // 구매 영수증 사진 (개인카드/현금 선택 시 필수)
        ClaimPhotoField(
          label: '구매 영수증 사진 (최대 1장)',
          photo: receiptPhoto,
          onPhotoSelected: onReceiptPhotoSelected,
          onPhotoRemoved: onReceiptPhotoRemoved,
          isRequired: receiptRequired,
        ),
      ],
    );
  }
}

/// 구매 금액 입력 필드
class _PurchaseAmountField extends StatefulWidget {
  const _PurchaseAmountField({
    required this.amount,
    required this.isRequired,
    required this.onChanged,
  });

  final int? amount;
  final bool isRequired;
  final ValueChanged<int?> onChanged;

  @override
  State<_PurchaseAmountField> createState() => _PurchaseAmountFieldState();
}

class _PurchaseAmountFieldState extends State<_PurchaseAmountField> {
  late final TextEditingController _controller;

  String _textOf(int? amount) =>
      amount != null && amount > 0 ? amount.toString() : '';

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: _textOf(widget.amount));
  }

  @override
  void didUpdateWidget(covariant _PurchaseAmountField oldWidget) {
    super.didUpdateWidget(oldWidget);
    final newText = _textOf(widget.amount);
    if (newText != _controller.text) {
      _controller.value = TextEditingValue(
        text: newText,
        selection: TextSelection.collapsed(offset: newText.length),
      );
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '구매 금액',
      isRequired: widget.isRequired,
      trailing: const Text(
        '원',
        style: TextStyle(fontSize: 14, color: ClaimFormColors.unit),
      ),
      below: TextField(
        controller: _controller,
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
        decoration: const InputDecoration(
          isCollapsed: true,
          hintText: '숫자 입력',
          hintStyle: TextStyle(
            fontSize: 14,
            color: ClaimFormColors.placeholder,
          ),
          border: InputBorder.none,
        ),
        onChanged: (value) {
          if (value.isEmpty) {
            widget.onChanged(null);
          } else {
            widget.onChanged(int.tryParse(value));
          }
        },
      ),
    );
  }
}

/// 구매 방법 선택 필드
class _PurchaseMethodField extends StatelessWidget {
  const _PurchaseMethodField({
    required this.isRequired,
    required this.methods,
    required this.selectedMethod,
    required this.onSelected,
  });

  final bool isRequired;
  final List<PurchaseMethod> methods;
  final PurchaseMethod? selectedMethod;
  final ValueChanged<PurchaseMethod?> onSelected;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '구매 방법',
      isRequired: isRequired,
      onTap: () => _showMethodSelector(context),
      trailing: const ClaimRowChevron(),
      below: ClaimValueText(
        value: selectedMethod?.name,
        placeholder: '구매 방법 선택',
      ),
    );
  }

  Future<void> _showMethodSelector(BuildContext context) async {
    final selected = await showModalBottomSheet<PurchaseMethod?>(
      context: context,
      builder: (context) => _PurchaseMethodSheet(
        methods: methods,
        selectedMethod: selectedMethod,
      ),
    );

    if (selected != null) {
      onSelected(selected);
    }
  }
}

/// 구매 방법 선택 바텀시트
class _PurchaseMethodSheet extends StatelessWidget {
  const _PurchaseMethodSheet({
    required this.methods,
    required this.selectedMethod,
  });

  final List<PurchaseMethod> methods;
  final PurchaseMethod? selectedMethod;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 헤더
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              '구매 방법 선택',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ),
          const Divider(height: 1),

          // 목록
          Flexible(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: methods.length,
              itemBuilder: (context, index) {
                final method = methods[index];
                final isSelected = selectedMethod?.code == method.code;

                return ListTile(
                  title: Text(method.name),
                  trailing: isSelected
                      ? const Icon(Icons.check, color: Colors.blue)
                      : null,
                  onTap: () => Navigator.pop(context, method),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
