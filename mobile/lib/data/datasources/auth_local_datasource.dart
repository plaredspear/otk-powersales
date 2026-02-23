import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:hive/hive.dart';

/// 인증 로컬 데이터소스
///
/// Secure Storage로 토큰을 관리하고, Hive로 아이디 기억하기 기능을 제공합니다.
class AuthLocalDataSource {
  final FlutterSecureStorage _secureStorage;

  final DeviceInfoPlugin _deviceInfo;

  // Secure Storage keys
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';
  static const String _autoLoginKey = 'auto_login';
  static const String _deviceIdKey = 'device_id';

  // Hive box and keys
  static const String _authBoxName = 'auth_box';
  static const String _savedEmployeeIdKey = 'saved_employee_id';
  static const String _rememberEmployeeIdKey = 'remember_employee_id';

  AuthLocalDataSource({
    FlutterSecureStorage? secureStorage,
    DeviceInfoPlugin? deviceInfo,
  })  : _secureStorage = secureStorage ?? const FlutterSecureStorage(),
        _deviceInfo = deviceInfo ?? DeviceInfoPlugin();

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

  // --- Device ID (단말기 바인딩) ---

  /// 디바이스 고유 ID 조회
  ///
  /// 1. Secure Storage에 저장된 ID가 있으면 반환
  /// 2. 없으면 플랫폼별 고유 식별자 취득 (Android: androidId, iOS: identifierForVendor)
  /// 3. 플랫폼 식별자도 없으면 UUID v4 생성
  /// 4. 생성된 ID를 Secure Storage에 저장 후 반환
  Future<String> getDeviceId() async {
    // 저장된 ID 확인
    final savedId = await _secureStorage.read(key: _deviceIdKey);
    if (savedId != null && savedId.isNotEmpty) {
      return savedId;
    }

    // 플랫폼별 고유 식별자 취득
    String? platformId;
    try {
      if (Platform.isAndroid) {
        final androidInfo = await _deviceInfo.androidInfo;
        platformId = androidInfo.id;
      } else if (Platform.isIOS) {
        final iosInfo = await _deviceInfo.iosInfo;
        platformId = iosInfo.identifierForVendor;
      }
    } catch (_) {
      // 플랫폼 식별자 취득 실패 시 무시
    }

    // 식별자가 없으면 타임스탬프 기반 고유 ID 생성
    final deviceId = (platformId != null && platformId.isNotEmpty)
        ? platformId
        : 'generated-${DateTime.now().microsecondsSinceEpoch}';

    // Secure Storage에 저장
    await _secureStorage.write(key: _deviceIdKey, value: deviceId);

    return deviceId;
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
