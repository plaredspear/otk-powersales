import '../../domain/entities/staff_evaluation.dart';

/// 거래처별 평가 행 모델 (DTO)
class AccountEvaluationModel {
  final String accountCode;
  final String accountName;
  final String? accountType;
  final int targetAmount;
  final int performanceAmount;
  final double attainmentRate;

  const AccountEvaluationModel({
    required this.accountCode,
    required this.accountName,
    required this.accountType,
    required this.targetAmount,
    required this.performanceAmount,
    required this.attainmentRate,
  });

  factory AccountEvaluationModel.fromJson(Map<String, dynamic> json) {
    return AccountEvaluationModel(
      accountCode: json['accountCode'] as String? ?? '',
      accountName: json['accountName'] as String? ?? '',
      accountType: json['accountType'] as String?,
      targetAmount: (json['targetAmount'] as num?)?.toInt() ?? 0,
      performanceAmount: (json['performanceAmount'] as num?)?.toInt() ?? 0,
      attainmentRate: (json['attainmentRate'] as num?)?.toDouble() ?? 0.0,
    );
  }

  AccountEvaluation toEntity() => AccountEvaluation(
        accountCode: accountCode,
        accountName: accountName,
        accountType: accountType,
        targetAmount: targetAmount,
        performanceAmount: performanceAmount,
        attainmentRate: attainmentRate,
      );
}

/// 여사원 평가조회 모델 (DTO)
///
/// `GET /api/v1/mobile/staff-evaluation` 응답(StaffEvaluationResponse) 매핑.
class StaffEvaluationModel {
  final String yearMonth;
  final double? branchScore;
  final int branchMaxScore;
  final List<AccountEvaluationModel> accounts;

  const StaffEvaluationModel({
    required this.yearMonth,
    required this.branchScore,
    required this.branchMaxScore,
    required this.accounts,
  });

  factory StaffEvaluationModel.fromJson(Map<String, dynamic> json) {
    return StaffEvaluationModel(
      yearMonth: json['yearMonth'] as String,
      branchScore: (json['branchScore'] as num?)?.toDouble(),
      branchMaxScore: (json['branchMaxScore'] as num?)?.toInt() ?? 30,
      accounts: (json['accounts'] as List<dynamic>? ?? [])
          .map((e) =>
              AccountEvaluationModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  StaffEvaluation toEntity() => StaffEvaluation(
        yearMonth: yearMonth,
        branchScore: branchScore,
        branchMaxScore: branchMaxScore,
        accounts: accounts.map((e) => e.toEntity()).toList(),
      );
}
