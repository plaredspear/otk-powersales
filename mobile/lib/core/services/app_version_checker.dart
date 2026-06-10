import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';

import '../network/dio_provider.dart';
import '../../data/datasources/app_version_api_datasource.dart';

/// 앱 버전 게이트 체커.
///
/// 현재 플랫폼/버전을 해석해 서버 버전 정책과 비교한다.
/// - 미지원 플랫폼(web/desktop)이거나 호출 실패 시 null 반환 → 게이트 skip(fail-open).
///   버전 체크 실패가 앱 진입을 막아선 안 된다.
class AppVersionChecker {
  final AppVersionApiDataSource _api;

  AppVersionChecker(this._api);

  Future<AppVersionResult?> check() async {
    // 백엔드 AppPlatform enum 값(대문자).
    final String platform;
    if (Platform.isAndroid) {
      platform = 'ANDROID';
    } else if (Platform.isIOS) {
      platform = 'IOS';
    } else {
      return null;
    }

    try {
      final info = await PackageInfo.fromPlatform();
      // buildNumber(문자열)를 versionCode(Long)로 변환. 파싱 불가 시 게이트 skip.
      final versionCode = int.tryParse(info.buildNumber);
      if (versionCode == null) return null;
      return await _api.check(platform: platform, versionCode: versionCode);
    } catch (_) {
      // 네트워크/파싱 실패 → 게이트 skip
      return null;
    }
  }
}

final appVersionApiDataSourceProvider = Provider<AppVersionApiDataSource>((ref) {
  return AppVersionApiDataSource(ref.watch(dioProvider));
});

final appVersionCheckerProvider = Provider<AppVersionChecker>((ref) {
  return AppVersionChecker(ref.watch(appVersionApiDataSourceProvider));
});
