import 'package:flutter/material.dart';

import '../pages/event_sales_tab_page.dart';
import '../pages/monthly_sales_tab_page.dart';

/// 매출 현황 메인 화면
///
/// 행사매출/월매출 탭을 포함하는 메인 화면입니다.
class SalesOverviewScreen extends StatefulWidget {
  const SalesOverviewScreen({super.key});

  @override
  State<SalesOverviewScreen> createState() => _SalesOverviewScreenState();
}

class _SalesOverviewScreenState extends State<SalesOverviewScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
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
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '행사 매출'),
            Tab(text: '월 매출'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          EventSalesTabPage(),
          MonthlySalesTabPage(),
        ],
      ),
    );
  }
}
