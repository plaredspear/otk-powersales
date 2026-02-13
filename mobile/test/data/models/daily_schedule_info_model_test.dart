import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/daily_schedule_info_model.dart';
import 'package:mobile/domain/entities/daily_schedule_info.dart';
import 'package:mobile/domain/entities/schedule_store_detail.dart';

void main() {
  group('ReportProgressModel', () {
    const testModel = ReportProgressModel(
      completed: 0,
      total: 3,
      workType: '진열',
    );

    final testJson = {
      'completed': 0,
      'total': 3,
      'work_type': '진열',
    };

    const testEntity = ReportProgress(
      completed: 0,
      total: 3,
      workType: '진열',
    );

    test('fromJson이 올바르게 동작한다', () {
      final result = ReportProgressModel.fromJson(testJson);
      expect(result.completed, 0);
      expect(result.total, 3);
      expect(result.workType, '진열');
    });

    test('toJson이 올바르게 동작한다', () {
      final result = testModel.toJson();
      expect(result, testJson);
    });

    test('toEntity가 올바르게 동작한다', () {
      final result = testModel.toEntity();
      expect(result, testEntity);
    });

    test('fromEntity가 올바르게 동작한다', () {
      final result = ReportProgressModel.fromEntity(testEntity);
      expect(result, testModel);
    });
  });

  group('ScheduleStoreDetailModel', () {
    const testModel = ScheduleStoreDetailModel(
      storeId: 1,
      storeName: '(주)이마트트레이더스명지점',
      workType1: '진열',
      workType2: '전담',
      workType3: '순회',
      isRegistered: false,
    );

    final testJson = {
      'store_id': 1,
      'store_name': '(주)이마트트레이더스명지점',
      'work_type_1': '진열',
      'work_type_2': '전담',
      'work_type_3': '순회',
      'is_registered': false,
    };

    const testEntity = ScheduleStoreDetail(
      storeId: 1,
      storeName: '(주)이마트트레이더스명지점',
      workType1: '진열',
      workType2: '전담',
      workType3: '순회',
      isRegistered: false,
    );

    test('fromJson이 snake_case 키를 올바르게 파싱한다', () {
      final result = ScheduleStoreDetailModel.fromJson(testJson);
      expect(result.storeId, 1);
      expect(result.storeName, '(주)이마트트레이더스명지점');
      expect(result.workType1, '진열');
      expect(result.workType2, '전담');
      expect(result.workType3, '순회');
      expect(result.isRegistered, false);
    });

    test('toJson이 snake_case 키로 올바르게 직렬화한다', () {
      final result = testModel.toJson();
      expect(result, testJson);
    });

    test('toEntity가 올바르게 동작한다', () {
      final result = testModel.toEntity();
      expect(result, testEntity);
    });

    test('fromEntity가 올바르게 동작한다', () {
      final result = ScheduleStoreDetailModel.fromEntity(testEntity);
      expect(result, testModel);
    });
  });

  group('DailyScheduleInfoModel', () {
    final testStores = [
      const ScheduleStoreDetailModel(
        storeId: 1,
        storeName: '(주)이마트트레이더스명지점',
        workType1: '진열',
        workType2: '전담',
        workType3: '순회',
        isRegistered: false,
      ),
      const ScheduleStoreDetailModel(
        storeId: 2,
        storeName: '롯데마트 사상',
        workType1: '진열',
        workType2: '전담',
        workType3: '격고',
        isRegistered: false,
      ),
    ];

    final testModel = DailyScheduleInfoModel(
      date: '2026-02-04',
      dayOfWeek: '화',
      memberName: '최금주',
      employeeNumber: '20030117',
      reportProgress: const ReportProgressModel(
        completed: 0,
        total: 3,
        workType: '진열',
      ),
      stores: testStores,
    );

    final testJson = {
      'date': '2026-02-04',
      'day_of_week': '화',
      'member_name': '최금주',
      'employee_number': '20030117',
      'report_progress': {
        'completed': 0,
        'total': 3,
        'work_type': '진열',
      },
      'stores': [
        {
          'store_id': 1,
          'store_name': '(주)이마트트레이더스명지점',
          'work_type_1': '진열',
          'work_type_2': '전담',
          'work_type_3': '순회',
          'is_registered': false,
        },
        {
          'store_id': 2,
          'store_name': '롯데마트 사상',
          'work_type_1': '진열',
          'work_type_2': '전담',
          'work_type_3': '격고',
          'is_registered': false,
        },
      ],
    };

    final testEntity = DailyScheduleInfo(
      date: '2026년 02월 04일(화)',
      memberName: '최금주',
      employeeNumber: '20030117',
      reportProgress: const ReportProgress(
        completed: 0,
        total: 3,
        workType: '진열',
      ),
      stores: const [
        ScheduleStoreDetail(
          storeId: 1,
          storeName: '(주)이마트트레이더스명지점',
          workType1: '진열',
          workType2: '전담',
          workType3: '순회',
          isRegistered: false,
        ),
        ScheduleStoreDetail(
          storeId: 2,
          storeName: '롯데마트 사상',
          workType1: '진열',
          workType2: '전담',
          workType3: '격고',
          isRegistered: false,
        ),
      ],
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        // Act
        final result = DailyScheduleInfoModel.fromJson(testJson);

        // Assert
        expect(result.date, '2026-02-04');
        expect(result.dayOfWeek, '화');
        expect(result.memberName, '최금주');
        expect(result.employeeNumber, '20030117');
        expect(result.reportProgress.completed, 0);
        expect(result.stores.length, 2);
        expect(result.stores[0].storeName, '(주)이마트트레이더스명지점');
      });

      test('빈 거래처 목록을 파싱할 수 있다', () {
        // Arrange
        final json = {
          'date': '2026-02-04',
          'day_of_week': '화',
          'member_name': '최금주',
          'employee_number': '20030117',
          'report_progress': {
            'completed': 0,
            'total': 0,
            'work_type': '진열',
          },
          'stores': [],
        };

        // Act
        final result = DailyScheduleInfoModel.fromJson(json);

        // Assert
        expect(result.stores, isEmpty);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        // Act
        final result = testModel.toJson();

        // Assert
        expect(result['date'], '2026-02-04');
        expect(result['day_of_week'], '화');
        expect(result['member_name'], '최금주');
        expect(result['employee_number'], '20030117');
        expect(result['report_progress']['work_type'], '진열');
        expect(result['stores'], isA<List>());
        expect(result['stores'].length, 2);
      });
    });

    group('toEntity', () {
      test('Domain Entity로 올바르게 변환해야 한다', () {
        // Act
        final result = testModel.toEntity();

        // Assert
        expect(result, isA<DailyScheduleInfo>());
        expect(result.date, '2026년 02월 04일(화)');
        expect(result.memberName, '최금주');
        expect(result.employeeNumber, '20030117');
        expect(result.stores.length, 2);
      });

      test('날짜와 요일을 결합하여 포맷된 문자열을 생성해야 한다', () {
        // Arrange
        final model = DailyScheduleInfoModel(
          date: '2020-08-04',
          dayOfWeek: '화',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: const [],
        );

        // Act
        final result = model.toEntity();

        // Assert
        expect(result.date, '2020년 08월 04일(화)');
      });
    });

    group('fromEntity', () {
      test('Domain Entity로부터 Model을 생성할 수 있다', () {
        // Act
        final result = DailyScheduleInfoModel.fromEntity(testEntity);

        // Assert
        expect(result.date, '2026-02-04');
        expect(result.dayOfWeek, '화');
        expect(result.memberName, '최금주');
        expect(result.employeeNumber, '20030117');
      });

      test('포맷된 날짜 문자열을 date와 dayOfWeek로 분리해야 한다', () {
        // Arrange
        final entity = DailyScheduleInfo(
          date: '2020년 08월 04일(화)',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgress(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: const [],
        );

        // Act
        final result = DailyScheduleInfoModel.fromEntity(entity);

        // Assert
        expect(result.date, '2020-08-04');
        expect(result.dayOfWeek, '화');
      });

      test('잘못된 날짜 형식에 대해 ArgumentError를 던진다', () {
        // Arrange
        final entity = DailyScheduleInfo(
          date: 'Invalid Date Format',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgress(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: const [],
        );

        // Act & Assert
        expect(
          () => DailyScheduleInfoModel.fromEntity(entity),
          throwsA(isA<ArgumentError>()),
        );
      });
    });

    group('왕복 변환', () {
      test('JSON → Model → JSON 왕복 변환이 정확해야 한다', () {
        // Act
        final model = DailyScheduleInfoModel.fromJson(testJson);
        final json = model.toJson();

        // Assert
        expect(json['date'], testJson['date']);
        expect(json['day_of_week'], testJson['day_of_week']);
        expect(json['member_name'], testJson['member_name']);
      });

      test('Entity → Model → Entity 왕복 변환이 정확해야 한다', () {
        // Act
        final model = DailyScheduleInfoModel.fromEntity(testEntity);
        final entity = model.toEntity();

        // Assert
        expect(entity.date, testEntity.date);
        expect(entity.memberName, testEntity.memberName);
        expect(entity.employeeNumber, testEntity.employeeNumber);
      });
    });

    group('equality', () {
      test('같은 값을 가진 모델이 동일하게 비교되어야 한다', () {
        // Arrange
        final model1 = DailyScheduleInfoModel(
          date: '2026-02-04',
          dayOfWeek: '화',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: testStores,
        );
        final model2 = DailyScheduleInfoModel(
          date: '2026-02-04',
          dayOfWeek: '화',
          memberName: '최금주',
          employeeNumber: '20030117',
          reportProgress: const ReportProgressModel(
            completed: 0,
            total: 3,
            workType: '진열',
          ),
          stores: testStores,
        );

        // Assert
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });
    });

    test('toString이 올바르게 동작해야 한다', () {
      // Act
      final result = testModel.toString();

      // Assert
      expect(result, contains('DailyScheduleInfoModel'));
      expect(result, contains('최금주'));
    });
  });
}
