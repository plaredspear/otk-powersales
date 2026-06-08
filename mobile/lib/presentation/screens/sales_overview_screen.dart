import 'package:flutter/material.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';

/// 매출 현황 허브 화면
///
/// 레거시 Heroku `promotion/month/main2.jsp` 와 동일하게 매출 조회 3종
/// (POS 매출 조회 / 월 매출 조회(전산) / 월 매출 조회(물류)) 으로 진입하는
/// 메뉴 버튼만 나열한다. 행사 매출·월 매출은 별도 화면으로 분리되어 있어
/// 본 허브에는 포함되지 않는다.
class SalesOverviewScreen extends StatelessWidget {
  const SalesOverviewScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('매출 현황')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _HubButton(
                label: 'POS 매출 조회',
                onTap: () =>
                    AppRouter.navigateTo(context, AppRouter.posSales),
              ),
              const SizedBox(height: 24),
              _HubButton(
                label: '월 매출 조회(전산)',
                onTap: () =>
                    AppRouter.navigateTo(context, AppRouter.electronicSales),
              ),
              const SizedBox(height: 24),
              _HubButton(
                label: '월 매출 조회(물류)',
                onTap: () =>
                    AppRouter.navigateTo(context, AppRouter.logisticsSales),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 매출 현황 허브 메뉴 버튼 — 레거시 네이비 라운드 버튼 스타일.
class _HubButton extends StatelessWidget {
  final String label;
  final VoidCallback onTap;

  const _HubButton({required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 88,
      child: ElevatedButton(
        onPressed: onTap,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.secondary,
          foregroundColor: AppColors.onSecondary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(44),
          ),
        ),
        child: Text(
          label,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
        ),
      ),
    );
  }
}
