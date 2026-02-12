import 'package:dio/dio.dart';
import '../models/login_response_model.dart';
import '../models/auth_token_model.dart';
import '../models/password_verification_response.dart';
import 'auth_remote_datasource.dart';

/// 인증 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
/// 현재는 코드 작성만 해두고, 실제 사용은 Backend 연동 시점에 시작합니다.
class AuthApiDataSource implements AuthRemoteDataSource {
  final Dio _dio;

  AuthApiDataSource(this._dio);

  @override
  Future<LoginResponseModel> login(String employeeId, String password) async {
    final response = await _dio.post(
      '/api/v1/auth/login',
      data: {
        'employee_id': employeeId,
        'password': password,
      },
    );
    return LoginResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<AuthTokenModel> refreshToken(String refreshToken) async {
    final response = await _dio.post(
      '/api/v1/auth/refresh',
      data: {
        'refresh_token': refreshToken,
      },
    );
    return AuthTokenModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    final response = await _dio.post(
      '/api/v1/auth/verify-password',
      data: {
        'current_password': currentPassword,
      },
    );
    final verification = PasswordVerificationResponse.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
    return verification.isValid;
  }

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    await _dio.post(
      '/api/v1/auth/change-password',
      data: {
        'current_password': currentPassword,
        'new_password': newPassword,
      },
    );
  }

  @override
  Future<void> logout() async {
    await _dio.post('/api/v1/auth/logout');
  }

  @override
  Future<void> recordGpsConsent() async {
    await _dio.post('/api/v1/auth/gps-consent');
  }
}
