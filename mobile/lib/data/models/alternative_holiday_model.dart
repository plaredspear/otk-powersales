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
      actualWorkDate: json['actualWorkDate'] as String,
      targetAltHolidayDate: json['targetAltHolidayDate'] as String,
      confirmAltHolidayDate: json['confirmAltHolidayDate'] as String?,
      status: json['status'] as String,
      changeReason: json['changeReason'] as String?,
      createdAt: json['createdAt'] as String,
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
