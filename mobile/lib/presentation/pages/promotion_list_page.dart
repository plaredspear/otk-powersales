import 'package:flutter/material.dart';

import '../widgets/promotion/promotion_list_view.dart';

/// 행사 목록 페이지 (독립 화면 — `행사 현황` 진입점).
///
/// 본문은 [PromotionListView] 가 담당하며, 본 페이지는 AppBar 만 제공한다.
class PromotionListPage extends StatelessWidget {
  const PromotionListPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('행사 현황'),
      ),
      body: const PromotionListView(),
    );
  }
}
