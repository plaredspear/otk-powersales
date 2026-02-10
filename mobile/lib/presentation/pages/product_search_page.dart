import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../providers/product_search_provider.dart';
import '../widgets/product_search/empty_search_guide.dart';
import '../widgets/product_search/product_search_app_bar.dart';

/// 제품검색 화면
///
/// 검색어를 입력하고 검색을 실행하는 화면입니다.
/// 검색 결과가 있으면 결과 화면으로 이동합니다.
class ProductSearchPage extends ConsumerStatefulWidget {
  const ProductSearchPage({super.key});

  @override
  ConsumerState<ProductSearchPage> createState() => _ProductSearchPageState();
}

class _ProductSearchPageState extends ConsumerState<ProductSearchPage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 이전 검색어가 있으면 복원
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final state = ref.read(productSearchProvider);
      if (state.query.isNotEmpty) {
        _searchController.text = state.query;
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onSearch() {
    final notifier = ref.read(productSearchProvider.notifier);
    notifier.search().then((_) {
      final state = ref.read(productSearchProvider);
      if (state.hasSearched && !state.isLoading && state.errorMessage == null) {
        AppRouter.navigateTo(context, AppRouter.productSearchResult);
      }
    });
  }

  void _onBarcodeTap() {
    // TODO: 바코드 스캐너 실행 후 결과로 검색
    // 현재는 Mock으로 동작
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('바코드 스캐너는 추후 구현 예정입니다'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productSearchProvider);
    final notifier = ref.read(productSearchProvider.notifier);

    // 에러 메시지 리스닝
    ref.listen(productSearchProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        notifier.clearError();
      }
    });

    final canSearch = state.canSearch;

    return Scaffold(
      appBar: ProductSearchAppBar(
        controller: _searchController,
        onBack: () {
          notifier.clearSearch();
          AppRouter.goBack(context);
        },
        onChanged: (value) => notifier.updateQuery(value),
        onSearch: canSearch ? _onSearch : null,
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : EmptySearchGuide(
              hasSearched: false,
              onBarcodeTap: _onBarcodeTap,
            ),
    );
  }
}
