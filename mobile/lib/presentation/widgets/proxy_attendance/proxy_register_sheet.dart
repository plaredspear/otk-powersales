import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/leader_daily_status.dart';

/// 대리출근 등록 가능 판정 결과 (레거시 mngDaily `btn-add-sch` 동등).
enum ProxyEligibility { ok, notToday, afterCutoff }

/// 선택 날짜·현재 시각으로 대리출근 가능 여부 판정.
///
/// 당일 + 17시 이전에만 등록 가능. 조장/AccountViewAll 대리출근 공통 규칙.
ProxyEligibility proxyEligibility(DateTime selectedDate, DateTime now) {
  final isToday = selectedDate.year == now.year &&
      selectedDate.month == now.month &&
      selectedDate.day == now.day;
  if (!isToday) return ProxyEligibility.notToday;
  if (now.hour >= 17) return ProxyEligibility.afterCutoff;
  return ProxyEligibility.ok;
}

extension ProxyEligibilityX on ProxyEligibility {
  bool get canRegister => this == ProxyEligibility.ok;

  /// 불가 사유 (레거시 alert 문구 정합). ok 면 null.
  String? get reason {
    switch (this) {
      case ProxyEligibility.ok:
        return null;
      case ProxyEligibility.notToday:
        return '당일 일정만 대리출근 등록할 수 있습니다.';
      case ProxyEligibility.afterCutoff:
        return '오후 5시 이후에는 대리출근 등록할 수 없습니다.';
    }
  }
}

/// 대리출근 등록 시트 — 해당 여사원의 미등록 거래처를 선택해 일괄 등록.
///
/// 레거시 mngDaily addSchedule(체크박스 목록) 동등. [employeeName] + 미등록 [accounts]
/// (호출부에서 미등록만 필터해 전달) 를 받아, 선택된 [LeaderDailyWorker] 목록을
/// `Navigator.pop` 으로 반환한다. 기본 전체 선택.
class ProxyRegisterSheet extends StatefulWidget {
  final String employeeName;
  final List<LeaderDailyWorker> accounts;

  const ProxyRegisterSheet({
    super.key,
    required this.employeeName,
    required this.accounts,
  });

  @override
  State<ProxyRegisterSheet> createState() => _ProxyRegisterSheetState();
}

class _ProxyRegisterSheetState extends State<ProxyRegisterSheet> {
  late final Set<int> _selected; // accounts 인덱스 집합

  @override
  void initState() {
    super.initState();
    // 기본 전체 선택 (레거시 addSchedule 미등록 항목 일괄).
    _selected = {for (var i = 0; i < widget.accounts.length; i++) i};
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${widget.employeeName} 대리출근 등록',
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '등록할 거래처를 선택하세요. 안전점검 미완료 거래처는 등록되지 않습니다.',
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.sm),
            Flexible(
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: widget.accounts.length,
                itemBuilder: (context, i) {
                  final a = widget.accounts[i];
                  final checked = _selected.contains(i);
                  return CheckboxListTile(
                    value: checked,
                    activeColor: AppColors.secondary,
                    contentPadding: EdgeInsets.zero,
                    controlAffinity: ListTileControlAffinity.leading,
                    title: Text(
                      a.accountName,
                      style: AppTypography.bodyMedium
                          .copyWith(color: AppColors.textPrimary),
                    ),
                    subtitle: a.workCategoryLabel.isEmpty
                        ? null
                        : Text(
                            a.workCategoryLabel,
                            style: AppTypography.bodySmall
                                .copyWith(color: AppColors.textSecondary),
                          ),
                    onChanged: (v) => setState(() {
                      if (v == true) {
                        _selected.add(i);
                      } else {
                        _selected.remove(i);
                      }
                    }),
                  );
                },
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.secondary,
                  foregroundColor: AppColors.white,
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
                ),
                onPressed: _selected.isEmpty
                    ? null
                    : () => Navigator.of(context).pop(
                          [for (final i in _selected) widget.accounts[i]],
                        ),
                child: Text('등록 (${_selected.length})'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
