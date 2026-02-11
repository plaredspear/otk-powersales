import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/inspection_field_type.dart';
import '../../../domain/entities/inspection_list_item.dart';
import '../../../domain/entities/inspection_theme.dart';

/// 현장 점검 등록 - 공통 필드 위젯
///
/// 포함 필드:
/// - 테마 선택 (필수)
/// - 분류 선택 (자사/경쟁사)
/// - 거래처 선택 (필수)
/// - 점검일 선택 (기본: 오늘)
/// - 현장 유형 선택 (필수)
class InspectionCommonForm extends StatelessWidget {
  /// 선택된 테마
  final InspectionTheme? selectedTheme;

  /// 선택된 분류
  final InspectionCategory category;

  /// 선택된 거래처명
  final String? selectedStoreName;

  /// 선택된 점검일
  final DateTime inspectionDate;

  /// 선택된 현장 유형
  final InspectionFieldType? selectedFieldType;

  /// 테마 선택 콜백
  final VoidCallback onThemeTap;

  /// 분류 변경 콜백
  final ValueChanged<InspectionCategory> onCategoryChanged;

  /// 거래처 선택 콜백
  final VoidCallback onStoreTap;

  /// 점검일 변경 콜백
  final ValueChanged<DateTime> onDateChanged;

  /// 현장 유형 선택 콜백
  final VoidCallback onFieldTypeTap;

  const InspectionCommonForm({
    super.key,
    required this.selectedTheme,
    required this.category,
    required this.selectedStoreName,
    required this.inspectionDate,
    required this.selectedFieldType,
    required this.onThemeTap,
    required this.onCategoryChanged,
    required this.onStoreTap,
    required this.onDateChanged,
    required this.onFieldTypeTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 테마 선택 (필수)
        _buildThemeField(context),
        const Divider(height: 1),

        // 분류 선택 (자사/경쟁사)
        _buildCategoryField(context),
        const Divider(height: 1),

        // 거래처 선택 (필수)
        _buildStoreField(context),
        const Divider(height: 1),

        // 점검일 선택
        _buildDateField(context),
        const Divider(height: 1),

        // 현장 유형 선택 (필수)
        _buildFieldTypeField(context),
        const Divider(height: 1),
      ],
    );
  }

  /// 테마 선택 필드
  Widget _buildThemeField(BuildContext context) {
    return ListTile(
      title: RichText(
        text: const TextSpan(
          children: [
            TextSpan(
              text: '테마',
              style: TextStyle(
                fontSize: 16,
                color: Colors.black87,
              ),
            ),
            TextSpan(
              text: ' *',
              style: TextStyle(
                fontSize: 16,
                color: Colors.red,
              ),
            ),
          ],
        ),
      ),
      subtitle: Text(
        selectedTheme?.name ?? '테마 선택',
        style: TextStyle(
          fontSize: 14,
          color: selectedTheme != null ? Colors.black : Colors.grey,
        ),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onThemeTap,
    );
  }

  /// 분류 선택 필드 (자사/경쟁사 토글)
  Widget _buildCategoryField(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          RichText(
            text: const TextSpan(
              children: [
                TextSpan(
                  text: '분류',
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.black87,
                  ),
                ),
                TextSpan(
                  text: ' *',
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.red,
                  ),
                ),
              ],
            ),
          ),
          const Spacer(),
          ToggleButtons(
            isSelected: [
              category == InspectionCategory.OWN,
              category == InspectionCategory.COMPETITOR,
            ],
            onPressed: (index) {
              final newCategory = index == 0
                  ? InspectionCategory.OWN
                  : InspectionCategory.COMPETITOR;
              onCategoryChanged(newCategory);
            },
            borderRadius: BorderRadius.circular(8),
            constraints: const BoxConstraints(
              minWidth: 80,
              minHeight: 36,
            ),
            children: const [
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 16),
                child: Text('자사'),
              ),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 16),
                child: Text('경쟁사'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 거래처 선택 필드
  Widget _buildStoreField(BuildContext context) {
    return ListTile(
      title: RichText(
        text: const TextSpan(
          children: [
            TextSpan(
              text: '거래처',
              style: TextStyle(
                fontSize: 16,
                color: Colors.black87,
              ),
            ),
            TextSpan(
              text: ' *',
              style: TextStyle(
                fontSize: 16,
                color: Colors.red,
              ),
            ),
          ],
        ),
      ),
      subtitle: Text(
        selectedStoreName ?? '거래처 선택',
        style: TextStyle(
          fontSize: 14,
          color: selectedStoreName != null ? Colors.black : Colors.grey,
        ),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onStoreTap,
    );
  }

  /// 점검일 선택 필드
  Widget _buildDateField(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');
    return ListTile(
      title: const Text(
        '점검일',
        style: TextStyle(
          fontSize: 16,
          color: Colors.black87,
        ),
      ),
      subtitle: Text(
        dateFormat.format(inspectionDate),
        style: const TextStyle(
          fontSize: 14,
          color: Colors.black,
        ),
      ),
      trailing: const Icon(Icons.calendar_today),
      onTap: () => _selectDate(context),
    );
  }

  /// 날짜 선택 다이얼로그
  Future<void> _selectDate(BuildContext context) async {
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: inspectionDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (pickedDate != null && pickedDate != inspectionDate) {
      onDateChanged(pickedDate);
    }
  }

  /// 현장 유형 선택 필드
  Widget _buildFieldTypeField(BuildContext context) {
    return ListTile(
      title: RichText(
        text: const TextSpan(
          children: [
            TextSpan(
              text: '현장 유형',
              style: TextStyle(
                fontSize: 16,
                color: Colors.black87,
              ),
            ),
            TextSpan(
              text: ' *',
              style: TextStyle(
                fontSize: 16,
                color: Colors.red,
              ),
            ),
          ],
        ),
      ),
      subtitle: Text(
        selectedFieldType?.name ?? '현장 유형 선택',
        style: TextStyle(
          fontSize: 14,
          color: selectedFieldType != null ? Colors.black : Colors.grey,
        ),
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onFieldTypeTap,
    );
  }
}
