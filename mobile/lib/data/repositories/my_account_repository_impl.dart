import '../../domain/repositories/my_account_repository.dart';
import '../datasources/my_account_remote_datasource.dart';

/// 내 거래처 Repository 구현체
///
/// Remote DataSource를 통해 API를 호출하고 Model → Entity로 변환합니다.
class MyAccountRepositoryImpl implements MyAccountRepository {
  final MyAccountRemoteDataSource _remoteDataSource;

  MyAccountRepositoryImpl({
    required MyAccountRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<MyAccountListResult> getMyAccounts({
    String? keyword,
    MyAccountScope scope = MyAccountScope.field,
  }) async {
    final response =
        await _remoteDataSource.getMyAccounts(keyword: keyword, scope: scope);

    final accounts = response.stores.map((model) => model.toEntity()).toList();

    return MyAccountListResult(
      accounts: accounts,
      totalCount: response.totalCount,
    );
  }
}
