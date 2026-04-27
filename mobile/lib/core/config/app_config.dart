/// 환경별 앱 설정
///
/// `--dart-define-from-file`로 주입된 컴파일 타임 상수를 제공합니다.
/// 미지정 시 로컬 개발용 기본값으로 폴백합니다.
///
/// Stage: `local` | `dev` | `prod`
///
/// 사용 예:
/// ```bash
/// # 최초 1회 (config/<stage>.json 생성)
/// make setup
/// # 실행 (STAGE 미지정 시 local)
/// make run STAGE=dev
/// ```
class AppConfig {
  AppConfig._();

  static const String baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const String env = String.fromEnvironment(
    'ENV',
    defaultValue: 'local',
  );

  static bool get isLocal => env == 'local';
  static bool get isDev => env == 'dev';
  static bool get isProduction => env == 'prod';
}
