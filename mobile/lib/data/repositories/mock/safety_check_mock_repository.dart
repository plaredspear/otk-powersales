import '../../../domain/entities/safety_check_category.dart';
import '../../../domain/entities/safety_check_item.dart';
import '../../../domain/entities/safety_check_today_status.dart';
import '../../../domain/entities/safety_check_submit_result.dart';
import '../../../domain/repositories/safety_check_repository.dart';

/// 안전점검 Mock Repository (테스트용)
class SafetyCheckMockRepository implements SafetyCheckRepository {
  List<SafetyCheckCategory>? customCategories;
  SafetyCheckTodayStatus? customTodayStatus;
  Exception? exceptionToThrow;
  bool _submitted = false;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    await _simulateDelay();
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return customCategories ?? _defaultCategories;
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() async {
    await _simulateDelay();
    if (exceptionToThrow != null) throw exceptionToThrow!;
    if (customTodayStatus != null) return customTodayStatus!;
    if (_submitted) {
      return SafetyCheckTodayStatus(
        completed: true,
        submittedAt: DateTime.now(),
      );
    }
    return const SafetyCheckTodayStatus(completed: false, submittedAt: null);
  }

  @override
  Future<SafetyCheckSubmitResult> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  }) async {
    await _simulateDelay();
    if (exceptionToThrow != null) throw exceptionToThrow!;
    if (_submitted) throw Exception('이미 안전점검을 완료하였습니다.');
    _submitted = true;
    return SafetyCheckSubmitResult(
      submittedAt: completeTime,
      safetyCheckCompleted: true,
    );
  }

  static final List<SafetyCheckCategory> _defaultCategories = [
    const SafetyCheckCategory(
      questionNum: 1,
      title: '안전예방 장비 착용',
      inputType: 'RADIO',
      required: true,
      options: ['예', '해당없음'],
      items: [
        SafetyCheckItem(seqNum: 1, contents: '손목보호대를 착용했습니다'),
        SafetyCheckItem(seqNum: 2, contents: '숨수건(화재피해 예방)을 소지하고 있습니다'),
        SafetyCheckItem(seqNum: 3, contents: '안전화를 착용했습니다'),
        SafetyCheckItem(seqNum: 4, contents: '진열업무시 코팅장갑 및 허리보호대를 착용합니다'),
        SafetyCheckItem(seqNum: 5, contents: '진열대가 높을 경우 안전사다리를 사용합니다'),
        SafetyCheckItem(seqNum: 6, contents: '시식행사 진행시 위생장갑을 사용합니다'),
        SafetyCheckItem(seqNum: 7, contents: '오뚜기 유니폼을 착용하였습니다'),
        SafetyCheckItem(seqNum: 8, contents: '오뚜기 판매여사원 명찰을 착용하였습니다'),
        SafetyCheckItem(seqNum: 9, contents: '(위생)마스크 착용 했습니다'),
      ],
    ),
    const SafetyCheckCategory(
      questionNum: 2,
      title: '안전사고 예방사항',
      inputType: 'CHECKBOX',
      required: false,
      items: [
        SafetyCheckItem(seqNum: 1, contents: '예방사항 항목 1'),
        SafetyCheckItem(seqNum: 2, contents: '예방사항 항목 2'),
      ],
    ),
  ];
}
