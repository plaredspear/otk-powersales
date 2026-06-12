import 'dart:io';

import 'package:package_info_plus/package_info_plus.dart';

/// 로그인 / 토큰 리프레시 요청에 실어 보내는 "현재 사용 중인 앱 버전" 메타.
///
/// 서버(`employee_info`)가 사용자별 현재 사용 버전을 기록해 웹 관리자에서 분포를 확인하는 용도다.
/// 부가 텔레메트리이므로 조회 실패 시 빈 맵을 반환해 인증 요청에는 영향을 주지 않는다.
///
/// - appVersionName: pubspec `version`(예: "1.0.7")
/// - appVersionCode: pubspec `build`(buildNumber) 정수(예: 9) — 파싱 불가 시 생략
/// - appPlatform: 백엔드 `AppPlatform` enum 값(`ANDROID`/`IOS`) — 그 외 플랫폼은 생략
Future<Map<String, dynamic>> appVersionFields() async {
  try {
    final info = await PackageInfo.fromPlatform();
    final platform = Platform.isAndroid
        ? 'ANDROID'
        : Platform.isIOS
            ? 'IOS'
            : null;
    return {
      'appVersionName': info.version,
      // 값이 null 이면(파싱 불가/미지원 플랫폼) 해당 키 자체를 생략한다(null-aware element).
      'appVersionCode': ?int.tryParse(info.buildNumber),
      'appPlatform': ?platform,
    };
  } catch (_) {
    return const {};
  }
}
