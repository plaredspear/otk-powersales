import '../repositories/my_store_repository.dart';

/// 내 거래처 목록 조회 UseCase
///
/// 한 달 일정에 등록된 거래처 목록을 조회합니다.
class GetMyStores {
  final MyStoreRepository _repository;

  GetMyStores(this._repository);

  /// 내 거래처 목록 조회 실행
  Future<MyStoreListResult> call() async {
    return await _repository.getMyStores();
  }
}
