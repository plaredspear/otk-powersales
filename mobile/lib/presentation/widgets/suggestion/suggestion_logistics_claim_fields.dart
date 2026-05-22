import 'package:flutter/material.dart';

/// 물류 클레임 카테고리 분기 입력 필드 (6 필드)
///
/// 거래처 / 클레임 항목 / 클레임 일자 / 차량번호 / 물류책임 / 중복 제안번호.
/// 거래처 선택 화면 / 물류책임 드롭다운은 별 스펙에서 lookup API 연결 시
/// onSelectAccount / onPickLogisticsResponsibility 콜백을 통해 결선된다.
class SuggestionLogisticsClaimFields extends StatelessWidget {
  const SuggestionLogisticsClaimFields({
    super.key,
    required this.accountName,
    required this.claimType,
    required this.claimDate,
    required this.carNumber,
    required this.logisticsResponsibility,
    required this.duplicateProposalNum,
    required this.onSelectAccount,
    required this.onClaimTypeChanged,
    required this.onClaimDateChanged,
    required this.onCarNumberChanged,
    required this.onLogisticsResponsibilityChanged,
    required this.onDuplicateProposalNumChanged,
  });

  final String? accountName;
  final String? claimType;
  final DateTime? claimDate;
  final String? carNumber;
  final String? logisticsResponsibility;
  final String? duplicateProposalNum;

  final VoidCallback onSelectAccount;
  final ValueChanged<String> onClaimTypeChanged;
  final ValueChanged<DateTime> onClaimDateChanged;
  final ValueChanged<String> onCarNumberChanged;
  final ValueChanged<String> onLogisticsResponsibilityChanged;
  final ValueChanged<String> onDuplicateProposalNumChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '물류 클레임 상세',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),

        _AccountField(
          accountName: accountName,
          onSelect: onSelectAccount,
        ),
        const SizedBox(height: 12),

        _TextField(
          label: '클레임 항목 *',
          hint: '예) 파손, 누락, 오배송',
          value: claimType,
          maxLength: 200,
          onChanged: onClaimTypeChanged,
        ),
        const SizedBox(height: 12),

        _ClaimDateField(
          value: claimDate,
          onChanged: onClaimDateChanged,
        ),
        const SizedBox(height: 12),

        _TextField(
          label: '차량번호',
          hint: '예) 12가1234',
          value: carNumber,
          maxLength: 20,
          onChanged: onCarNumberChanged,
        ),
        const SizedBox(height: 12),

        _TextField(
          label: '물류책임',
          hint: '예) 본사, 물류센터, 운송사',
          value: logisticsResponsibility,
          maxLength: 20,
          onChanged: onLogisticsResponsibilityChanged,
        ),
        const SizedBox(height: 12),

        _TextField(
          label: '중복 제안번호',
          hint: '연관된 기존 제안번호 (선택)',
          value: duplicateProposalNum,
          maxLength: 255,
          onChanged: onDuplicateProposalNumChanged,
        ),
      ],
    );
  }
}

class _AccountField extends StatelessWidget {
  const _AccountField({
    required this.accountName,
    required this.onSelect,
  });

  final String? accountName;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Text(
              '거래처 *',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
            const Spacer(),
            OutlinedButton.icon(
              onPressed: onSelect,
              icon: const Icon(Icons.search, size: 18),
              label: const Text('선택'),
              style: OutlinedButton.styleFrom(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(0, 36),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.white,
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(4),
          ),
          child: Text(
            accountName ?? '거래처 선택',
            style: TextStyle(
              fontSize: 14,
              color: accountName != null
                  ? Colors.black87
                  : Colors.grey.shade600,
              fontWeight: accountName != null
                  ? FontWeight.w500
                  : FontWeight.normal,
            ),
          ),
        ),
      ],
    );
  }
}

class _ClaimDateField extends StatelessWidget {
  const _ClaimDateField({
    required this.value,
    required this.onChanged,
  });

  final DateTime? value;
  final ValueChanged<DateTime> onChanged;

  @override
  Widget build(BuildContext context) {
    final label = value == null
        ? '클레임 일자 선택'
        : '${value!.year}-${value!.month.toString().padLeft(2, '0')}-${value!.day.toString().padLeft(2, '0')}';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '클레임 일자 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        InkWell(
          onTap: () async {
            final now = DateTime.now();
            final picked = await showDatePicker(
              context: context,
              initialDate: value ?? now,
              firstDate: DateTime(now.year - 5),
              lastDate: now,
            );
            if (picked != null) {
              onChanged(picked);
            }
          },
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    label,
                    style: TextStyle(
                      fontSize: 14,
                      color: value != null
                          ? Colors.black87
                          : Colors.grey.shade600,
                      fontWeight: value != null
                          ? FontWeight.w500
                          : FontWeight.normal,
                    ),
                  ),
                ),
                const Icon(Icons.calendar_today, size: 18, color: Colors.grey),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _TextField extends StatefulWidget {
  const _TextField({
    required this.label,
    required this.hint,
    required this.value,
    required this.maxLength,
    required this.onChanged,
  });

  final String label;
  final String hint;
  final String? value;
  final int maxLength;
  final ValueChanged<String> onChanged;

  @override
  State<_TextField> createState() => _TextFieldState();
}

class _TextFieldState extends State<_TextField> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.value ?? '');
  }

  @override
  void didUpdateWidget(_TextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    final next = widget.value ?? '';
    if (_controller.text != next) {
      _controller.value = TextEditingValue(
        text: next,
        selection: TextSelection.collapsed(offset: next.length),
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _controller,
          maxLength: widget.maxLength,
          decoration: InputDecoration(
            hintText: widget.hint,
            border: const OutlineInputBorder(),
            isDense: true,
            counterText: '',
          ),
          onChanged: widget.onChanged,
        ),
      ],
    );
  }
}
