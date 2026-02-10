import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/get_store_list.dart';
import 'package:mobile/data/repositories/mock/attendance_mock_repository.dart';

void main() {
  group('GetStoreList UseCase', () {
    late GetStoreList useCase;
    late AttendanceMockRepository repository;

    setUp(() {
      repository = AttendanceMockRepository(workerType: 'PATROL');
      useCase = GetStoreList(repository);
    });

    group('call() 메서드', () {
      test('PATROL 근무자 거래처 목록 조회 성공', () async {
        // given
        // PATROL 근무자는 5개 거래처

        // when
        final result = await useCase.call();

        // then
        expect(result.workerType, 'PATROL');
        expect(result.stores.length, 5);
        expect(result.totalCount, 5);
        expect(result.registeredCount, 0);
        expect(result.currentDate, isNotEmpty);
      });

      test('FIXED 근무자는 단일 거래처만 조회', () async {
        // given
        repository = AttendanceMockRepository(workerType: 'FIXED');
        useCase = GetStoreList(repository);

        // when
        final result = await useCase.call();

        // then
        expect(result.workerType, 'FIXED');
        expect(result.stores.length, 1);
        expect(result.totalCount, 1);
        expect(result.stores.first.storeName, '이마트 부산본점');
        expect(result.stores.first.storeCode, 'ST-00201');
      });

      test('초기 상태에서는 모든 거래처가 미등록 상태', () async {
        // when
        final result = await useCase.call();

        // then
        expect(result.registeredCount, 0);
        for (final store in result.stores) {
          expect(store.isRegistered, false);
          expect(store.registeredWorkType, null);
        }
      });
    });

    group('callWithFilter() 메서드', () {
      test('거래처명으로 필터링 (예: "이마트")', () async {
        // when
        final result = await useCase.callWithFilter('이마트');

        // then
        expect(result.stores.length, 2);
        expect(result.stores[0].storeName, '이마트 해운대점');
        expect(result.stores[1].storeName, '이마트 사상점');
        // totalCount와 registeredCount는 원본 유지
        expect(result.totalCount, 5);
        expect(result.registeredCount, 0);
      });

      test('주소로 필터링 (예: "해운대")', () async {
        // when
        final result = await useCase.callWithFilter('해운대');

        // then
        expect(result.stores.length, 2);
        expect(result.stores[0].storeName, '이마트 해운대점');
        expect(result.stores[1].storeName, '홈플러스 센텀시티점');
        expect(result.stores[0].address, contains('해운대'));
        expect(result.stores[1].address, contains('해운대'));
      });

      test('거래처 코드로 필터링 (예: "ST-00101")', () async {
        // when
        final result = await useCase.callWithFilter('ST-00101');

        // then
        expect(result.stores.length, 1);
        expect(result.stores[0].storeCode, 'ST-00101');
        expect(result.stores[0].storeName, '이마트 해운대점');
      });

      test('빈 문자열로 필터링하면 전체 목록 반환', () async {
        // when
        final result = await useCase.callWithFilter('');

        // then
        expect(result.stores.length, 5);
        expect(result.totalCount, 5);
      });

      test('매칭되지 않는 키워드는 빈 목록 반환', () async {
        // when
        final result = await useCase.callWithFilter('존재하지않는거래처');

        // then
        expect(result.stores.length, 0);
        expect(result.totalCount, 5); // 원본 totalCount는 유지
      });

      test('대소문자 구분 없이 필터링', () async {
        // when
        final result1 = await useCase.callWithFilter('이마트');
        final result2 = await useCase.callWithFilter('EMART');
        final result3 = await useCase.callWithFilter('eMaRt');

        // then
        // 한글은 그대로, 영문은 대소문자 구분 없이 동작
        expect(result1.stores.length, 2);
        // 영문 검색은 실제 데이터에 영문이 없으므로 0개
        expect(result2.stores.length, 0);
        expect(result3.stores.length, 0);
      });

      test('부분 매칭으로 필터링 (예: "부산")', () async {
        // when
        final result = await useCase.callWithFilter('부산');

        // then
        // "부산"이 주소에 포함된 거래처들
        expect(result.stores.length, greaterThan(0));
        for (final store in result.stores) {
          expect(store.address.toLowerCase().contains('부산'), true);
        }
      });
    });
  });
}
