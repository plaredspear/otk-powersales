import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/dio_provider.dart';
import '../../data/datasources/safety_check_status_api_datasource.dart';
import 'safety_check_status_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================
final safetyCheckStatusDataSourceProvider =
    Provider<SafetyCheckStatusApiDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return SafetyCheckStatusApiDataSource(dio);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================
class SafetyCheckStatusNotifier extends StateNotifier<SafetyCheckStatusState> {
  final SafetyCheckStatusApiDataSource _dataSource;

  SafetyCheckStatusNotifier(this._dataSource)
      : super(SafetyCheckStatusState.initial());

  Future<void> fetchStatus() async {
    state = state.toLoading();
    try {
      final data = await _dataSource.getStatus(date: state.dateString);
      state = state.toLoaded(data);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  Future<void> goToPreviousDay() {
    state = state.withDate(
      state.selectedDate.subtract(const Duration(days: 1)),
    );
    return fetchStatus();
  }

  Future<void> goToNextDay() {
    state = state.withDate(
      state.selectedDate.add(const Duration(days: 1)),
    );
    return fetchStatus();
  }

  void toggleCard(int memberId) {
    state = state.toggleCard(memberId);
  }

  void clearError() {
    state = SafetyCheckStatusState(
      selectedDate: state.selectedDate,
      data: state.data,
    );
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================
final safetyCheckStatusProvider = StateNotifierProvider<
    SafetyCheckStatusNotifier, SafetyCheckStatusState>((ref) {
  final dataSource = ref.watch(safetyCheckStatusDataSourceProvider);
  return SafetyCheckStatusNotifier(dataSource);
});
