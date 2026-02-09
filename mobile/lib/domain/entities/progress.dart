import 'package:flutter/material.dart';

/// 진도율 상태 Enum
/// 목표 대비 실적 진행 상태를 나타냅니다.
enum ProgressStatus {
  /// 목표 초과 (100% 이상)
  exceeded('초과', Colors.green),

  /// 목표 부족 (100% 미만)
  insufficient('부족', Colors.red),

  /// 목표 달성 (정확히 100%)
  achieved('달성', Colors.blue);

  const ProgressStatus(this.displayName, this.color);

  /// 화면 표시용 이름
  final String displayName;

  /// 상태별 컬러
  final Color color;
}

/// 진도율 엔티티
///
/// 목표 대비 실적의 진도율을 계산하고 관리합니다.
/// 초과/부족 여부를 판정하고 색상 코드를 제공합니다.
class Progress {
  /// 목표금액 (원)
  final int targetAmount;

  /// 실적금액 (원)
  final int actualAmount;

  /// 진도율 퍼센트 (%)
  final double percentage;

  /// 차액 (실적 - 목표)
  final int difference;

  /// 진도율 상태 (초과/부족/달성)
  final ProgressStatus status;

  const Progress({
    required this.targetAmount,
    required this.actualAmount,
    required this.percentage,
    required this.difference,
    required this.status,
  });

  /// 목표와 실적으로 진도율 계산
  ///
  /// [targetAmount]: 목표금액
  /// [actualAmount]: 실적금액
  ///
  /// Returns: 계산된 진도율 엔티티
  factory Progress.calculate({
    required int targetAmount,
    required int actualAmount,
  }) {
    // 목표가 0인 경우 진도율을 0으로 처리
    final percentage = targetAmount == 0
        ? 0.0
        : (actualAmount / targetAmount) * 100;

    final difference = actualAmount - targetAmount;

    // 진도율 상태 판정
    final ProgressStatus status;
    if (percentage > 100) {
      status = ProgressStatus.exceeded;
    } else if (percentage < 100) {
      status = ProgressStatus.insufficient;
    } else {
      status = ProgressStatus.achieved;
    }

    return Progress(
      targetAmount: targetAmount,
      actualAmount: actualAmount,
      percentage: percentage,
      difference: difference,
      status: status,
    );
  }

  /// 목표 초과 여부
  bool get isExceeded => status == ProgressStatus.exceeded;

  /// 목표 부족 여부
  bool get isInsufficient => status == ProgressStatus.insufficient;

  /// 목표 달성 여부
  bool get isAchieved => status == ProgressStatus.achieved;

  /// 진도율 색상
  Color get color => status.color;

  /// 진도율 상태명
  String get statusDisplayName => status.displayName;

  /// 퍼센트를 소수점 1자리로 포맷
  String get formattedPercentage => percentage.toStringAsFixed(1);

  /// 불변성을 유지하며 일부 필드를 변경한 새 인스턴스 생성
  Progress copyWith({
    int? targetAmount,
    int? actualAmount,
    double? percentage,
    int? difference,
    ProgressStatus? status,
  }) {
    return Progress(
      targetAmount: targetAmount ?? this.targetAmount,
      actualAmount: actualAmount ?? this.actualAmount,
      percentage: percentage ?? this.percentage,
      difference: difference ?? this.difference,
      status: status ?? this.status,
    );
  }

  /// JSON으로 변환
  Map<String, dynamic> toJson() {
    return {
      'targetAmount': targetAmount,
      'actualAmount': actualAmount,
      'percentage': percentage,
      'difference': difference,
      'status': status.name,
    };
  }

  /// JSON에서 엔티티 생성
  factory Progress.fromJson(Map<String, dynamic> json) {
    return Progress(
      targetAmount: json['targetAmount'] as int,
      actualAmount: json['actualAmount'] as int,
      percentage: (json['percentage'] as num).toDouble(),
      difference: json['difference'] as int,
      status: ProgressStatus.values.firstWhere(
        (s) => s.name == json['status'],
        orElse: () => ProgressStatus.insufficient,
      ),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is Progress &&
        other.targetAmount == targetAmount &&
        other.actualAmount == actualAmount &&
        other.percentage == percentage &&
        other.difference == difference &&
        other.status == status;
  }

  @override
  int get hashCode {
    return Object.hash(
      targetAmount,
      actualAmount,
      percentage,
      difference,
      status,
    );
  }

  @override
  String toString() {
    return 'Progress(targetAmount: $targetAmount, actualAmount: $actualAmount, '
        'percentage: $percentage%, difference: $difference, '
        'status: ${status.displayName})';
  }
}
