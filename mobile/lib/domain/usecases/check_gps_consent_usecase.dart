import '../repositories/auth_repository.dart';

/// GPS 동의 상태 확인 UseCase
///
/// 포그라운드 복귀 시 GPS 재동의 필요 여부를 확인합니다.
/// - 1시간 이내 재확인은 스킵 (호출 빈도 제어)
/// - API 호출 실패 시 false 반환 (앱 사용을 차단하지 않음)
class CheckGpsConsentUseCase {
  final AuthRepository _repository;

  /// 마지막 확인 시점
  DateTime? _lastCheckTime;

  /// 빈도 제어 간격 (1시간)
  static const _throttleDuration = Duration(hours: 1);

  CheckGpsConsentUseCase(this._repository);

  /// GPS 재동의가 필요한지 확인
  ///
  /// Returns: true면 GPS 동의 화면 표시 필요
  Future<bool> call() async {
    // 빈도 제어: 1시간 이내 재확인 스킵
    if (_lastCheckTime != null) {
      final elapsed = DateTime.now().difference(_lastCheckTime!);
      if (elapsed < _throttleDuration) {
        return false;
      }
    }

    try {
      final status = await _repository.getGpsConsentStatus();
      _lastCheckTime = DateTime.now();
      return status.requiresGpsConsent;
    } catch (_) {
      // API 호출 실패 시 재동의 확인 스킵
      // 서버 강제(403)가 보안을 보완
      return false;
    }
  }

  /// 마지막 확인 시점 초기화 (테스트용)
  void resetLastCheckTime() {
    _lastCheckTime = null;
  }
}
