import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/shelf_life_item.dart';
import '../providers/shelf_life_delete_provider.dart';
import '../providers/shelf_life_delete_state.dart';
import '../widgets/shelf_life/shelf_life_delete_card.dart';

/// 유통기한 삭제 페이지
///
/// 관리 화면에서 전달받은 유통기한 목록에서 선택적으로 삭제합니다.
/// 전체/그룹/개별 선택 기능을 제공합니다.
class ShelfLifeDeletePage extends ConsumerStatefulWidget {
  final List<ShelfLifeItem> items;

  const ShelfLifeDeletePage({super.key, required this.items});

  @override
  ConsumerState<ShelfLifeDeletePage> createState() =>
      _ShelfLifeDeletePageState();
}

class _ShelfLifeDeletePageState extends ConsumerState<ShelfLifeDeletePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(shelfLifeDeleteProvider.notifier).setItems(widget.items);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(shelfLifeDeleteProvider);

    // 삭제 완료 리스닝
    ref.listen(shelfLifeDeleteProvider, (previous, next) {
      if (next.isDeleted && !(previous?.isDeleted ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${previous?.selectedCount ?? 0}건이 삭제되었습니다'),
          ),
        );
        AppRouter.goBack(context);
      }
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(shelfLifeDeleteProvider.notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('유통기한 삭제'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        children: [
          // 전체 선택 체크박스
          _buildSelectAllHeader(state),
          const Divider(height: 1),

          // 목록
          Expanded(child: _buildBody(state)),
        ],
      ),

      // 삭제 버튼
      bottomNavigationBar: _buildDeleteButton(state),
    );
  }

  Widget _buildSelectAllHeader(ShelfLifeDeleteState state) {
    return InkWell(
      onTap: () {
        ref.read(shelfLifeDeleteProvider.notifier).toggleAll();
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            Checkbox(
              value: state.isAllSelected,
              onChanged: (_) {
                ref.read(shelfLifeDeleteProvider.notifier).toggleAll();
              },
              activeColor: AppColors.error,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              visualDensity: VisualDensity.compact,
            ),
            const SizedBox(width: AppSpacing.sm),
            Text(
              '전체',
              style: AppTypography.headlineSmall,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBody(ShelfLifeDeleteState state) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.items.isEmpty) {
      return Center(
        child: Text(
          '삭제할 항목이 없습니다',
          style: AppTypography.bodyLarge
              .copyWith(color: AppColors.textSecondary),
        ),
      );
    }

    return ListView(
      children: [
        // 유통기한 지남 그룹
        if (state.expiredItems.isNotEmpty) ...[
          _buildGroupCheckbox(
            isExpired: true,
            isSelected: state.isExpiredGroupSelected,
            count: state.expiredItems.length,
          ),
          ...state.expiredItems.map((item) => ShelfLifeDeleteCard(
                item: item,
                isSelected: state.selectedIds.contains(item.id),
                onChanged: (_) {
                  ref.read(shelfLifeDeleteProvider.notifier).toggleItem(item.id);
                },
              )),
          const SizedBox(height: AppSpacing.md),
        ],

        // 유통기한 전 그룹
        if (state.activeItems.isNotEmpty) ...[
          _buildGroupCheckbox(
            isExpired: false,
            isSelected: state.isActiveGroupSelected,
            count: state.activeItems.length,
          ),
          ...state.activeItems.map((item) => ShelfLifeDeleteCard(
                item: item,
                isSelected: state.selectedIds.contains(item.id),
                onChanged: (_) {
                  ref.read(shelfLifeDeleteProvider.notifier).toggleItem(item.id);
                },
              )),
        ],

        const SizedBox(height: AppSpacing.lg),
      ],
    );
  }

  Widget _buildGroupCheckbox({
    required bool isExpired,
    required bool isSelected,
    required int count,
  }) {
    return InkWell(
      onTap: () {
        ref
            .read(shelfLifeDeleteProvider.notifier)
            .toggleGroup(expired: isExpired);
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.sm,
        ),
        child: Row(
          children: [
            Checkbox(
              value: isSelected,
              onChanged: (_) {
                ref
                    .read(shelfLifeDeleteProvider.notifier)
                    .toggleGroup(expired: isExpired);
              },
              activeColor: AppColors.error,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              visualDensity: VisualDensity.compact,
            ),
            const SizedBox(width: AppSpacing.xs),
            Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: isExpired ? AppColors.error : AppColors.warning,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            Text(
              isExpired ? '유통기한 지남' : '유통기한 전',
              style: AppTypography.headlineSmall.copyWith(
                color: isExpired ? AppColors.error : AppColors.warning,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDeleteButton(ShelfLifeDeleteState state) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: SizedBox(
          width: double.infinity,
          height: AppSpacing.buttonHeight,
          child: ElevatedButton(
            onPressed: state.canDelete
                ? () {
                    ref
                        .read(shelfLifeDeleteProvider.notifier)
                        .deleteSelected();
                  }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              foregroundColor: AppColors.white,
              disabledBackgroundColor: AppColors.divider,
              disabledForegroundColor: AppColors.textTertiary,
              shape: RoundedRectangleBorder(
                borderRadius: AppSpacing.buttonBorderRadius,
              ),
            ),
            child: Text(
              state.canDelete
                  ? '삭제 (${state.selectedCount}건)'
                  : '삭제',
            ),
          ),
        ),
      ),
    );
  }
}
