import '../entities/my_account.dart';
import '../entities/my_account_meta.dart';

/// 내 거래처 조회 범위 — 레거시 화면 유형별 거래처 조회 기준 차이.
///
/// - [sales] : 매출 계열(POS/전산/월매출). 부서장(AccountViewAll)이면 전체 거래처를 노출.
/// - [field] : 현장 활동 계열(판촉/점검/소비기한/클레임). 부서장 전체조회 분기 없음.
/// - [order] : 주문 작성 계열. 진열 일정 union + 주문가능 abctypecode 필터(레거시 accountSelectList order=order).
///
/// 여사원/조장 경로는 sales/field 두 유형이 동일하다.
enum MyAccountScope {
  sales,
  field,
  order;

  /// 백엔드 `scope` 쿼리 파라미터 값 (field 는 기본값이라 미전송)
  String? get queryValue => switch (this) {
        MyAccountScope.sales => 'sales',
        MyAccountScope.order => 'order',
        MyAccountScope.field => null,
      };
}

/// 내 거래처 목록 결과 값 객체
///
/// 한 달 일정에 등록된 거래처 목록과 총 건수를 담습니다.
class MyAccountListResult {
  /// 거래처 목록
  final List<MyAccount> accounts;

  /// 총 거래처 수
  final int totalCount;

  /// 거래처 표시 기준 안내 (서버 제공). 구버전 서버 응답 등 미제공 시 null.
  final MyAccountMeta? meta;

  const MyAccountListResult({
    required this.accounts,
    required this.totalCount,
    this.meta,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! MyAccountListResult) return false;
    if (other.totalCount != totalCount) return false;
    if (other.meta != meta) return false;
    if (other.accounts.length != accounts.length) return false;
    for (var i = 0; i < accounts.length; i++) {
      if (other.accounts[i] != accounts[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      Object.hashAll(accounts),
      totalCount,
      meta,
    );
  }

  @override
  String toString() {
    return 'MyAccountListResult(accounts: ${accounts.length}, '
        'totalCount: $totalCount, meta: $meta)';
  }
}

/// 내 거래처 Repository 인터페이스
///
/// 한 달 일정에 등록된 거래처 목록 조회를 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class MyAccountRepository {
  /// 내 거래처 목록 조회
  ///
  /// 한 달 일정에 등록된 거래처 목록을 조회합니다.
  /// 인증 토큰으로 사용자를 식별합니다.
  ///
  /// Returns: 거래처 목록과 총 건수
  Future<MyAccountListResult> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  });
}
