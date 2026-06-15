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
///
/// 높이는 화면의 70% 를 기준으로 둔다(거래처 선택과 동일).
///  - [searchHint] == null (짧은 목록): 콘텐츠 높이에 맞춰 뜨되 70% 를 상한으로 둔다.
///  - [searchHint] != null (검색 가능, 긴 목록): 높이를 70% 로 고정한다. 검색 결과 수에
///    따라 높이가 출렁이지 않도록(최초 높이 유지) 고정값을 쓴다.
class SingleSelectSheet<T> extends StatefulWidget {
  const SingleSelectSheet({
    super.key,
    required this.title,
    required this.options,
    required this.selectedValue,
    this.searchHint,
  });

  /// 시트 제목 (예: '상태 선택', '분류 선택').
  final String title;

  /// 선택 가능한 항목들 (첫 항목에 "전체"를 두는 패턴을 권장).
  final List<SingleSelectOption<T>> options;

  /// 현재 선택된 값 (강조 표시용).
  final T selectedValue;

  /// 검색창 힌트. null 이면 검색창을 표시하지 않는다(항목이 적은 목록용).
  final String? searchHint;

  /// 바텀시트로 표시하고 선택 결과를 반환한다 (취소 시 null).
  static Future<SingleSelectResult<T>?> show<T>(
    BuildContext context, {
    required String title,
    required List<SingleSelectOption<T>> options,
    required T selectedValue,
    String? searchHint,
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
        searchHint: searchHint,
      ),
    );
  }

  @override
  State<SingleSelectSheet<T>> createState() => _SingleSelectSheetState<T>();
}

class _SingleSelectSheetState<T> extends State<SingleSelectSheet<T>> {
  final TextEditingController _searchController = TextEditingController();
  String _query = '';

  bool get _searchable => widget.searchHint != null;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<SingleSelectOption<T>> get _filteredOptions {
    if (_query.isEmpty) return widget.options;
    final q = _query.toLowerCase();
    return widget.options
        .where((o) => o.label.toLowerCase().contains(q))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final sheetHeight = MediaQuery.of(context).size.height * 0.7;

    final column = Column(
      // 검색 가능 시: 고정 높이를 꽉 채워 높이를 일정하게 유지.
      // 검색 없음: 콘텐츠 높이에 맞춤(상한은 ConstrainedBox).
      mainAxisSize: _searchable ? MainAxisSize.max : MainAxisSize.min,
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
            child: Text(widget.title, style: AppTypography.headlineSmall),
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        if (_searchable) ...[
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              onChanged: (v) => setState(() => _query = v.trim()),
              decoration: InputDecoration(
                hintText: widget.searchHint,
                prefixIcon: const Icon(Icons.search),
                isDense: true,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                ),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
        ],
        // 검색 가능: Expanded 로 고정 높이를 채워 스크롤. 검색 없음: Flexible 로 콘텐츠 높이.
        if (_searchable)
          Expanded(child: _buildList(fill: true))
        else
          Flexible(child: _buildList(fill: false)),
      ],
    );

    final content = SafeArea(child: column);

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: _searchable
          ? SizedBox(height: sheetHeight, child: content)
          : ConstrainedBox(
              constraints: BoxConstraints(maxHeight: sheetHeight),
              child: content,
            ),
    );
  }

  Widget _buildList({required bool fill}) {
    final options = _filteredOptions;
    if (options.isEmpty) {
      final message = Text(
        '검색 결과가 없습니다',
        style: AppTypography.bodyMedium.copyWith(color: AppColors.textSecondary),
      );
      // 고정 높이(검색 가능)에서는 가운데 정렬해 빈 영역을 유지한다.
      return fill
          ? Center(child: message)
          : Padding(
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
              child: message,
            );
    }
    return ListView.separated(
      shrinkWrap: !fill,
      itemCount: options.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        final option = options[index];
        final isSelected = option.value == widget.selectedValue;
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
