import '../entities/attendance_result.dart';
import '../entities/attendance_status.dart';
import '../entities/account_schedule_item.dart';

/// 거래처 목록 조회 결과
class AccountListResult {
  final List<AccountScheduleItem> accounts;
  final int totalCount;
  final int registeredCount;
  final String currentDate;
  final bool safetyCheckCompleted;

  const AccountListResult({
    required this.accounts,
    required this.totalCount,
    required this.registeredCount,
    required this.currentDate,
    required this.safetyCheckCompleted,
  });
}

/// 출근등록 현황 조회 결과
class AttendanceStatusResult {
  final int totalCount;
  final int registeredCount;
  final List<AttendanceStatus> statusList;
  final String currentDate;

  const AttendanceStatusResult({
    required this.totalCount,
    required this.registeredCount,
    required this.statusList,
    required this.currentDate,
  });
}

/// 출근등록 Repository 인터페이스
abstract class AttendanceRepository {
  /// 오늘 출근 거래처 목록 조회
  Future<AccountListResult> getAccountList({String? keyword});

  /// 출근등록
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    required double latitude,
    required double longitude,
  });

  /// 출근등록 현황 조회
  Future<AttendanceStatusResult> getAttendanceStatus();
}
