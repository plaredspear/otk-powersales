import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/my_store.dart';
import '../providers/my_stores_provider.dart';
import '../widgets/my_stores/my_store_card.dart';
import '../widgets/my_stores/my_store_search_bar.dart';
import '../widgets/my_stores/store_count_header.dart';
import '../widgets/my_stores/store_detail_popup.dart';

/// 내 거래처 페이지
///
/// 한 달 일정에 등록된 거래처 목록을 표시합니다.
/// 검색, 전화 걸기, 거래처 상세 팝업 기능을 제공합니다.
class MyStoresPage extends ConsumerStatefulWidget {
  const MyStoresPage({super.key});

  @override
  ConsumerState<MyStoresPage> createState() => _MyStoresPageState();
}

class _MyStoresPageState extends ConsumerState<MyStoresPage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 페이지 진입 시 거래처 목록 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(myStoresProvider.notifier).loadStores();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  /// 검색 실행
  void _onSearch() {
    final keyword = _searchController.text.trim();
    ref.read(myStoresProvider.notifier).searchStores(keyword);
  }

  /// 전화 걸기
  Future<void> _onPhoneTap(MyStore store) async {
    if (store.phoneNumber == null || store.phoneNumber!.isEmpty) return;

    final uri = Uri(scheme: 'tel', path: store.phoneNumber);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('전화 앱을 실행할 수 없습니다'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    }
  }

  /// 거래처 카드 탭 (상세 팝업)
  void _onStoreTap(MyStore store) {
    StoreDetailPopup.show(
      context,
      store: store,
      onOrderStatusTap: () {
        // 주문서 현황 - 미구현 시 안내
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('준비 중입니다'),
            duration: Duration(seconds: 2),
          ),
        );
      },
      onSalesStatusTap: () {
        // 매출 현황 - 미구현 시 안내
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('준비 중입니다'),
            duration: Duration(seconds: 2),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(myStoresProvider);

    // 에러 메시지 리스닝
    ref.listen(myStoresProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(myStoresProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('내 거래처'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 검색 바
          MyStoreSearchBar(
            controller: _searchController,
            onSearch: _onSearch,
          ),
          // 건수 헤더
          StoreCountHeader(count: state.displayCount),
          // 거래처 목록
          Expanded(
            child: _buildBody(state),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(myStoresState) {
    // 로딩 상태
    if (myStoresState.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (재시도 버튼)
    if (myStoresState.errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '일시적인 오류가 발생했습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref.read(myStoresProvider.notifier).loadStores();
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 거래처 목록 비어있음 (API 결과 자체가 없는 경우)
    if (myStoresState.isStoresEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.store_outlined,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '등록된 거래처가 없습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    // 검색 결과 없음
    if (myStoresState.isSearchEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search_off,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '검색 결과가 없습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    // 거래처 카드 리스트
    return ListView.builder(
      itemCount: myStoresState.filteredStores.length,
      itemBuilder: (context, index) {
        final store = myStoresState.filteredStores[index];
        return MyStoreCard(
          store: store,
          onTap: () => _onStoreTap(store),
          onPhoneTap: store.phoneNumber != null
              ? () => _onPhoneTap(store)
              : null,
        );
      },
    );
  }
}
