import 'suggestion_form.dart';

/// 제안 첨부 사진
class SuggestionAttachment {
  final int id;
  final String s3Url;
  final String? fileName;
  final int sortOrder;

  const SuggestionAttachment({
    required this.id,
    required this.s3Url,
    this.fileName,
    required this.sortOrder,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionAttachment &&
        other.id == id &&
        other.s3Url == s3Url &&
        other.fileName == fileName &&
        other.sortOrder == sortOrder;
  }

  @override
  int get hashCode => Object.hash(id, s3Url, fileName, sortOrder);
}

/// 제안/물류클레임 상세 엔티티
///
/// 백엔드 `SuggestionResponse` 대응. 물류클레임은 OLS 조치사항(조치상태/물류책임/
/// 물류센터/중복접수)을 추가로 표시한다.
class SuggestionDetail {
  final int id;
  final String proposalNumber;
  final SuggestionCategory category;
  final String categoryName;
  final String title;
  final String content;
  final String? productCode;
  final String? sapAccountCode;
  final DateTime createdAt;

  // 물류클레임 전용
  final String? claimType;
  final String? claimTypeMeasures;
  final DateTime? claimDate;
  final String? carNumber;
  final String? logisticsResponsibility;
  final String? receptionLogisticsCenter;
  final String? responsibleLogisticsCenter;

  /// 조치상태 enum 이름 (UNCONFIRMED/IN_PROGRESS/COMPLETED/DUPLICATE_RECEPTION) — nullable
  final String? actionStatus;

  /// OLS 조치사항 — 조치번호/조치 담당자/조치내용 (레거시 logisticsclaimview.jsp 45~66행)
  final String? actionNum;
  final String? actionManager;
  final String? actionContent;
  final String? duplicateProposalNum;

  /// 제안 상태 enum 이름 (SUBMITTED/IN_REVIEW/ACCEPTED/REJECTED)
  final String status;

  /// '오뚜기 접수사원' (등록사원명/사번) — 물류클레임 상세에서 조장에게만 노출, 그 외 null
  /// (레거시 logisticsclaimview 권한분기 동등)
  final String? receptionEmployeeName;
  final String? receptionEmployeeCode;

  final List<SuggestionAttachment> attachments;

  const SuggestionDetail({
    required this.id,
    required this.proposalNumber,
    required this.category,
    required this.categoryName,
    required this.title,
    required this.content,
    this.productCode,
    this.sapAccountCode,
    required this.createdAt,
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
    this.receptionEmployeeName,
    this.receptionEmployeeCode,
    this.attachments = const [],
  });

  bool get isLogisticsClaim => category == SuggestionCategory.logisticsClaim;

  /// '오뚜기 접수사원' 정보(이름)가 존재하는지 — 조장 권한으로 조회 시에만 채워진다.
  bool get hasReceptionEmployee =>
      receptionEmployeeName != null && receptionEmployeeName!.isNotEmpty;

  bool get hasAttachments => attachments.isNotEmpty;

  /// 조치상태 한글 라벨 (백엔드 enum 은 name 으로 직렬화되므로 모바일에서 매핑)
  String? get actionStatusLabel => switch (actionStatus) {
        'UNCONFIRMED' => '미확인',
        'IN_PROGRESS' => '조치중',
        'COMPLETED' => '조치 완료',
        'DUPLICATE_RECEPTION' => '중복접수',
        _ => actionStatus,
      };

  /// 제안 상태 한글 라벨
  String get statusLabel => switch (status) {
        'SUBMITTED' => '접수',
        'IN_REVIEW' => '검토중',
        'ACCEPTED' => '승인',
        'REJECTED' => '반려',
        _ => status,
      };

  /// OLS 조치사항(물류클레임 조치 결과)이 하나라도 존재하는지
  bool get hasActionInfo =>
      (actionStatus != null && actionStatus!.isNotEmpty) ||
      (actionNum != null && actionNum!.isNotEmpty) ||
      (actionManager != null && actionManager!.isNotEmpty) ||
      (actionContent != null && actionContent!.isNotEmpty) ||
      (logisticsResponsibility != null && logisticsResponsibility!.isNotEmpty) ||
      (claimTypeMeasures != null && claimTypeMeasures!.isNotEmpty) ||
      (responsibleLogisticsCenter != null &&
          responsibleLogisticsCenter!.isNotEmpty) ||
      (duplicateProposalNum != null && duplicateProposalNum!.isNotEmpty);

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is SuggestionDetail &&
        other.id == id &&
        other.proposalNumber == proposalNumber &&
        other.category == category &&
        other.categoryName == categoryName &&
        other.title == title &&
        other.content == content &&
        other.productCode == productCode &&
        other.sapAccountCode == sapAccountCode &&
        other.createdAt == createdAt &&
        other.claimType == claimType &&
        other.claimTypeMeasures == claimTypeMeasures &&
        other.claimDate == claimDate &&
        other.carNumber == carNumber &&
        other.logisticsResponsibility == logisticsResponsibility &&
        other.receptionLogisticsCenter == receptionLogisticsCenter &&
        other.responsibleLogisticsCenter == responsibleLogisticsCenter &&
        other.actionStatus == actionStatus &&
        other.actionNum == actionNum &&
        other.actionManager == actionManager &&
        other.actionContent == actionContent &&
        other.duplicateProposalNum == duplicateProposalNum &&
        other.status == status &&
        other.receptionEmployeeName == receptionEmployeeName &&
        other.receptionEmployeeCode == receptionEmployeeCode &&
        _listEquals(other.attachments, attachments);
  }

  @override
  int get hashCode => Object.hashAll([
        id,
        proposalNumber,
        category,
        categoryName,
        title,
        content,
        productCode,
        sapAccountCode,
        createdAt,
        claimType,
        claimTypeMeasures,
        claimDate,
        carNumber,
        logisticsResponsibility,
        receptionLogisticsCenter,
        responsibleLogisticsCenter,
        actionStatus,
        actionNum,
        actionManager,
        actionContent,
        duplicateProposalNum,
        status,
        receptionEmployeeName,
        receptionEmployeeCode,
        Object.hashAll(attachments),
      ]);
}

bool _listEquals<T>(List<T> a, List<T> b) {
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}
