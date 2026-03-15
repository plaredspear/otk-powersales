import '../../domain/entities/attendance_result.dart';
import '../../domain/repositories/attendance_repository.dart';
import '../datasources/attendance_api_datasource.dart';

/// Attendance Repository 구현체
///
/// AttendanceApiDataSource에 위임하여 Backend API와 통신한다.
class AttendanceRepositoryImpl implements AttendanceRepository {
  final AttendanceApiDataSource _dataSource;

  AttendanceRepositoryImpl({
    required AttendanceApiDataSource dataSource,
  }) : _dataSource = dataSource;

  @override
  Future<StoreListResult> getStoreList({String? keyword}) {
    return _dataSource.getStoreList(keyword: keyword);
  }

  @override
  Future<AttendanceResult> registerAttendance({
    required int scheduleId,
    required double latitude,
    required double longitude,
    String? workType,
  }) {
    return _dataSource.registerAttendance(
      scheduleId: scheduleId,
      latitude: latitude,
      longitude: longitude,
      workType: workType,
    );
  }

  @override
  Future<AttendanceStatusResult> getAttendanceStatus() {
    return _dataSource.getAttendanceStatus();
  }
}
