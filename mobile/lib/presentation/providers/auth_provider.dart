import 'dart:async';

import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/network/request_cancel_controller.dart';
import '../../core/services/fcm_token_registrar.dart';
import '../../core/session/session_reset_controller.dart';
import '../../data/datasources/auth_api_datasource.dart';
import '../../data/datasources/auth_local_datasource.dart';
import '../../data/datasources/auth_remote_datasource.dart';
import '../../data/repositories/auth_repository_impl.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/auto_login_usecase.dart';
import '../../domain/usecases/change_password_usecase.dart';
import '../../domain/usecases/check_gps_consent_usecase.dart';
import '../../domain/usecases/login_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import 'auth_state.dart';

// --- Providers ---

/// Auth Remote DataSource Provider
final authRemoteDataSourceProvider = Provider<AuthRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return AuthApiDataSource(dio);
});

/// Auth Repository Provider
final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final remoteDataSource = ref.watch(authRemoteDataSourceProvider);
  final localDataSource = ref.watch(authLocalDataSourceProvider);
  return AuthRepositoryImpl(
    remoteDataSource: remoteDataSource,
    localDataSource: localDataSource,
  );
});

/// Auth Local DataSource Provider
final authLocalDataSourceProvider = Provider<AuthLocalDataSource>((ref) {
  return AuthLocalDataSource();
});

/// LoginUseCase Provider
final loginUseCaseProvider = Provider<LoginUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return LoginUseCase(repository);
});

/// AutoLoginUseCase Provider
final autoLoginUseCaseProvider = Provider<AutoLoginUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return AutoLoginUseCase(repository);
});

/// ChangePasswordUseCase Provider
final changePasswordUseCaseProvider = Provider<ChangePasswordUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return ChangePasswordUseCase(repository);
});

/// LogoutUseCase Provider
final logoutUseCaseProvider = Provider<LogoutUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return LogoutUseCase(repository);
});

/// CheckGpsConsentUseCase Provider
final checkGpsConsentUseCaseProvider = Provider<CheckGpsConsentUseCase>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return CheckGpsConsentUseCase(repository);
});

// --- AuthNotifier ---

/// 인증 상태 관리 Notifier
///
/// 로그인, 자동 로그인, 비밀번호 변경, GPS 동의, 로그아웃 등
/// 인증 관련 상태를 관리합니다.
class AuthNotifier extends StateNotifier<AuthState> {
  final LoginUseCase _loginUseCase;
  final AutoLoginUseCase _autoLoginUseCase;
  final ChangePasswordUseCase _changePasswordUseCase;
  final LogoutUseCase _logoutUseCase;
  final AuthLocalDataSource _localDataSource;
  final AuthRepository _repository;
  final FcmTokenRegistrar _fcmTokenRegistrar;

  AuthNotifier({
    required LoginUseCase loginUseCase,
    required AutoLoginUseCase autoLoginUseCase,
    required ChangePasswordUseCase changePasswordUseCase,
    required LogoutUseCase logoutUseCase,
    required AuthLocalDataSource localDataSource,
    required AuthRepository repository,
    required FcmTokenRegistrar fcmTokenRegistrar,
  })  : _loginUseCase = loginUseCase,
        _autoLoginUseCase = autoLoginUseCase,
        _changePasswordUseCase = changePasswordUseCase,
        _logoutUseCase = logoutUseCase,
        _localDataSource = localDataSource,
        _repository = repository,
        _fcmTokenRegistrar = fcmTokenRegistrar,
        super(AuthState.initial());

  /// 인증 완료 시 FCM 토큰을 서버에 등록한다 (fire-and-forget — 인증 흐름 비차단).
  void _registerFcmToken() {
    unawaited(_fcmTokenRegistrar.registerCurrentToken());
  }

  /// 앱 시작 시 초기화
  ///
  /// 저장된 사번을 로드하고, 자동 로그인을 시도합니다.
  Future<void> initialize() async {
    // 저장된 사번 로드
    await loadSavedEmployeeNumber();

    // 자동 로그인 시도
    await tryAutoLogin();
  }

  /// 저장된 사번 로드 (Hive)
  Future<void> loadSavedEmployeeNumber() async {
    try {
      final savedId = await _localDataSource.getSavedEmployeeNumber();
      final rememberEnabled =
          await _localDataSource.isRememberEmployeeNumberEnabled();
      state = state.copyWith(
        savedEmployeeNumber: savedId,
        rememberEmployeeNumber: rememberEnabled,
      );
    } catch (_) {
      // Hive 로드 실패 시 무시
    }
  }

  /// 자동 로그인 시도
  ///
  /// Secure Storage에서 Refresh Token을 확인하고,
  /// 유효하면 자동 로그인을 수행합니다.
  Future<void> tryAutoLogin() async {
    try {
      final isAutoLogin = await _localDataSource.isAutoLoginEnabled();
      // 앱 종료/ProviderScope 재생성으로 notifier 가 dispose 되면 state 대입은
      // 예외를 던진다. await 뒤마다 mounted 를 확인해 dispose 후 작업을 가드한다.
      if (!mounted) return;
      if (!isAutoLogin) {
        state = state.toUnauthenticated();
        return;
      }

      final refreshToken = await _localDataSource.getRefreshToken();
      if (!mounted) return;
      if (refreshToken == null || refreshToken.isEmpty) {
        state = state.toUnauthenticated();
        return;
      }

      state = state.toLoading();

      // 토큰 갱신 시도 (refresh token 회전 — 새 토큰 페어 발급)
      final newToken =
          await _autoLoginUseCase(refreshToken: refreshToken);

      // 새 토큰 저장 — 회전된 refresh token 을 반드시 저장해야 다음 실행 시
      // 재사용 탐지(Token Family revoke)로 잠기지 않는다.
      await _localDataSource.saveAccessToken(newToken.accessToken);
      await _localDataSource.saveRefreshToken(newToken.refreshToken);

      // refresh 응답은 토큰만 주므로 /me 로 사용자 정보를 조회해 인증을 완료한다.
      // (방금 저장한 access token 이 인터셉터를 통해 자동 첨부됨)
      final user = await _repository.getMe();
      if (!mounted) return;
      state = state.toAuthenticated(user);
      _registerFcmToken();
    } catch (e) {
      // 백그라운드 전환 등으로 요청이 취소된 경우 — 인증 실패가 아니므로 토큰을
      // 삭제하지 않는다. 재개 시 다시 자동 로그인을 시도할 수 있어야 한다.
      if (isRequestCancelled(e)) return;
      // 자동 로그인 실패 → 토큰 삭제 → 로그인 화면
      await _localDataSource.clearTokens();
      if (!mounted) return;
      state = state.toUnauthenticated();
    }
  }

  /// 로그인 수행
  ///
  /// [employeeCode]: 사번
  /// [password]: 비밀번호
  /// [rememberEmployeeNumber]: 아이디 기억하기
  /// [autoLogin]: 자동 로그인
  Future<void> login({
    required String employeeCode,
    required String password,
    required bool rememberEmployeeNumber,
    required bool autoLogin,
  }) async {
    state = state.toLoading();

    try {
      final result = await _loginUseCase(
        employeeCode: employeeCode,
        password: password,
        rememberEmployeeNumber: rememberEmployeeNumber,
        autoLogin: autoLogin,
      );

      // 토큰 저장
      await _localDataSource.saveAccessToken(result.token.accessToken);
      await _localDataSource.saveRefreshToken(result.token.refreshToken);

      // 아이디 기억하기 처리
      if (rememberEmployeeNumber) {
        await _localDataSource.saveEmployeeNumber(employeeCode);
      } else {
        await _localDataSource.clearSavedEmployeeNumber();
      }

      // 저장소(Hive)와 메모리 상태를 동기화한다.
      // 동기화하지 않으면 같은 세션에서 로그인 → 로그아웃 후 로그인 화면 재진입 시
      // 앱 시작 시점에 읽어둔 옛 사번이 그대로 프리필되는 문제가 있다.
      // (rememberEmployeeNumber=false 인 경우 화면 프리필은 아래 _loadSavedSettings 가
      //  rememberEmployeeNumber 플래그로 걸러낸다.)
      state = state.copyWith(
        savedEmployeeNumber:
            rememberEmployeeNumber ? employeeCode : state.savedEmployeeNumber,
        rememberEmployeeNumber: rememberEmployeeNumber,
      );

      // 자동 로그인 설정
      await _localDataSource.setAutoLogin(autoLogin);

      // 비밀번호 변경 / GPS 동의 필요 여부 확인
      if (result.passwordChangeRequired) {
        state = state.copyWith(
          isLoading: false,
          user: result.user,
          errorMessage: null,
          passwordChangeRequired: true,
          requiresGpsConsent: result.requiresGpsConsent,
          rememberEmployeeNumber: rememberEmployeeNumber,
          autoLogin: autoLogin,
          isInitialized: true,
        );
      } else if (result.requiresGpsConsent) {
        state = state.copyWith(
          isLoading: false,
          user: result.user,
          errorMessage: null,
          passwordChangeRequired: false,
          requiresGpsConsent: true,
          rememberEmployeeNumber: rememberEmployeeNumber,
          autoLogin: autoLogin,
          isInitialized: true,
        );
      } else {
        // 모든 조건 통과 → 인증 완료
        state = state.toAuthenticated(result.user);
        _registerFcmToken();
      }
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 비밀번호 변경 (강제/자발 통합 — Spec #584).
  ///
  /// 강제 변경 (`state.passwordChangeRequired=true`) 시 [currentPassword] 는 null/empty 로 호출.
  /// 응답에 새 토큰 페어 포함 — 호출 측에서 별도 저장 없이 [AuthRepositoryImpl] 가 처리.
  Future<void> changePassword({
    String? currentPassword,
    required String newPassword,
  }) async {
    state = state.toLoading();

    try {
      await _changePasswordUseCase(
        currentPassword: currentPassword,
        newPassword: newPassword,
      );

      // 비밀번호 변경 완료 후 GPS 동의 확인
      if (state.requiresGpsConsent) {
        state = state.copyWith(
          isLoading: false,
          passwordChangeRequired: false,
          errorMessage: null,
        );
      } else {
        // 모든 조건 통과 → 인증 완료
        state = state.toAuthenticated(state.user!);
        _registerFcmToken();
      }
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// GPS 동의 기록
  ///
  /// [agreementNumber]: 동의한 약관 번호 (선택)
  Future<void> recordGpsConsent({String? agreementNumber}) async {
    state = state.toLoading();

    try {
      final result = await _repository.recordGpsConsent(
        agreementNumber: agreementNumber,
      );

      // 새 access_token 저장
      await _localDataSource.saveAccessToken(result.accessToken);

      // GPS 동의 완료 → 인증 완료
      if (state.user != null) {
        state = state.toAuthenticated(state.user!);
        _registerFcmToken();
      } else {
        state = state.copyWith(
          isLoading: false,
          requiresGpsConsent: false,
        );
      }
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 로그아웃
  Future<void> logout() async {
    // FCM 토큰 해제 — access token 이 아직 유효한 지금(토큰 삭제 전) 수행.
    await _fcmTokenRegistrar.unregister();

    try {
      await _logoutUseCase();
    } catch (_) {
      // 로그아웃 실패해도 로컬 토큰은 삭제
    }

    // 토큰 삭제
    await _localDataSource.clearTokens();
    await _localDataSource.setAutoLogin(false);

    // 전역 상태 초기화 — 루트 ProviderScope 를 재생성해 모든 Provider(도메인 캐시 포함)를
    // 폐기한다. 이를 통해 다른 계정으로 재로그인 시 이전 사용자의 잔여 데이터가 노출되지 않는다.
    // (별도로 state 를 unauthenticated 로 바꾸지 않아도 재생성된 세션이 로그인 화면에서 시작한다.)
    SessionResetController.instance.requestReset();
  }

  /// 에러 메시지 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

/// Auth StateNotifier Provider
final authProvider =
    StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    loginUseCase: ref.watch(loginUseCaseProvider),
    autoLoginUseCase: ref.watch(autoLoginUseCaseProvider),
    changePasswordUseCase: ref.watch(changePasswordUseCaseProvider),
    logoutUseCase: ref.watch(logoutUseCaseProvider),
    localDataSource: ref.watch(authLocalDataSourceProvider),
    repository: ref.watch(authRepositoryProvider),
    fcmTokenRegistrar: ref.watch(fcmTokenRegistrarProvider),
  );
});
