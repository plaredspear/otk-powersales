/// SAP 거래처 여신 한도 조회 응답 모델 (Spec #594).
///
/// API: `GET /api/v1/mobile/clients/{external_key}/loan-inquiry`
class LoanInquiryResponseModel {
  /// 거래처 SAP 코드 (Account.externalKey)
  final String externalKey;

  /// SAP `TotalCredit` — 본 스펙 화면 표시 안 함
  final int totalCredit;

  /// SAP `CreditBalance` — `CreditBalanceDisplay` 표시 (P2-M)
  final int creditBalance;

  /// SAP `CreditCurrency` — 화면 표시 안 함
  final String currency;

  /// 응답 생성 시각 (ISO 8601 with offset)
  final String dataAsOf;

  const LoanInquiryResponseModel({
    required this.externalKey,
    required this.totalCredit,
    required this.creditBalance,
    required this.currency,
    required this.dataAsOf,
  });

  factory LoanInquiryResponseModel.fromJson(Map<String, dynamic> json) {
    return LoanInquiryResponseModel(
      externalKey: json['externalKey'] as String,
      totalCredit: (json['totalCredit'] as num).toInt(),
      creditBalance: (json['creditBalance'] as num).toInt(),
      currency: json['currency'] as String,
      dataAsOf: json['dataAsOf'] as String,
    );
  }
}
