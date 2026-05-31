import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../domain/entities/product.dart';
import '../providers/product_search_provider.dart';
import '../widgets/product_search/empty_search_guide.dart';
import '../widgets/product_search/product_search_app_bar.dart';

/// 제품검색 화면
///
/// 검색어를 입력하고 검색을 실행하는 화면입니다.
/// 검색 결과가 있으면 결과 화면으로 이동합니다.
class ProductSearchPage extends ConsumerStatefulWidget {
  /// 제품 선택 모드 — 결과 화면에서 고른 제품을 호출부로 반환(pop)한다.
  final bool selectionMode;

  const ProductSearchPage({super.key, this.selectionMode = false});

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

  Future<void> _onSearch() async {
    final notifier = ref.read(productSearchProvider.notifier);
    await notifier.search();
    if (!mounted) return;

    final state = ref.read(productSearchProvider);
    if (!state.hasSearched || state.isLoading || state.errorMessage != null) {
      return;
    }

    // 결과 화면으로 이동 (선택 모드 전달)
    final selected = await AppRouter.navigateTo<Product>(
      context,
      AppRouter.productSearchResult,
      arguments: widget.selectionMode,
    );
    // 선택 모드: 결과에서 고른 제품을 다시 호출부로 반환
    if (widget.selectionMode && selected != null && mounted) {
      Navigator.of(context).pop(selected);
    }
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
          : EmptySearchGuide(hasSearched: false, onBarcodeTap: _onBarcodeTap),
    );
  }
}
