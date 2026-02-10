import '../repositories/attendance_repository.dart';

/// 거래처 목록 조회 UseCase
///
/// 오늘 출근해야 할 거래처 목록을 조회합니다.
class GetStoreList {
  final AttendanceRepository _repository;

  GetStoreList(this._repository);

  /// 거래처 목록 조회 실행
  Future<StoreListResult> call() async {
    return await _repository.getStoreList();
  }

  /// 키워드로 거래처 필터링 (클라이언트 사이드)
  ///
  /// 거래처명, 주소, 거래처코드에서 키워드를 검색합니다.
  Future<StoreListResult> callWithFilter(String keyword) async {
    final result = await _repository.getStoreList();

    if (keyword.isEmpty) {
      return result;
    }

    final lowerKeyword = keyword.toLowerCase();
    final filteredStores = result.stores.where((store) {
      return store.storeName.toLowerCase().contains(lowerKeyword) ||
          store.address.toLowerCase().contains(lowerKeyword) ||
          store.storeCode.toLowerCase().contains(lowerKeyword);
    }).toList();

    return StoreListResult(
      workerType: result.workerType,
      stores: filteredStores,
      totalCount: result.totalCount,
      registeredCount: result.registeredCount,
      currentDate: result.currentDate,
    );
  }
}
