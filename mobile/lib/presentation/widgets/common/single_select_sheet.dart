import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 단일-선택 바텀시트의 한 항목.
class SingleSelectOption<T> {
  const SingleSelectOption({required this.value, required this.label});

  /// 선택 시 돌려줄 값 (예: null = "전체", enum, 코드 문자열).
  final T value;

  /// 화면에 표시할 라벨.
  final String label;
}

/// 단일-선택 결과 래퍼.
///
/// [SingleSelectSheet.show] 는 취소(바깥 탭) 시 null 을 반환하므로, "전체"처럼
/// [value] 가 null 인 선택과 구분하기 위해 결과를 이 래퍼로 감싸 돌려준다.
class SingleSelectResult<T> {
  const SingleSelectResult(this.value);

  /// 선택된 값.
  final T value;
}

/// 목록에서 하나를 고르는 공용 바텀시트.
///
/// 거래처 선택([AccountSelectorSheet])과 동일한 바텀시트 UX 로, 주문 상태·점검 분류·
/// 공지 분류 등 "목록에서 하나 선택" 필터를 단일 패턴으로 통일한다.
/// 현재 선택 항목은 네이비 강조 + 체크로 표시한다.
class SingleSelectSheet<T> extends StatelessWidget {
  const SingleSelectSheet({
    super.key,
    required this.title,
    required this.options,
    required this.selectedValue,
  });

  /// 시트 제목 (예: '상태 선택', '분류 선택').
  final String title;

  /// 선택 가능한 항목들 (첫 항목에 "전체"를 두는 패턴을 권장).
  final List<SingleSelectOption<T>> options;

  /// 현재 선택된 값 (강조 표시용).
  final T selectedValue;

  /// 바텀시트로 표시하고 선택 결과를 반환한다 (취소 시 null).
  static Future<SingleSelectResult<T>?> show<T>(
    BuildContext context, {
    required String title,
    required List<SingleSelectOption<T>> options,
    required T selectedValue,
  }) {
    return showModalBottomSheet<SingleSelectResult<T>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (_) => SingleSelectSheet<T>(
        title: title,
        options: options,
        selectedValue: selectedValue,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: AppSpacing.sm),
          // 핸들 바
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.divider,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          // 제목
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(title, style: AppTypography.headlineSmall),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Flexible(child: _buildList(context)),
        ],
      ),
    );
  }

  Widget _buildList(BuildContext context) {
    return ListView.separated(
      shrinkWrap: true,
      itemCount: options.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        final option = options[index];
        final isSelected = option.value == selectedValue;
        return ListTile(
          title: Text(
            option.label,
            style: AppTypography.bodyLarge.copyWith(
              color: isSelected ? AppColors.secondary : AppColors.textPrimary,
              fontWeight: isSelected ? FontWeight.w700 : FontWeight.w400,
            ),
          ),
          trailing: isSelected
              ? const Icon(Icons.check, size: 20, color: AppColors.secondary)
              : null,
          onTap: () =>
              Navigator.of(context).pop(SingleSelectResult<T>(option.value)),
        );
      },
    );
  }
}
