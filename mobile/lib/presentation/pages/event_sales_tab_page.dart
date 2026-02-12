import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../providers/event_sales_provider.dart';
import '../widgets/sales/event_card_widget.dart';
import 'event_detail_page.dart';

/// 행사매출 탭 페이지
///
/// 행사 목록을 표시하는 탭 페이지입니다.
class EventSalesTabPage extends ConsumerStatefulWidget {
  const EventSalesTabPage({super.key});

  @override
  ConsumerState<EventSalesTabPage> createState() => _EventSalesTabPageState();
}

class _EventSalesTabPageState extends ConsumerState<EventSalesTabPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(eventSalesProvider.notifier).initialize();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(eventSalesProvider);
    final notifier = ref.read(eventSalesProvider.notifier);

    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('에러: ${state.errorMessage}'),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => notifier.refresh(),
              child: const Text('다시 시도'),
            ),
          ],
        ),
      );
    }

    if (state.isEmpty) {
      return const Center(
        child: Text('등록된 행사가 없습니다'),
      );
    }

    return Column(
      children: [
        // 검색 필터 영역 (TODO: 구현 필요)
        Container(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              const Text('거래처 선택 및 기간 선택 (TODO)'),
              ElevatedButton(
                onPressed: () => notifier.search(),
                child: const Text('검색'),
              ),
            ],
          ),
        ),

        const Divider(height: 1),

        // 행사 목록
        Expanded(
          child: RefreshIndicator(
            onRefresh: () => notifier.refresh(),
            child: ListView.builder(
              itemCount: state.events.length,
              itemBuilder: (context, index) {
                final event = state.events[index];
                return EventCardWidget(
                  event: event,
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => EventDetailPage(eventId: event.id),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ),

        // 페이지네이션 (TODO: 구현 필요)
        if (state.hasResults)
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextButton(
                  onPressed: state.currentPage > 1
                      ? () => notifier.loadPreviousPage()
                      : null,
                  child: const Text('이전'),
                ),
                Text(' 페이지 ${state.currentPage} '),
                TextButton(
                  onPressed: () => notifier.loadNextPage(),
                  child: const Text('다음'),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
