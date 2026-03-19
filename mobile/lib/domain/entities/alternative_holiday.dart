/// 대체휴무 신청 엔티티
class AlternativeHoliday {
  final int id;
  final DateTime actualWorkDate;
  final DateTime targetAltHolidayDate;
  final DateTime? confirmAltHolidayDate;
  final String status;
  final String? changeReason;
  final DateTime createdAt;

  const AlternativeHoliday({
    required this.id,
    required this.actualWorkDate,
    required this.targetAltHolidayDate,
    this.confirmAltHolidayDate,
    required this.status,
    this.changeReason,
    required this.createdAt,
  });
}
