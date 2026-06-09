import '../../../domain/entities/my_account.dart';
import '../../../domain/repositories/my_account_repository.dart';

/// 내 거래처 Mock Repository
///
/// Backend API 개발 전 프론트엔드 개발을 위한 Mock 데이터 제공.
/// Flutter-First 전략에 따라 하드코딩된 데이터로 UI/UX 검증.
class MyAccountMockRepository implements MyAccountRepository {
  /// 네트워크 지연 시뮬레이션 (300ms)
  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Mock 거래처 데이터
  static final List<MyAccount> _mockAccounts = [
    const MyAccount(
      accountId: 1025172,
      accountName: '(유)경산식품',
      accountCode: '1025172',
      address: '전라남도 목포시 임암로20번길 6 (상동)',
      representativeName: '김정자',
      phoneNumber: '061-123-4567',
    ),
    const MyAccount(
      accountId: 1025829,
      accountName: '(유)공일구물류',
      accountCode: '1025829',
      address: '전남 순천시 해룡면 대안마산길 80',
      representativeName: '천명관',
      phoneNumber: '061-234-5678',
    ),
    const MyAccount(
      accountId: 1030456,
      accountName: '대성마트',
      accountCode: '1030456',
      address: '광주광역시 서구 상무대로 1234',
      representativeName: '이대성',
      phoneNumber: '062-345-6789',
    ),
    const MyAccount(
      accountId: 1031789,
      accountName: '삼성유통',
      accountCode: '1031789',
      address: '전남 나주시 빛가람로 567',
      representativeName: '박삼성',
      phoneNumber: '061-456-7890',
    ),
    const MyAccount(
      accountId: 1032100,
      accountName: '한라식자재',
      accountCode: '1032100',
      address: '제주특별자치도 제주시 한라산로 890',
      representativeName: '강한라',
      phoneNumber: '064-567-8901',
    ),
    const MyAccount(
      accountId: 1033201,
      accountName: '남도푸드',
      accountCode: '1033201',
      address: '전남 여수시 여수대로 345',
      representativeName: '정남도',
      phoneNumber: '061-678-9012',
    ),
    const MyAccount(
      accountId: 1034302,
      accountName: '(주)새롬유통',
      accountCode: '1034302',
      address: '광주광역시 북구 첨단과기로 234',
      representativeName: '최새롬',
    ),
    const MyAccount(
      accountId: 1035403,
      accountName: '경남식품',
      accountCode: '1035403',
      address: '경남 창원시 성산구 중앙대로 678',
      representativeName: '윤경남',
      phoneNumber: '055-789-0123',
    ),
    const MyAccount(
      accountId: 1036504,
      accountName: '호남마트',
      accountCode: '1036504',
      address: '전북 전주시 덕진구 백제대로 901',
      representativeName: '임호남',
      phoneNumber: '063-890-1234',
    ),
    const MyAccount(
      accountId: 1037605,
      accountName: '(유)신선도매',
      accountCode: '1037605',
      address: '전남 광양시 광양읍 인덕로 456',
      representativeName: '한신선',
      phoneNumber: '061-901-2345',
    ),
    const MyAccount(
      accountId: 1038706,
      accountName: '풍성식자재',
      accountCode: '1038706',
      address: '광주광역시 남구 봉선로 789',
      representativeName: '오풍성',
      phoneNumber: '062-012-3456',
    ),
    const MyAccount(
      accountId: 1039807,
      accountName: '(주)동광유통',
      accountCode: '1039807',
      address: '전남 목포시 평화로 123',
      representativeName: '서동광',
      phoneNumber: '061-123-4568',
    ),
  ];

  @override
  Future<MyAccountListResult> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  }) async {
    await _simulateDelay();

    return MyAccountListResult(
      accounts: List.unmodifiable(_mockAccounts),
      totalCount: _mockAccounts.length,
    );
  }
}
