import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../data/models/safety_check_status_model.dart';

/// 안전점검 현황 여사원 카드 위젯
class SafetyCheckMemberCard extends StatelessWidget {
  final MemberStatusModel member;
  final bool isExpanded;
  final VoidCallback? onTap;

  const SafetyCheckMemberCard({
    super.key,
    required this.member,
    this.isExpanded = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: member.submitted ? onTap : null,
        borderRadius: AppSpacing.cardBorderRadius,
        child: Container(
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              if (isExpanded && member.submitted) ...[
                const Divider(height: 1),
                _buildEquipmentSection(),
                if (member.precautions != null &&
                    member.precautions!.isNotEmpty) ...[
                  const Divider(height: 1),
                  _buildPrecautionSection(),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: AppSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '${member.employeeName} (${member.employeeCode})',
                  style: AppTypography.headlineSmall,
                ),
              ),
              Icon(
                member.submitted
                    ? Icons.check_circle
                    : Icons.check_circle_outline,
                color: member.submitted ? AppColors.success : AppColors.border,
                size: AppSpacing.iconSize,
              ),
            ],
          ),
          if (member.accountName != null) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              member.accountName!,
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.xs),
          if (member.submitted) ...[
            Text(
              '제출시간: ${_formatTime(member.submittedAt)}',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '예 ${member.yesCount} / 해당없음 ${member.noCount}',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ] else
            Text(
              '미제출',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.error,
                fontWeight: FontWeight.w600,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildEquipmentSection() {
    return Padding(
      padding: AppSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '안전장비 착용',
            style: AppTypography.labelLarge.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          ...member.equipments.map((e) => Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.xs),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SizedBox(
                      width: 20,
                      child: Text(
                        '${e.seqNum})',
                        style: AppTypography.bodySmall,
                      ),
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    Expanded(
                      child: Text(e.label, style: AppTypography.bodySmall),
                    ),
                    Text(
                      ': ${e.answer}',
                      style: AppTypography.bodySmall.copyWith(
                        color: e.answer == '예'
                            ? AppColors.success
                            : AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildPrecautionSection() {
    final items = member.precautions!.split(';').where((s) => s.trim().isNotEmpty).toList();
    return Padding(
      padding: AppSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '예방사항 (${items.length}건)',
            style: AppTypography.labelLarge.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          ...items.map((item) => Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.xs),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('· ', style: TextStyle(fontSize: 14)),
                    Expanded(
                      child: Text(
                        item.trim(),
                        style: AppTypography.bodySmall,
                      ),
                    ),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  String _formatTime(String? dateTimeStr) {
    if (dateTimeStr == null) return '';
    try {
      final dt = DateTime.parse(dateTimeStr);
      return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    } catch (_) {
      return '';
    }
  }
}
