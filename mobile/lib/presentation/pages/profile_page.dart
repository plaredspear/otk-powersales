import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../providers/auth_provider.dart';

/// 내 정보(프로필) 페이지
///
/// 전체메뉴 Drawer 상단 이름 영역을 탭하면 진입한다.
/// 레거시 Heroku GNB `top_info`(노란 헤더 + 이름) + `mypage/modify.jsp`
/// 폼 행 스타일을 정합해 사용자 정보를 표시하고, 계정 액션
/// (비밀번호 변경 · 로그아웃) 진입점을 제공한다.
///
/// 레거시와 달리 "내 일정"은 전체메뉴에 별도 항목이 있으므로 여기에 두지 않는다.
class ProfilePage extends ConsumerWidget {
  const ProfilePage({super.key});

  /// 원본 권한(appauthority__c) → 표시 라벨.
  ///
  /// 백엔드 원본 role 값이 미지정(null/공백)이면 도메인 번역 단계에서 실제 "여사원"과
  /// 구분되지 않으므로, 도메인 role 대신 [User.rawRole](SF picklist 원문)을 직접 라벨링한다.
  /// 미지정이면 "-" 로 표시해 실제 여사원과 혼동되지 않게 한다.
  static String _roleLabel(String? rawRole) {
    switch (rawRole) {
      case '여사원':
        return '여사원';
      case '조장':
        return '조장';
      case '지점장':
        return '지점장';
      case 'AccountViewAll':
        return '영업부장';
      case null:
      case '':
        return '-';
      default:
        // 예상 밖 picklist 값은 원문 그대로 노출 (마이그레이션/정합 이상 가시화)
        return rawRole;
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user;
    final name = user?.name ?? '사용자';
    final orgName = user?.orgName ?? '';

    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        title: const Text('내 정보'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 노란 헤더 (레거시 GNB top_info 정합)
          Container(
            width: double.infinity,
            color: AppColors.legacyYellow,
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$name님',
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: AppColors.black,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  user == null ? '' : _roleLabel(user.rawRole),
                  style: const TextStyle(
                    fontSize: 14,
                    color: AppColors.legacyTextSub,
                  ),
                ),
              ],
            ),
          ),

          // 기본 정보 (레거시 modify.jsp form_wrap 행 정합)
          _infoRow(label: '아이디', value: user?.employeeCode ?? ''),
          _infoRow(label: '소속', value: orgName),
          _infoRow(
            label: '권한',
            value: user == null ? '' : _roleLabel(user.rawRole),
          ),

          const SizedBox(height: 12),
          const Divider(height: 1, thickness: 8, color: AppColors.surface),

          // 계정 액션
          _actionRow(
            label: '비밀번호 변경',
            onTap: () =>
                Navigator.of(context).pushNamed(AppRouter.verifyPassword),
          ),
          _actionRow(
            label: '로그아웃',
            onTap: () => _handleLogout(context, ref),
          ),
        ],
      ),
    );
  }

  /// 읽기 전용 정보 행 (라벨 / 값)
  Widget _infoRow({required String label, required String value}) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.symmetric(vertical: 16),
      decoration: const BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Color(0xFFE6E6E6)),
        ),
      ),
      child: Row(
        children: [
          SizedBox(
            width: 72,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 14,
                color: AppColors.legacyTextMute,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 15,
                color: AppColors.black,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 탭 가능한 액션 행 (라벨 + 우측 화살표)
  Widget _actionRow({required String label, required VoidCallback onTap}) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(color: AppColors.divider),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: const TextStyle(
                  fontSize: 15,
                  color: AppColors.black,
                ),
              ),
            ),
            const Icon(
              Icons.chevron_right,
              size: 22,
              color: AppColors.textTertiary,
            ),
          ],
        ),
      ),
    );
  }

  /// 로그아웃 처리 (전체메뉴 Drawer 로그아웃과 동일 흐름)
  void _handleLogout(BuildContext context, WidgetRef ref) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: TextStyle(color: AppColors.textSecondary),
            ),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(dialogContext).pop(); // 다이얼로그 닫기
              // 로그아웃 → 루트 ProviderScope 재생성으로 로그인 화면 전환까지 처리되므로
              // 별도의 수동 네비게이션은 두지 않는다.
              await ref.read(authProvider.notifier).logout();
            },
            child: Text(
              '확인',
              style: TextStyle(color: AppColors.otokiRed),
            ),
          ),
        ],
      ),
    );
  }
}
