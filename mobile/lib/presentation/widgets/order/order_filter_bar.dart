import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order.dart';

/// 주문 필터 바 위젯
///
/// 거래처 드롭다운, 상태 드롭다운, 납기일 범위, 검색 버튼을 포함합니다.
class OrderFilterBar extends StatelessWidget {
  /// 거래처 목록 (id -> name)
  final Map<int, String> clients;

  /// 선택된 거래처 ID
  final int? selectedClientId;

  /// 선택된 승인상태 코드
  final String? selectedStatus;

  /// 납기일 시작
  final String? deliveryDateFrom;

  /// 납기일 종료
  final String? deliveryDateTo;

  /// 거래처 선택 콜백
  final void Function(int? clientId, String? clientName) onClientChanged;

  /// 상태 선택 콜백
  final ValueChanged<String?> onStatusChanged;

  /// 납기일 범위 선택 콜백
  final void Function(String? from, String? to) onDateRangeChanged;

  /// 검색 버튼 콜백
  final VoidCallback onSearch;

  const OrderFilterBar({
    super.key,
    required this.clients,
    this.selectedClientId,
    this.selectedStatus,
    this.deliveryDateFrom,
    this.deliveryDateTo,
    required this.onClientChanged,
    required this.onStatusChanged,
    required this.onDateRangeChanged,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      color: AppColors.surface,
      child: Column(
        children: [
          // 1행: 거래처 + 상태 드롭다운
          Row(
            children: [
              // 거래처 드롭다운
              Expanded(
                child: _buildClientDropdown(context),
              ),
              const SizedBox(width: AppSpacing.sm),
              // 상태 드롭다운
              Expanded(
                child: _buildStatusDropdown(context),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          // 2행: 납기일 범위 + 검색 버튼
          Row(
            children: [
              Expanded(
                child: _buildDateRangeSelector(context),
              ),
              const SizedBox(width: AppSpacing.sm),
              SizedBox(
                height: 40,
                child: ElevatedButton(
                  onPressed: onSearch,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.otokiBlue,
                    foregroundColor: AppColors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusMd),
                    ),
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.lg,
                    ),
                  ),
                  child: const Text('검색'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildClientDropdown(BuildContext context) {
    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<int?>(
          value: selectedClientId,
          isExpanded: true,
          icon: const Icon(Icons.arrow_drop_down, size: 20),
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textPrimary,
          ),
          hint: Text(
            '거래처 전체',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          items: [
            DropdownMenuItem<int?>(
              value: null,
              child: Text(
                '거래처 전체',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
            ...clients.entries.map((entry) {
              return DropdownMenuItem<int?>(
                value: entry.key,
                child: Text(
                  entry.value,
                  style: AppTypography.bodySmall,
                  overflow: TextOverflow.ellipsis,
                ),
              );
            }),
          ],
          onChanged: (value) {
            onClientChanged(value, value != null ? clients[value] : null);
          },
        ),
      ),
    );
  }

  Widget _buildStatusDropdown(BuildContext context) {
    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String?>(
          value: selectedStatus,
          isExpanded: true,
          icon: const Icon(Icons.arrow_drop_down, size: 20),
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textPrimary,
          ),
          hint: Text(
            '상태 전체',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          items: [
            DropdownMenuItem<String?>(
              value: null,
              child: Text(
                '상태 전체',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
            ...ApprovalStatus.values.map((status) {
              return DropdownMenuItem<String?>(
                value: status.code,
                child: Text(
                  status.displayName,
                  style: AppTypography.bodySmall,
                ),
              );
            }),
          ],
          onChanged: onStatusChanged,
        ),
      ),
    );
  }

  Widget _buildDateRangeSelector(BuildContext context) {
    final fromDisplay = deliveryDateFrom ?? '';
    final toDisplay = deliveryDateTo ?? '';
    final displayText = fromDisplay.isNotEmpty || toDisplay.isNotEmpty
        ? '납기일 $fromDisplay~$toDisplay'
        : '납기일 선택';

    return InkWell(
      onTap: () => _showDateRangePicker(context),
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Container(
        height: 40,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.calendar_today,
              size: 16,
              color: AppColors.textTertiary,
            ),
            const SizedBox(width: AppSpacing.xs),
            Expanded(
              child: Text(
                displayText,
                style: AppTypography.bodySmall.copyWith(
                  color: fromDisplay.isNotEmpty
                      ? AppColors.textPrimary
                      : AppColors.textTertiary,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showDateRangePicker(BuildContext context) async {
    // 현재 선택된 범위 기본값
    final now = DateTime.now();
    final initialFrom = deliveryDateFrom != null
        ? DateTime.parse(deliveryDateFrom!)
        : now.subtract(const Duration(days: 7));
    final initialTo = deliveryDateTo != null
        ? DateTime.parse(deliveryDateTo!)
        : now;

    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      initialDateRange: DateTimeRange(
        start: initialFrom,
        end: initialTo,
      ),
      locale: const Locale('ko', 'KR'),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: Theme.of(context).colorScheme.copyWith(
                  primary: AppColors.otokiBlue,
                ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      final fromStr =
          '${picked.start.year}-${picked.start.month.toString().padLeft(2, '0')}-${picked.start.day.toString().padLeft(2, '0')}';
      final toStr =
          '${picked.end.year}-${picked.end.month.toString().padLeft(2, '0')}-${picked.end.day.toString().padLeft(2, '0')}';
      onDateRangeChanged(fromStr, toStr);
    }
  }
}
