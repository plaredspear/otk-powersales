import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/attendance_result.dart';
import 'package:mobile/domain/entities/attendance_status.dart';
import 'package:mobile/domain/entities/store_schedule_item.dart';
import 'package:mobile/presentation/providers/attendance_state.dart';

void main() {
  group('AttendanceState', () {
    test('initial() 기본 상태 생성', () {
      final state = AttendanceState.initial();

      expect(state.isLoading, false);
      expect(state.isRegistering, false);
      expect(state.errorMessage, null);
      expect(state.workerType, null);
      expect(state.allStores, isEmpty);
      expect(state.filteredStores, isEmpty);
      expect(state.totalCount, 0);
      expect(state.registeredCount, 0);
      expect(state.selectedWorkType, 'ROOM_TEMP');
      expect(state.selectedStoreId, null);
      expect(state.searchKeyword, '');
      expect(state.registrationResult, null);
      expect(state.statusList, isEmpty);
    });

    test('toLoading 로딩 상태로 전환', () {
      final state = AttendanceState.initial();
      final loadingState = state.toLoading();

      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, null);
    });

    test('toRegistering 등록 중 상태로 전환', () {
      final state = AttendanceState.initial();
      final registeringState = state.toRegistering();

      expect(registeringState.isRegistering, true);
      expect(registeringState.errorMessage, null);
    });

    test('toError 에러 상태로 전환', () {
      final state = AttendanceState.initial();
      final errorState = state.toError('에러 메시지');

      expect(errorState.isLoading, false);
      expect(errorState.isRegistering, false);
      expect(errorState.errorMessage, '에러 메시지');
    });

    test('isFixedWorker FIXED 근무자일 때 true 반환', () {
      final state = AttendanceState.initial().copyWith(workerType: 'FIXED');

      expect(state.isFixedWorker, true);
    });

    test('isFixedWorker PATROL 근무자일 때 false 반환', () {
      final state = AttendanceState.initial().copyWith(workerType: 'PATROL');

      expect(state.isFixedWorker, false);
    });

    test('isFixedWorker IRREGULAR 근무자일 때 false 반환', () {
      final state = AttendanceState.initial().copyWith(workerType: 'IRREGULAR');

      expect(state.isFixedWorker, false);
    });

    test('isAllRegistered 모든 거래처 등록 완료 시 true 반환', () {
      final state = AttendanceState.initial().copyWith(
        totalCount: 5,
        registeredCount: 5,
      );

      expect(state.isAllRegistered, true);
    });

    test('isAllRegistered 등록 수가 총 수를 초과할 때 true 반환', () {
      final state = AttendanceState.initial().copyWith(
        totalCount: 5,
        registeredCount: 6,
      );

      expect(state.isAllRegistered, true);
    });

    test('isAllRegistered 미등록 거래처가 있을 때 false 반환', () {
      final state = AttendanceState.initial().copyWith(
        totalCount: 5,
        registeredCount: 3,
      );

      expect(state.isAllRegistered, false);
    });

    test('isAllRegistered 총 수가 0일 때 false 반환', () {
      final state = AttendanceState.initial().copyWith(
        totalCount: 0,
        registeredCount: 0,
      );

      expect(state.isAllRegistered, false);
    });

    test('remainingCount 남은 거래처 수 계산', () {
      final state = AttendanceState.initial().copyWith(
        totalCount: 5,
        registeredCount: 2,
      );

      expect(state.remainingCount, 3);
    });

    test('unregisteredStores 미등록 거래처만 필터링', () {
      final stores = [
        const StoreScheduleItem(
          storeId: 101,
          storeName: '이마트 해운대점',
          storeCode: 'ST-00101',
          workCategory: '진열',
          address: '부산시 해운대구 센텀2로 25',
          isRegistered: false,
        ),
        const StoreScheduleItem(
          storeId: 102,
          storeName: '홈플러스 서면점',
          storeCode: 'ST-00102',
          workCategory: '순회',
          address: '부산시 부산진구 서면로 68번길 9',
          isRegistered: true,
          registeredWorkType: 'ROOM_TEMP',
        ),
        const StoreScheduleItem(
          storeId: 103,
          storeName: '롯데마트 광복점',
          storeCode: 'ST-00103',
          workCategory: '진열',
          address: '부산시 중구 중앙대로 2',
          isRegistered: false,
        ),
      ];

      final state = AttendanceState.initial().copyWith(
        filteredStores: stores,
      );

      final unregistered = state.unregisteredStores;

      expect(unregistered.length, 2);
      expect(unregistered[0].storeId, 101);
      expect(unregistered[1].storeId, 103);
    });

    test('copyWith 상태 복사 및 업데이트', () {
      final originalState = AttendanceState.initial();

      final updatedState = originalState.copyWith(
        isLoading: true,
        workerType: 'PATROL',
        totalCount: 5,
        registeredCount: 2,
        selectedWorkType: 'REFRIGERATED',
        selectedStoreId: 101,
        searchKeyword: '이마트',
      );

      expect(updatedState.isLoading, true);
      expect(updatedState.workerType, 'PATROL');
      expect(updatedState.totalCount, 5);
      expect(updatedState.registeredCount, 2);
      expect(updatedState.selectedWorkType, 'REFRIGERATED');
      expect(updatedState.selectedStoreId, 101);
      expect(updatedState.searchKeyword, '이마트');
    });

    test('selectedWorkType 기본값은 ROOM_TEMP', () {
      final state = AttendanceState.initial();

      expect(state.selectedWorkType, 'ROOM_TEMP');
    });

    test('copyWith errorMessage를 null로 초기화', () {
      final state = AttendanceState.initial().copyWith(
        errorMessage: '에러 발생',
      );

      expect(state.errorMessage, '에러 발생');

      final clearedState = state.copyWith(errorMessage: null);

      expect(clearedState.errorMessage, null);
    });

    test('copyWith 등록 결과 설정', () {
      final result = AttendanceResult(
        attendanceId: 1001,
        storeId: 101,
        storeName: '이마트 해운대점',
        workType: 'ROOM_TEMP',
        registeredAt: DateTime(2025, 1, 15, 9, 30),
        totalCount: 5,
        registeredCount: 1,
      );

      final state = AttendanceState.initial().copyWith(
        registrationResult: result,
      );

      expect(state.registrationResult, result);
      expect(state.registrationResult?.storeId, 101);
      expect(state.registrationResult?.storeName, '이마트 해운대점');
    });

    test('copyWith 출근등록 현황 리스트 설정', () {
      final statusList = [
        AttendanceStatus(
          storeId: 101,
          storeName: '이마트 해운대점',
          status: 'COMPLETED',
          workType: 'ROOM_TEMP',
          registeredAt: DateTime(2025, 1, 15, 9, 30),
        ),
        const AttendanceStatus(
          storeId: 102,
          storeName: '홈플러스 서면점',
          status: 'PENDING',
        ),
      ];

      final state = AttendanceState.initial().copyWith(
        statusList: statusList,
      );

      expect(state.statusList.length, 2);
      expect(state.statusList[0].storeId, 101);
      expect(state.statusList[0].isCompleted, true);
      expect(state.statusList[1].storeId, 102);
      expect(state.statusList[1].isPending, true);
    });
  });
}
