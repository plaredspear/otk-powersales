/// 환경별 앱 설정
///
/// `--dart-define-from-file`로 주입된 컴파일 타임 상수를 제공합니다.
/// 미지정 시 로컬 개발용 기본값으로 폴백합니다.
///
/// 사용 예:
/// ```bash
/// # 로컬 개발 (config/dev.json.example을 복사하여 사용)
/// cp config/dev.json.example config/dev.json
/// flutter run --dart-define-from-file=config/dev.json
/// ```
class AppConfig {
  AppConfig._();

  static const String baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  static const String env = String.fromEnvironment(
    'ENV',
    defaultValue: 'dev',
  );

  static bool get isProduction => env == 'prod';
}
