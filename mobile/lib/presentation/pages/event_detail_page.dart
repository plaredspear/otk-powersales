import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/event.dart';
import '../../domain/entities/event_sales_info.dart';
import '../providers/event_detail_provider.dart';
import '../providers/event_detail_state.dart';
import '../widgets/sales/daily_sales_list_widget.dart';
import '../widgets/sales/sales_info_widget.dart';
import 'daily_sales_registration_page.dart';

/// 행사 상세 페이지
///
/// 행사 정보와 매출 정보를 표시하는 페이지입니다.
class EventDetailPage extends ConsumerStatefulWidget {
  final String eventId;

  const EventDetailPage({
    super.key,
    required this.eventId,
  });

  @override
  ConsumerState<EventDetailPage> createState() => _EventDetailPageState();
}

class _EventDetailPageState extends ConsumerState<EventDetailPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(eventDetailProvider.notifier).loadEventDetail(widget.eventId);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(eventDetailProvider);
    final notifier = ref.read(eventDetailProvider.notifier);

    if (state.isLoading) {
      return Scaffold(
        appBar: AppBar(title: const Text('행사 상세')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (state.errorMessage != null || state.event == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('행사 상세')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('에러: ${state.errorMessage ?? "행사를 찾을 수 없습니다"}'),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => notifier.refresh(widget.eventId),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    final event = state.event!;
    final salesInfo = state.salesInfo;

    return Scaffold(
      appBar: AppBar(
        title: const Text('행사 상세'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '매출'),
            Tab(text: '행사 정보'),
          ],
        ),
      ),
      body: Column(
        children: [
          // 행사 간략 정보
          Container(
            padding: const EdgeInsets.all(16),
            color: Colors.grey.shade100,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Chip(
                  label: Text(event.eventType),
                  backgroundColor: Colors.blue.shade100,
                ),
                const SizedBox(height: 8),
                Text(
                  event.eventName,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${event.startDate.toString().substring(0, 10)} ~ ${event.endDate.toString().substring(0, 10)}',
                  style: const TextStyle(color: Colors.grey),
                ),
              ],
            ),
          ),

          // 탭 콘텐츠
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // 매출 탭
                _buildSalesTab(state, salesInfo),

                // 행사 정보 탭
                _buildEventInfoTab(event),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSalesTab(EventDetailState state, EventSalesInfo? salesInfo) {
    return RefreshIndicator(
      onRefresh: () => ref
          .read(eventDetailProvider.notifier)
          .refresh(widget.eventId),
      child: ListView(
        children: [
          if (salesInfo != null) SalesInfoWidget(salesInfo: salesInfo),

          // 일매출 등록 버튼
          if (state.canRegisterDailySales && !state.isTodayRegistered)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: ElevatedButton.icon(
                onPressed: () async {
                  final result = await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) =>
                          DailySalesRegistrationPage(event: state.event!),
                    ),
                  );
                  // 등록 성공 시 화면 새로고침
                  if (result == true && mounted) {
                    ref
                        .read(eventDetailProvider.notifier)
                        .refresh(widget.eventId);
                  }
                },
                icon: const Icon(Icons.add),
                label: const Text('일매출 등록'),
              ),
            ),

          if (state.isTodayRegistered)
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                '✓ 오늘 일매출 등록 완료',
                style: TextStyle(
                  color: Colors.green,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),

          DailySalesListWidget(dailySales: state.dailySalesList),
        ],
      ),
    );
  }

  Widget _buildEventInfoTab(Event event) {
    return ListView(
      children: [
        const Padding(
          padding: EdgeInsets.all(16),
          child: Text(
            '행사 제품',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),

        if (event.mainProduct != null)
          ListTile(
            leading: const Chip(
              label: Text('대표상품'),
              backgroundColor: Colors.orange,
            ),
            title: Text(event.mainProduct!.productName),
            subtitle: Text(event.mainProduct!.productCode),
          ),

        const Divider(),

        if (event.subProducts.isNotEmpty) ...[
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Text(
              '기타 제품',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          ...event.subProducts.map((product) {
            return ListTile(
              title: Text(product.productName),
              subtitle: Text(product.productCode),
            );
          }),
        ],
      ],
    );
  }
}
