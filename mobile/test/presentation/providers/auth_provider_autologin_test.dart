import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/fcm_token_registrar.dart';
import 'package:mobile/core/services/push_notification_service.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';
import 'package:mobile/data/datasources/fcm_token_api_datasource.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/auto_login_usecase.dart';
import 'package:mobile/domain/usecases/change_password_usecase.dart';
import 'package:mobile/domain/usecases/login_usecase.dart';
import 'package:mobile/domain/usecases/logout_usecase.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';
import 'package:mobile/presentation/providers/auth_state.dart';

/// 자동 로그인 정합 회귀 테스트.
///
/// 레거시 Heroku 는 앱이 isAutoLogin 저장값을 보관해 로그인 화면에 이전 선택을
/// 유지시킨다. 이에 정합하도록 (1) 저장된 `auto_login` 플래그가 로그인 화면 상태로
/// 복원되고, (2) 미인증 전환 시에도 그 선택이 보존되는지 검증한다.

/// 메모리 백킹 AuthLocalDataSource fake.
///
/// FlutterSecureStorage 는 PlatformInterface 위임 구조라 단위 테스트에서 직접
/// 대체하기 어렵다. 대신 저장/조회 계약을 그대로 재현한 fake datasource 를 주입해
/// Notifier 의 복원 로직만 격리 검증한다.
class _FakeAuthLocalDataSource implements AuthLocalDataSource {
  final Map<String, String> _secure = {};
  final Map<String, dynamic> _hive = {};

  @override
  Future<void> setAutoLogin(bool enabled) async {
    _secure['auto_login'] = enabled.toString();
  }

  @override
  Future<bool> isAutoLoginEnabled() async => _secure['auto_login'] == 'true';

  @override
  Future<String?> getSavedEmployeeNumber() async {
    final remember = _hive['remember'] == true;
    if (!remember) return null;
    return _hive['saved'] as String?;
  }

  @override
  Future<bool> isRememberEmployeeNumberEnabled() async =>
      _hive['remember'] == true;

  @override
  Future<void> saveEmployeeNumber(String employeeCode) async {
    _hive['saved'] = employeeCode;
    _hive['remember'] = true;
  }

  @override
  Future<void> clearSavedEmployeeNumber() async {
    _hive.remove('saved');
    _hive['remember'] = false;
  }

  @override
  Future<void> saveAccessToken(String token) async =>
      _secure['access'] = token;

  @override
  Future<void> saveRefreshToken(String token) async =>
      _secure['refresh'] = token;

  @override
  Future<String?> getAccessToken() async => _secure['access'];

  @override
  Future<String?> getRefreshToken() async => _secure['refresh'];

  @override
  Future<void> clearTokens() async {
    _secure.remove('access');
    _secure.remove('refresh');
  }

  @override
  Future<String> getDeviceId() async => 'test-device';
}

/// FCM 등록을 no-op 으로 대체하는 fake (loadSavedEmployeeNumber 경로는 호출 안 함).
class _FakeFcmTokenRegistrar extends FcmTokenRegistrar {
  _FakeFcmTokenRegistrar()
      : super(
          push: PushNotificationService(),
          api: FcmTokenApiDataSource(Dio()),
        );

  @override
  Future<void> registerCurrentToken() async {}

  @override
  Future<void> registerToken(String token) async {}

  @override
  Future<void> unregister() async {}
}

/// 호출되지 않는 UseCase 의존을 위한 최소 Mock Repository.
class _MockAuthRepository implements AuthRepository {
  @override
  Future<LoginResult> login(String employeeCode, String password) async =>
      throw UnimplementedError();

  @override
  Future<User> getMe() async => throw UnimplementedError();

  @override
  Future<AuthToken> refreshToken(String refreshToken) async =>
      throw UnimplementedError();

  @override
  Future<AuthToken> changePassword({
    String? currentPassword,
    required String newPassword,
  }) async =>
      throw UnimplementedError();

  @override
  Future<void> logout() async {}

  @override
  Future<GpsConsentTerms> getGpsConsentTerms() async =>
      throw UnimplementedError();

  @override
  Future<GpsConsentStatus> getGpsConsentStatus() async =>
      throw UnimplementedError();

  @override
  Future<GpsConsentRecordResult> recordGpsConsent({
    String? agreementNumber,
  }) async =>
      throw UnimplementedError();
}

void main() {
  group('AuthState.toUnauthenticated', () {
    test('autoLogin 값을 보존한다 (자동 로그인 실패 후에도 UI 선택 유지)', () {
      const state = AuthState(
        user: User(
          id: 1,
          employeeCode: '20010585',
          name: '홍길동',
          role: 'ROLE_USER',
        ),
        autoLogin: true,
        rememberEmployeeNumber: true,
        savedEmployeeNumber: '20010585',
      );

      final result = state.toUnauthenticated();

      expect(result.autoLogin, isTrue,
          reason: '레거시 정합: 미인증 전환 시 자동 로그인 선택을 보존해야 함');
      expect(result.rememberEmployeeNumber, isTrue);
      expect(result.savedEmployeeNumber, '20010585');
      expect(result.user, isNull, reason: 'clearUser 로 사용자 정보는 제거되어야 함');
      expect(result.isInitialized, isTrue);
    });

    test('autoLogin=false 도 그대로 유지한다', () {
      const state = AuthState(autoLogin: false);

      final result = state.toUnauthenticated();

      expect(result.autoLogin, isFalse);
    });
  });

  group('AuthNotifier.loadSavedEmployeeNumber (자동 로그인 복원)', () {
    late _FakeAuthLocalDataSource localDataSource;
    late _MockAuthRepository repository;

    setUp(() {
      localDataSource = _FakeAuthLocalDataSource();
      repository = _MockAuthRepository();
    });

    AuthNotifier buildNotifier() {
      return AuthNotifier(
        loginUseCase: LoginUseCase(repository),
        autoLoginUseCase: AutoLoginUseCase(repository),
        changePasswordUseCase: ChangePasswordUseCase(repository),
        logoutUseCase: LogoutUseCase(repository),
        localDataSource: localDataSource,
        repository: repository,
        fcmTokenRegistrar: _FakeFcmTokenRegistrar(),
      );
    }

    test('저장된 auto_login=true 를 state.autoLogin 으로 복원한다', () async {
      await localDataSource.setAutoLogin(true);
      final notifier = buildNotifier();

      await notifier.loadSavedEmployeeNumber();

      expect(notifier.state.autoLogin, isTrue,
          reason: '저장된 자동 로그인 선택이 로그인 화면 상태로 복원되어야 함');
    });

    test('저장된 auto_login=false 는 state.autoLogin=false 로 복원한다', () async {
      await localDataSource.setAutoLogin(false);
      final notifier = buildNotifier();

      await notifier.loadSavedEmployeeNumber();

      expect(notifier.state.autoLogin, isFalse);
    });

    test('저장값 없으면 state.autoLogin 은 기본값 false', () async {
      final notifier = buildNotifier();

      await notifier.loadSavedEmployeeNumber();

      expect(notifier.state.autoLogin, isFalse);
    });

    test('사번 기억하기와 자동 로그인을 함께 복원한다', () async {
      await localDataSource.saveEmployeeNumber('20010585');
      await localDataSource.setAutoLogin(true);
      final notifier = buildNotifier();

      await notifier.loadSavedEmployeeNumber();

      expect(notifier.state.savedEmployeeNumber, '20010585');
      expect(notifier.state.rememberEmployeeNumber, isTrue);
      expect(notifier.state.autoLogin, isTrue);
    });
  });
}
