import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../providers/attendance_provider.dart';
import '../widgets/common/primary_button.dart';
import '../../app_router.dart';

/// 출근등록 완료 화면
class AttendanceCompletePage extends ConsumerWidget {
  const AttendanceCompletePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(attendanceProvider);
    final result = state.registrationResult;

    if (result == null) {
      // 등록 결과가 없으면 이전 화면으로
      WidgetsBinding.instance.addPostFrameCallback((_) {
        AppRouter.goBack(context);
      });
      return const Scaffold(body: SizedBox.shrink());
    }

    final workTypeLabel =
        result.workType == 'ROOM_TEMP' ? '상온' : '냉장/냉동';
    final isAllDone = result.isAllRegistered;

    return Scaffold(
      appBar: AppBar(
        title: const Text('등록 완료'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        automaticallyImplyLeading: false,
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 체크 아이콘
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: AppColors.success.withValues(alpha: 0.1),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.check_circle,
                  size: 56,
                  color: AppColors.success,
                ),
              ),
              const SizedBox(height: 24),

              // 등록 완료 메시지
              const Text(
                '출근등록 완료',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 16),

              // 거래처명
              Text(
                result.storeName,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: AppColors.otokiBlue,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),

              // 근무유형 뱃지
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.otokiYellow.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  workTypeLabel,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // 등록 현황
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(
                      Icons.check_circle_outline,
                      size: 20,
                      color: AppColors.otokiBlue,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '${result.registeredCount} / ${result.totalCount} 거래처 등록 완료',
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ],
                ),
              ),

              if (isAllDone) ...[
                const SizedBox(height: 16),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  decoration: BoxDecoration(
                    color: AppColors.success.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: AppColors.success.withValues(alpha: 0.3),
                    ),
                  ),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.celebration,
                        size: 20,
                        color: AppColors.success,
                      ),
                      SizedBox(width: 8),
                      Text(
                        '모든 거래처 등록 완료!',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.success,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(
            top: BorderSide(color: AppColors.border),
          ),
        ),
        child: isAllDone
            ? PrimaryButton(
                text: '홈으로 돌아가기',
                onPressed: () {
                  ref
                      .read(attendanceProvider.notifier)
                      .clearRegistrationResult();
                  AppRouter.navigateToAndRemoveAll(
                      context, AppRouter.main);
                },
              )
            : Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        ref
                            .read(attendanceProvider.notifier)
                            .clearRegistrationResult();
                        AppRouter.navigateToAndRemoveAll(
                            context, AppRouter.main);
                      },
                      style: OutlinedButton.styleFrom(
                        padding:
                            const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        side:
                            const BorderSide(color: AppColors.border),
                      ),
                      child: const Text(
                        '홈으로',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 2,
                    child: PrimaryButton(
                      text: '다음 등록',
                      onPressed: () {
                        ref
                            .read(attendanceProvider.notifier)
                            .prepareNextRegistration();
                        AppRouter.goBack(context);
                      },
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
