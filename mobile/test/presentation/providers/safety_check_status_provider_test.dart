import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/safety_check_status_api_datasource.dart';
import 'package:mobile/data/models/safety_check_status_model.dart';
import 'package:mobile/presentation/providers/safety_check_status_provider.dart';
import 'package:mobile/presentation/providers/safety_check_status_state.dart';

class FakeSafetyCheckStatusApiDataSource extends SafetyCheckStatusApiDataSource {
  SafetyCheckStatusModel? statusToReturn;
  Exception? exceptionToThrow;
  String? lastRequestedDate;

  FakeSafetyCheckStatusApiDataSource() : super(Dio());

  @override
  Future<SafetyCheckStatusModel> getStatus({String? date}) async {
    lastRequestedDate = date;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return statusToReturn!;
  }
}

void main() {
  group('SafetyCheckStatusNotifier', () {
    late SafetyCheckStatusNotifier notifier;
    late FakeSafetyCheckStatusApiDataSource fakeDataSource;

    setUp(() {
      fakeDataSource = FakeSafetyCheckStatusApiDataSource();
      notifier = SafetyCheckStatusNotifier(fakeDataSource);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.data, isNull);
      expect(notifier.state.errorMessage, isNull);
    });

    group('fetchStatus', () {
      test('조회 성공 시 데이터를 업데이트해야 한다', () async {
        fakeDataSource.statusToReturn = _sampleStatus;

        await notifier.fetchStatus();

        expect(notifier.state.data, isNotNull);
        expect(notifier.state.data!.totalCount, 2);
        expect(notifier.state.data!.submittedCount, 1);
        expect(notifier.state.data!.notSubmittedCount, 1);
        expect(notifier.state.data!.members.length, 2);
        expect(notifier.state.isLoading, false);
      });

      test('조회 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeDataSource.exceptionToThrow = Exception('네트워크 오류');

        await notifier.fetchStatus();

        expect(notifier.state.isError, true);
        expect(notifier.state.errorMessage, contains('네트워크 오류'));
      });
    });

    group('날짜 이동', () {
      test('goToPreviousDay는 날짜를 1일 감소시키고 API를 호출해야 한다', () async {
        fakeDataSource.statusToReturn = _emptyStatus;
        final initialDate = notifier.state.selectedDate;

        await notifier.goToPreviousDay();

        expect(
          notifier.state.selectedDate.day,
          initialDate.subtract(const Duration(days: 1)).day,
        );
      });

      test('goToNextDay는 날짜를 1일 증가시키고 API를 호출해야 한다', () async {
        fakeDataSource.statusToReturn = _emptyStatus;
        final initialDate = notifier.state.selectedDate;

        await notifier.goToNextDay();

        expect(
          notifier.state.selectedDate.day,
          initialDate.add(const Duration(days: 1)).day,
        );
      });
    });

    group('toggleCard', () {
      test('카드 토글 시 expandedCardIds에 추가/제거되어야 한다', () {
        notifier.toggleCard(42);
        expect(notifier.state.expandedCardIds.contains(42), true);

        notifier.toggleCard(42);
        expect(notifier.state.expandedCardIds.contains(42), false);
      });
    });

    group('clearError', () {
      test('에러 상태를 초기화해야 한다', () async {
        fakeDataSource.exceptionToThrow = Exception('오류');
        await notifier.fetchStatus();
        expect(notifier.state.isError, true);

        notifier.clearError();
        expect(notifier.state.isError, false);
        expect(notifier.state.errorMessage, isNull);
      });
    });
  });

  group('SafetyCheckStatusState', () {
    test('submittedMembers는 제출한 사원만 반환해야 한다', () {
      final state = SafetyCheckStatusState(
        selectedDate: DateTime(2026, 3, 17),
        data: _sampleStatus,
      );

      expect(state.submittedMembers.length, 1);
      expect(state.submittedMembers.first.employeeName, '홍길동');
    });

    test('notSubmittedMembers는 미제출 사원만 반환해야 한다', () {
      final state = SafetyCheckStatusState(
        selectedDate: DateTime(2026, 3, 17),
        data: _sampleStatus,
      );

      expect(state.notSubmittedMembers.length, 1);
      expect(state.notSubmittedMembers.first.employeeName, '김영희');
    });

    test('isEmpty는 데이터가 있지만 members가 비어있을 때 true', () {
      final state = SafetyCheckStatusState(
        selectedDate: DateTime(2026, 3, 17),
        data: _emptyStatus,
      );

      expect(state.isEmpty, true);
    });

    test('dateString은 YYYY-MM-DD 형식을 반환해야 한다', () {
      final state = SafetyCheckStatusState(
        selectedDate: DateTime(2026, 3, 7),
      );

      expect(state.dateString, '2026-03-07');
    });
  });
}

final _sampleStatus = SafetyCheckStatusModel(
  date: '2026-03-17',
  totalCount: 2,
  submittedCount: 1,
  notSubmittedCount: 1,
  members: [
    MemberStatusModel(
      id: 42,
      employeeNumber: '123456',
      employeeName: '홍길동',
      accountName: '이마트 강남점',
      submitted: true,
      submittedAt: '2026-03-17T09:15:30',
      equipments: [
        EquipmentStatusModel(seqNum: 1, label: '손목보호대 착용', answer: '예'),
        EquipmentStatusModel(seqNum: 2, label: '숨수건 소지', answer: '예'),
      ],
      yesCount: 7,
      noCount: 2,
      precautions: '매장 내 안전사고 유의;중량물 취급 시 주의',
      precautionCount: 2,
    ),
    MemberStatusModel(
      id: 55,
      employeeNumber: '654321',
      employeeName: '김영희',
      accountName: '홈플러스 역삼점',
      submitted: false,
      equipments: [],
      yesCount: 0,
      noCount: 0,
      precautionCount: 0,
    ),
  ],
);

final _emptyStatus = SafetyCheckStatusModel(
  date: '2026-03-17',
  totalCount: 0,
  submittedCount: 0,
  notSubmittedCount: 0,
  members: [],
);
