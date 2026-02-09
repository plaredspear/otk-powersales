import '../../../domain/entities/safety_check_category.dart';
import '../../../domain/entities/safety_check_item.dart';
import '../../../domain/entities/safety_check_today_status.dart';
import '../../../domain/entities/safety_check_submit_result.dart';
import '../../../domain/repositories/safety_check_repository.dart';

/// 안전점검 Mock Repository
///
/// Backend API 개발 전 Flutter-First 전략에 따라
/// 하드코딩된 Mock 데이터로 안전점검 기능을 구현합니다.
class SafetyCheckMockRepository implements SafetyCheckRepository {
  /// 커스텀 카테고리 목록 (테스트용)
  List<SafetyCheckCategory>? customCategories;

  /// 커스텀 오늘 점검 상태 (테스트용)
  SafetyCheckTodayStatus? customTodayStatus;

  /// 테스트용 예외
  Exception? exceptionToThrow;

  /// 제출 완료 여부 추적
  bool _submitted = false;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    return customCategories ?? _defaultCategories;
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    if (customTodayStatus != null) {
      return customTodayStatus!;
    }

    // 제출 완료 후에는 completed=true 반환
    if (_submitted) {
      return SafetyCheckTodayStatus(
        completed: true,
        submittedAt: DateTime.now(),
      );
    }

    return const SafetyCheckTodayStatus(
      completed: false,
      submittedAt: null,
    );
  }

  @override
  Future<SafetyCheckSubmitResult> submit(List<int> checkedItemIds) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    if (_submitted) {
      throw Exception('이미 안전점검을 완료하였습니다.');
    }

    _submitted = true;

    return SafetyCheckSubmitResult(
      submissionId: 1,
      submittedAt: DateTime.now(),
      safetyCheckCompleted: true,
    );
  }

  /// Mock 데이터: 기본 안전점검 카테고리 목록
  static final List<SafetyCheckCategory> _defaultCategories = [
    const SafetyCheckCategory(
      id: 1,
      name: '안전예방 장비 착용',
      description: '아래 항목을 모두 체크하세요',
      items: [
        SafetyCheckItem(
            id: 1, label: '손목보호대', sortOrder: 1, required: true),
        SafetyCheckItem(
            id: 2, label: '송수건(화재피해 예방)', sortOrder: 2, required: true),
        SafetyCheckItem(
            id: 3, label: '안전화', sortOrder: 3, required: true),
        SafetyCheckItem(
            id: 4,
            label: '진열업무 코팅장갑 및 허리보호대',
            sortOrder: 4,
            required: true),
        SafetyCheckItem(
            id: 5, label: '안전사다리', sortOrder: 5, required: true),
        SafetyCheckItem(
            id: 6, label: '시식행사 위생장갑', sortOrder: 6, required: true),
        SafetyCheckItem(
            id: 7, label: '오뚜기 유니폼', sortOrder: 7, required: true),
      ],
    ),
    const SafetyCheckCategory(
      id: 2,
      name: '사고 예방',
      description: '아래 항목을 모두 체크하세요',
      items: [
        SafetyCheckItem(
            id: 8, label: '지게차 근접 금지', sortOrder: 1, required: true),
        SafetyCheckItem(
            id: 9, label: '무거운 제품 운반 자세', sortOrder: 2, required: true),
        SafetyCheckItem(
            id: 10, label: '장애물 시야 확보', sortOrder: 3, required: true),
        SafetyCheckItem(
            id: 11, label: '카트 이동 주의', sortOrder: 4, required: true),
        SafetyCheckItem(
            id: 12, label: '매장/계단 뛰기 금지', sortOrder: 5, required: true),
        SafetyCheckItem(
            id: 13, label: '미끄러운 바닥 주의', sortOrder: 6, required: true),
        SafetyCheckItem(
            id: 14,
            label: '높은곳 진열 2인1조',
            sortOrder: 7,
            required: true),
        SafetyCheckItem(
            id: 15,
            label: '후방창고 적재 안전',
            sortOrder: 8,
            required: true),
      ],
    ),
  ];
}
