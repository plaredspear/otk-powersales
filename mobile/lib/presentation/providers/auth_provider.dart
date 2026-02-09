import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/auth_local_datasource.dart';
import '../../data/repositories/mock/auth_mock_repository.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/auto_login_usecase.dart';
import '../../domain/usecases/change_password_usecase.dart';
import '../../domain/usecases/login_usecase.dart';
import '../../domain/usecases/logout_usecase.dart';
import 'auth_state.dart';

// --- Providers ---

/// Auth Repository Provider
final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthMockRepository();
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

  AuthNotifier({
    required LoginUseCase loginUseCase,
    required AutoLoginUseCase autoLoginUseCase,
    required ChangePasswordUseCase changePasswordUseCase,
    required LogoutUseCase logoutUseCase,
    required AuthLocalDataSource localDataSource,
    required AuthRepository repository,
  })  : _loginUseCase = loginUseCase,
        _autoLoginUseCase = autoLoginUseCase,
        _changePasswordUseCase = changePasswordUseCase,
        _logoutUseCase = logoutUseCase,
        _localDataSource = localDataSource,
        _repository = repository,
        super(AuthState.initial());

  /// 앱 시작 시 초기화
  ///
  /// 저장된 사번을 로드하고, 자동 로그인을 시도합니다.
  Future<void> initialize() async {
    // 저장된 사번 로드
    await loadSavedEmployeeId();

    // 자동 로그인 시도
    await tryAutoLogin();
  }

  /// 저장된 사번 로드 (Hive)
  Future<void> loadSavedEmployeeId() async {
    try {
      final savedId = await _localDataSource.getSavedEmployeeId();
      final rememberEnabled =
          await _localDataSource.isRememberEmployeeIdEnabled();
      state = state.copyWith(
        savedEmployeeId: savedId,
        rememberEmployeeId: rememberEnabled,
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
      if (!isAutoLogin) {
        state = state.toUnauthenticated();
        return;
      }

      final refreshToken = await _localDataSource.getRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        state = state.toUnauthenticated();
        return;
      }

      state = state.toLoading();

      // 토큰 갱신 시도
      final newToken =
          await _autoLoginUseCase(refreshToken: refreshToken);

      // 새 토큰 저장
      await _localDataSource.saveAccessToken(newToken.accessToken);
      await _localDataSource.saveRefreshToken(newToken.refreshToken);

      // 자동 로그인 성공 시에도 user 정보가 필요하나,
      // refresh API는 token만 반환하므로 미인증 상태로 처리하지 않고
      // 홈 화면으로 이동 (향후 /me API로 사용자 정보 조회 가능)
      // 현재 Mock에서는 간단히 인증 완료 처리
      state = state.toUnauthenticated();
      // NOTE: 실제 API 연동 시에는 토큰으로 사용자 정보를 조회하여
      // toAuthenticated(user)를 호출해야 합니다.
      // 현재 Mock 단계에서는 자동 로그인 시 로그인 화면으로 이동합니다.
    } catch (_) {
      // 자동 로그인 실패 → 토큰 삭제 → 로그인 화면
      await _localDataSource.clearTokens();
      state = state.toUnauthenticated();
    }
  }

  /// 로그인 수행
  ///
  /// [employeeId]: 사번
  /// [password]: 비밀번호
  /// [rememberEmployeeId]: 아이디 기억하기
  /// [autoLogin]: 자동 로그인
  Future<void> login({
    required String employeeId,
    required String password,
    required bool rememberEmployeeId,
    required bool autoLogin,
  }) async {
    state = state.toLoading();

    try {
      final result = await _loginUseCase(
        employeeId: employeeId,
        password: password,
        rememberEmployeeId: rememberEmployeeId,
        autoLogin: autoLogin,
      );

      // 토큰 저장
      await _localDataSource.saveAccessToken(result.token.accessToken);
      await _localDataSource.saveRefreshToken(result.token.refreshToken);

      // 아이디 기억하기 처리
      if (rememberEmployeeId) {
        await _localDataSource.saveEmployeeId(employeeId);
      } else {
        await _localDataSource.clearSavedEmployeeId();
      }

      // 자동 로그인 설정
      await _localDataSource.setAutoLogin(autoLogin);

      // 비밀번호 변경 / GPS 동의 필요 여부 확인
      if (result.requiresPasswordChange) {
        state = state.copyWith(
          isLoading: false,
          user: result.user,
          errorMessage: null,
          requiresPasswordChange: true,
          requiresGpsConsent: result.requiresGpsConsent,
          rememberEmployeeId: rememberEmployeeId,
          autoLogin: autoLogin,
          isInitialized: true,
        );
      } else if (result.requiresGpsConsent) {
        state = state.copyWith(
          isLoading: false,
          user: result.user,
          errorMessage: null,
          requiresPasswordChange: false,
          requiresGpsConsent: true,
          rememberEmployeeId: rememberEmployeeId,
          autoLogin: autoLogin,
          isInitialized: true,
        );
      } else {
        // 모든 조건 통과 → 인증 완료
        state = state.toAuthenticated(result.user);
      }
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  /// 비밀번호 변경
  ///
  /// [currentPassword]: 현재 비밀번호
  /// [newPassword]: 새 비밀번호
  Future<void> changePassword({
    required String currentPassword,
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
          requiresPasswordChange: false,
          errorMessage: null,
        );
      } else {
        // 모든 조건 통과 → 인증 완료
        state = state.toAuthenticated(state.user!);
      }
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  /// GPS 동의 기록
  Future<void> recordGpsConsent() async {
    state = state.toLoading();

    try {
      await _repository.recordGpsConsent();

      // GPS 동의 완료 → 인증 완료
      state = state.toAuthenticated(state.user!);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  /// 로그아웃
  Future<void> logout() async {
    try {
      await _logoutUseCase();
    } catch (_) {
      // 로그아웃 실패해도 로컬 토큰은 삭제
    }

    // 토큰 삭제
    await _localDataSource.clearTokens();
    await _localDataSource.setAutoLogin(false);

    // 상태 초기화
    state = state.toUnauthenticated();
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
  );
});
