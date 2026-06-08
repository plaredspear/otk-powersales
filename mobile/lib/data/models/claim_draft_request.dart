import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/claim_form.dart';

/// 클레임 임시저장 요청 Model (multipart/form-data, POST /api/v1/mobile/claims/draft)
///
/// 검증 없이 현재 폼 상태를 전송한다. 값이 없는(빈) 필드는 생략한다.
/// 사진은 경로가 있는 경우에만 첨부한다(서버는 전달된 사진만 갱신).
class ClaimDraftRequest {
  const ClaimDraftRequest({required this.form});

  final ClaimRegisterForm? form;

  factory ClaimDraftRequest.fromForm(ClaimRegisterForm? form) =>
      ClaimDraftRequest(form: form);

  Future<FormData> toFormData() async {
    final fields = <String, dynamic>{};
    final f = form;

    if (f != null) {
      if (f.accountId > 0) fields['accountId'] = f.accountId.toString();
      if (f.accountName.isNotEmpty) fields['accountName'] = f.accountName;
      if (f.productCode.isNotEmpty) fields['productCode'] = f.productCode;
      if (f.productName.isNotEmpty) fields['productName'] = f.productName;
      fields['dateType'] = f.dateType.toJson();
      fields['date'] = f.date.toIso8601String().substring(0, 10);
      if (f.categoryId.isNotEmpty) fields['claimType1'] = f.categoryId;
      if (f.subcategoryId.isNotEmpty) fields['claimType2'] = f.subcategoryId;
      if (f.defectDescription.isNotEmpty) {
        fields['defectDescription'] = f.defectDescription;
      }
      if (f.defectQuantity > 0) {
        fields['defectQuantity'] = f.defectQuantity.toString();
      }
      if (f.purchaseAmount != null && f.purchaseAmount! > 0) {
        fields['purchaseAmount'] = f.purchaseAmount.toString();
      }
      if (f.purchaseMethodCode != null && f.purchaseMethodCode!.isNotEmpty) {
        fields['purchaseMethodCode'] = f.purchaseMethodCode!;
      }
      if (f.requestTypeCode != null && f.requestTypeCode!.isNotEmpty) {
        fields['requestTypeCode'] = f.requestTypeCode!;
      }

      await _addPhoto(fields, 'defectPhoto', f.defectPhoto);
      await _addPhoto(fields, 'labelPhoto', f.labelPhoto);
      await _addPhoto(fields, 'receiptPhoto', f.receiptPhoto);
    }

    return FormData.fromMap(fields);
  }

  Future<void> _addPhoto(
    Map<String, dynamic> fields,
    String key,
    File? photo,
  ) async {
    if (photo == null || photo.path.isEmpty) return;
    fields[key] = await MultipartFile.fromFile(
      photo.path,
      filename: photo.path.split('/').last,
      contentType: MediaType('image', 'jpeg'),
    );
  }
}
