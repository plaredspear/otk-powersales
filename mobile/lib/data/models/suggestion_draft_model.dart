import 'dart:io';

import '../../domain/entities/suggestion_draft.dart';

/// 제안하기 임시저장 Model (`GET /api/v1/mobile/suggestions/draft` 응답 data)
///
/// scalar 필드는 [fromJson] 으로 파싱하고, 사진은 presigned URL(`photoUrls`)만 담는다.
/// 데이터소스가 URL 을 임시 파일로 내려받아 [withPhotos] 로 [File] 을 채운 뒤 [toEntity] 한다.
class SuggestionDraftModel {
  const SuggestionDraftModel({
    this.category,
    this.title,
    this.content,
    this.productCode,
    this.productName,
    this.accountId,
    this.accountName,
    this.sapAccountCode,
    this.claimType,
    this.claimDate,
    this.carNumber,
    this.logisticsResponsibility,
    this.duplicateProposalNum,
    this.actionStatus,
    this.photoUrls = const [],
    this.photos = const [],
  });

  final String? category;
  final String? title;
  final String? content;
  final String? productCode;
  final String? productName;
  final int? accountId;
  final String? accountName;
  final String? sapAccountCode;
  final String? claimType;

  /// 클레임 발생일자 ("yyyy-MM-dd" 원문)
  final String? claimDate;
  final String? carNumber;
  final String? logisticsResponsibility;
  final String? duplicateProposalNum;
  final String? actionStatus;

  /// presigned URL (최대 2, 없으면 빈 배열)
  final List<String> photoUrls;

  /// 데이터소스가 URL 을 내려받아 채운 임시 파일
  final List<File> photos;

  factory SuggestionDraftModel.fromJson(Map<String, dynamic> json) {
    final urls = (json['photoUrls'] as List<dynamic>?)
            ?.map((e) => e as String)
            .toList() ??
        const <String>[];
    return SuggestionDraftModel(
      category: json['category'] as String?,
      title: json['title'] as String?,
      content: json['content'] as String?,
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      accountId: (json['accountId'] as num?)?.toInt(),
      accountName: json['accountName'] as String?,
      sapAccountCode: json['sapAccountCode'] as String?,
      claimType: json['claimType'] as String?,
      claimDate: json['claimDate'] as String?,
      carNumber: json['carNumber'] as String?,
      logisticsResponsibility: json['logisticsResponsibility'] as String?,
      duplicateProposalNum: json['duplicateProposalNum'] as String?,
      actionStatus: json['actionStatus'] as String?,
      photoUrls: urls,
    );
  }

  /// 내려받은 사진 파일을 채운 사본 반환
  SuggestionDraftModel withPhotos(List<File> photos) {
    return SuggestionDraftModel(
      category: category,
      title: title,
      content: content,
      productCode: productCode,
      productName: productName,
      accountId: accountId,
      accountName: accountName,
      sapAccountCode: sapAccountCode,
      claimType: claimType,
      claimDate: claimDate,
      carNumber: carNumber,
      logisticsResponsibility: logisticsResponsibility,
      duplicateProposalNum: duplicateProposalNum,
      actionStatus: actionStatus,
      photoUrls: photoUrls,
      photos: photos,
    );
  }

  SuggestionDraft toEntity() {
    return SuggestionDraft(
      category: category,
      title: title,
      content: content,
      productCode: productCode,
      productName: productName,
      accountId: accountId,
      accountName: accountName,
      sapAccountCode: sapAccountCode,
      claimType: claimType,
      claimDate: _parseDate(claimDate),
      carNumber: carNumber,
      logisticsResponsibility: logisticsResponsibility,
      duplicateProposalNum: duplicateProposalNum,
      actionStatus: actionStatus,
      photos: photos,
    );
  }

  static DateTime? _parseDate(String? raw) {
    if (raw == null || raw.isEmpty) return null;
    return DateTime.tryParse(raw);
  }
}
