import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/usecases/get_inspection_detail_usecase.dart';
import 'inspection_detail_state.dart';
import 'inspection_list_provider.dart';

// ============================================
// 1. UseCase Provider
// ============================================

/// GetInspectionDetail UseCase Provider
final getInspectionDetailUseCaseProvider =
    Provider<GetInspectionDetailUseCase>((ref) {
  final repository = ref.watch(inspectionRepositoryProvider);
  return GetInspectionDetailUseCase(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 현장점검 상세 화면 상태 관리 Notifier
class InspectionDetailNotifier extends StateNotifier<InspectionDetailState> {
  final GetInspectionDetailUseCase _getInspectionDetail;

  InspectionDetailNotifier({
    required GetInspectionDetailUseCase getInspectionDetail,
  })  : _getInspectionDetail = getInspectionDetail,
        super(InspectionDetailState.initial());

  /// 현장점검 상세 조회
  ///
  /// [id]: 조회할 점검 ID
  Future<void> loadDetail(int id) async {
    state = state.toLoading();

    try {
      final detail = await _getInspectionDetail.call(id);
      state = state.copyWith(
        detail: detail,
        isLoading: false,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// InspectionDetail StateNotifier Provider
///
/// inspectionId를 family 파라미터로 받아 점검 상세를 관리합니다.
final inspectionDetailProvider = StateNotifierProvider.family<
    InspectionDetailNotifier, InspectionDetailState, int>((ref, inspectionId) {
  final useCase = ref.watch(getInspectionDetailUseCaseProvider);

  final notifier = InspectionDetailNotifier(
    getInspectionDetail: useCase,
  );

  // 자동으로 상세 로딩
  notifier.loadDetail(inspectionId);

  return notifier;
});
