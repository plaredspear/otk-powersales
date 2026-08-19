import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/my_account_api_datasource.dart';
import 'package:mobile/domain/repositories/my_account_repository.dart';

/// 거래처 셀렉터가 서버에 보내는 쿼리 파라미터 계약 검증.
///
/// 특히 [MyAccountScope.orderWrite] 는 `scope=order_write` 라는 새 값이 아니라
/// `scope=order` + `purpose=write` 로 보내야 한다 — 구버전(롤백된) 서버가 모르는 scope 값을
/// `field` 로 떨어뜨려 주문가능 유형 필터·진열 union 이 빠지는 것을 막기 위한 하위 호환 장치다.
void main() {
  late MyAccountApiDataSource dataSource;
  late Dio dio;
  late Map<String, dynamic> capturedQuery;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
    dataSource = MyAccountApiDataSource(dio);
    capturedQuery = {};
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        capturedQuery = Map<String, dynamic>.from(options.queryParameters);
        handler.resolve(Response(
          requestOptions: options,
          statusCode: 200,
          data: {
            'success': true,
            'data': {'accounts': <dynamic>[], 'totalCount': 0},
            'message': '내 거래처 목록 조회 성공',
          },
        ));
      },
    ));
  });

  group('MyAccountApiDataSource - scope 쿼리 파라미터', () {
    test('field 는 기본값이라 scope/purpose 를 보내지 않는다', () async {
      await dataSource.getMyAccounts();

      expect(capturedQuery.containsKey('scope'), isFalse);
      expect(capturedQuery.containsKey('purpose'), isFalse);
    });

    test('sales 는 scope=sales 만 보낸다', () async {
      await dataSource.getMyAccounts(scope: MyAccountScope.sales);

      expect(capturedQuery['scope'], 'sales');
      expect(capturedQuery.containsKey('purpose'), isFalse);
    });

    test('order(주문 조회 필터) 는 scope=order 만 보낸다', () async {
      await dataSource.getMyAccounts(scope: MyAccountScope.order);

      expect(capturedQuery['scope'], 'order');
      expect(capturedQuery.containsKey('purpose'), isFalse);
    });

    test('orderWrite(주문서 작성) 는 scope=order + purpose=write 를 보낸다', () async {
      await dataSource.getMyAccounts(scope: MyAccountScope.orderWrite);

      // 구버전 서버는 purpose 를 무시하므로 order(이전 동작)로 안전 폴백된다.
      expect(capturedQuery['scope'], 'order');
      expect(capturedQuery['purpose'], 'write');
    });

    test('keyword 는 scope 와 함께 전달된다', () async {
      await dataSource.getMyAccounts(
        keyword: '경산',
        scope: MyAccountScope.orderWrite,
      );

      expect(capturedQuery['keyword'], '경산');
      expect(capturedQuery['scope'], 'order');
      expect(capturedQuery['purpose'], 'write');
    });
  });
}
