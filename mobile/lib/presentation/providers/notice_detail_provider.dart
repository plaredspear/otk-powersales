import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/usecases/get_notice_post_detail_usecase.dart';
import 'notice_detail_state.dart';
import 'notice_list_provider.dart';

// --- Dependency Providers ---

/// GetNoticePostDetail UseCase Provider
final getNoticePostDetailUseCaseProvider =
    Provider<GetNoticePostDetailUseCase>((ref) {
  final repository = ref.watch(noticeRepositoryProvider);
  return GetNoticePostDetailUseCase(repository);
});

// --- NoticeDetailNotifier ---

/// 공지사항 상세 상태 관리 Notifier
///
/// 공지사항 상세 정보 로딩 기능을 관리합니다.
class NoticeDetailNotifier extends StateNotifier<NoticeDetailState> {
  final GetNoticePostDetailUseCase _getNoticePostDetail;

  NoticeDetailNotifier({
    required GetNoticePostDetailUseCase getNoticePostDetail,
  })  : _getNoticePostDetail = getNoticePostDetail,
        super(NoticeDetailState.initial());

  /// 공지사항 상세 조회
  Future<void> loadDetail(int noticeId) async {
    try {
      state = state.toLoading();

      final detail = await _getNoticePostDetail.call(noticeId);

      state = state.toSuccess(detail);
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 새로고침
  Future<void> refresh(int noticeId) async {
    await loadDetail(noticeId);
  }

  /// 상태 초기화
  void reset() {
    state = NoticeDetailState.initial();
  }
}

/// Notice Detail Provider
final noticeDetailProvider =
    StateNotifierProvider<NoticeDetailNotifier, NoticeDetailState>((ref) {
  final getNoticePostDetail = ref.watch(getNoticePostDetailUseCaseProvider);
  return NoticeDetailNotifier(getNoticePostDetail: getNoticePostDetail);
});
