import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/order_form_api_datasource.dart';
import 'package:mobile/data/models/order_form/order_draft_request_model.dart';
import 'package:mobile/data/models/order_form/order_request_payload_model.dart';

void main() {
  late OrderFormApiDataSource dataSource;
  late Dio dio;

  setUp(() {
    dio = Dio(BaseOptions(baseUrl: 'https://api.test.com'));
    dataSource = OrderFormApiDataSource(dio);
  });

  group('OrderFormApiDataSource (#598 P1-M)', () {
    group('getLoanInquiry (#594)', () {
      test('200 응답 → LoanInquiryResponseModel 매핑', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.method, 'GET');
            expect(options.path,
                '/api/v1/mobile/clients/EK001/loan-inquiry');
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'data': {
                  'externalKey': 'EK001',
                  'totalCredit': 10000000,
                  'creditBalance': 2500000,
                  'currency': 'KRW',
                  'dataAsOf': '2026-05-04T03:00:00+09:00',
                },
                'message': '여신 한도 조회 성공',
              },
            ));
          },
        ));

        final result = await dataSource.getLoanInquiry(externalKey: 'EK001');

        expect(result.externalKey, 'EK001');
        expect(result.creditBalance, 2500000);
        expect(result.totalCredit, 10000000);
      });
    });

    group('getOrderDraft (#596 GET)', () {
      test('data null 응답 → null 반환', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {'data': null},
            ));
          },
        ));

        final result = await dataSource.getOrderDraft();

        expect(result, isNull);
      });

      test('정상 응답 → OrderDraftResponseModel 매핑', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'data': {
                  'draftId': 99,
                  'accountId': 5678,
                  'accountName': '거래처명',
                  'accountExternalKey': 'EK001',
                  'deliveryDate': '2026-05-08',
                  'totalAmount': 1234567,
                  'savedAt': '2026-05-04T10:00:00Z',
                  'lines': [
                    {
                      'lineNumber': 10,
                      'productCode': 'P001',
                      'productName': '제품A',
                      'unit': 'BOX',
                      'quantity': 10,
                      'quantityPieces': 100,
                      'quantityBoxes': 10,
                      'unitPrice': 12345,
                      'amount': 1234500,
                    }
                  ],
                },
              },
            ));
          },
        ));

        final result = await dataSource.getOrderDraft();

        expect(result, isNotNull);
        expect(result!.draftId, 99);
        expect(result.lines, hasLength(1));
        expect(result.lines[0].productCode, 'P001');
      });
    });

    group('saveOrderDraft (#596 POST)', () {
      test('요청 본문 직렬화 + 응답 매핑', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.method, 'POST');
            expect(options.path, '/api/v1/mobile/orders/draft');
            final body = options.data as Map<String, dynamic>;
            expect(body['accountId'], 5678);
            expect(body['totalAmount'], 1234567);
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'data': {
                  'draftId': 99,
                  'savedAt': '2026-05-04T10:00:00Z',
                },
                'message': '임시저장이 완료되었습니다',
              },
            ));
          },
        ));

        final request = OrderDraftRequestModel(
          accountId: 5678,
          deliveryDate: '2026-05-08',
          totalAmount: 1234567,
          lines: const [
            OrderDraftRequestLineModel(
              lineNumber: 10,
              productCode: 'P001',
              unit: 'BOX',
              quantity: 10,
            ),
          ],
        );

        final result = await dataSource.saveOrderDraft(request);

        expect(result.draftId, 99);
      });
    });

    group('deleteOrderDraft (#596 DELETE)', () {
      test('204 응답 정상 처리', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.method, 'DELETE');
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 204,
              data: null,
            ));
          },
        ));

        await dataSource.deleteOrderDraft();
      });
    });

    group('submitOrderRequest (#592)', () {
      test('clientRequestId → Idempotency-Key 헤더 부착', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.method, 'POST');
            expect(options.path, '/api/v1/mobile/order-requests');
            expect(
              options.headers['Idempotency-Key'],
              '11111111-1111-4111-8111-111111111111',
            );
            final body = options.data as Map<String, dynamic>;
            expect(body['accountId'], 5678);
            expect(body['lines'], hasLength(1));
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'data': {
                  'orderRequestId': 12345,
                  'orderRequestNumber': 'ORD-20260504-12345',
                  'status': 'SENT',
                  'totalAmount': 1234567,
                },
                'message': '주문 요청이 접수되었습니다',
              },
            ));
          },
        ));

        final payload = OrderRequestPayloadModel(
          clientRequestId: '11111111-1111-4111-8111-111111111111',
          accountId: 5678,
          deliveryDate: '2026-05-08',
          totalAmount: 1234567,
          lines: const [
            OrderRequestLineModel(
              lineNumber: 10,
              productCode: 'P001',
              quantity: 10,
              unit: 'BOX',
              quantityPieces: 100,
              quantityBoxes: 10,
            ),
          ],
        );

        final result = await dataSource.submitOrderRequest(payload);

        expect(result.status, 'SENT');
        expect(result.orderRequestId, 12345);
      });

      test('clientRequestId 미부여 시 헤더 미부착', () async {
        dio.interceptors.add(InterceptorsWrapper(
          onRequest: (options, handler) {
            expect(options.headers['Idempotency-Key'], isNull);
            handler.resolve(Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'data': {
                  'orderRequestId': 1,
                  'orderRequestNumber': 'ORD-1',
                  'status': 'SENT',
                  'totalAmount': 1,
                },
              },
            ));
          },
        ));

        final payload = OrderRequestPayloadModel(
          accountId: 1,
          deliveryDate: '2026-05-08',
          totalAmount: 1,
          lines: const [
            OrderRequestLineModel(
              lineNumber: 10,
              productCode: 'P001',
              quantity: 1,
              unit: 'EA',
              quantityPieces: 1,
              quantityBoxes: 0,
            ),
          ],
        );

        await dataSource.submitOrderRequest(payload);
      });
    });
  });
}
