import '../../domain/entities/alternative_holiday.dart';
import '../../domain/repositories/alternative_holiday_repository.dart';
import '../datasources/alternative_holiday_api_datasource.dart';

/// AlternativeHolidayRepository 구현체
class AlternativeHolidayRepositoryImpl implements AlternativeHolidayRepository {
  final AlternativeHolidayApiDataSource _dataSource;

  const AlternativeHolidayRepositoryImpl(this._dataSource);

  @override
  Future<AlternativeHoliday> createAlternativeHoliday({
    required DateTime actualWorkDate,
    required DateTime targetAltHolidayDate,
  }) async {
    final model = await _dataSource.createAlternativeHoliday(
      actualWorkDate: _formatDate(actualWorkDate),
      targetAltHolidayDate: _formatDate(targetAltHolidayDate),
    );
    return model.toEntity();
  }

  @override
  Future<List<AlternativeHoliday>> getAlternativeHolidays({
    DateTime? startDate,
    DateTime? endDate,
  }) async {
    final models = await _dataSource.getAlternativeHolidays(
      startDate: startDate != null ? _formatDate(startDate) : null,
      endDate: endDate != null ? _formatDate(endDate) : null,
    );
    return models.map((m) => m.toEntity()).toList();
  }

  @override
  Future<List<DateTime>> getHolidays(int year) async {
    final dates = await _dataSource.getHolidays(year);
    return dates.map((d) => DateTime.parse(d)).toList();
  }

  String _formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
