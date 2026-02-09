import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/safety_check_mock_repository.dart';
import '../../domain/repositories/safety_check_repository.dart';
import '../../domain/usecases/get_safety_check_items.dart';
import '../../domain/usecases/get_safety_check_today_status.dart';
import '../../domain/usecases/submit_safety_check.dart';
import 'safety_check_state.dart';

/// SafetyCheck Repository Provider
final safetyCheckRepositoryProvider = Provider<SafetyCheckRepository>((ref) {
  return SafetyCheckMockRepository();
});

/// GetSafetyCheckItems UseCase Provider
final getSafetyCheckItemsUseCaseProvider =
    Provider<GetSafetyCheckItems>((ref) {
  final repository = ref.watch(safetyCheckRepositoryProvider);
  return GetSafetyCheckItems(repository);
});

/// GetSafetyCheckTodayStatus UseCase Provider
final getSafetyCheckTodayStatusUseCaseProvider =
    Provider<GetSafetyCheckTodayStatus>((ref) {
  final repository = ref.watch(safetyCheckRepositoryProvider);
  return GetSafetyCheckTodayStatus(repository);
});

/// SubmitSafetyCheck UseCase Provider
final submitSafetyCheckUseCaseProvider = Provider<SubmitSafetyCheck>((ref) {
  final repository = ref.watch(safetyCheckRepositoryProvider);
  return SubmitSafetyCheck(repository);
});

/// 안전점검 화면 상태 관리 Notifier
///
/// 체크리스트 로딩, 항목 체크/해제, 제출 상태를 관리한다.
class SafetyCheckNotifier extends StateNotifier<SafetyCheckState> {
  SafetyCheckNotifier(
    this._getSafetyCheckItems,
    this._submitSafetyCheck,
  ) : super(SafetyCheckState.initial());

  final GetSafetyCheckItems _getSafetyCheckItems;
  final SubmitSafetyCheck _submitSafetyCheck;

  /// 체크리스트 항목 조회
  Future<void> fetchItems() async {
    state = state.toLoading();

    try {
      final categories = await _getSafetyCheckItems();
      state = state.toLoaded(categories);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 항목 체크 상태 토글
  void toggleItem(int itemId) {
    state = state.toggleItem(itemId);
  }

  /// 안전점검 제출
  Future<void> submit() async {
    if (!state.allRequiredChecked) return;

    state = state.toSubmitting();

    try {
      await _submitSafetyCheck(checkedItemIds: state.checkedItemIds);
      state = state.toSubmitted();
    } catch (e) {
      final errorMessage = e.toString();
      // 중복 제출 (409) 시 이미 완료로 처리
      if (errorMessage.contains('이미 안전점검을 완료')) {
        state = state.toSubmitted();
      } else {
        state = state.toError(errorMessage);
      }
    }
  }
}

/// SafetyCheck StateNotifier Provider
final safetyCheckProvider =
    StateNotifierProvider<SafetyCheckNotifier, SafetyCheckState>((ref) {
  final getItems = ref.watch(getSafetyCheckItemsUseCaseProvider);
  final submit = ref.watch(submitSafetyCheckUseCaseProvider);
  return SafetyCheckNotifier(getItems, submit);
});
