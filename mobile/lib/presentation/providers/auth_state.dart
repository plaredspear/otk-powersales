import '../../domain/entities/user.dart';

/// 인증 상태
///
/// 앱 전체의 인증 상태를 관리합니다.
/// - initial: 앱 시작 직후, 자동 로그인 확인 전
/// - loading: API 호출 중 (로그인, 자동 로그인, 비밀번호 변경 등)
/// - authenticated: 인증 완료 (홈 화면 진입 가능)
/// - unauthenticated: 미인증 (로그인 화면 표시)
/// - requiresPasswordChange: 초기 비밀번호 변경 필요
/// - requiresGpsConsent: GPS 동의 필요
/// - error: 인증 에러 발생
class AuthState {
  /// 로딩 상태 (API 호출 중)
  final bool isLoading;

  /// 로그인된 사용자 정보
  final User? user;

  /// 에러 메시지
  final String? errorMessage;

  /// 비밀번호 변경 필요 여부
  final bool requiresPasswordChange;

  /// GPS 동의 필요 여부
  final bool requiresGpsConsent;

  /// 기억된 사번 (아이디 기억하기)
  final String? savedEmployeeId;

  /// 아이디 기억하기 활성화 여부
  final bool rememberEmployeeId;

  /// 자동 로그인 활성화 여부
  final bool autoLogin;

  /// 초기화 완료 여부 (자동 로그인 확인 완료)
  final bool isInitialized;

  const AuthState({
    this.isLoading = false,
    this.user,
    this.errorMessage,
    this.requiresPasswordChange = false,
    this.requiresGpsConsent = false,
    this.savedEmployeeId,
    this.rememberEmployeeId = false,
    this.autoLogin = false,
    this.isInitialized = false,
  });

  /// 초기 상태
  factory AuthState.initial() {
    return const AuthState();
  }

  /// 로딩 상태로 전환
  AuthState toLoading() {
    return copyWith(
      isLoading: true,
      errorMessage: null,
    );
  }

  /// 인증 완료 상태로 전환
  AuthState toAuthenticated(User user) {
    return copyWith(
      isLoading: false,
      user: user,
      errorMessage: null,
      requiresPasswordChange: false,
      requiresGpsConsent: false,
      isInitialized: true,
    );
  }

  /// 미인증 상태로 전환
  AuthState toUnauthenticated() {
    return AuthState(
      isLoading: false,
      user: null,
      errorMessage: null,
      requiresPasswordChange: false,
      requiresGpsConsent: false,
      savedEmployeeId: savedEmployeeId,
      rememberEmployeeId: rememberEmployeeId,
      autoLogin: false,
      isInitialized: true,
    );
  }

  /// 에러 상태로 전환
  AuthState toError(String message) {
    return copyWith(
      isLoading: false,
      errorMessage: message,
      isInitialized: true,
    );
  }

  /// 인증 완료 여부
  bool get isAuthenticated =>
      user != null &&
      !requiresPasswordChange &&
      !requiresGpsConsent;

  /// copyWith
  AuthState copyWith({
    bool? isLoading,
    User? user,
    String? errorMessage,
    bool? requiresPasswordChange,
    bool? requiresGpsConsent,
    String? savedEmployeeId,
    bool? rememberEmployeeId,
    bool? autoLogin,
    bool? isInitialized,
  }) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      user: user ?? this.user,
      errorMessage: errorMessage,
      requiresPasswordChange:
          requiresPasswordChange ?? this.requiresPasswordChange,
      requiresGpsConsent: requiresGpsConsent ?? this.requiresGpsConsent,
      savedEmployeeId: savedEmployeeId ?? this.savedEmployeeId,
      rememberEmployeeId: rememberEmployeeId ?? this.rememberEmployeeId,
      autoLogin: autoLogin ?? this.autoLogin,
      isInitialized: isInitialized ?? this.isInitialized,
    );
  }
}
