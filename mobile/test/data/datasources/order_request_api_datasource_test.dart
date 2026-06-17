import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/order_request_api_datasource.dart';
import 'package:mobile/data/datasources/order_request_remote_datasource.dart';
import 'package:mobile/data/models/order_cancel_model.dart';
import 'package:mobile/data/models/order_request_detail_model.dart';

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
            if (options.path == '/api/v1/mobile/me/order-requests') {
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'items': [
                      {
                        'id': 1,
                        'orderRequestNumber': 'OP20260301',
                        'clientId': 42,
                        'clientName': '홈플러스 강남점',
                        'orderDate': '2026-03-01',
                        'deliveryDate': '2026-03-05',
                        'totalAmount': 1500000,
                        'orderRequestStatus': 'APPROVED',
                        'isClosed': false,
                      }
                    ],
                    'total': 1,
                    'truncated': false,
                    'fetchedAt': '2026-03-01T10:30:00+09:00',
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
        expect(result.items.length, 1);
        expect(result.items[0].id, 1);
        expect(result.items[0].orderRequestNumber, 'OP20260301');
        expect(result.items[0].clientId, 42);
        expect(result.items[0].clientName, '홈플러스 강남점');
        expect(result.total, 1);
        expect(result.truncated, false);
        expect(result.fetchedAt, DateTime.parse('2026-03-01T10:30:00+09:00'));
      });

      test('clientId, status 지정 시 queryParameters에 포함', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/order-requests') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'items': [],
                    'total': 0,
                    'truncated': false,
                    'fetchedAt': '2026-03-01T10:30:00+09:00',
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

      test('필수 외 파라미터 미지정 + page/size 미전송 검증', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/order-requests') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'items': [],
                    'total': 0,
                    'truncated': false,
                    'fetchedAt': '2026-03-01T10:30:00+09:00',
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
        // 페이징 파라미터는 클라이언트 슬라이스 정책으로 송신 안 함
        expect(capturedParams!.containsKey('page'), false);
        expect(capturedParams!.containsKey('size'), false);
        expect(capturedParams!['sortBy'], 'orderDate');
        expect(capturedParams!['sortDir'], 'DESC');
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

    group('searchProductsForOrder', () {
      test('정상 응답 시 ProductForOrderModel 목록 반환 + 차단값 매핑', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/products/search/order') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'content': [
                      {
                        'productCode': '18110014',
                        'productName': '열라면_용기105G',
                        'barcode': '8801045570716',
                        'storageType': '실온',
                        'shelfLife': '7개월',
                        'unitPrice': 1200,
                        'boxSize': 30,
                        'isFavorite': false,
                        'categoryMid': '봉지면',
                        'categorySub': '가정',
                        'productType': 'EXCLUSIVE',
                        'tasteGiftType': 'TASTING_GIFT',
                      }
                    ],
                    'totalElements': 1,
                  },
                  'message': '조회 성공',
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        final result =
            await dataSource.searchProductsForOrder(query: '열라면');

        expect(result.length, 1);
        expect(result[0].productCode, '18110014');
        expect(result[0].unitPrice, 1200);
        expect(result[0].boxSize, 30);
        expect(result[0].productType, 'EXCLUSIVE');
        expect(result[0].tasteGiftType, 'TASTING_GIFT');
        expect(capturedParams!['query'], '열라면');
      });

      test('categoryMid/categorySub 지정 시 queryParameters에 포함', () async {
        Map<String, dynamic>? capturedParams;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/products/search/order') {
              capturedParams = options.queryParameters;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {'content': [], 'totalElements': 0},
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        await dataSource.searchProductsForOrder(
          query: '라면',
          categoryMid: '봉지면',
          categorySub: '가정',
        );

        expect(capturedParams!['categoryMid'], '봉지면');
        expect(capturedParams!['categorySub'], '가정');
      });
    });

    group('getOrderRequestDetail', () {
      test('정상 API 응답 시 OrderRequestDetailModel 반환', () async {
        String? capturedPath;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.path == '/api/v1/mobile/me/order-requests/7') {
              capturedPath = options.path;
              handler.resolve(Response(
                data: {
                  'success': true,
                  'data': {
                    'id': 7,
                    'orderRequestNumber': 'OP20260301',
                    'clientId': 42,
                    'clientName': '홈플러스 강남점',
                    'clientDeadlineTime': '14:00',
                    'orderDate': '2026-03-01',
                    'deliveryDate': '2026-03-05',
                    'totalAmount': 1500000,
                    'totalApprovedAmount': 1400000,
                    'orderRequestStatus': 'APPROVED',
                    'isClosed': true,
                    'orderedItemCount': 1,
                    'orderedItems': [
                      {
                        'orderProductId': 101,
                        'productCode': 'P001',
                        'productName': '진라면',
                        'totalQuantityBoxes': 10.0,
                        'totalQuantityPieces': 320,
                        'isCancelled': false,
                      }
                    ],
                    'orderProcessingStatusList': [
                      {
                        'sapOrderNumber': 'SAP1234',
                        'items': [
                          {
                            'productCode': 'P001',
                            'productName': '진라면',
                            'deliveredQuantity': '320',
                            'deliveryStatus': 'DELIVERED',
                            'driverName': '김기사',
                            'vehicle': '12가3456',
                            'driverPhone': '010-1234-5678',
                            'scheduleTime': '09:00',
                            'completeTime': '10:30',
                          }
                        ],
                      }
                    ],
                    'rejectedItems': null,
                  },
                  'message': '조회 성공',
                },
                statusCode: 200,
                requestOptions: options,
              ));
              return;
            }
            handler.reject(DioException(requestOptions: options));
          },
        ));

        final result = await dataSource.getOrderRequestDetail(orderId: 7);

        expect(capturedPath, '/api/v1/mobile/me/order-requests/7');
        expect(result, isA<OrderRequestDetailModel>());
        expect(result.id, 7);
        expect(result.orderRequestNumber, 'OP20260301');
        expect(result.clientName, '홈플러스 강남점');
        expect(result.isClosed, true);
        expect(result.totalApprovedAmount, 1400000);
        expect(result.orderedItems.length, 1);
        expect(result.orderedItems[0].productCode, 'P001');
        expect(result.orderProcessingStatusList, isNotNull);
        expect(result.orderProcessingStatusList!.length, 1);
        expect(result.orderProcessingStatusList![0].sapOrderNumber, 'SAP1234');
        expect(result.orderProcessingStatusList![0].items[0].driverName, '김기사');
        expect(result.rejectedItems, isNull);
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
          () => dataSource.getOrderRequestDetail(orderId: 7),
          throwsA(isA<DioException>()),
        );
      });
    });

    group('cancelOrderRequest', () {
      test('정상 응답 시 OrderCancelResponseModel 반환 + 경로/바디 검증', () async {
        String? capturedPath;
        Object? capturedBody;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedPath = options.path;
            capturedBody = options.data;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': {
                  'orderRequestId': 7,
                  'orderRequestNumber': 'OP20260301',
                  'orderRequestStatus': 'CANCELED',
                  'cancelledLines': [
                    {
                      'orderProductId': 101,
                      'lineNumber': 1,
                      'productCode': '01101123',
                      'cancelledAt': '2026-06-17T10:00:00',
                    },
                    {
                      'orderProductId': 102,
                      'lineNumber': 2,
                      'productCode': '01101456',
                      'cancelledAt': '2026-06-17T10:00:00',
                    },
                  ],
                },
                'message': '주문이 취소되었습니다',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        final result = await dataSource.cancelOrderRequest(
          orderId: 7,
          request: const OrderCancelRequestModel(orderProductIds: [101, 102]),
        );

        expect(capturedPath, '/api/v1/mobile/me/order-requests/7/cancel');
        expect(capturedBody, {
          'orderProductIds': [101, 102]
        });
        expect(result, isA<OrderCancelResponseModel>());
        expect(result.orderRequestId, 7);
        expect(result.orderRequestStatus, 'CANCELED');
        expect(result.cancelledLines.length, 2);
        expect(result.cancelledLines[0].orderProductId, 101);
        expect(result.cancelledLines[0].productCode, '01101123');

        final entity = result.toEntity();
        expect(entity.cancelledCount, 2);
      });

      test('전체 취소 시 빈 배열 바디 전송', () async {
        Object? capturedBody;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedBody = options.data;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': {
                  'orderRequestId': 7,
                  'orderRequestNumber': 'OP20260301',
                  'orderRequestStatus': 'CANCELED',
                  'cancelledLines': [],
                },
                'message': '주문이 취소되었습니다',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        await dataSource.cancelOrderRequest(
          orderId: 7,
          request: const OrderCancelRequestModel(orderProductIds: []),
        );

        expect(capturedBody, {'orderProductIds': <int>[]});
      });

      test('서버 400 응답 시 DioException 발생', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(DioException(
              requestOptions: options,
              response: Response(
                data: {
                  'success': false,
                  'error': {
                    'code': 'ORD_CANCEL_DEADLINE_PASSED',
                    'message': '주문 취소 마감 시각이 지났습니다',
                  },
                },
                statusCode: 400,
                requestOptions: options,
              ),
              type: DioExceptionType.badResponse,
            ));
          },
        ));

        expect(
          () => dataSource.cancelOrderRequest(
            orderId: 7,
            request: const OrderCancelRequestModel(orderProductIds: [101]),
          ),
          throwsA(isA<DioException>()),
        );
      });
    });

    group('resendOrderRequest', () {
      test('정상 응답 시 POST 경로 검증 + 정상 완료', () async {
        String? capturedPath;
        String? capturedMethod;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedPath = options.path;
            capturedMethod = options.method;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': null,
                'message': '주문이 재전송되었습니다',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        await dataSource.resendOrderRequest(orderId: 7);

        expect(capturedPath, '/api/v1/mobile/me/order-requests/7/resend');
        expect(capturedMethod, 'POST');
      });

      test('서버 400(INVALID_ORDER_STATUS) 응답 시 DioException 발생', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(DioException(
              requestOptions: options,
              response: Response(
                data: {
                  'success': false,
                  'error': {
                    'code': 'INVALID_ORDER_STATUS',
                    'message': '전송실패 상태의 주문만 재전송할 수 있습니다',
                  },
                },
                statusCode: 400,
                requestOptions: options,
              ),
              type: DioExceptionType.badResponse,
            ));
          },
        ));

        expect(
          () => dataSource.resendOrderRequest(orderId: 7),
          throwsA(isA<DioException>()),
        );
      });
    });

    group('getFavoriteProducts', () {
      test('정상 응답 시 GET 경로 검증 + ProductForOrderModel 목록 반환', () async {
        String? capturedPath;
        String? capturedMethod;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedPath = options.path;
            capturedMethod = options.method;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': [
                  {
                    'productCode': '18110014',
                    'productName': '열라면_용기105G',
                    'barcode': '8801045570716',
                    'storageType': '실온',
                    'shelfLife': '7개월',
                    'unitPrice': 1200,
                    'boxSize': 30,
                    'isFavorite': true,
                    'categoryMid': '라면',
                    'categorySub': '가정',
                    'productType': null,
                    'tasteGiftType': null,
                  },
                ],
                'message': '조회 성공',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        final result = await dataSource.getFavoriteProducts();

        expect(capturedPath, '/api/v1/mobile/me/favorite-products');
        expect(capturedMethod, 'GET');
        expect(result.length, 1);
        expect(result[0].productCode, '18110014');
        expect(result[0].isFavorite, true);
      });

      test('빈 목록 응답 시 빈 리스트 반환', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              data: {'success': true, 'data': [], 'message': '조회 성공'},
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        final result = await dataSource.getFavoriteProducts();

        expect(result, isEmpty);
      });
    });

    group('addToFavorites', () {
      test('정상 응답 시 POST 경로 검증', () async {
        String? capturedPath;
        String? capturedMethod;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedPath = options.path;
            capturedMethod = options.method;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': null,
                'message': '즐겨찾기에 추가되었습니다',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        await dataSource.addToFavorites(productCode: '18110014');

        expect(capturedPath, '/api/v1/mobile/me/favorite-products/18110014');
        expect(capturedMethod, 'POST');
      });

      test('서버 400(ALREADY_FAVORITED) 응답 시 DioException 발생', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.reject(DioException(
              requestOptions: options,
              response: Response(
                data: {
                  'success': false,
                  'error': {
                    'code': 'ALREADY_FAVORITED',
                    'message': '이미 즐겨찾기에 추가된 제품입니다',
                  },
                },
                statusCode: 400,
                requestOptions: options,
              ),
              type: DioExceptionType.badResponse,
            ));
          },
        ));

        expect(
          () => dataSource.addToFavorites(productCode: '18110014'),
          throwsA(isA<DioException>()),
        );
      });
    });

    group('removeFromFavorites', () {
      test('정상 응답 시 DELETE 경로 검증', () async {
        String? capturedPath;
        String? capturedMethod;

        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            capturedPath = options.path;
            capturedMethod = options.method;
            handler.resolve(Response(
              data: {
                'success': true,
                'data': null,
                'message': '즐겨찾기에서 해제되었습니다',
              },
              statusCode: 200,
              requestOptions: options,
            ));
          },
        ));

        await dataSource.removeFromFavorites(productCode: '18110014');

        expect(capturedPath, '/api/v1/mobile/me/favorite-products/18110014');
        expect(capturedMethod, 'DELETE');
      });
    });
  });
}
