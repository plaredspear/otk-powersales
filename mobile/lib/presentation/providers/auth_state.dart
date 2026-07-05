import '../../domain/entities/user.dart';

/// 인증 상태
///
/// 앱 전체의 인증 상태를 관리합니다.
/// - initial: 앱 시작 직후, 자동 로그인 확인 전
/// - loading: API 호출 중 (로그인, 자동 로그인, 비밀번호 변경 등)
/// - authenticated: 인증 완료 (홈 화면 진입 가능)
/// - unauthenticated: 미인증 (로그인 화면 표시)
/// - passwordChangeRequired: 초기 비밀번호 변경 필요
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
  final bool passwordChangeRequired;

  /// GPS 동의 필요 여부
  final bool requiresGpsConsent;

  /// 기억된 사번 (아이디 기억하기)
  final String? savedEmployeeNumber;

  /// 아이디 기억하기 활성화 여부
  final bool rememberEmployeeNumber;

  /// 자동 로그인 활성화 여부
  final bool autoLogin;

  /// 초기화 완료 여부 (자동 로그인 확인 완료)
  final bool isInitialized;

  const AuthState({
    this.isLoading = false,
    this.user,
    this.errorMessage,
    this.passwordChangeRequired = false,
    this.requiresGpsConsent = false,
    this.savedEmployeeNumber,
    this.rememberEmployeeNumber = false,
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
      passwordChangeRequired: false,
      requiresGpsConsent: false,
      isInitialized: true,
    );
  }

  /// 미인증 상태로 전환
  ///
  /// 로그인 화면 프리필용 사용자 설정(사번 기억하기 / 자동 로그인 선택)은 보존한다.
  /// 자동 로그인 실패 등으로 미인증이 되어도, 저장소의 `auto_login` 플래그는 그대로이며
  /// 레거시(앱이 isAutoLogin 저장값을 계속 보관)와 정합하도록 UI 선택을 유지해야 한다.
  ///
  /// "보존이 기본, 리셋만 명시" 구조로 `copyWith` 를 사용한다 — State 에 필드가 추가돼도
  /// 사용자 설정 필드가 조용히 기본값으로 리셋되는 누락 취약을 방지한다.
  AuthState toUnauthenticated() {
    return copyWith(
      isLoading: false,
      clearUser: true,
      passwordChangeRequired: false,
      requiresGpsConsent: false,
      autoLogin: autoLogin,
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
      !passwordChangeRequired &&
      !requiresGpsConsent;

  /// copyWith
  ///
  /// [clearUser] 가 true 면 [user] 를 null 로 강제한다 — nullable 필드는 `user ?? this.user`
  /// 로는 null 로 되돌릴 수 없으므로, 미인증/로그아웃 전환에서 명시적 clear 플래그를 쓴다.
  AuthState copyWith({
    bool? isLoading,
    User? user,
    bool clearUser = false,
    String? errorMessage,
    bool? passwordChangeRequired,
    bool? requiresGpsConsent,
    String? savedEmployeeNumber,
    bool? rememberEmployeeNumber,
    bool? autoLogin,
    bool? isInitialized,
  }) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      user: clearUser ? null : (user ?? this.user),
      errorMessage: errorMessage,
      passwordChangeRequired:
          passwordChangeRequired ?? this.passwordChangeRequired,
      requiresGpsConsent: requiresGpsConsent ?? this.requiresGpsConsent,
      savedEmployeeNumber: savedEmployeeNumber ?? this.savedEmployeeNumber,
      rememberEmployeeNumber: rememberEmployeeNumber ?? this.rememberEmployeeNumber,
      autoLogin: autoLogin ?? this.autoLogin,
      isInitialized: isInitialized ?? this.isInitialized,
    );
  }
}
