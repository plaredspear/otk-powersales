import '../../domain/entities/attendance_summary.dart';
import '../../domain/entities/expiry_alert.dart';
import '../../domain/entities/notice.dart';
import '../../domain/entities/schedule.dart';

/// 홈 화면 Mock 데이터
class HomeMockData {
  /// 오늘 일정 목록 (일정이 있는 경우)
  static const List<Schedule> todaySchedules = [
    Schedule(
      scheduleId: 'a0X000001',
      employeeName: '홍길동',
      employeeSfid: '005000001',
      storeName: '이마트 부산점',
      storeSfid: '001000001',
      workCategory: '순회',
      workType: '정기',
      isCommuteRegistered: false,
    ),
    Schedule(
      scheduleId: 'a0X000002',
      employeeName: '홍길동',
      employeeSfid: '005000001',
      storeName: '홈플러스 해운대점',
      storeSfid: '001000002',
      workCategory: '격고',
      workType: '정기',
      isCommuteRegistered: true,
    ),
    Schedule(
      scheduleId: 'a0X000003',
      employeeName: '홍길동',
      employeeSfid: '005000001',
      storeName: '롯데마트 서면점',
      storeSfid: '001000003',
      workCategory: '고정',
      isCommuteRegistered: false,
    ),
  ];

  /// 오늘 일정 목록 (일정이 없는 경우)
  static const List<Schedule> emptySchedules = [];

  /// 출근 현황 집계
  static const AttendanceSummary attendanceSummary = AttendanceSummary(
    totalCount: 3,
    registeredCount: 1,
  );

  /// 유통기한 알림
  static const ExpiryAlert expiryAlert = ExpiryAlert(
    branchName: '부산1지점',
    employeeName: '최금주',
    employeeId: '20030117',
    expiryCount: 1,
  );

  /// 공지사항 목록
  static final List<Notice> notices = [
    Notice(
      id: 1,
      title: '2월 영업 목표 달성 현황',
      type: 'BRANCH',
      createdAt: DateTime.parse('2026-02-05T10:00:00.000Z'),
    ),
    Notice(
      id: 2,
      title: '신제품 출시 안내',
      type: 'ALL',
      createdAt: DateTime.parse('2026-02-04T09:00:00.000Z'),
    ),
    Notice(
      id: 3,
      title: '부산지역 프로모션 안내',
      type: 'BRANCH',
      createdAt: DateTime.parse('2026-02-03T14:00:00.000Z'),
    ),
    Notice(
      id: 4,
      title: '2월 교육 일정 안내',
      type: 'ALL',
      createdAt: DateTime.parse('2026-02-02T11:00:00.000Z'),
    ),
    Notice(
      id: 5,
      title: '설 연휴 배송 일정 변경 안내',
      type: 'ALL',
      createdAt: DateTime.parse('2026-02-01T09:00:00.000Z'),
    ),
  ];

  /// 빈 공지사항 목록
  static const List<Notice> emptyNotices = [];

  /// 현재 날짜
  static const String currentDate = '2026-02-07';
}
