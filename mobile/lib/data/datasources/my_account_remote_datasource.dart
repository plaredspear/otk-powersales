import '../../domain/entities/my_account_meta.dart';
import '../../domain/repositories/my_account_repository.dart';
import '../models/my_account_model.dart';

/// 내 거래처 API 응답 모델
class MyAccountListResponse {
  final List<MyAccountModel> accounts;
  final int totalCount;

  /// 거래처 표시 기준 안내 (서버 제공). 구버전 서버 응답 시 null.
  final MyAccountMeta? meta;

  const MyAccountListResponse({
    required this.accounts,
    required this.totalCount,
    this.meta,
  });

  factory MyAccountListResponse.fromJson(Map<String, dynamic> json) {
    final accountsJson = json['accounts'] as List<dynamic>? ?? [];
    final accounts = accountsJson
        .map((e) => MyAccountModel.fromJson(e as Map<String, dynamic>))
        .toList();
    final totalCount = json['totalCount'] as int;
    final metaJson = json['meta'] as Map<String, dynamic>?;

    return MyAccountListResponse(
      accounts: accounts,
      totalCount: totalCount,
      meta: metaJson == null ? null : MyAccountMeta.fromJson(metaJson),
    );
  }
}

/// 내 거래처 Remote DataSource 인터페이스
abstract class MyAccountRemoteDataSource {
  /// GET /api/v1/mobile/accounts/my
  Future<MyAccountListResponse> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  });
}
