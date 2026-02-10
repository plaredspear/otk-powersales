import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 제품검색 전용 AppBar
///
/// 뒤로가기 버튼, 검색어 입력 필드, 검색 버튼을 포함합니다.
class ProductSearchAppBar extends StatelessWidget
    implements PreferredSizeWidget {
  /// 검색어 입력 컨트롤러
  final TextEditingController controller;

  /// 검색 버튼 탭 콜백
  final VoidCallback? onSearch;

  /// 뒤로가기 콜백
  final VoidCallback onBack;

  /// 검색어 변경 콜백
  final ValueChanged<String>? onChanged;

  /// 자동 포커스 여부
  final bool autofocus;

  /// 힌트 텍스트
  final String hintText;

  const ProductSearchAppBar({
    super.key,
    required this.controller,
    this.onSearch,
    required this.onBack,
    this.onChanged,
    this.autofocus = true,
    this.hintText = '검색어 입력',
  });

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: AppColors.white,
      foregroundColor: AppColors.textPrimary,
      elevation: 0,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_ios, size: 20),
        onPressed: onBack,
      ),
      title: TextField(
        controller: controller,
        autofocus: autofocus,
        onChanged: onChanged,
        onSubmitted: (_) => onSearch?.call(),
        decoration: InputDecoration(
          hintText: hintText,
          hintStyle: const TextStyle(
            color: AppColors.textTertiary,
            fontSize: 16,
          ),
          border: InputBorder.none,
          contentPadding: EdgeInsets.zero,
        ),
        style: const TextStyle(
          fontSize: 16,
          color: AppColors.textPrimary,
        ),
      ),
      actions: [
        IconButton(
          icon: Icon(
            Icons.search,
            color: onSearch != null
                ? AppColors.textPrimary
                : AppColors.textTertiary,
          ),
          onPressed: onSearch,
        ),
      ],
    );
  }
}
