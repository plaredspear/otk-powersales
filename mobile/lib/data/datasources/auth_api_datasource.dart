import 'package:dio/dio.dart';
import '../../core/services/app_version_fields.dart';
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
    );
    return AuthTokenModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<UserModel> getMe() async {
    final response = await _dio.get('/api/v1/mobile/auth/me');
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
