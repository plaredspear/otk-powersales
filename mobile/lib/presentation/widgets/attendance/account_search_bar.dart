import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 거래처 검색 바 위젯
class AccountSearchBar extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  const AccountSearchBar({
    super.key,
    required this.controller,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(12),
      ),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        decoration: InputDecoration(
          hintText: '거래처명, 주소, 거래처코드 검색',
          hintStyle: const TextStyle(
            fontSize: 14,
            color: AppColors.textTertiary,
          ),
          prefixIcon: const Icon(
            Icons.search,
            color: AppColors.otokiYellow,
            // 흰 배경 위 노란 아이콘 시인성 확보용 검은 테두리(스트로크)
            shadows: [
              Shadow(color: Colors.black, offset: Offset(-1, -1)),
              Shadow(color: Colors.black, offset: Offset(1, -1)),
              Shadow(color: Colors.black, offset: Offset(1, 1)),
              Shadow(color: Colors.black, offset: Offset(-1, 1)),
            ],
          ),
          suffixIcon: controller.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(
                    Icons.clear,
                    size: 20,
                    color: AppColors.textTertiary,
                  ),
                  onPressed: () {
                    controller.clear();
                    onChanged('');
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        ),
        style: const TextStyle(
          fontSize: 14,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}
