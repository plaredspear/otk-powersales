import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/config/app_config.dart';

void main() {
  group('AppConfig', () {
    test('baseUrl 기본값은 localhost:8080이어야 한다', () {
      // --dart-define-from-file 미지정 시 기본값
      expect(AppConfig.baseUrl, 'http://localhost:8080');
    });

    test('env 기본값은 dev여야 한다', () {
      expect(AppConfig.env, 'dev');
    });

    test('기본 환경에서 isProduction은 false여야 한다', () {
      expect(AppConfig.isProduction, false);
    });
  });
}
