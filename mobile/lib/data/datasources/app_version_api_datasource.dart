import 'package:dio/dio.dart';

/// 앱 버전 체크 API 응답 (백엔드 `AppVersionCheckDto`).
class AppVersionResult {
  /// 최신 versionCode 가 현재보다 큼 (권장 업데이트).
  final bool updateAvailable;

  /// 현재 버전 초과~최신 사이에 강제 버전 존재 (강제 업데이트, 진입 차단).
  final bool forceUpdate;

  /// 최신 버전 이름 (예: "1.2.3").
  final String? latestVersionName;

  /// 릴리스 노트.
  final String? releaseNote;

  /// 다운로드 URL — Android=APK presigned URL, iOS=`itms-services://...` (미설정 시 null).
  final String? downloadUrl;

  const AppVersionResult({
    required this.updateAvailable,
    required this.forceUpdate,
    this.latestVersionName,
    this.releaseNote,
    this.downloadUrl,
  });

  factory AppVersionResult.fromJson(Map<String, dynamic> json) {
    return AppVersionResult(
      updateAvailable: json['updateAvailable'] as bool? ?? false,
      forceUpdate: json['forceUpdate'] as bool? ?? false,
      latestVersionName: json['latestVersionName'] as String?,
      releaseNote: json['releaseNote'] as String?,
      downloadUrl: json['downloadUrl'] as String?,
    );
  }
}

/// 앱 패키지 버전 체크 API 데이터소스.
///
/// 백엔드: `GET /api/v1/mobile/app-package/check` (인증 불필요 — 스플래시에서 로그인 전 호출).
class AppVersionApiDataSource {
  final Dio _dio;

  AppVersionApiDataSource(this._dio);

  /// [platform] 은 백엔드 `AppPlatform` enum 값(`ANDROID`/`IOS`).
  /// [versionCode] 는 현재 앱 빌드 번호(Long).
  Future<AppVersionResult> check({
    required String platform,
    required int versionCode,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/app-package/check',
      queryParameters: {
        'platform': platform,
        'versionCode': versionCode,
      },
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return AppVersionResult.fromJson(data);
  }
}
