import 'package:flutter/material.dart';

/// 필터 타입
enum FilterType {
  dropdown, // 드롭다운 선택
  textInput, // 텍스트 입력
}

/// 검색 필터 옵션
class FilterOption {
  final String label;
  final String value;

  const FilterOption({
    required this.label,
    required this.value,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is FilterOption &&
          runtimeType == other.runtimeType &&
          label == other.label &&
          value == other.value;

  @override
  int get hashCode => label.hashCode ^ value.hashCode;
}

/// 검색 필터 위젯
/// 드롭다운 선택 또는 텍스트 입력을 지원합니다.
class SearchFilterWidget extends StatefulWidget {
  /// 필터 레이블
  final String label;

  /// 필터 타입 (dropdown 또는 textInput)
  final FilterType filterType;

  /// 드롭다운 옵션 목록 (filterType이 dropdown일 때 필수)
  final List<FilterOption>? options;

  /// 초기 선택된 값
  final String? initialValue;

  /// 텍스트 입력 힌트 (filterType이 textInput일 때 사용)
  final String? hintText;

  /// 값이 변경될 때 호출되는 콜백
  final Function(String value)? onChanged;

  /// 검색 버튼 클릭 시 호출되는 콜백 (textInput에서 사용)
  final Function(String value)? onSearch;

  const SearchFilterWidget({
    super.key,
    required this.label,
    required this.filterType,
    this.options,
    this.initialValue,
    this.hintText,
    this.onChanged,
    this.onSearch,
  }) : assert(
          filterType == FilterType.textInput || options != null,
          'options는 filterType이 dropdown일 때 필수입니다.',
        );

  @override
  State<SearchFilterWidget> createState() => _SearchFilterWidgetState();
}

class _SearchFilterWidgetState extends State<SearchFilterWidget> {
  late String? _selectedValue;
  late TextEditingController _textController;

  @override
  void initState() {
    super.initState();
    _selectedValue = widget.initialValue;
    _textController = TextEditingController(text: widget.initialValue);
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),
        widget.filterType == FilterType.dropdown
            ? _buildDropdown()
            : _buildTextInput(),
      ],
    );
  }

  Widget _buildDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey),
        borderRadius: BorderRadius.circular(8),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          isExpanded: true,
          value: _selectedValue,
          hint: Text(widget.hintText ?? '선택하세요'),
          items: widget.options!.map((FilterOption option) {
            return DropdownMenuItem<String>(
              value: option.value,
              child: Text(option.label),
            );
          }).toList(),
          onChanged: (String? newValue) {
            if (newValue != null) {
              setState(() {
                _selectedValue = newValue;
              });
              widget.onChanged?.call(newValue);
            }
          },
        ),
      ),
    );
  }

  Widget _buildTextInput() {
    return TextField(
      controller: _textController,
      decoration: InputDecoration(
        hintText: widget.hintText ?? '입력하세요',
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 12,
        ),
        suffixIcon: IconButton(
          icon: const Icon(Icons.search),
          onPressed: () {
            widget.onSearch?.call(_textController.text);
          },
        ),
      ),
      onChanged: (value) {
        widget.onChanged?.call(value);
      },
      onSubmitted: (value) {
        widget.onSearch?.call(value);
      },
    );
  }
}
