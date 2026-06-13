import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/my_account.dart';
import '../providers/client_order_list_provider.dart';
import '../providers/my_accounts_provider.dart';
import 'sales_status_page.dart';
import '../widgets/my_accounts/my_account_card.dart';
import '../widgets/my_accounts/my_account_search_bar.dart';
import '../widgets/my_accounts/account_count_header.dart';
import '../widgets/my_accounts/account_detail_popup.dart';

/// 내 거래처 페이지
///
/// 한 달 일정에 등록된 거래처 목록을 표시합니다.
/// 검색, 전화 걸기, 거래처 상세 팝업 기능을 제공합니다.
class MyAccountsPage extends ConsumerStatefulWidget {
  const MyAccountsPage({super.key});

  @override
  ConsumerState<MyAccountsPage> createState() => _MyAccountsPageState();
}

class _MyAccountsPageState extends ConsumerState<MyAccountsPage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 페이지 진입 시 거래처 목록 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(myAccountsProvider.notifier).loadAccounts();
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
    if (keyword.isEmpty) {
      ref.read(myAccountsProvider.notifier).clearSearch();
      return;
    }
    if (keyword.length < 2) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('2자 이상 입력해주세요'),
          duration: Duration(seconds: 2),
        ),
      );
      return;
    }
    ref.read(myAccountsProvider.notifier).loadAccounts(keyword: keyword);
  }

  /// 전화 걸기
  Future<void> _onPhoneTap(MyAccount account) async {
    if (account.phoneNumber == null || account.phoneNumber!.isEmpty) return;

    final uri = Uri(scheme: 'tel', path: account.phoneNumber);
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
  ///
  /// 레거시(heroku `account/list.jsp`의 `popData`) 정합:
  /// - 주문서 현황 → `/order/list?type=client&selectCode=...` (거래처별 주문 탭)
  /// - 매출 현황   → `/sales/eventList?selectCode=...` (매출 현황: 행사 매출 탭 진입)
  /// 두 경우 모두 선택한 거래처를 미리 지정한 뒤 진입한다.
  void _onAccountTap(MyAccount account) {
    AccountDetailPopup.show(
      context,
      account: account,
      onOrderStatusTap: () => _openClientOrders(account),
      onSalesStatusTap: () => _openSalesPromotions(account),
    );
  }

  /// 주문서 현황: 거래처별 주문 탭으로 진입 (거래처 + 오늘 납기일 사전 지정 후 조회).
  void _openClientOrders(MyAccount account) {
    final notifier = ref.read(clientOrderListProvider.notifier);
    final now = DateTime.now();
    final todayStr =
        '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
    notifier.selectAccount(account.accountId, account.accountName);
    notifier.updateDeliveryDate(todayStr);
    notifier.searchOrders();
    AppRouter.navigateTo(
      context,
      AppRouter.orderList,
      arguments: 1, // 거래처별 주문 탭
    );
  }

  /// 매출 현황: 매출 현황 화면(행사 매출 + 월 매출 탭)으로 진입.
  ///
  /// 레거시처럼 행사 매출 탭이 기본이며, 선택한 거래처를 행사 매출에 사전 지정한다
  /// (진입 시 [SalesStatusPage]가 거래처 필터를 설정하고 자동 조회). 월 매출 탭은
  /// 레거시 동작대로 거래처를 전달하지 않아 재선택이 필요하다.
  void _openSalesPromotions(MyAccount account) {
    AppRouter.navigateTo(
      context,
      AppRouter.salesStatus,
      arguments: SalesStatusArgs(
        initialTabIndex: 0, // 행사 매출 탭
        presetAccountId: account.accountId,
        presetAccountName: account.accountName,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(myAccountsProvider);

    // 에러 메시지 리스닝
    ref.listen(myAccountsProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
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
          MyAccountSearchBar(
            controller: _searchController,
            onSearch: _onSearch,
            onClear: () {
              ref.read(myAccountsProvider.notifier).clearSearch();
            },
          ),
          // 건수 헤더
          AccountCountHeader(count: state.displayCount),
          // 거래처 목록
          Expanded(
            child: _buildBody(state),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(myAccountsState) {
    // 로딩 상태
    if (myAccountsState.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (재시도 버튼)
    if (myAccountsState.errorMessage != null) {
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
                ref.read(myAccountsProvider.notifier).loadAccounts();
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 거래처 목록 비어있음 (API 결과 자체가 없는 경우)
    if (myAccountsState.isAccountsEmpty) {
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
    if (myAccountsState.isSearchEmpty) {
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
      itemCount: myAccountsState.accounts.length,
      itemBuilder: (context, index) {
        final account = myAccountsState.accounts[index];
        return MyAccountCard(
          account: account,
          onTap: () => _onAccountTap(account),
          onPhoneTap: account.phoneNumber != null
              ? () => _onPhoneTap(account)
              : null,
        );
      },
    );
  }
}
