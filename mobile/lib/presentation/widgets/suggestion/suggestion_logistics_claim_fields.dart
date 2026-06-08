import 'package:flutter/material.dart';

import '../../../domain/entities/suggestion_form.dart';

/// 제안하기 필드 공용 라벨 (필수 항목은 빨간 `*` 표기 — 레거시 정합)
class SuggestionFieldLabel extends StatelessWidget {
  const SuggestionFieldLabel({
    super.key,
    required this.text,
    this.required = false,
  });

  final String text;
  final bool required;

  @override
  Widget build(BuildContext context) {
    return RichText(
      text: TextSpan(
        text: text,
        style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: Colors.black87,
        ),
        children: required
            ? const [
                TextSpan(
                  text: ' *',
                  style: TextStyle(color: Colors.red),
                ),
              ]
            : null,
      ),
    );
  }
}

/// 거래처 선택 필드 (물류 클레임 필수)
///
/// 레거시는 담당 거래처 select2 드롭다운. 신규에서는 거래처 선택 화면(별 스펙)
/// 진입용 탭 박스로 구성한다.
class SuggestionAccountField extends StatelessWidget {
  const SuggestionAccountField({
    super.key,
    required this.accountName,
    required this.onSelect,
  });

  final String? accountName;
  final VoidCallback onSelect;

  @override
  Widget build(BuildContext context) {
    final selected = accountName != null && accountName!.isNotEmpty;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SuggestionFieldLabel(text: '거래처', required: true),
        const SizedBox(height: 8),
        InkWell(
          onTap: onSelect,
          borderRadius: BorderRadius.circular(4),
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
                    selected ? accountName! : '거래처 선택',
                    style: TextStyle(
                      fontSize: 14,
                      color: selected ? Colors.black87 : Colors.grey.shade600,
                      fontWeight: selected ? FontWeight.w500 : FontWeight.normal,
                    ),
                  ),
                ),
                Icon(Icons.expand_more, size: 20, color: Colors.grey.shade600),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// 클레임 항목 선택 필드 (물류 클레임 필수)
///
/// 레거시 suggestWrite.jsp 하드코딩 6 옵션 드롭다운 ([kSuggestionClaimTypeOptions]).
class SuggestionClaimTypeField extends StatelessWidget {
  const SuggestionClaimTypeField({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final String? value;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    // 임의 입력값(레거시 임시저장 복원 등)이 옵션에 없을 수 있으므로 가드
    final selected = (value != null && kSuggestionClaimTypeOptions.contains(value))
        ? value
        : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SuggestionFieldLabel(text: '클레임 항목', required: true),
        const SizedBox(height: 8),
        DropdownButtonFormField<String>(
          initialValue: selected,
          isExpanded: true,
          hint: Text(
            '항목 선택',
            style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
          ),
          icon: Icon(Icons.expand_more, color: Colors.grey.shade600),
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            isDense: true,
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 14),
          ),
          items: kSuggestionClaimTypeOptions
              .map(
                (option) => DropdownMenuItem<String>(
                  value: option,
                  child: Text(
                    option,
                    style: const TextStyle(fontSize: 14),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              )
              .toList(),
          onChanged: (v) {
            if (v != null) onChanged(v);
          },
        ),
      ],
    );
  }
}

/// 물류 클레임 발생일자 필드 (물류 클레임 필수)
class SuggestionClaimDateField extends StatelessWidget {
  const SuggestionClaimDateField({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final DateTime? value;
  final ValueChanged<DateTime> onChanged;

  @override
  Widget build(BuildContext context) {
    final label = value == null
        ? '발생일자 선택'
        : '${value!.year}-${value!.month.toString().padLeft(2, '0')}-${value!.day.toString().padLeft(2, '0')}';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SuggestionFieldLabel(text: '물류 클레임 발생일자', required: true),
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
          borderRadius: BorderRadius.circular(4),
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
                      color: value != null ? Colors.black87 : Colors.grey.shade600,
                      fontWeight: value != null ? FontWeight.w500 : FontWeight.normal,
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

/// 차량 번호 입력 필드 (물류 클레임 선택 — 선택 입력)
class SuggestionCarNumberField extends StatefulWidget {
  const SuggestionCarNumberField({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final String? value;
  final ValueChanged<String> onChanged;

  @override
  State<SuggestionCarNumberField> createState() =>
      _SuggestionCarNumberFieldState();
}

class _SuggestionCarNumberFieldState extends State<SuggestionCarNumberField> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.value ?? '');
  }

  @override
  void didUpdateWidget(SuggestionCarNumberField oldWidget) {
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
        const SuggestionFieldLabel(text: '차량 번호'),
        const SizedBox(height: 8),
        TextField(
          controller: _controller,
          maxLength: 20,
          decoration: const InputDecoration(
            hintText: '내용 입력',
            border: OutlineInputBorder(),
            isDense: true,
            counterText: '',
          ),
          onChanged: widget.onChanged,
        ),
      ],
    );
  }
}
