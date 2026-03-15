import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/safety_check_api_datasource.dart';
import '../../data/repositories/safety_check_repository_impl.dart';
import '../../domain/repositories/safety_check_repository.dart';
import '../../domain/usecases/get_safety_check_items.dart';
import '../../domain/usecases/get_safety_check_today_status.dart';
import '../../domain/usecases/submit_safety_check.dart';
import 'safety_check_state.dart';

/// SafetyCheck Repository Provider (실제 API)
final safetyCheckRepositoryProvider = Provider<SafetyCheckRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = SafetyCheckApiDataSource(dio);
  return SafetyCheckRepositoryImpl(remoteDataSource: dataSource);
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

/// 안전점검 화면 상태 관리 Notifier (V1)
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

  /// 섹션 1: 장비 라디오 응답 설정
  void setEquipmentAnswer(int seqNum, String answer) {
    state = state.setEquipmentAnswer(seqNum, answer);
  }

  /// 섹션 2: 예방사항 체크박스 토글
  void togglePrecaution(int seqNum) {
    state = state.togglePrecaution(seqNum);
  }

  /// 안전점검 제출
  Future<void> submit() async {
    if (!state.allRequiredChecked) return;

    state = state.toSubmitting();

    try {
      final equipments = state.equipmentAnswers.entries
          .map((e) => EquipmentAnswer(seqNum: e.key, answer: e.value))
          .toList()
        ..sort((a, b) => a.seqNum.compareTo(b.seqNum));

      // 체크된 예방사항의 contents 텍스트 수집
      final precautions = <String>[];
      if (state.categories != null) {
        for (final category in state.categories!) {
          if (category.inputType == 'CHECKBOX') {
            for (final item in category.items) {
              if (state.precautionChecks[item.seqNum] == true) {
                precautions.add(item.contents);
              }
            }
          }
        }
      }

      await _submitSafetyCheck(
        startTime: state.startTime ?? DateTime.now(),
        completeTime: DateTime.now(),
        equipments: equipments,
        precautions: precautions.isEmpty ? null : precautions,
      );

      state = state.toSubmitted();
    } on DioException catch (e) {
      if (e.response?.statusCode == 409) {
        // 중복 제출 → 이미 완료로 처리
        state = state.toSubmitted();
      } else {
        state = state.toError(_extractErrorMessage(e));
      }
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  String _extractErrorMessage(DioException e) {
    try {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) {
        final error = data['error'];
        if (error is Map<String, dynamic>) {
          return error['message'] as String? ?? '서버 오류가 발생했습니다.';
        }
      }
    } catch (_) {}
    return '네트워크 오류가 발생했습니다.';
  }
}

/// SafetyCheck StateNotifier Provider
final safetyCheckProvider =
    StateNotifierProvider<SafetyCheckNotifier, SafetyCheckState>((ref) {
  final getItems = ref.watch(getSafetyCheckItemsUseCaseProvider);
  final submit = ref.watch(submitSafetyCheckUseCaseProvider);
  return SafetyCheckNotifier(getItems, submit);
});
