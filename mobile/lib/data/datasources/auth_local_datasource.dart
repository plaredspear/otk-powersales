import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:hive/hive.dart';

/// 인증 로컬 데이터소스
///
/// Secure Storage로 토큰을 관리하고, Hive로 아이디 기억하기 기능을 제공합니다.
class AuthLocalDataSource {
  final FlutterSecureStorage _secureStorage;

  // Secure Storage keys
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';
  static const String _autoLoginKey = 'auto_login';

  // Hive box and keys
  static const String _authBoxName = 'auth_box';
  static const String _savedEmployeeIdKey = 'saved_employee_id';
  static const String _rememberEmployeeIdKey = 'remember_employee_id';

  AuthLocalDataSource({
    FlutterSecureStorage? secureStorage,
  }) : _secureStorage = secureStorage ?? const FlutterSecureStorage();

  // --- Secure Storage (토큰 관리) ---

  /// Access Token 저장
  Future<void> saveAccessToken(String token) async {
    await _secureStorage.write(key: _accessTokenKey, value: token);
  }

  /// Refresh Token 저장
  Future<void> saveRefreshToken(String token) async {
    await _secureStorage.write(key: _refreshTokenKey, value: token);
  }

  /// Access Token 조회
  Future<String?> getAccessToken() async {
    return await _secureStorage.read(key: _accessTokenKey);
  }

  /// Refresh Token 조회
  Future<String?> getRefreshToken() async {
    return await _secureStorage.read(key: _refreshTokenKey);
  }

  /// 토큰 삭제 (로그아웃 시)
  Future<void> clearTokens() async {
    await _secureStorage.delete(key: _accessTokenKey);
    await _secureStorage.delete(key: _refreshTokenKey);
  }

  /// 자동 로그인 설정 저장
  Future<void> setAutoLogin(bool enabled) async {
    await _secureStorage.write(
      key: _autoLoginKey,
      value: enabled.toString(),
    );
  }

  /// 자동 로그인 설정 조회
  Future<bool> isAutoLoginEnabled() async {
    final value = await _secureStorage.read(key: _autoLoginKey);
    return value == 'true';
  }

  // --- Hive (아이디 기억하기) ---

  /// Hive Box 열기 (초기화)
  Future<Box> _openBox() async {
    if (Hive.isBoxOpen(_authBoxName)) {
      return Hive.box(_authBoxName);
    }
    return await Hive.openBox(_authBoxName);
  }

  /// 사번 저장 (아이디 기억하기)
  Future<void> saveEmployeeId(String employeeId) async {
    final box = await _openBox();
    await box.put(_savedEmployeeIdKey, employeeId);
    await box.put(_rememberEmployeeIdKey, true);
  }

  /// 저장된 사번 조회
  Future<String?> getSavedEmployeeId() async {
    final box = await _openBox();
    final remember = box.get(_rememberEmployeeIdKey, defaultValue: false) as bool;
    if (!remember) return null;
    return box.get(_savedEmployeeIdKey) as String?;
  }

  /// 저장된 사번 삭제 (아이디 기억하기 해제)
  Future<void> clearSavedEmployeeId() async {
    final box = await _openBox();
    await box.delete(_savedEmployeeIdKey);
    await box.put(_rememberEmployeeIdKey, false);
  }

  /// 아이디 기억하기 여부 조회
  Future<bool> isRememberEmployeeIdEnabled() async {
    final box = await _openBox();
    return box.get(_rememberEmployeeIdKey, defaultValue: false) as bool;
  }
}
