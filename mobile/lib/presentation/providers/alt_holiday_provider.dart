import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/alternative_holiday_api_datasource.dart';
import '../../data/repositories/alternative_holiday_repository_impl.dart';
import '../../domain/entities/alternative_holiday.dart';
import '../../domain/repositories/alternative_holiday_repository.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final altHolidayRepositoryProvider =
    Provider<AlternativeHolidayRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return AlternativeHolidayRepositoryImpl(
    AlternativeHolidayApiDataSource(dio),
  );
});

// ============================================
// 2. Request State + Notifier
// ============================================

class AltHolidayRequestState {
  final bool isLoading;
  final String? errorMessage;
  final bool isSubmitted;
  final DateTime? actualWorkDate;
  final DateTime? targetAltHolidayDate;
  final List<DateTime> holidays;

  const AltHolidayRequestState({
    this.isLoading = false,
    this.errorMessage,
    this.isSubmitted = false,
    this.actualWorkDate,
    this.targetAltHolidayDate,
    this.holidays = const [],
  });

  bool get canSubmit =>
      actualWorkDate != null && targetAltHolidayDate != null && !isLoading;

  AltHolidayRequestState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    bool? isSubmitted,
    DateTime? actualWorkDate,
    bool clearActualWorkDate = false,
    DateTime? targetAltHolidayDate,
    bool clearTargetDate = false,
    List<DateTime>? holidays,
  }) {
    return AltHolidayRequestState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      isSubmitted: isSubmitted ?? this.isSubmitted,
      actualWorkDate:
          clearActualWorkDate ? null : (actualWorkDate ?? this.actualWorkDate),
      targetAltHolidayDate: clearTargetDate
          ? null
          : (targetAltHolidayDate ?? this.targetAltHolidayDate),
      holidays: holidays ?? this.holidays,
    );
  }
}

class AltHolidayRequestNotifier extends StateNotifier<AltHolidayRequestState> {
  final AlternativeHolidayRepository _repository;

  AltHolidayRequestNotifier(this._repository)
      : super(const AltHolidayRequestState());

  Future<void> loadHolidays(int year) async {
    try {
      final holidays = await _repository.getHolidays(year);
      state = state.copyWith(holidays: holidays);
    } catch (_) {
      // 공휴일 로드 실패 시 무시 (서버 검증이 최종 방어선)
    }
  }

  void selectActualWorkDate(DateTime date) {
    state = state.copyWith(actualWorkDate: date, clearError: true);
  }

  void selectTargetAltHolidayDate(DateTime date) {
    state = state.copyWith(targetAltHolidayDate: date, clearError: true);
  }

  Future<void> submit() async {
    if (!state.canSubmit) return;

    state = state.copyWith(isLoading: true, clearError: true);
    try {
      await _repository.createAlternativeHoliday(
        actualWorkDate: state.actualWorkDate!,
        targetAltHolidayDate: state.targetAltHolidayDate!,
      );
      state = state.copyWith(isLoading: false, isSubmitted: true);
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

// ============================================
// 3. History State + Notifier
// ============================================

class AltHolidayHistoryState {
  final bool isLoading;
  final String? errorMessage;
  final List<AlternativeHoliday> items;
  final bool hasLoaded;

  const AltHolidayHistoryState({
    this.isLoading = false,
    this.errorMessage,
    this.items = const [],
    this.hasLoaded = false,
  });

  bool get isEmpty => hasLoaded && items.isEmpty;

  AltHolidayHistoryState copyWith({
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
    List<AlternativeHoliday>? items,
    bool? hasLoaded,
  }) {
    return AltHolidayHistoryState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      items: items ?? this.items,
      hasLoaded: hasLoaded ?? this.hasLoaded,
    );
  }
}

class AltHolidayHistoryNotifier extends StateNotifier<AltHolidayHistoryState> {
  final AlternativeHolidayRepository _repository;

  AltHolidayHistoryNotifier(this._repository)
      : super(const AltHolidayHistoryState());

  Future<void> loadHistory() async {
    state = state.copyWith(isLoading: true, clearError: true);
    try {
      final items = await _repository.getAlternativeHolidays();
      state = state.copyWith(
        isLoading: false,
        items: items,
        hasLoaded: true,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
        hasLoaded: true,
      );
    }
  }
}

// ============================================
// 4. Provider Definitions
// ============================================

final altHolidayRequestProvider =
    StateNotifierProvider.autoDispose<AltHolidayRequestNotifier, AltHolidayRequestState>(
        (ref) {
  return AltHolidayRequestNotifier(
    ref.watch(altHolidayRepositoryProvider),
  );
});

final altHolidayHistoryProvider =
    StateNotifierProvider.autoDispose<AltHolidayHistoryNotifier, AltHolidayHistoryState>(
        (ref) {
  return AltHolidayHistoryNotifier(
    ref.watch(altHolidayRepositoryProvider),
  );
});
