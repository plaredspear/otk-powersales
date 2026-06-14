import 'package:dio/dio.dart';
import '../../core/services/app_version_fields.dart';
import 'auth_interceptor.dart';
import '../models/change_password_request.dart';
import '../models/login_response_model.dart';
import '../models/auth_token_model.dart';
import '../models/password_verification_response.dart';
import '../models/user_model.dart';
import 'auth_remote_datasource.dart';

/// 인증 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
/// 현재는 코드 작성만 해두고, 실제 사용은 Backend 연동 시점에 시작합니다.
class AuthApiDataSource implements AuthRemoteDataSource {
  final Dio _dio;

  AuthApiDataSource(this._dio);

  @override
  Future<LoginResponseModel> login(
      String employeeCode, String password, String deviceId) async {
    final response = await _dio.post(
      '/api/v1/mobile/auth/login',
      data: {
        'employeeCode': employeeCode,
        'password': password,
        'deviceId': deviceId,
        // 현재 사용 중인 앱 버전 보고 (서버가 사용자별 현재 버전 기록).
        ...await appVersionFields(),
      },
    );
    return LoginResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<AuthTokenModel> refreshToken(String refreshToken) async {
    final response = await _dio.post(
      '/api/v1/mobile/auth/refresh',
      data: {
        'refreshToken': refreshToken,
        // 현재 사용 중인 앱 버전 보고 (서버가 사용자별 현재 버전 기록).
        ...await appVersionFields(),
      },
      // 명시적 자동 로그인의 refresh 실패는 호출측(tryAutoLogin)이 단독 처리한다.
      // 인터셉터가 401 을 가로채 _forceLogout(세션 재생성)하면 로그인 화면이 두 번
      // 쌓이므로, 이 요청은 인터셉터의 강제 로그아웃 대상에서 제외한다.
      options: Options(
        extra: {AuthInterceptor.skipAuthLogoutExtraKey: true},
      ),
    );
    return AuthTokenModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<UserModel> getMe() async {
    // getMe 는 시작 시 자동 로그인(tryAutoLogin)에서만 호출된다. 여기서 401 이 나도
    // 인터셉터가 _forceLogout(세션 재생성 + "세션 만료" 안내)을 하면 안 된다 — 자동
    // 로그인 실패는 호출측이 조용히 로그인 화면으로 전환한다. 매 실행마다 만료된
    // 세션으로 자동 로그인을 재시도하며 "세션 만료" 안내가 반복되는 것을 막는다.
    final response = await _dio.get(
      '/api/v1/mobile/auth/me',
      options: Options(
        extra: {AuthInterceptor.skipAuthLogoutExtraKey: true},
      ),
    );
    return UserModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    final response = await _dio.post(
      '/api/v1/mobile/auth/verify-password',
      data: {
        'currentPassword': currentPassword,
      },
    );
    final verification = PasswordVerificationResponse.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
    return verification.isValid;
  }

  @override
  Future<AuthTokenModel> changePassword({
    String? currentPassword,
    required String newPassword,
  }) async {
    final request = ChangePasswordRequest(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
    final response = await _dio.post(
      '/api/v1/mobile/auth/change-password',
      data: request.toJson(),
    );
    return AuthTokenModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<void> logout() async {
    await _dio.post('/api/v1/mobile/auth/logout');
  }

  @override
  Future<Map<String, dynamic>> getGpsConsentTerms() async {
    final response = await _dio.get('/api/v1/mobile/auth/gps-consent/terms');
    return response.data['data'] as Map<String, dynamic>;
  }

  @override
  Future<Map<String, dynamic>> getGpsConsentStatus() async {
    final response = await _dio.get('/api/v1/mobile/auth/gps-consent/status');
    return response.data['data'] as Map<String, dynamic>;
  }

  @override
  Future<Map<String, dynamic>> recordGpsConsent({String? agreementNumber}) async {
    final response = await _dio.post(
      '/api/v1/mobile/auth/gps-consent',
      data: agreementNumber != null
          ? {'agreementNumber': agreementNumber}
          : null,
    );
    return response.data['data'] as Map<String, dynamic>;
  }
}
