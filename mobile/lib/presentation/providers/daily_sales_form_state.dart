import 'dart:io';

import '../../domain/entities/event.dart';

/// 일매출 등록 폼 제출 상태
enum SubmitStatus {
  /// 대기 중
  idle,

  /// 제출 중
  submitting,

  /// 제출 성공
  success,

  /// 제출 실패
  error,
}

/// 일매출 등록 폼 상태
class DailySalesFormState {
  /// 선택된 행사
  final Event? selectedEvent;

  /// 매출 일자 (오늘 날짜 고정)
  final DateTime date;

  /// 대표제품 판매단가 (원)
  final int? mainProductPrice;

  /// 대표제품 판매수량 (개)
  final int? mainProductQuantity;

  /// 대표제품 총 판매금액 (원)
  final int? mainProductAmount;

  /// 기타제품 코드
  final String? subProductCode;

  /// 기타제품명
  final String? subProductName;

  /// 기타제품 판매수량 (개)
  final int? subProductQuantity;

  /// 기타제품 총 판매금액 (원)
  final int? subProductAmount;

  /// 첨부 사진
  final File? photo;

  /// 제출 상태
  final SubmitStatus submitStatus;

  /// 에러 메시지
  final String? errorMessage;

  const DailySalesFormState({
    this.selectedEvent,
    required this.date,
    this.mainProductPrice,
    this.mainProductQuantity,
    this.mainProductAmount,
    this.subProductCode,
    this.subProductName,
    this.subProductQuantity,
    this.subProductAmount,
    this.photo,
    this.submitStatus = SubmitStatus.idle,
    this.errorMessage,
  });

  /// 초기 상태 생성
  factory DailySalesFormState.initial({Event? event}) {
    return DailySalesFormState(
      selectedEvent: event,
      date: DateTime.now(),
    );
  }

  /// 대표제품 정보가 완전히 입력되었는지 확인
  bool get hasMainProduct =>
      mainProductPrice != null &&
      mainProductQuantity != null &&
      mainProductAmount != null;

  /// 기타제품 정보가 완전히 입력되었는지 확인
  bool get hasSubProduct =>
      subProductCode != null &&
      subProductName != null &&
      subProductQuantity != null &&
      subProductAmount != null;

  /// 최소 하나의 제품 정보가 입력되었는지 확인
  bool get hasAnyProduct => hasMainProduct || hasSubProduct;

  /// 등록 가능 여부 (모든 필수 항목 충족)
  bool get isValid => hasAnyProduct && photo != null;

  /// 임시저장 가능 여부 (항상 true)
  bool get isDraftValid => true;

  /// 제출 중 여부
  bool get isSubmitting => submitStatus == SubmitStatus.submitting;

  /// 제출 성공 여부
  bool get isSuccess => submitStatus == SubmitStatus.success;

  /// 제출 실패 여부
  bool get isError => submitStatus == SubmitStatus.error;

  /// 대표제품 총 판매금액 자동 계산
  int? calculateMainProductAmount() {
    if (mainProductPrice != null && mainProductQuantity != null) {
      return mainProductPrice! * mainProductQuantity!;
    }
    return null;
  }

  /// 제출 중 상태로 변경
  DailySalesFormState toSubmitting() {
    return copyWith(
      submitStatus: SubmitStatus.submitting,
      errorMessage: null,
    );
  }

  /// 제출 성공 상태로 변경
  DailySalesFormState toSuccess() {
    return copyWith(
      submitStatus: SubmitStatus.success,
      errorMessage: null,
    );
  }

  /// 제출 실패 상태로 변경
  DailySalesFormState toError(String message) {
    return copyWith(
      submitStatus: SubmitStatus.error,
      errorMessage: message,
    );
  }

  /// 상태 초기화 (제출 상태만 리셋)
  DailySalesFormState resetSubmitStatus() {
    return DailySalesFormState(
      selectedEvent: selectedEvent,
      date: date,
      mainProductPrice: mainProductPrice,
      mainProductQuantity: mainProductQuantity,
      mainProductAmount: mainProductAmount,
      subProductCode: subProductCode,
      subProductName: subProductName,
      subProductQuantity: subProductQuantity,
      subProductAmount: subProductAmount,
      photo: photo,
      submitStatus: SubmitStatus.idle,
      errorMessage: null,
    );
  }

  DailySalesFormState copyWith({
    Event? selectedEvent,
    DateTime? date,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    File? photo,
    SubmitStatus? submitStatus,
    String? errorMessage,
  }) {
    return DailySalesFormState(
      selectedEvent: selectedEvent ?? this.selectedEvent,
      date: date ?? this.date,
      mainProductPrice: mainProductPrice ?? this.mainProductPrice,
      mainProductQuantity: mainProductQuantity ?? this.mainProductQuantity,
      mainProductAmount: mainProductAmount ?? this.mainProductAmount,
      subProductCode: subProductCode ?? this.subProductCode,
      subProductName: subProductName ?? this.subProductName,
      subProductQuantity: subProductQuantity ?? this.subProductQuantity,
      subProductAmount: subProductAmount ?? this.subProductAmount,
      photo: photo ?? this.photo,
      submitStatus: submitStatus ?? this.submitStatus,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is DailySalesFormState &&
        other.selectedEvent == selectedEvent &&
        other.date == date &&
        other.mainProductPrice == mainProductPrice &&
        other.mainProductQuantity == mainProductQuantity &&
        other.mainProductAmount == mainProductAmount &&
        other.subProductCode == subProductCode &&
        other.subProductName == subProductName &&
        other.subProductQuantity == subProductQuantity &&
        other.subProductAmount == subProductAmount &&
        other.photo == photo &&
        other.submitStatus == submitStatus &&
        other.errorMessage == errorMessage;
  }

  @override
  int get hashCode {
    return Object.hash(
      selectedEvent,
      date,
      mainProductPrice,
      mainProductQuantity,
      mainProductAmount,
      subProductCode,
      subProductName,
      subProductQuantity,
      subProductAmount,
      photo,
      submitStatus,
      errorMessage,
    );
  }

  @override
  String toString() {
    return 'DailySalesFormState(selectedEvent: $selectedEvent, date: $date, '
        'mainProductPrice: $mainProductPrice, mainProductQuantity: $mainProductQuantity, '
        'mainProductAmount: $mainProductAmount, '
        'subProductCode: $subProductCode, subProductName: $subProductName, '
        'subProductQuantity: $subProductQuantity, subProductAmount: $subProductAmount, '
        'photo: $photo, submitStatus: $submitStatus, errorMessage: $errorMessage)';
  }
}
