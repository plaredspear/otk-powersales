import '../../domain/entities/notice_post_detail.dart';

/// 공지사항 상세 화면 상태
///
/// 로딩/에러 상태와 상세 데이터를 포함합니다.
class NoticeDetailState {
  /// 로딩 상태
  final bool isLoading;

  /// 에러 메시지
  final String? errorMessage;

  /// 공지사항 상세 데이터
  final NoticePostDetail? detail;

  const NoticeDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.detail,
  });

  /// 초기 상태
  factory NoticeDetailState.initial() {
    return const NoticeDetailState();
  }

  /// 로딩 상태로 전환
  NoticeDetailState toLoading() {
    return const NoticeDetailState(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 에러 상태로 전환
  NoticeDetailState toError(String message) {
    return NoticeDetailState(
      isLoading: false,
      errorMessage: message,
    );
  }

  /// 성공 상태로 전환
  NoticeDetailState toSuccess(NoticePostDetail detail) {
    return NoticeDetailState(
      isLoading: false,
      errorMessage: null,
      detail: detail,
    );
  }

  /// 데이터가 로드되었는지 여부
  bool get hasData => detail != null;

  NoticeDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    NoticePostDetail? detail,
  }) {
    return NoticeDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      detail: detail ?? this.detail,
    );
  }
}
