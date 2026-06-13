import 'package:flutter/material.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import 'monthly_sales_tab_page.dart';
import '../widgets/promotion/promotion_list_view.dart';

/// 매출 현황 페이지.
///
/// 레거시(heroku `promotion/event/list.jsp` + `promotion/month/list.jsp`) 정합:
/// 두 화면은 상단 `tab_menu`(행사 매출 / 월 매출)로 묶인 하나의 "매출 현황" 화면이며,
/// `account/list.jsp`의 `매출 현황` 링크(`/sales/eventList`)는 행사 매출 탭으로 진입한다.
/// 탭 전환 시 거래처(selectCode)는 전달되지 않으므로, 월 매출 탭은 거래처를 다시 선택한다
/// (레거시 month/list.jsp 동작 그대로 — [MonthlySalesTabPage]가 자체 거래처 선택을 제공).
class SalesStatusPage extends StatefulWidget {
  /// 초기 선택 탭 인덱스 (0: 행사 매출, 1: 월 매출).
  final int initialTabIndex;

  const SalesStatusPage({super.key, this.initialTabIndex = 0});

  @override
  State<SalesStatusPage> createState() => _SalesStatusPageState();
}

class _SalesStatusPageState extends State<SalesStatusPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex,
    );
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('매출 현황'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '행사 매출'),
            Tab(text: '월 매출'),
          ],
          labelColor: AppColors.otokiBlue,
          unselectedLabelColor: AppColors.textTertiary,
          indicatorColor: AppColors.otokiBlue,
          indicatorWeight: AppSpacing.tabIndicatorWeight,
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          // 행사 매출 (레거시 sales/eventList) — 진입 전 거래처가 사전 지정될 수 있음
          PromotionListView(),
          // 월 매출 (레거시 sales/monthList) — 거래처 재선택 필요
          MonthlySalesTabPage(),
        ],
      ),
    );
  }
}
