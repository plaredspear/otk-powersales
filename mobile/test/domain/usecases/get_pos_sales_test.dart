import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/pos_sales.dart';
import 'package:mobile/domain/repositories/pos_sales_repository.dart';
import 'package:mobile/domain/usecases/get_pos_sales.dart';

/// Mock Repository for testing
class MockPosSalesRepository implements PosSalesRepository {
  final List<PosSales> _mockData;

  MockPosSalesRepository(this._mockData);

  @override
  Future<List<PosSales>> getPosSales({
    required DateTime startDate,
    required DateTime endDate,
    String? storeName,
    String? productName,
  }) async {
    var result = _mockData.where((sale) {
      // 날짜 범위 필터링
      final isInDateRange = !sale.salesDate.isBefore(startDate) &&
          !sale.salesDate.isAfter(endDate);
      if (!isInDateRange) return false;

      // 매장명 필터링
      if (storeName != null && !sale.storeName.contains(storeName)) {
        return false;
      }

      // 제품명 필터링
      if (productName != null && !sale.productName.contains(productName)) {
        return false;
      }

      return true;
    }).toList();

    // 날짜순 정렬 (최신순)
    result.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return result;
  }

  @override
  Future<List<PosSales>> getPosSalesByProduct({
    required String productCode,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    var result = _mockData.where((sale) {
      // 날짜 범위 필터링
      final isInDateRange = !sale.salesDate.isBefore(startDate) &&
          !sale.salesDate.isAfter(endDate);
      if (!isInDateRange) return false;

      // 제품 코드 필터링
      if (sale.productCode != productCode) {
        return false;
      }

      return true;
    }).toList();

    // 날짜순 정렬 (최신순)
    result.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return result;
  }

  @override
  Future<List<PosSales>> getPosSalesByStore({
    required String storeName,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    var result = _mockData.where((sale) {
      // 날짜 범위 필터링
      final isInDateRange = !sale.salesDate.isBefore(startDate) &&
          !sale.salesDate.isAfter(endDate);
      if (!isInDateRange) return false;

      // 매장명 필터링 (정확히 일치)
      if (sale.storeName != storeName) {
        return false;
      }

      return true;
    }).toList();

    // 날짜순 정렬 (최신순)
    result.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return result;
  }
}

void main() {
  late List<PosSales> mockData;
  late MockPosSalesRepository mockRepository;
  late GetPosSalesUseCase useCase;

  setUp(() {
    // Mock 데이터 준비
    mockData = [
      PosSales(
        storeName: '이마트 강남점',
        productName: '진라면',
        salesDate: DateTime(2026, 1, 15),
        quantity: 100,
        amount: 50000,
      ),
      PosSales(
        storeName: '이마트 강남점',
        productName: '참깨라면',
        salesDate: DateTime(2026, 1, 16),
        quantity: 80,
        amount: 40000,
      ),
      PosSales(
        storeName: '홈플러스 서초점',
        productName: '진라면',
        salesDate: DateTime(2026, 1, 17),
        quantity: 120,
        amount: 60000,
      ),
      PosSales(
        storeName: '롯데마트 잠실점',
        productName: '진라면',
        salesDate: DateTime(2026, 1, 18),
        quantity: 90,
        amount: 45000,
      ),
    ];

    mockRepository = MockPosSalesRepository(mockData);
    useCase = GetPosSalesUseCase(mockRepository);
  });

  group('GetPosSalesUseCase - call', () {
    test('날짜 범위로 POS 매출을 조회한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      expect(result.length, 4);
      expect(result, mockData.reversed.toList()); // 최신순 정렬 확인
    });

    test('특정 기간의 POS 매출을 조회한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 15),
        endDate: DateTime(2026, 1, 16),
      );

      expect(result.length, 2);
      expect(result[0].salesDate, DateTime(2026, 1, 16)); // 최신순
      expect(result[1].salesDate, DateTime(2026, 1, 15));
    });

    test('매장명으로 POS 매출을 필터링한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
      );

      expect(result.length, 2);
      expect(result.every((sale) => sale.storeName.contains('이마트')), true);
    });

    test('제품명으로 POS 매출을 필터링한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        productName: '진라면',
      );

      expect(result.length, 3);
      expect(result.every((sale) => sale.productName.contains('진라면')), true);
    });

    test('매장명과 제품명으로 동시에 필터링한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
        productName: '진라면',
      );

      expect(result.length, 1);
      expect(result[0].storeName, '이마트 강남점');
      expect(result[0].productName, '진라면');
    });

    test('조건에 맞는 데이터가 없으면 빈 리스트를 반환한다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
      );

      expect(result, isEmpty);
    });

    test('시작일이 종료일보다 이후면 ArgumentError를 발생시킨다', () async {
      expect(
        () => useCase.call(
          startDate: DateTime(2026, 1, 31),
          endDate: DateTime(2026, 1, 1),
        ),
        throwsArgumentError,
      );
    });

    test('결과가 날짜순(최신순)으로 정렬된다', () async {
      final result = await useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      for (int i = 0; i < result.length - 1; i++) {
        expect(
          result[i].salesDate.isAfter(result[i + 1].salesDate) ||
              result[i].salesDate.isAtSameMomentAs(result[i + 1].salesDate),
          true,
        );
      }
    });
  });

  group('GetPosSalesUseCase - getByStore', () {
    test('매장별 POS 매출을 조회한다', () async {
      final result = await useCase.getByStore(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
      );

      expect(result.length, 2);
      expect(result.every((sale) => sale.storeName.contains('이마트')), true);
    });

    test('매장명이 빈 문자열이면 ArgumentError를 발생시킨다', () async {
      expect(
        () => useCase.getByStore(
          startDate: DateTime(2026, 1, 1),
          endDate: DateTime(2026, 1, 31),
          storeName: '',
        ),
        throwsArgumentError,
      );
    });
  });

  group('GetPosSalesUseCase - getByProduct', () {
    test('제품별 POS 매출을 조회한다', () async {
      final result = await useCase.getByProduct(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        productName: '진라면',
      );

      expect(result.length, 3);
      expect(result.every((sale) => sale.productName.contains('진라면')), true);
    });

    test('제품명이 빈 문자열이면 ArgumentError를 발생시킨다', () async {
      expect(
        () => useCase.getByProduct(
          startDate: DateTime(2026, 1, 1),
          endDate: DateTime(2026, 1, 31),
          productName: '',
        ),
        throwsArgumentError,
      );
    });
  });

  group('GetPosSalesUseCase - getByStoreAndProduct', () {
    test('매장 + 제품별 POS 매출을 조회한다', () async {
      final result = await useCase.getByStoreAndProduct(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
        productName: '진라면',
      );

      expect(result.length, 1);
      expect(result[0].storeName, '이마트 강남점');
      expect(result[0].productName, '진라면');
    });

    test('매장명이 빈 문자열이면 ArgumentError를 발생시킨다', () async {
      expect(
        () => useCase.getByStoreAndProduct(
          startDate: DateTime(2026, 1, 1),
          endDate: DateTime(2026, 1, 31),
          storeName: '',
          productName: '진라면',
        ),
        throwsArgumentError,
      );
    });

    test('제품명이 빈 문자열이면 ArgumentError를 발생시킨다', () async {
      expect(
        () => useCase.getByStoreAndProduct(
          startDate: DateTime(2026, 1, 1),
          endDate: DateTime(2026, 1, 31),
          storeName: '이마트',
          productName: '',
        ),
        throwsArgumentError,
      );
    });
  });

  group('GetPosSalesUseCase - calculateTotalAmount', () {
    test('POS 매출 합계를 정확하게 계산한다', () {
      final sales = mockData.take(2).toList();
      final total = useCase.calculateTotalAmount(sales);

      expect(total, 50000 + 40000);
    });

    test('빈 리스트의 합계는 0이다', () {
      final total = useCase.calculateTotalAmount([]);

      expect(total, 0);
    });

    test('단일 항목의 합계를 계산한다', () {
      final sales = [mockData.first];
      final total = useCase.calculateTotalAmount(sales);

      expect(total, 50000);
    });
  });

  group('GetPosSalesUseCase - calculateTotalQuantity', () {
    test('POS 매출 수량 합계를 정확하게 계산한다', () {
      final sales = mockData.take(2).toList();
      final total = useCase.calculateTotalQuantity(sales);

      expect(total, 100 + 80);
    });

    test('빈 리스트의 수량 합계는 0이다', () {
      final total = useCase.calculateTotalQuantity([]);

      expect(total, 0);
    });

    test('단일 항목의 수량 합계를 계산한다', () {
      final sales = [mockData.first];
      final total = useCase.calculateTotalQuantity(sales);

      expect(total, 100);
    });

    test('전체 데이터의 수량 합계를 계산한다', () {
      final total = useCase.calculateTotalQuantity(mockData);

      expect(total, 100 + 80 + 120 + 90);
    });
  });

  group('GetPosSalesUseCase - 비동기 처리', () {
    test('call은 Future를 반환한다', () {
      final result = useCase.call(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
      );

      expect(result, isA<Future<List<PosSales>>>());
    });

    test('getByStore는 Future를 반환한다', () {
      final result = useCase.getByStore(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
      );

      expect(result, isA<Future<List<PosSales>>>());
    });

    test('getByProduct는 Future를 반환한다', () {
      final result = useCase.getByProduct(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        productName: '진라면',
      );

      expect(result, isA<Future<List<PosSales>>>());
    });

    test('getByStoreAndProduct는 Future를 반환한다', () {
      final result = useCase.getByStoreAndProduct(
        startDate: DateTime(2026, 1, 1),
        endDate: DateTime(2026, 1, 31),
        storeName: '이마트',
        productName: '진라면',
      );

      expect(result, isA<Future<List<PosSales>>>());
    });
  });
}
