import '../entities/my_store.dart';

/// 내 거래처 목록 결과 값 객체
///
/// 한 달 일정에 등록된 거래처 목록과 총 건수를 담습니다.
class MyStoreListResult {
  /// 거래처 목록
  final List<MyStore> stores;

  /// 총 거래처 수
  final int totalCount;

  const MyStoreListResult({
    required this.stores,
    required this.totalCount,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MyStoreListResult) return false;
    if (other.totalCount != totalCount) return false;
    if (other.stores.length != stores.length) return false;
    for (var i = 0; i < stores.length; i++) {
      if (other.stores[i] != stores[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(stores),
      totalCount,
    );
  }

  @override
  String toString() {
    return 'MyStoreListResult(stores: ${stores.length}, '
        'totalCount: $totalCount)';
  }
}

/// 내 거래처 Repository 인터페이스
///
/// 한 달 일정에 등록된 거래처 목록 조회를 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class MyStoreRepository {
  /// 내 거래처 목록 조회
  ///
  /// 한 달 일정에 등록된 거래처 목록을 조회합니다.
  /// 인증 토큰으로 사용자를 식별합니다.
  ///
  /// Returns: 거래처 목록과 총 건수
  Future<MyStoreListResult> getMyStores();
}
