import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repositories/mock/daily_sales_mock_repository.dart';
import '../../domain/entities/event.dart';
import '../../domain/repositories/daily_sales_repository.dart';
import '../../domain/usecases/register_daily_sales.dart';
import 'daily_sales_form_state.dart';

// --- Dependency Providers ---

/// DailySalesRepository Provider (Mock)
final dailySalesRepositoryProvider = Provider<DailySalesRepository>((ref) {
  return DailySalesMockRepository();
});

/// RegisterDailySales UseCase Provider
final registerDailySalesUseCaseProvider = Provider<RegisterDailySales>((ref) {
  final repository = ref.watch(dailySalesRepositoryProvider);
  return RegisterDailySales(repository);
});

// --- DailySalesFormNotifier ---

/// 일매출 등록 폼 상태 관리 Notifier
///
/// 일매출 등록 폼의 상태를 관리하고, 제품 정보 입력, 사진 첨부,
/// 최종 등록 및 임시저장 기능을 제공합니다.
class DailySalesFormNotifier extends StateNotifier<DailySalesFormState> {
  final RegisterDailySales _registerDailySales;

  DailySalesFormNotifier({
    required RegisterDailySales registerDailySales,
    Event? initialEvent,
  })  : _registerDailySales = registerDailySales,
        super(DailySalesFormState.initial(event: initialEvent));

  /// 대표제품 정보 업데이트
  ///
  /// [price]: 판매단가 (원)
  /// [quantity]: 판매수량 (개)
  /// [amount]: 총 판매금액 (원, 자동 계산 가능)
  void updateMainProduct({
    int? price,
    int? quantity,
    int? amount,
  }) {
    // 업데이트할 값 결정 (null이면 기존 값 유지)
    final newPrice = price ?? state.mainProductPrice;
    final newQuantity = quantity ?? state.mainProductQuantity;

    // amount가 명시적으로 전달되지 않으면 자동 계산
    final calculatedAmount = amount ??
        (newPrice != null && newQuantity != null
            ? newPrice * newQuantity
            : null);

    state = state.copyWith(
      mainProductPrice: price,
      mainProductQuantity: quantity,
      mainProductAmount: calculatedAmount,
    );
  }

  /// 기타제품 정보 업데이트
  ///
  /// [code]: 기타제품 코드
  /// [name]: 기타제품명
  /// [quantity]: 판매수량 (개)
  /// [amount]: 총 판매금액 (원)
  void updateSubProduct({
    String? code,
    String? name,
    int? quantity,
    int? amount,
  }) {
    state = state.copyWith(
      subProductCode: code,
      subProductName: name,
      subProductQuantity: quantity,
      subProductAmount: amount,
    );
  }

  /// 사진 업데이트
  ///
  /// [photo]: 첨부할 사진 파일
  void updatePhoto(File? photo) {
    state = state.copyWith(photo: photo);
  }

  /// 행사 선택
  ///
  /// [event]: 선택한 행사
  void selectEvent(Event event) {
    state = state.copyWith(selectedEvent: event);
  }

  /// 일매출 최종 등록
  ///
  /// 필수 항목(제품 정보, 사진) 검증 후 등록합니다.
  /// Returns: 등록 성공 여부
  Future<bool> submit() async {
    // 유효성 검증
    if (!state.isValid) {
      state = state.toError('필수 항목을 모두 입력해주세요');
      return false;
    }

    if (state.selectedEvent == null) {
      state = state.toError('행사를 선택해주세요');
      return false;
    }

    // 제출 중 상태로 변경
    state = state.toSubmitting();

    try {
      final request = DailySalesRequest(
        eventId: state.selectedEvent!.id,
        salesDate: state.date,
        mainProductPrice: state.mainProductPrice,
        mainProductQuantity: state.mainProductQuantity,
        mainProductAmount: state.mainProductAmount,
        subProductCode: state.subProductCode,
        subProductName: state.subProductName,
        subProductQuantity: state.subProductQuantity,
        subProductAmount: state.subProductAmount,
        photo: state.photo,
      );

      await _registerDailySales.call(request);

      // 제출 성공 상태로 변경
      state = state.toSuccess();
      return true;
    } catch (e) {
      // 제출 실패 상태로 변경
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
      return false;
    }
  }

  /// 일매출 임시저장
  ///
  /// 유효성 검증 없이 현재 입력 상태를 임시저장합니다.
  /// Returns: 임시저장 성공 여부
  Future<bool> saveDraft() async {
    if (state.selectedEvent == null) {
      state = state.toError('행사를 선택해주세요');
      return false;
    }

    // 제출 중 상태로 변경
    state = state.toSubmitting();

    try {
      final request = DailySalesRequest(
        eventId: state.selectedEvent!.id,
        salesDate: state.date,
        mainProductPrice: state.mainProductPrice,
        mainProductQuantity: state.mainProductQuantity,
        mainProductAmount: state.mainProductAmount,
        subProductCode: state.subProductCode,
        subProductName: state.subProductName,
        subProductQuantity: state.subProductQuantity,
        subProductAmount: state.subProductAmount,
        photo: state.photo,
      );

      await _registerDailySales.saveDraft(request);

      // 제출 성공 상태로 변경
      state = state.toSuccess();
      return true;
    } catch (e) {
      // 제출 실패 상태로 변경
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
      );
      return false;
    }
  }

  /// 폼 상태 초기화
  ///
  /// 제출 상태만 리셋하고 입력 데이터는 유지합니다.
  void resetSubmitStatus() {
    state = state.resetSubmitStatus();
  }

  /// 전체 폼 초기화
  ///
  /// 모든 입력 데이터와 상태를 초기화합니다.
  void reset({Event? event}) {
    state = DailySalesFormState.initial(event: event);
  }
}

/// DailySalesForm StateNotifier Provider
final dailySalesFormProvider =
    StateNotifierProvider<DailySalesFormNotifier, DailySalesFormState>((ref) {
  return DailySalesFormNotifier(
    registerDailySales: ref.watch(registerDailySalesUseCaseProvider),
  );
});

/// 행사별 DailySalesForm StateNotifier Provider
///
/// [eventId]: 행사 ID를 파라미터로 받아 해당 행사로 초기화된 Provider 제공
final dailySalesFormProviderFamily = StateNotifierProvider.family<
    DailySalesFormNotifier,
    DailySalesFormState,
    Event?>((ref, event) {
  return DailySalesFormNotifier(
    registerDailySales: ref.watch(registerDailySalesUseCaseProvider),
    initialEvent: event,
  );
});
