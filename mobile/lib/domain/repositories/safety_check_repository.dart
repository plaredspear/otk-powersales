import '../entities/safety_check_category.dart';
import '../entities/safety_check_today_status.dart';
import '../entities/safety_check_submit_result.dart';

/// 안전점검 Repository 인터페이스
abstract class SafetyCheckRepository {
  /// 안전점검 체크리스트 항목 조회
  Future<List<SafetyCheckCategory>> getItems();

  /// 오늘 안전점검 완료 여부 조회
  Future<SafetyCheckTodayStatus> getTodayStatus();

  /// 안전점검 제출 (V1 구조)
  Future<SafetyCheckSubmitResult> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  });
}

/// 장비 항목 응답
class EquipmentAnswer {
  final int seqNum;
  final String answer; // "예" or "해당없음"

  const EquipmentAnswer({
    required this.seqNum,
    required this.answer,
  });

  Map<String, dynamic> toJson() => {
        'seqNum': seqNum,
        'answer': answer,
      };
}
