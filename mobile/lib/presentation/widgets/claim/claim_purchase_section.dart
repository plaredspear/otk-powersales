import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../domain/entities/claim_code.dart';
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
    final hasPurchaseAmount =
        purchaseAmount != null && purchaseAmount! > 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 섹션 구분선
        const _SectionDivider(),
        const SizedBox(height: 16),

        // 구매 금액 입력
        _PurchaseAmountField(
          amount: purchaseAmount,
          onChanged: onPurchaseAmountChanged,
        ),
        const SizedBox(height: 16),

        // 구매 방법 선택 (구매금액 입력 시 필수)
        _PurchaseMethodField(
          isRequired: hasPurchaseAmount,
          methods: purchaseMethods,
          selectedMethod: selectedPurchaseMethod,
          onSelected: onPurchaseMethodSelected,
        ),
        const SizedBox(height: 16),

        // 구매 영수증 사진 (구매금액 입력 시 필수)
        ClaimPhotoField(
          label: '구매 영수증 사진',
          photo: receiptPhoto,
          onPhotoSelected: onReceiptPhotoSelected,
          onPhotoRemoved: onReceiptPhotoRemoved,
          isRequired: hasPurchaseAmount,
        ),
      ],
    );
  }
}

/// 섹션 구분선
class _SectionDivider extends StatelessWidget {
  const _SectionDivider();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Divider(color: Colors.grey.shade400, thickness: 1),
        ),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            '구매 정보 (선택)',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
              color: Colors.black54,
            ),
          ),
        ),
        Expanded(
          child: Divider(color: Colors.grey.shade400, thickness: 1),
        ),
      ],
    );
  }
}

/// 구매 금액 입력 필드
class _PurchaseAmountField extends StatelessWidget {
  const _PurchaseAmountField({
    required this.amount,
    required this.onChanged,
  });

  final int? amount;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    final controller = TextEditingController(
      text: amount != null && amount! > 0 ? amount.toString() : '',
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '구매 금액',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: const InputDecoration(
            hintText: '금액 입력',
            suffixText: '원',
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          ),
          onChanged: (value) {
            if (value.isEmpty) {
              onChanged(null);
            } else {
              final parsed = int.tryParse(value);
              onChanged(parsed);
            }
          },
        ),
      ],
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          isRequired ? '구매 방법 *' : '구매 방법',
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(4),
            side: BorderSide(color: Colors.grey.shade300),
          ),
          title: Text(
            selectedMethod?.name ?? '구매 방법 선택',
            style: TextStyle(
              fontSize: 14,
              color: selectedMethod == null
                  ? Colors.grey.shade600
                  : Colors.black87,
            ),
          ),
          trailing: const Icon(Icons.arrow_forward_ios, size: 16),
          onTap: () => _showMethodSelector(context),
        ),
      ],
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
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
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
