import '../../../domain/entities/my_store.dart';
import '../../../domain/repositories/my_store_repository.dart';

/// 내 거래처 Mock Repository
///
/// Backend API 개발 전 프론트엔드 개발을 위한 Mock 데이터 제공.
/// Flutter-First 전략에 따라 하드코딩된 데이터로 UI/UX 검증.
class MyStoreMockRepository implements MyStoreRepository {
  /// 네트워크 지연 시뮬레이션 (300ms)
  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Mock 거래처 데이터
  static final List<MyStore> _mockStores = [
    const MyStore(
      storeId: 1025172,
      storeName: '(유)경산식품',
      storeCode: '1025172',
      address: '전라남도 목포시 임암로20번길 6 (상동)',
      representativeName: '김정자',
      phoneNumber: '061-123-4567',
    ),
    const MyStore(
      storeId: 1025829,
      storeName: '(유)공일구물류',
      storeCode: '1025829',
      address: '전남 순천시 해룡면 대안마산길 80',
      representativeName: '천명관',
      phoneNumber: '061-234-5678',
    ),
    const MyStore(
      storeId: 1030456,
      storeName: '대성마트',
      storeCode: '1030456',
      address: '광주광역시 서구 상무대로 1234',
      representativeName: '이대성',
      phoneNumber: '062-345-6789',
    ),
    const MyStore(
      storeId: 1031789,
      storeName: '삼성유통',
      storeCode: '1031789',
      address: '전남 나주시 빛가람로 567',
      representativeName: '박삼성',
      phoneNumber: '061-456-7890',
    ),
    const MyStore(
      storeId: 1032100,
      storeName: '한라식자재',
      storeCode: '1032100',
      address: '제주특별자치도 제주시 한라산로 890',
      representativeName: '강한라',
      phoneNumber: '064-567-8901',
    ),
    const MyStore(
      storeId: 1033201,
      storeName: '남도푸드',
      storeCode: '1033201',
      address: '전남 여수시 여수대로 345',
      representativeName: '정남도',
      phoneNumber: '061-678-9012',
    ),
    const MyStore(
      storeId: 1034302,
      storeName: '(주)새롬유통',
      storeCode: '1034302',
      address: '광주광역시 북구 첨단과기로 234',
      representativeName: '최새롬',
    ),
    const MyStore(
      storeId: 1035403,
      storeName: '경남식품',
      storeCode: '1035403',
      address: '경남 창원시 성산구 중앙대로 678',
      representativeName: '윤경남',
      phoneNumber: '055-789-0123',
    ),
    const MyStore(
      storeId: 1036504,
      storeName: '호남마트',
      storeCode: '1036504',
      address: '전북 전주시 덕진구 백제대로 901',
      representativeName: '임호남',
      phoneNumber: '063-890-1234',
    ),
    const MyStore(
      storeId: 1037605,
      storeName: '(유)신선도매',
      storeCode: '1037605',
      address: '전남 광양시 광양읍 인덕로 456',
      representativeName: '한신선',
      phoneNumber: '061-901-2345',
    ),
    const MyStore(
      storeId: 1038706,
      storeName: '풍성식자재',
      storeCode: '1038706',
      address: '광주광역시 남구 봉선로 789',
      representativeName: '오풍성',
      phoneNumber: '062-012-3456',
    ),
    const MyStore(
      storeId: 1039807,
      storeName: '(주)동광유통',
      storeCode: '1039807',
      address: '전남 목포시 평화로 123',
      representativeName: '서동광',
      phoneNumber: '061-123-4568',
    ),
  ];

  @override
  Future<MyStoreListResult> getMyStores() async {
    await _simulateDelay();

    return MyStoreListResult(
      stores: List.unmodifiable(_mockStores),
      totalCount: _mockStores.length,
    );
  }
}
