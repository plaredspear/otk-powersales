import 'package:flutter/material.dart';

import 'monthly_sales_tab_page.dart';

/// 월 매출 페이지 (독립 화면 — 드로어 `월 매출` 진입점).
///
/// 본문은 [MonthlySalesTabPage] 가 담당하며, 본 페이지는 AppBar 만 제공한다.
class MonthlySalesPage extends StatelessWidget {
  const MonthlySalesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('물류 매출 조회')),
      body: const MonthlySalesTabPage(),
    );
  }
}
