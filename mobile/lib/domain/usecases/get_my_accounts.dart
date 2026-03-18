import '../repositories/my_account_repository.dart';

/// 내 거래처 목록 조회 UseCase
///
/// 한 달 일정에 등록된 거래처 목록을 조회합니다.
class GetMyAccounts {
  final MyAccountRepository _repository;

  GetMyAccounts(this._repository);

  /// 내 거래처 목록 조회 실행
  Future<MyAccountListResult> call() async {
    return await _repository.getMyAccounts();
  }
}
