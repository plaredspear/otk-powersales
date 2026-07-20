import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/attendance_status.dart';

/// 출근등록 현황 팝업 위젯
class AttendanceStatusPopup extends StatelessWidget {
  final List<AttendanceStatus> statusList;
  final int totalCount;
  final int registeredCount;

  /// 기준 날짜 (yyyy-MM-dd). 레거시 팝업 헤더의 "2020년 08월 20일 (목)" 대응.
  final String? currentDate;

  const AttendanceStatusPopup({
    super.key,
    required this.statusList,
    required this.totalCount,
    required this.registeredCount,
    this.currentDate,
  });

  /// BottomSheet로 현황 팝업 표시
  static void show(
    BuildContext context, {
    required List<AttendanceStatus> statusList,
    required int totalCount,
    required int registeredCount,
    String? currentDate,
  }) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => AttendanceStatusPopup(
        statusList: statusList,
        totalCount: totalCount,
        registeredCount: registeredCount,
        currentDate: currentDate,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.6,
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 핸들 바
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.divider,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 16),

          // 제목 + 카운터
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  _formatTitle(),
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.otokiBlue.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '$registeredCount / $totalCount',
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: AppColors.otokiBlue,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(height: 1),
          const SizedBox(height: 8),

          // 상태 리스트 (빈 경우에도 안내를 명시적으로 노출)
          Flexible(
            child: statusList.isEmpty
                ? const Padding(
                    padding: EdgeInsets.symmetric(vertical: 32),
                    child: Text(
                      '오늘 등록된 근무지가 없습니다.',
                      style: TextStyle(
                        fontSize: 14,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  )
                : ListView.separated(
                    shrinkWrap: true,
                    itemCount: statusList.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final status = statusList[index];
                      return _buildStatusItem(status);
                    },
                  ),
          ),

          const SizedBox(height: 16),

          // 닫기 버튼
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                side: const BorderSide(color: AppColors.border),
              ),
              child: const Text(
                '닫기',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 팝업 제목: 기준일이 있으면 레거시처럼 "yyyy년 MM월 dd일 (요일)"
  String _formatTitle() {
    final date = currentDate;
    if (date == null) return '출근등록 현황';
    final parsed = DateTime.tryParse(date);
    if (parsed == null) return '출근등록 현황';
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final month = parsed.month.toString().padLeft(2, '0');
    final day = parsed.day.toString().padLeft(2, '0');
    return '${parsed.year}년 $month월 $day일 (${weekdays[parsed.weekday - 1]})';
  }

  /// 근무지 1행
  ///
  /// 레거시 home.jsp `#popPlace3` 정합:
  /// - 대기: 거래처명·근태 모두 회색(`.state_waiting`), 근태는 "대기"
  /// - 완료: 거래처명 기본색, 근태는 초록(`.state_finish`) "완료".
  ///   근무유형4(workingCategory4)가 있을 때만 둘째 줄에 "(상온)" 등을 덧붙인다
  ///   (행사 스케줄만 값이 있고 진열은 null).
  Widget _buildStatusItem(AttendanceStatus status) {
    final isCompleted = status.isCompleted;
    final secondWorkType = status.secondWorkType;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: isCompleted
            ? AppColors.success.withValues(alpha: 0.04)
            : AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          Icon(
            isCompleted ? Icons.check_circle : Icons.schedule,
            size: 20,
            color: isCompleted ? AppColors.success : AppColors.textTertiary,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              status.accountName,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: isCompleted
                    ? AppColors.textPrimary
                    : AppColors.textSecondary,
              ),
            ),
          ),
          // 근태: 완료(초록, 근무유형4가 있으면 2줄) / 대기(회색)
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                isCompleted ? '완료' : '대기',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color:
                      isCompleted ? AppColors.success : AppColors.textTertiary,
                ),
              ),
              if (isCompleted && secondWorkType != null)
                Text(
                  '($secondWorkType)',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w500,
                    color: AppColors.success,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}
