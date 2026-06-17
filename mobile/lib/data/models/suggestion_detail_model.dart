import '../../domain/entities/suggestion_detail.dart';
import '../../domain/entities/suggestion_form.dart';

/// 제안 첨부 Model
class SuggestionAttachmentModel {
  final int id;
  final String s3Url;
  final String? fileName;
  final int sortOrder;

  const SuggestionAttachmentModel({
    required this.id,
    required this.s3Url,
    this.fileName,
    required this.sortOrder,
  });

  factory SuggestionAttachmentModel.fromJson(Map<String, dynamic> json) {
    return SuggestionAttachmentModel(
      id: (json['id'] as num).toInt(),
      s3Url: json['s3Url'] as String? ?? '',
      fileName: json['fileName'] as String?,
      sortOrder: (json['sortOrder'] as num?)?.toInt() ?? 0,
    );
  }

  SuggestionAttachment toEntity() {
    return SuggestionAttachment(
      id: id,
      s3Url: s3Url,
      fileName: fileName,
      sortOrder: sortOrder,
    );
  }
}

/// 제안/물류클레임 상세 Model
///
/// 백엔드 `SuggestionResponse` JSON 대응. 모든 물류 필드는 nullable.
class SuggestionDetailModel {
  final int id;
  final String proposalNumber;
  final String category;
  final String categoryName;
  final String title;
  final String content;
  final String? productCode;
  final String? sapAccountCode;
  final String? claimType;
  final String? claimTypeMeasures;
  final String? claimDate;
  final String? carNumber;
  final String? logisticsResponsibility;
  final String? receptionLogisticsCenter;
  final String? responsibleLogisticsCenter;
  final String? actionStatus;
  final String? actionNum;
  final String? actionManager;
  final String? actionContent;
  final String? duplicateProposalNum;
  final String status;
  final String createdAt;
  final List<SuggestionAttachmentModel> attachments;

  const SuggestionDetailModel({
    required this.id,
    required this.proposalNumber,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    this.productCode,
    this.sapAccountCode,
    this.claimType,
    this.claimTypeMeasures,
    this.claimDate,
    this.carNumber,
    this.logisticsResponsibility,
    this.receptionLogisticsCenter,
    this.responsibleLogisticsCenter,
    this.actionStatus,
    this.actionNum,
    this.actionManager,
    this.actionContent,
    this.duplicateProposalNum,
    required this.status,
    required this.createdAt,
    this.attachments = const [],
  });

  factory SuggestionDetailModel.fromJson(Map<String, dynamic> json) {
    return SuggestionDetailModel(
      id: (json['id'] as num).toInt(),
      proposalNumber: json['proposalNumber'] as String? ?? '',
      category: json['category'] as String? ?? 'NEW_PRODUCT',
      categoryName: json['categoryName'] as String? ?? '',
      title: json['title'] as String? ?? '',
      content: json['content'] as String? ?? '',
      productCode: json['productCode'] as String?,
      sapAccountCode: json['sapAccountCode'] as String?,
      claimType: json['claimType'] as String?,
      claimTypeMeasures: json['claimTypeMeasures'] as String?,
      claimDate: json['claimDate'] as String?,
      carNumber: json['carNumber'] as String?,
      logisticsResponsibility: json['logisticsResponsibility'] as String?,
      receptionLogisticsCenter: json['receptionLogisticsCenter'] as String?,
      responsibleLogisticsCenter: json['responsibleLogisticsCenter'] as String?,
      actionStatus: json['actionStatus'] as String?,
      actionNum: json['actionNum'] as String?,
      actionManager: json['actionManager'] as String?,
      actionContent: json['actionContent'] as String?,
      duplicateProposalNum: json['duplicateProposalNum'] as String?,
      status: json['status'] as String? ?? 'SUBMITTED',
      createdAt: json['createdAt'] as String? ?? '',
      attachments: (json['attachments'] as List<dynamic>? ?? const [])
          .map((e) =>
              SuggestionAttachmentModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  SuggestionDetail toEntity() {
    return SuggestionDetail(
      id: id,
      proposalNumber: proposalNumber,
      category: SuggestionCategory.fromCode(category),
      categoryName: categoryName,
      title: title,
      content: content,
      productCode: productCode,
      sapAccountCode: sapAccountCode,
      createdAt: DateTime.tryParse(createdAt) ??
          DateTime.fromMillisecondsSinceEpoch(0),
      claimType: claimType,
      claimTypeMeasures: claimTypeMeasures,
      claimDate: claimDate != null ? DateTime.tryParse(claimDate!) : null,
      carNumber: carNumber,
      logisticsResponsibility: logisticsResponsibility,
      receptionLogisticsCenter: receptionLogisticsCenter,
      responsibleLogisticsCenter: responsibleLogisticsCenter,
      actionStatus: actionStatus,
      actionNum: actionNum,
      actionManager: actionManager,
      actionContent: actionContent,
      duplicateProposalNum: duplicateProposalNum,
      status: status,
      attachments: attachments.map((m) => m.toEntity()).toList(),
    );
  }
}
