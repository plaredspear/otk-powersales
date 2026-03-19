import '../../domain/entities/alternative_holiday.dart';

class AlternativeHolidayModel {
  final int id;
  final String actualWorkDate;
  final String targetAltHolidayDate;
  final String? confirmAltHolidayDate;
  final String status;
  final String? changeReason;
  final String createdAt;

  const AlternativeHolidayModel({
    required this.id,
    required this.actualWorkDate,
    required this.targetAltHolidayDate,
    this.confirmAltHolidayDate,
    required this.status,
    this.changeReason,
    required this.createdAt,
  });

  factory AlternativeHolidayModel.fromJson(Map<String, dynamic> json) {
    return AlternativeHolidayModel(
      id: json['id'] as int,
      actualWorkDate: json['actual_work_date'] as String,
      targetAltHolidayDate: json['target_alt_holiday_date'] as String,
      confirmAltHolidayDate: json['confirm_alt_holiday_date'] as String?,
      status: json['status'] as String,
      changeReason: json['change_reason'] as String?,
      createdAt: json['created_at'] as String,
    );
  }

  AlternativeHoliday toEntity() {
    return AlternativeHoliday(
      id: id,
      actualWorkDate: DateTime.parse(actualWorkDate),
      targetAltHolidayDate: DateTime.parse(targetAltHolidayDate),
      confirmAltHolidayDate: confirmAltHolidayDate != null
          ? DateTime.parse(confirmAltHolidayDate!)
          : null,
      status: status,
      changeReason: changeReason,
      createdAt: DateTime.parse(createdAt),
    );
  }
}
