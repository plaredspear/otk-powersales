import '../../domain/entities/expiry_alert.dart';
import '../../domain/entities/notice.dart';
import '../../domain/entities/schedule.dart';

/// 홈 화면 Mock 데이터
class HomeMockData {
  /// 오늘 일정 목록 (일정이 있는 경우)
  static const List<Schedule> todaySchedules = [
    Schedule(
      id: 1,
      storeName: '이마트 부산점',
      startTime: '09:00',
      endTime: '12:00',
      type: '순회',
    ),
    Schedule(
      id: 2,
      storeName: '홈플러스 해운대점',
      startTime: '13:00',
      endTime: '15:00',
      type: '격고',
    ),
    Schedule(
      id: 3,
      storeName: '롯데마트 서면점',
      startTime: '15:30',
      endTime: '17:00',
      type: '고정',
    ),
  ];

  /// 오늘 일정 목록 (일정이 없는 경우)
  static const List<Schedule> emptySchedules = [];

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
