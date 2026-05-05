import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/order_request_api_datasource.dart';
import 'package:mobile/data/datasources/order_request_remote_datasource.dart';

void main() {
  group('OrderRequestApiDataSource', () {
    late OrderRequestApiDataSource dataSource;
    late Dio dio;

    setUp(() {
      dio = Dio(BaseOptions(baseUrl: 'http://localhost'));
      dataSource = OrderRequestApiDataSource(dio);
    });

    group('getMyOrderRequests', () {
      test('정상 API 응답 시 OrderRequestListResponseModel 반환', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/orders') {
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [
                      {
                        'id': 1,
                        'orderRequestNumber': 'OP20260301',
                        'clientId': 42,
                        'clientName': '홈플러스 강남점',
                        'orderDate': '2026-03-01',
                        'deliveryDate': '2026-03-05',
                        'totalAmount': 1500000,
                        'approvalStatus': 'APPROVED',
                        'isClosed': false,
                      }
                    ],
                    'totalElements': 1,
                    'totalPages': 1,
                    'number': 0,
                    'size': 20,
                    'first': true,
                    'last': true,
                  },
                  'message': '내 주문 목록 조회 성공',
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        final result = await dataSource.getMyOrderRequests();

        expect(result, isA<OrderRequestListResponseModel>());
        expect(result.content.length, 1);
        expect(result.content[0].id, 1);
        expect(result.content[0].orderRequestNumber, 'OP20260301');
        expect(result.content[0].clientId, 42);
        expect(result.content[0].clientName, '홈플러스 강남점');
        expect(result.totalElements, 1);
        expect(result.totalPages, 1);
        expect(result.first, true);
        expect(result.last, true);
      });

      test('clientId, status 지정 시 queryParameters에 포함', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/orders') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [],
                    'totalElements': 0,
                    'totalPages': 0,
                    'number': 0,
                    'size': 20,
                    'first': true,
                    'last': true,
                  },
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        await dataSource.getMyOrderRequests(
          clientId: 42,
          status: 'APPROVED',
          deliveryDateFrom: '2026-03-01',
          deliveryDateTo: '2026-03-07',
        );

        expect(capturedParams, isNotNull);
        expect(capturedParams!['clientId'], 42);
        expect(capturedParams!['status'], 'APPROVED');
        expect(capturedParams!['deliveryDateFrom'], '2026-03-01');
        expect(capturedParams!['deliveryDateTo'], '2026-03-07');
      });

      test('clientId null 시 queryParameters에 미포함', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/orders') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [],
                    'totalElements': 0,
                    'totalPages': 0,
                    'number': 0,
                    'size': 20,
                    'first': true,
                    'last': true,
                  },
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        await dataSource.getMyOrderRequests();

        expect(capturedParams, isNotNull);
        expect(capturedParams!.containsKey('clientId'), false);
        expect(capturedParams!.containsKey('status'), false);
        expect(capturedParams!.containsKey('deliveryDateFrom'), false);
        expect(capturedParams!.containsKey('deliveryDateTo'), false);
        expect(capturedParams!['sortBy'], 'orderDate');
        expect(capturedParams!['sortDir'], 'DESC');
        expect(capturedParams!['page'], 0);
        expect(capturedParams!['size'], 20);
      });

      test('서버 500 응답 시 DioException 발생', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(DioException(
              requestOptions: options,
              response: Response(
                data: {'message': 'Internal Server Error'},
                statusCode: 500,
                requestOptions: options,
              ),
              type: DioExceptionType.badResponse,
            ));
          },
        ));

        expect(
          () => dataSource.getMyOrderRequests(),
          throwsA(isA<DioException>()),
        );
      });
    });

    group('getClientOrders', () {
      test('정상 API 응답 시 ClientOrderListResponseModel 반환', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/client-orders') {
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [
                      {
                        'sapOrderNumber': '300011396',
                        'clientId': 42,
                        'clientName': '홈플러스 강남점',
                        'totalAmount': 850000,
                      }
                    ],
                    'totalElements': 1,
                    'totalPages': 1,
                    'number': 0,
                    'size': 20,
                    'first': true,
                    'last': true,
                  },
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        final result = await dataSource.getClientOrders(clientId: 42);

        expect(result.content.length, 1);
        expect(result.content[0].sapOrderNumber, '300011396');
        expect(result.content[0].clientId, 42);
        expect(result.content[0].totalAmount, 850000);
        expect(result.totalElements, 1);
      });

      test('clientId는 항상 queryParameters에 포함', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/client-orders') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [],
                    'totalElements': 0,
                    'totalPages': 0,
                    'number': 0,
                    'size': 20,
                    'first': true,
                    'last': true,
                  },
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        await dataSource.getClientOrders(
          clientId: 42,
          deliveryDate: '2026-03-05',
        );

        expect(capturedParams, isNotNull);
        expect(capturedParams!['clientId'], 42);
        expect(capturedParams!['deliveryDate'], '2026-03-05');
      });
    });

    group('미구현 메서드', () {
      test('getOrderRequestDetail은 UnimplementedError 발생', () {
        expect(
          () => dataSource.getOrderRequestDetail(orderId: 1),
          throwsA(isA<UnimplementedError>()),
        );
      });

      test('resendOrderRequest는 UnimplementedError 발생', () {
        expect(
          () => dataSource.resendOrderRequest(orderId: 1),
          throwsA(isA<UnimplementedError>()),
        );
      });
    });
  });
}
