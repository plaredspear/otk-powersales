import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/event.dart';
import 'package:mobile/domain/entities/event.dart' as event_entity;
import 'package:mobile/presentation/providers/daily_sales_form_state.dart';

void main() {
  group('SubmitStatus Enum', () {
    test('모든 상태가 정의되어 있다', () {
      expect(SubmitStatus.idle, isNotNull);
      expect(SubmitStatus.submitting, isNotNull);
      expect(SubmitStatus.success, isNotNull);
      expect(SubmitStatus.error, isNotNull);
    });
  });

  group('DailySalesFormState', () {
    final testDate = DateTime(2026, 2, 12);
    final testPhoto = File('/test/photo.jpg');
    final testEvent = Event(
      id: 'event-001',
      eventType: '[시식]',
      eventName: '진라면 시식 행사',
      startDate: DateTime(2026, 2, 1),
      endDate: DateTime(2026, 2, 28),
      customerId: 'C001',
      customerName: '이마트',
      assigneeId: 'EMP001',
    );

    group('초기화', () {
      test('initial() 팩토리로 초기 상태를 생성한다', () {
        final state = DailySalesFormState.initial();

        expect(state.selectedEvent, isNull);
        expect(state.date, isA<DateTime>());
        expect(state.mainProductPrice, isNull);
        expect(state.photo, isNull);
        expect(state.submitStatus, SubmitStatus.idle);
        expect(state.errorMessage, isNull);
      });

      test('initial() 팩토리에 event를 전달하면 설정된다', () {
        final state = DailySalesFormState.initial(event: testEvent);

        expect(state.selectedEvent, testEvent);
        expect(state.date, isA<DateTime>());
      });
    });

    group('Getter 테스트', () {
      test('hasMainProduct - true (모든 필드 입력)', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
        );

        expect(state.hasMainProduct, true);
      });

      test('hasMainProduct - false (일부 필드 누락)', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          // mainProductQuantity, mainProductAmount 누락
        );

        expect(state.hasMainProduct, false);
      });

      test('hasSubProduct - true (모든 필드 입력)', () {
        final state = DailySalesFormState(
          date: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
        );

        expect(state.hasSubProduct, true);
      });

      test('hasSubProduct - false (일부 필드 누락)', () {
        final state = DailySalesFormState(
          date: testDate,
          subProductCode: 'P001',
          // 나머지 필드 누락
        );

        expect(state.hasSubProduct, false);
      });

      test('hasAnyProduct - true (대표제품만)', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
        );

        expect(state.hasAnyProduct, true);
      });

      test('hasAnyProduct - true (기타제품만)', () {
        final state = DailySalesFormState(
          date: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
        );

        expect(state.hasAnyProduct, true);
      });

      test('hasAnyProduct - false (둘 다 없음)', () {
        final state = DailySalesFormState(date: testDate);

        expect(state.hasAnyProduct, false);
      });

      test('isValid - true (제품 + 사진)', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        expect(state.isValid, true);
      });

      test('isValid - false (제품 없음)', () {
        final state = DailySalesFormState(
          date: testDate,
          photo: testPhoto,
        );

        expect(state.isValid, false);
      });

      test('isValid - false (사진 없음)', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
        );

        expect(state.isValid, false);
      });

      test('isDraftValid - 항상 true', () {
        final state = DailySalesFormState(date: testDate);

        expect(state.isDraftValid, true);
      });

      test('isSubmitting - submitStatus가 submitting일 때 true', () {
        final state = DailySalesFormState(
          date: testDate,
          submitStatus: SubmitStatus.submitting,
        );

        expect(state.isSubmitting, true);
      });

      test('isSuccess - submitStatus가 success일 때 true', () {
        final state = DailySalesFormState(
          date: testDate,
          submitStatus: SubmitStatus.success,
        );

        expect(state.isSuccess, true);
      });

      test('isError - submitStatus가 error일 때 true', () {
        final state = DailySalesFormState(
          date: testDate,
          submitStatus: SubmitStatus.error,
        );

        expect(state.isError, true);
      });
    });

    group('calculateMainProductAmount', () {
      test('단가와 수량이 모두 있으면 자동 계산된다', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
        );

        expect(state.calculateMainProductAmount(), 10000);
      });

      test('단가만 있으면 null을 반환한다', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );

        expect(state.calculateMainProductAmount(), isNull);
      });

      test('수량만 있으면 null을 반환한다', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductQuantity: 10,
        );

        expect(state.calculateMainProductAmount(), isNull);
      });

      test('둘 다 없으면 null을 반환한다', () {
        final state = DailySalesFormState(date: testDate);

        expect(state.calculateMainProductAmount(), isNull);
      });
    });

    group('Helper 메서드', () {
      test('toSubmitting()은 제출 중 상태로 변경한다', () {
        final state = DailySalesFormState(date: testDate);
        final newState = state.toSubmitting();

        expect(newState.submitStatus, SubmitStatus.submitting);
        expect(newState.errorMessage, isNull);
      });

      test('toSuccess()는 제출 성공 상태로 변경한다', () {
        final state = DailySalesFormState(date: testDate);
        final newState = state.toSuccess();

        expect(newState.submitStatus, SubmitStatus.success);
        expect(newState.errorMessage, isNull);
      });

      test('toError()는 제출 실패 상태로 변경하고 에러 메시지를 설정한다', () {
        final state = DailySalesFormState(date: testDate);
        final newState = state.toError('에러 발생');

        expect(newState.submitStatus, SubmitStatus.error);
        expect(newState.errorMessage, '에러 발생');
      });

      test('resetSubmitStatus()는 제출 상태를 idle로 초기화한다', () {
        final state = DailySalesFormState(
          date: testDate,
          submitStatus: SubmitStatus.success,
          errorMessage: '이전 에러',
        );
        final newState = state.resetSubmitStatus();

        expect(newState.submitStatus, SubmitStatus.idle);
        expect(newState.errorMessage, isNull);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경한다', () {
        final original = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );
        final copied = original.copyWith(
          mainProductQuantity: 10,
        );

        expect(copied.mainProductPrice, 1000);
        expect(copied.mainProductQuantity, 10);
      });

      test('원본을 변경하지 않는다 (불변성)', () {
        final original = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );
        final copied = original.copyWith(mainProductPrice: 2000);

        expect(original.mainProductPrice, 1000);
        expect(copied.mainProductPrice, 2000);
      });
    });

    group('Equality', () {
      test('같은 값을 가진 상태는 동일하다', () {
        final state1 = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );
        final state2 = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );

        expect(state1, state2);
        expect(state1.hashCode, state2.hashCode);
      });

      test('다른 값을 가진 상태는 다르다', () {
        final state1 = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
        );
        final state2 = DailySalesFormState(
          date: testDate,
          mainProductPrice: 2000,
        );

        expect(state1, isNot(state2));
      });
    });

    group('toString', () {
      test('toString()은 모든 필드를 포함한다', () {
        final state = DailySalesFormState(
          date: testDate,
          mainProductPrice: 1000,
          submitStatus: SubmitStatus.idle,
        );
        final str = state.toString();

        expect(str, contains('DailySalesFormState'));
        expect(str, contains('1000'));
        expect(str, contains('SubmitStatus.idle'));
      });
    });
  });
}
