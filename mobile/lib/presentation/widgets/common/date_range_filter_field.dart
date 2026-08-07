import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import 'range_calendar_picker.dart';

/// 기간(시작일~종료일) 조회 인라인 선택 필드.
///
/// 주문 현황(레거시 `order/list.jsp`) 납기일 UI 정합 — `[라벨]  시작 ~ 종료` 한 줄로
/// 표시하고, 탭하면 클레임 현황과 동일한 범위 달력 모달([showRangeCalendar])을 연다.
/// 조회 가능 기간([firstDate]/[lastDate])과 최대 범위([maxRangeDays])는 화면별 조건에
/// 맞게 지정한다([maxRangeDays] 가 null 이면 범위 일수 제한 없음).
///
/// 라벨이 길어(예: "클레임 발생일") 한 줄에 `라벨 + 기간 + 달력 아이콘` 이 모두 들어가지
/// 않으면 라벨을 기간 위로 올린 2단 배치로 자동 전환한다. 날짜는 잘리면 의미를 잃으므로
/// ellipsis 로 자르지 않고 배치를 바꾸는 쪽을 택한다(필드 높이 [fieldHeight] 는 유지).
class DateRangeFilterField extends StatelessWidget {
  /// 앞에 붙는 라벨(예: 기간, 점검일).
  final String label;

  /// 현재 시작일.
  final DateTime startDate;

  /// 현재 종료일.
  final DateTime endDate;

  /// 선택 가능한 최초 날짜(null 이면 제한 없음).
  final DateTime? firstDate;

  /// 선택 가능한 마지막 날짜(null 이면 제한 없음).
  final DateTime? lastDate;

  /// 최대 선택 범위 일수(null 이면 제한 없음).
  final int? maxRangeDays;

  /// 범위 선택 완료 콜백.
  final void Function(DateTime start, DateTime end) onChanged;

  const DateRangeFilterField({
    super.key,
    required this.label,
    required this.startDate,
    required this.endDate,
    required this.onChanged,
    this.firstDate,
    this.lastDate,
    this.maxRangeDays,
  });

  static final _dateFormat = DateFormat('yyyy-MM-dd');

  /// 모든 화면에서 통일하는 기간 필드 높이.
  /// 레거시 search_top 행 높이(50)와 정합하며 충분한 터치 타깃을 제공한다.
  static const double fieldHeight = 50;

  Future<void> _pick(BuildContext context) async {
    final picked = await showRangeCalendar(
      context,
      initialStart: startDate,
      initialEnd: endDate,
      firstDate: firstDate,
      lastDate: lastDate,
      maxRangeDays: maxRangeDays,
    );
    if (picked != null) {
      onChanged(picked.start, picked.end);
    }
  }

  /// 달력 아이콘 크기(2단 전환 판정 시 폭 계산에 사용).
  static const double _iconSize = 18;

  double _textWidth(BuildContext context, String text, TextStyle style,
      TextScaler scaler) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textDirection: Directionality.of(context),
      textScaler: scaler,
      maxLines: 1,
    )..layout();
    return painter.width;
  }

  @override
  Widget build(BuildContext context) {
    final scaler = MediaQuery.textScalerOf(context);
    final labelStyle =
        AppTypography.bodyMedium.copyWith(color: AppColors.textSecondary);
    final valueStyle =
        AppTypography.bodyMedium.copyWith(color: AppColors.textPrimary);
    final rangeText =
        '${_dateFormat.format(startDate)} ~ ${_dateFormat.format(endDate)}';

    // 부모 배경과 무관하게 어디서나 동일하게 보이도록 컴포넌트가 배경/보더/높이를
    // 직접 소유한다(흰색 입력 필드형, 고정 높이 [fieldHeight]).
    return SizedBox(
      height: fieldHeight,
      child: Material(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: InkWell(
          onTap: () => _pick(context),
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              border: Border.all(color: AppColors.border),
            ),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final oneLineWidth =
                    _textWidth(context, label, labelStyle, scaler) +
                        AppSpacing.md +
                        _textWidth(context, rangeText, valueStyle, scaler) +
                        AppSpacing.sm +
                        _iconSize;
                final stacked = oneLineWidth > constraints.maxWidth;

                return Row(
                  children: [
                    Expanded(
                      child: stacked
                          // 큰 글꼴 설정에서 2단 높이가 [fieldHeight] 를 넘지 않도록
                          // 축소만 허용(확대 없음).
                          ? FittedBox(
                              fit: BoxFit.scaleDown,
                              alignment: Alignment.centerLeft,
                              child: Column(
                                mainAxisSize: MainAxisSize.min,
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    label,
                                    style: AppTypography.bodySmall.copyWith(
                                        color: AppColors.textSecondary),
                                    maxLines: 1,
                                  ),
                                  Text(
                                    rangeText,
                                    style: valueStyle,
                                    maxLines: 1,
                                  ),
                                ],
                              ),
                            )
                          : Row(
                              children: [
                                Text(label, style: labelStyle),
                                const SizedBox(width: AppSpacing.md),
                                Expanded(
                                  child: Text(
                                    rangeText,
                                    style: valueStyle,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    const Icon(
                      Icons.calendar_today,
                      size: _iconSize,
                      color: AppColors.textTertiary,
                    ),
                  ],
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}
