import '../../domain/repositories/my_account_repository.dart';
import '../models/my_account_model.dart';

/// 내 거래처 API 응답 모델
class MyAccountListResponse {
  final List<MyAccountModel> accounts;
  final int totalCount;

  const MyAccountListResponse({
    required this.accounts,
    required this.totalCount,
  });

  factory MyAccountListResponse.fromJson(Map<String, dynamic> json) {
    final accountsJson = json['accounts'] as List<dynamic>? ?? [];
    final accounts = accountsJson
        .map((e) => MyAccountModel.fromJson(e as Map<String, dynamic>))
        .toList();
    final totalCount = json['totalCount'] as int;

    return MyAccountListResponse(
      accounts: accounts,
      totalCount: totalCount,
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
