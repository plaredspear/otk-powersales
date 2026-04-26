import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:intl/intl.dart';

/// 테스트 헬퍼 함수
///
/// 모든 테스트에서 공통으로 필요한 초기화 작업을 수행합니다.
class TestHelper {
  static bool _initialized = false;

  /// 테스트 환경 초기화
  ///
  /// - intl 패키지 locale 데이터 초기화
  /// - 기타 공통 설정
  static Future<void> initialize() async {
    if (_initialized) {
      return;
    }

    TestWidgetsFlutterBinding.ensureInitialized();

    // intl 패키지 locale 데이터 초기화
    await initializeDateFormatting('ko_KR', null);
    await initializeDateFormatting('en_US', null);

    // 기본 locale 설정
    Intl.defaultLocale = 'ko_KR';

    _initialized = true;
  }
}
