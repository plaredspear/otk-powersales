import 'claim_photo.dart';

/// 클레임 상세 도메인 엔티티
class ClaimDetail {
  final int claimId;
  // 제품정보
  final String? productName;
  final String? productCode;
  final DateTime? manufacturingDate;
  final String? logisticsCenter;
  final DateTime? expirationDate;
  final String? orderNumber;
  // 클레임정보
  final String? claimNo;
  final String? accountName;
  final String? accountCode;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final int? defectQuantity;
  final bool? sampleCollectionFlag;
  /// 상단 뱃지 문구 — 코스모스 조치상태(미회신 시 '미확인').
  /// 알라딘 DKRetail__Status__c(전송상태) 는 신규 시스템에서 전이되지 않아 표시 축으로 쓰지 않는다.
  final String actionStatusLabel;
  // sfSendStatus/sfSendStatusLabel : 신규→알라딘 전송상태. 알라딘 이관(마이그레이션) 건은 null.
  final String? sfSendStatus;
  final String? sfSendStatusLabel;
  final DateTime? customerDeliveryDate;
  final String? detailSnsName;
  final String? dateType;
  final String? dateTypeLabel;
  final DateTime? date;
  final String? purchaseMethodName;
  final int? purchaseAmount;
  final String? requestTypeName;
  final String? division;
  // 불만정보
  final String? defectDescription;
  // 채널정보
  final DateTime? interfaceDate;
  final String? channel;
  final String? channelLabel;
  final String? employeeName;
  final String? employeePhone;
  final String? jikwee;
  // 처리·조치정보
  final String? counselNumber;
  final String? actionCode;
  final String? actionStatus;
  final String? reasonType;
  final String? actContent;
  // 메타
  final DateTime createdAt;
  final List<ClaimPhoto> photos;

  const ClaimDetail({
    required this.claimId,
    this.productName,
    this.productCode,
    this.manufacturingDate,
    this.logisticsCenter,
    this.expirationDate,
    this.orderNumber,
    this.claimNo,
    this.accountName,
    this.accountCode,
    this.categoryLabel,
    this.subcategoryLabel,
    this.defectQuantity,
    this.sampleCollectionFlag,
    required this.actionStatusLabel,
    this.sfSendStatus,
    this.sfSendStatusLabel,
    this.customerDeliveryDate,
    this.detailSnsName,
    this.dateType,
    this.dateTypeLabel,
    this.date,
    this.purchaseMethodName,
    this.purchaseAmount,
    this.requestTypeName,
    this.division,
    this.defectDescription,
    this.interfaceDate,
    this.channel,
    this.channelLabel,
    this.employeeName,
    this.employeePhone,
    this.jikwee,
    this.counselNumber,
    this.actionCode,
    this.actionStatus,
    this.reasonType,
    this.actContent,
    required this.createdAt,
    required this.photos,
  });

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ClaimDetail && claimId == other.claimId;

  @override
  int get hashCode => claimId.hashCode;
}
