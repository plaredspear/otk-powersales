import '../../domain/entities/claim_detail.dart';
import 'claim_photo_model.dart';

/// 클레임 상세 데이터 모델 (JSON 매핑)
class ClaimDetailModel {
  final int claimId;
  // 제품정보
  final String? productName;
  final String? productCode;
  final String? manufacturingDate;
  final String? logisticsCenter;
  final String? expirationDate;
  final String? orderNumber;
  // 클레임정보
  final String? claimNo;
  final String? accountName;
  final String? accountCode;
  final String? categoryLabel;
  final String? subcategoryLabel;
  final num? defectQuantity;
  final bool? sampleCollectionFlag;
  // status/statusLabel : 알라딘 DKRetail__Status__c (알라딘→코스모스 전송상태) — 표시 전용.
  final String status;
  final String statusLabel;
  // sfSendStatus/sfSendStatusLabel : 신규→알라딘 전송상태. 알라딘 이관(마이그레이션) 건은 null.
  final String? sfSendStatus;
  final String? sfSendStatusLabel;
  final String? customerDeliveryDate;
  final String? detailSnsName;
  final String? dateType;
  final String? dateTypeLabel;
  final String? date;
  final String? purchaseMethodName;
  final num? purchaseAmount;
  final String? requestTypeName;
  final String? division;
  // 불만정보
  final String? defectDescription;
  // 채널정보
  final String? interfaceDate;
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
  final List<ClaimPhotoModel> photos;

  const ClaimDetailModel({
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
    required this.status,
    required this.statusLabel,
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

  factory ClaimDetailModel.fromJson(Map<String, dynamic> json) {
    return ClaimDetailModel(
      claimId: json['claimId'] as int,
      productName: json['productName'] as String?,
      productCode: json['productCode'] as String?,
      manufacturingDate: json['manufacturingDate'] as String?,
      logisticsCenter: json['logisticsCenter'] as String?,
      expirationDate: json['expirationDate'] as String?,
      orderNumber: json['orderNumber'] as String?,
      claimNo: json['claimNo'] as String?,
      accountName: json['accountName'] as String?,
      accountCode: json['accountCode'] as String?,
      categoryLabel: json['categoryLabel'] as String?,
      subcategoryLabel: json['subcategoryLabel'] as String?,
      defectQuantity: json['defectQuantity'] as num?,
      sampleCollectionFlag: json['sampleCollectionFlag'] as bool?,
      status: json['status'] as String,
      statusLabel: json['statusLabel'] as String,
      sfSendStatus: json['sfSendStatus'] as String?,
      sfSendStatusLabel: json['sfSendStatusLabel'] as String?,
      customerDeliveryDate: json['customerDeliveryDate'] as String?,
      detailSnsName: json['detailSnsName'] as String?,
      dateType: json['dateType'] as String?,
      dateTypeLabel: json['dateTypeLabel'] as String?,
      date: json['date'] as String?,
      purchaseMethodName: json['purchaseMethodName'] as String?,
      purchaseAmount: json['purchaseAmount'] as num?,
      requestTypeName: json['requestTypeName'] as String?,
      division: json['division'] as String?,
      defectDescription: json['defectDescription'] as String?,
      interfaceDate: json['interfaceDate'] as String?,
      channel: json['channel'] as String?,
      channelLabel: json['channelLabel'] as String?,
      employeeName: json['employeeName'] as String?,
      employeePhone: json['employeePhone'] as String?,
      jikwee: json['jikwee'] as String?,
      counselNumber: json['counselNumber'] as String?,
      actionCode: json['actionCode'] as String?,
      actionStatus: json['actionStatus'] as String?,
      reasonType: json['reasonType'] as String?,
      actContent: json['actContent'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      photos: (json['photos'] as List<dynamic>? ?? [])
          .map((e) => ClaimPhotoModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  DateTime? _parseDate(String? value) =>
      value != null ? DateTime.tryParse(value) : null;

  ClaimDetail toEntity() => ClaimDetail(
        claimId: claimId,
        productName: productName,
        productCode: productCode,
        manufacturingDate: _parseDate(manufacturingDate),
        logisticsCenter: logisticsCenter,
        expirationDate: _parseDate(expirationDate),
        orderNumber: orderNumber,
        claimNo: claimNo,
        accountName: accountName,
        accountCode: accountCode,
        categoryLabel: categoryLabel,
        subcategoryLabel: subcategoryLabel,
        defectQuantity: defectQuantity?.toInt(),
        sampleCollectionFlag: sampleCollectionFlag,
        status: status,
        statusLabel: statusLabel,
        sfSendStatus: sfSendStatus,
        sfSendStatusLabel: sfSendStatusLabel,
        customerDeliveryDate: _parseDate(customerDeliveryDate),
        detailSnsName: detailSnsName,
        dateType: dateType,
        dateTypeLabel: dateTypeLabel,
        date: _parseDate(date),
        purchaseMethodName: purchaseMethodName,
        purchaseAmount: purchaseAmount?.toInt(),
        requestTypeName: requestTypeName,
        division: division,
        defectDescription: defectDescription,
        interfaceDate: _parseDate(interfaceDate),
        channel: channel,
        channelLabel: channelLabel,
        employeeName: employeeName,
        employeePhone: employeePhone,
        jikwee: jikwee,
        counselNumber: counselNumber,
        actionCode: actionCode,
        actionStatus: actionStatus,
        reasonType: reasonType,
        actContent: actContent,
        createdAt: createdAt,
        photos: photos.map((e) => e.toEntity()).toList(),
      );
}
