import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../providers/inspection_list_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/inspection/inspection_card.dart';
import '../widgets/inspection/inspection_filter_bar.dart';

/// 현장점검 목록 페이지
///
/// 거래처/분류/기간 필터로 현장점검 목록을 조회하고 표시합니다.
class InspectionListPage extends ConsumerStatefulWidget {
  const InspectionListPage({super.key});

  @override
  ConsumerState<InspectionListPage> createState() =>
      _InspectionListPageState();
}

class _InspectionListPageState extends ConsumerState<InspectionListPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(inspectionListProvider.notifier).initialize();
    });
  }

  void _onItemTap(InspectionListItem item) {
    AppRouter.navigateTo(
      context,
      AppRouter.inspectionDetail,
      arguments: item.id,
    );
  }

  void _onRegisterTap() {
    AppRouter.navigateTo(
      context,
      AppRouter.inspectionRegister,
    ).then((_) {
      // 등록 후 복귀 시 목록 새로고침
      ref.read(inspectionListProvider.notifier).searchInspections();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(inspectionListProvider);

    // 에러 리스닝
    ref.listen(inspectionListProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(inspectionListProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('현장 점검'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        children: [
          // 검색 필터
          InspectionFilterBar(
            stores: state.stores,
            selectedStoreId: state.selectedStoreId,
            selectedCategory: state.selectedCategory,
            fromDate: state.fromDate,
            toDate: state.toDate,
            onStoreChanged: (id, name) {
              ref.read(inspectionListProvider.notifier).selectStore(id, name);
            },
            onCategoryChanged: (category) {
              ref.read(inspectionListProvider.notifier).selectCategory(category);
            },
            onFromDateChanged: (date) {
              ref.read(inspectionListProvider.notifier).updateFromDate(date);
            },
            onToDateChanged: (date) {
              ref.read(inspectionListProvider.notifier).updateToDate(date);
            },
            onSearch: () {
              ref.read(inspectionListProvider.notifier).searchInspections();
            },
          ),

          // 결과 헤더 (전체 개수)
          if (state.hasSearched) _buildResultHeader(state.totalCount),

          // 목록
          Expanded(child: _buildBody(state.isLoading, state.items)),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _onRegisterTap,
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.onPrimary,
        icon: const Icon(Icons.add),
        label: const Text('점검'),
      ),
    );
  }

  Widget _buildResultHeader(int totalCount) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          Text(
            '내 현장 점검 ($totalCount)',
            style: AppTypography.headlineSmall,
          ),
        ],
      ),
    );
  }

  Widget _buildBody(bool isLoading, List<InspectionListItem> items) {
    if (isLoading) {
      return const Center(child: LoadingIndicator());
    }

    if (items.isEmpty) {
      return const Center(
        child: Text('검색 결과가 없습니다'),
      );
    }

    return ListView.builder(
      itemCount: items.length,
      itemBuilder: (context, index) {
        final item = items[index];
        return InspectionCard(
          item: item,
          onTap: () => _onItemTap(item),
        );
      },
    );
  }
}
