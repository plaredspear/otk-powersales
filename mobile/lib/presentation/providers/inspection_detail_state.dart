import '../../domain/entities/inspection_detail.dart';

/// 현장점검 상세 화면 상태
class InspectionDetailState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 현장점검 상세 정보
  final InspectionDetail? detail;

  const InspectionDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.detail,
  });

  /// 초기 상태
  factory InspectionDetailState.initial() {
    return const InspectionDetailState();
  }

  /// 로딩 상태로 전환
  InspectionDetailState toLoading() {
    return copyWith(isLoading: true, errorMessage: null);
  }

  /// 에러 상태로 전환
  InspectionDetailState toError(String message) {
    return copyWith(isLoading: false, errorMessage: message);
  }

  /// 데이터가 로드되었는지 여부
  bool get hasData => detail != null;

  /// 자사 점검 여부
  bool get isOwn => hasData && detail!.isOwn;

  /// 경쟁사 점검 여부
  bool get isCompetitor => hasData && detail!.isCompetitor;

  /// 경쟁사 시식 여부
  bool get hasTasting => isCompetitor && detail!.competitorTasting == true;

  InspectionDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    InspectionDetail? detail,
  }) {
    return InspectionDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      detail: detail ?? this.detail,
    );
  }
}
