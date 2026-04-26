import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/promotion.dart';
import 'package:mobile/presentation/widgets/promotion/promotion_amount_text.dart';

void main() {
  // ============================================
  // PromotionItem
  // ============================================
  group('PromotionItem', () {
    test('fromJson 정상 파싱', () {
      // When
      final item = PromotionItem.fromJson(_sampleItemJson);

      // Then
      expect(item.id, 1);
      expect(item.promotionNumber, 'P-2025-001');
      expect(item.promotionName, '이마트 죽전점 행사');
      expect(item.promotionTypeName, '시식행사');
      expect(item.accountName, '이마트 죽전점');
      expect(item.startDate, '2025-03-01');
      expect(item.endDate, '2025-03-15');
      expect(item.category, '라면');
      expect(item.standLocation, '1층 중앙');
      expect(item.targetAmount, 5000000);
      expect(item.actualAmount, 3500000);
      expect(item.isClosed, false);
      expect(item.myScheduleDate, '2025-03-10');
    });

    test('fromJson nullable 필드가 null이면 null로 파싱된다', () {
      // Given
      final json = {
        'id': 2,
        'promotion_number': 'P-2025-002',
        'promotion_name': null,
        'promotion_type_name': null,
        'account_name': null,
        'start_date': '2025-03-01',
        'end_date': '2025-03-15',
        'category': null,
        'stand_location': null,
        'target_amount': null,
        'actual_amount': null,
        'is_closed': true,
        'my_schedule_date': null,
      };

      // When
      final item = PromotionItem.fromJson(json);

      // Then
      expect(item.promotionName, isNull);
      expect(item.promotionTypeName, isNull);
      expect(item.accountName, isNull);
      expect(item.category, isNull);
      expect(item.standLocation, isNull);
      expect(item.targetAmount, isNull);
      expect(item.actualAmount, isNull);
      expect(item.myScheduleDate, isNull);
      expect(item.isClosed, true);
    });
  });

  // ============================================
  // PromotionDetail
  // ============================================
  group('PromotionDetail', () {
    test('fromJson 정상 파싱 (employees 포함)', () {
      // When
      final detail = PromotionDetail.fromJson(_sampleDetailJson);

      // Then
      expect(detail.id, 10);
      expect(detail.promotionNumber, 'P-2025-010');
      expect(detail.promotionName, '홈플러스 수지점 시식행사');
      expect(detail.promotionTypeName, '시식행사');
      expect(detail.accountName, '홈플러스 수지점');
      expect(detail.startDate, '2025-03-01');
      expect(detail.endDate, '2025-03-15');
      expect(detail.category, '라면');
      expect(detail.standLocation, '1층 중앙');
      expect(detail.targetAmount, 4800000);
      expect(detail.actualAmount, 3200000);
      expect(detail.isClosed, false);
      expect(detail.primaryProductName, '진라면');
      expect(detail.otherProduct, '참깨라면');
      expect(detail.message, '행사 진행 중');
      expect(detail.productType, '라면류');
      expect(detail.remark, '비고 사항');
      expect(detail.employees, hasLength(2));
      expect(detail.employees[0].employeeName, '김철수');
      expect(detail.employees[1].employeeName, '이영희');
    });

    test('fromJson employees 빈 배열이면 빈 리스트', () {
      // Given
      final json = Map<String, dynamic>.from(_sampleDetailJson);
      json['employees'] = [];

      // When
      final detail = PromotionDetail.fromJson(json);

      // Then
      expect(detail.employees, isEmpty);
    });

    test('fromJson employees 키가 없으면 빈 리스트', () {
      // Given
      final json = Map<String, dynamic>.from(_sampleDetailJson);
      json.remove('employees');

      // When
      final detail = PromotionDetail.fromJson(json);

      // Then
      expect(detail.employees, isEmpty);
    });

    // ----------------------------------------
    // achievementRate 계산
    // ----------------------------------------
    group('achievementRate', () {
      test('정상 계산: (actual / target) * 100', () {
        final detail = PromotionDetail.fromJson(_sampleDetailJson);
        // 3200000 / 4800000 * 100 = 66.666...
        expect(detail.achievementRate, closeTo(66.67, 0.01));
      });

      test('targetAmount가 0이면 null 반환', () {
        final json = Map<String, dynamic>.from(_sampleDetailJson);
        json['target_amount'] = 0;

        final detail = PromotionDetail.fromJson(json);
        expect(detail.achievementRate, isNull);
      });

      test('targetAmount가 null이면 null 반환', () {
        final json = Map<String, dynamic>.from(_sampleDetailJson);
        json['target_amount'] = null;

        final detail = PromotionDetail.fromJson(json);
        expect(detail.achievementRate, isNull);
      });

      test('actualAmount가 null이면 0으로 계산', () {
        final json = Map<String, dynamic>.from(_sampleDetailJson);
        json['actual_amount'] = null;
        json['target_amount'] = 1000000;

        final detail = PromotionDetail.fromJson(json);
        expect(detail.achievementRate, 0.0);
      });
    });
  });

  // ============================================
  // PromotionEmployee
  // ============================================
  group('PromotionEmployee', () {
    test('fromJson nullable 필드 처리', () {
      // Given
      final json = {
        'id': 100,
        'employee_name': null,
        'schedule_date': null,
        'work_status': null,
        'work_type3': null,
        'professional_promotion_team': null,
        'target_amount': null,
        'actual_amount': null,
      };

      // When
      final employee = PromotionEmployee.fromJson(json);

      // Then
      expect(employee.id, 100);
      expect(employee.employeeName, isNull);
      expect(employee.scheduleDate, isNull);
      expect(employee.workStatus, isNull);
      expect(employee.workType3, isNull);
      expect(employee.professionalPromotionTeam, isNull);
      expect(employee.targetAmount, isNull);
      expect(employee.actualAmount, isNull);
    });

    test('fromJson 정상 파싱', () {
      // When
      final employee = PromotionEmployee.fromJson(_sampleEmployeeJson);

      // Then
      expect(employee.id, 1);
      expect(employee.employeeName, '김철수');
      expect(employee.scheduleDate, '2025-03-10');
      expect(employee.workStatus, '출근');
      expect(employee.workType3, '시식');
      expect(employee.professionalPromotionTeam, 'A팀');
      expect(employee.targetAmount, 500000);
      expect(employee.actualAmount, 350000);
    });
  });

  // ============================================
  // PromotionAmountText.formatAmount
  // ============================================
  group('PromotionAmountText.formatAmount', () {
    test('null이면 "-" 반환', () {
      expect(PromotionAmountText.formatAmount(null), '-');
    });

    test('10000 이상이면 만 단위 (4800000 -> "480만")', () {
      expect(PromotionAmountText.formatAmount(4800000), '480만');
    });

    test('10000 미만이면 원 단위 (5000 -> "5,000원")', () {
      expect(PromotionAmountText.formatAmount(5000), '5,000원');
    });

    test('정확히 10000이면 만 단위 (10000 -> "1만")', () {
      expect(PromotionAmountText.formatAmount(10000), '1만');
    });

    test('0이면 원 단위 ("0원")', () {
      expect(PromotionAmountText.formatAmount(0), '0원');
    });

    test('큰 금액 천 단위 콤마 (12345만)', () {
      expect(PromotionAmountText.formatAmount(123450000), '12,345만');
    });
  });
}

// ============================================
// Test Data
// ============================================

final _sampleItemJson = <String, dynamic>{
  'id': 1,
  'promotion_number': 'P-2025-001',
  'promotion_name': '이마트 죽전점 행사',
  'promotion_type_name': '시식행사',
  'account_name': '이마트 죽전점',
  'start_date': '2025-03-01',
  'end_date': '2025-03-15',
  'category': '라면',
  'stand_location': '1층 중앙',
  'target_amount': 5000000,
  'actual_amount': 3500000,
  'is_closed': false,
  'my_schedule_date': '2025-03-10',
};

final _sampleEmployeeJson = <String, dynamic>{
  'id': 1,
  'employee_name': '김철수',
  'schedule_date': '2025-03-10',
  'work_status': '출근',
  'work_type3': '시식',
  'professional_promotion_team': 'A팀',
  'target_amount': 500000,
  'actual_amount': 350000,
};

final _sampleDetailJson = <String, dynamic>{
  'id': 10,
  'promotion_number': 'P-2025-010',
  'promotion_name': '홈플러스 수지점 시식행사',
  'promotion_type_name': '시식행사',
  'account_name': '홈플러스 수지점',
  'start_date': '2025-03-01',
  'end_date': '2025-03-15',
  'category': '라면',
  'stand_location': '1층 중앙',
  'target_amount': 4800000,
  'actual_amount': 3200000,
  'is_closed': false,
  'primary_product_name': '진라면',
  'other_product': '참깨라면',
  'message': '행사 진행 중',
  'product_type': '라면류',
  'remark': '비고 사항',
  'employees': [
    {
      'id': 1,
      'employee_name': '김철수',
      'schedule_date': '2025-03-10',
      'work_status': '출근',
      'work_type3': '시식',
      'professional_promotion_team': 'A팀',
      'target_amount': 500000,
      'actual_amount': 350000,
    },
    {
      'id': 2,
      'employee_name': '이영희',
      'schedule_date': '2025-03-11',
      'work_status': '출근',
      'work_type3': '판매',
      'professional_promotion_team': 'B팀',
      'target_amount': 600000,
      'actual_amount': 480000,
    },
  ],
};
