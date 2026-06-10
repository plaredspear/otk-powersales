import 'package:flutter/material.dart';

import '../../../domain/entities/suggestion_form.dart';

// ============================================================
// 레거시 suggestWrite.jsp 정합 디자인 토큰
//
// 레거시 제안하기 화면은 박스형 입력이 아닌 "플랫 리스트" 구조다 —
// 각 필드는 풀폭 1px 하단 구분선으로 구획되고, 라벨 아래 값/플레이스홀더가
// 평면 텍스트로 놓이며, 액션은 pill(stadium) 버튼으로 라벨 우측에 붙는다.
// ============================================================
const Color kSuggestionLabelColor = Color(0xFF222222);
const Color kSuggestionValueColor = Color(0xFF222222);
const Color kSuggestionPlaceholderColor = Color(0xFF9E9E9E);
const Color kSuggestionDividerColor = Color(0xFFE5E5E5);
const Color kSuggestionGuideColor = Color(0xFFE53935);
const Color kSuggestionPillBorderColor = Color(0xFFCCCCCC);
const Color kSuggestionPillTextColor = Color(0xFF333333);

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
          fontSize: 15,
          fontWeight: FontWeight.w700,
          color: kSuggestionLabelColor,
        ),
        children: required
            ? const [
                TextSpan(
                  text: ' *',
                  style: TextStyle(
                    color: kSuggestionGuideColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ]
            : null,
      ),
    );
  }
}

/// 레거시 정합 — 한 줄(필드) 스캐폴드.
///
/// 풀폭 하단 1px 구분선 + 좌우 16 인셋. 라벨 행(좌: 라벨 / 우: [trailing] 액션) →
/// 선택적 [guideText](빨간 안내) → 선택적 [child](값/입력) 순으로 쌓는다.
class SuggestionFieldRow extends StatelessWidget {
  const SuggestionFieldRow({
    super.key,
    required this.label,
    this.required = false,
    this.trailing,
    this.guideText,
    this.child,
  });

  final String label;
  final bool required;
  final Widget? trailing;
  final String? guideText;
  final Widget? child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: kSuggestionDividerColor)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: SuggestionFieldLabel(text: label, required: required),
              ),
              ?trailing,
            ],
          ),
          if (guideText != null) ...[
            const SizedBox(height: 6),
            Text(
              guideText!,
              style: const TextStyle(fontSize: 13, color: kSuggestionGuideColor),
            ),
          ],
          if (child != null) ...[
            const SizedBox(height: 10),
            child!,
          ],
        ],
      ),
    );
  }
}

/// 레거시 정합 — 라벨 우측 pill(stadium) 버튼 (바코드 / 선택 / 사진 선택)
class SuggestionPillButton extends StatelessWidget {
  const SuggestionPillButton({
    super.key,
    required this.icon,
    required this.label,
    this.onPressed,
  });

  final IconData icon;
  final String label;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(label, style: const TextStyle(fontSize: 13)),
      style: OutlinedButton.styleFrom(
        foregroundColor: kSuggestionPillTextColor,
        side: const BorderSide(color: kSuggestionPillBorderColor),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        minimumSize: const Size(0, 34),
        visualDensity: VisualDensity.compact,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      ),
    );
  }
}

/// 레거시 정합 — 탭하여 값을 고르는 평면 행 (거래처 / 클레임 항목 / 발생일자).
///
/// 박스 없이 좌측 값(또는 플레이스홀더) + 우측 아이콘(chevron/calendar)만 노출한다.
class SuggestionSelectValue extends StatelessWidget {
  const SuggestionSelectValue({
    super.key,
    required this.text,
    required this.placeholder,
    required this.icon,
    required this.onTap,
  });

  final String? text;
  final String placeholder;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final filled = text != null && text!.isNotEmpty;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(
          children: [
            Expanded(
              child: Text(
                filled ? text! : placeholder,
                style: TextStyle(
                  fontSize: 15,
                  color: filled
                      ? kSuggestionValueColor
                      : kSuggestionPlaceholderColor,
                ),
              ),
            ),
            Icon(icon, size: 20, color: kSuggestionPlaceholderColor),
          ],
        ),
      ),
    );
  }
}

/// 레거시 정합 — 박스 없는(borderless) 평면 텍스트 입력 (제목 / 내용 / 차량번호).
class SuggestionBorderlessField extends StatelessWidget {
  const SuggestionBorderlessField({
    super.key,
    required this.controller,
    required this.hint,
    this.maxLength,
    this.maxLines = 1,
    this.onChanged,
  });

  final TextEditingController controller;
  final String hint;
  final int? maxLength;
  final int maxLines;
  final ValueChanged<String>? onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      maxLength: maxLength,
      maxLines: maxLines,
      style: const TextStyle(fontSize: 15, color: kSuggestionValueColor),
      decoration: InputDecoration(
        isDense: true,
        contentPadding: EdgeInsets.zero,
        border: InputBorder.none,
        enabledBorder: InputBorder.none,
        focusedBorder: InputBorder.none,
        counterText: '',
        hintText: hint,
        hintStyle: const TextStyle(
          fontSize: 15,
          color: kSuggestionPlaceholderColor,
        ),
      ),
      onChanged: onChanged,
    );
  }
}

/// 거래처 선택 필드 (물류 클레임 필수)
///
/// 레거시는 담당 거래처 select2 드롭다운. 신규에서는 거래처 선택 화면(별 스펙)
/// 진입용 평면 선택 행으로 구성한다.
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
    return SuggestionFieldRow(
      label: '거래처',
      required: true,
      child: SuggestionSelectValue(
        text: accountName,
        placeholder: '거래처 선택',
        icon: Icons.expand_more,
        onTap: onSelect,
      ),
    );
  }
}

/// 클레임 항목 선택 필드 (물류 클레임 필수)
///
/// 레거시 suggestWrite.jsp 하드코딩 6 옵션 ([kSuggestionClaimTypeOptions]).
/// 평면 선택 행을 탭하면 바텀시트로 옵션을 고른다.
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
    final selected =
        (value != null && kSuggestionClaimTypeOptions.contains(value))
            ? value
            : null;
    return SuggestionFieldRow(
      label: '클레임 항목',
      required: true,
      child: SuggestionSelectValue(
        text: selected,
        placeholder: '항목 선택',
        icon: Icons.expand_more,
        onTap: () => _showPicker(context),
      ),
    );
  }

  Future<void> _showPicker(BuildContext context) async {
    final picked = await showModalBottomSheet<String>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final option in kSuggestionClaimTypeOptions)
              ListTile(
                title: Text(option, style: const TextStyle(fontSize: 15)),
                trailing: option == value
                    ? const Icon(Icons.check, color: kSuggestionPillTextColor)
                    : null,
                onTap: () => Navigator.of(ctx).pop(option),
              ),
          ],
        ),
      ),
    );
    if (picked != null) onChanged(picked);
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
        ? null
        : '${value!.year}-${value!.month.toString().padLeft(2, '0')}-${value!.day.toString().padLeft(2, '0')}';
    return SuggestionFieldRow(
      label: '물류 클레임 발생일자',
      required: true,
      child: SuggestionSelectValue(
        text: label,
        placeholder: '발생일자 선택',
        icon: Icons.calendar_today,
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
      ),
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
    return SuggestionFieldRow(
      label: '차량 번호',
      child: SuggestionBorderlessField(
        controller: _controller,
        hint: '내용 입력',
        maxLength: 20,
        onChanged: widget.onChanged,
      ),
    );
  }
}
