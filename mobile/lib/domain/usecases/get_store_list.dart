import '../repositories/attendance_repository.dart';

/// 거래처 목록 조회 UseCase
///
/// 오늘 출근해야 할 거래처 목록을 조회합니다.
class GetStoreList {
  final AttendanceRepository _repository;

  GetStoreList(this._repository);

  /// 거래처 목록 조회 실행
  Future<StoreListResult> call({String? keyword}) async {
    return await _repository.getStoreList(keyword: keyword);
  }
}
