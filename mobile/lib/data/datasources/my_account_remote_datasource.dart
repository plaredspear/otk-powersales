import '../models/my_account_model.dart';

/// 내 거래처 API 응답 모델
class MyAccountListResponse {
  final List<MyAccountModel> stores;
  final int totalCount;

  const MyAccountListResponse({
    required this.stores,
    required this.totalCount,
  });

  factory MyAccountListResponse.fromJson(Map<String, dynamic> json) {
    final storesJson = json['stores'] as List<dynamic>? ?? [];
    final stores = storesJson
        .map((e) => MyAccountModel.fromJson(e as Map<String, dynamic>))
        .toList();
    final totalCount = json['total_count'] as int;

    return MyAccountListResponse(
      stores: stores,
      totalCount: totalCount,
    );
  }
}

/// 내 거래처 Remote DataSource 인터페이스
abstract class MyAccountRemoteDataSource {
  /// GET /api/v1/accounts/my
  Future<MyAccountListResponse> getMyAccounts({String? keyword});
}
