import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/suggestion_register_request.dart';
import 'package:mobile/domain/entities/suggestion_form.dart';

void main() {
  group('SuggestionRegisterRequest.toJson', () {
    test('신제품 — 필수 3 필드만 직렬화', () {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.newProduct.code,
        title: '신제품 제안',
        content: '저당 라면 시리즈',
      );

      final json = request.toJson();

      expect(json, {
        'category': 'NEW_PRODUCT',
        'title': '신제품 제안',
        'content': '저당 라면 시리즈',
      });
    });

    test('기존제품 — productCode 포함', () {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.existingProduct.code,
        title: '품질 개선',
        content: '포장 변경 제안',
        productCode: '12345678',
      );

      final json = request.toJson();

      expect(json['productCode'], '12345678');
    });

    test('물류 클레임 — 7 필드 모두 직렬화 + claimDate ISO 포맷', () {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.logisticsClaim.code,
        title: '물류 클레임 제안',
        content: '오배송 발생',
        accountId: 100,
        sapAccountCode: 'SAP-0001',
        claimType: '파손',
        claimDate: DateTime(2026, 5, 22),
        carNumber: '12가1234',
        logisticsResponsibility: '운송사',
        duplicateProposalNum: 'PROP-001',
      );

      final json = request.toJson();

      expect(json['accountId'], 100);
      expect(json['sapAccountCode'], 'SAP-0001');
      expect(json['claimType'], '파손');
      expect(json['claimDate'], '2026-05-22');
      expect(json['carNumber'], '12가1234');
      expect(json['logisticsResponsibility'], '운송사');
      expect(json['duplicateProposalNum'], 'PROP-001');
    });

    test('빈 문자열 필드는 키 자체를 제외한다', () {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.logisticsClaim.code,
        title: '제목',
        content: '내용',
        productCode: '',
        sapAccountCode: '',
        claimType: '',
        carNumber: '',
        logisticsResponsibility: '',
        duplicateProposalNum: '',
      );

      final json = request.toJson();

      expect(json.containsKey('productCode'), false);
      expect(json.containsKey('sapAccountCode'), false);
      expect(json.containsKey('claimType'), false);
      expect(json.containsKey('carNumber'), false);
      expect(json.containsKey('logisticsResponsibility'), false);
      expect(json.containsKey('duplicateProposalNum'), false);
    });

    test('claimDate 한자리 월/일 zero-padded', () {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.logisticsClaim.code,
        title: 't',
        content: 'c',
        claimDate: DateTime(2026, 1, 3),
      );

      final json = request.toJson();

      expect(json['claimDate'], '2026-01-03');
    });
  });

  group('SuggestionRegisterRequest.toFormData', () {
    test('request part 가 application/json + JSON body 로 구성된다', () async {
      final request = SuggestionRegisterRequest(
        category: SuggestionCategory.newProduct.code,
        title: '신제품 제안',
        content: '저당 라면',
      );

      final formData = await request.toFormData();

      // `request` part 1개, photos 0개
      expect(formData.files.length, 1);
      final entry = formData.files.first;
      expect(entry.key, 'request');
      expect(entry.value.contentType?.mimeType, 'application/json');

      // body 검증
      final bodyBytes = <int>[];
      await for (final chunk in entry.value.finalize()) {
        bodyBytes.addAll(chunk);
      }
      final body = utf8.decode(bodyBytes);
      final decoded = jsonDecode(body) as Map<String, dynamic>;
      expect(decoded['category'], 'NEW_PRODUCT');
      expect(decoded['title'], '신제품 제안');
      expect(decoded['content'], '저당 라면');
    });

    test('fromEntity 변환 — Form 의 7 필드가 그대로 매핑된다', () {
      final form = SuggestionRegisterForm(
        category: SuggestionCategory.logisticsClaim,
        title: '제목',
        content: '내용',
        accountId: 100,
        accountName: '오뚜기 농협',
        sapAccountCode: 'SAP-0001',
        claimType: '파손',
        claimDate: DateTime(2026, 5, 22),
        carNumber: '12가1234',
        logisticsResponsibility: '운송사',
        duplicateProposalNum: 'PROP-001',
      );

      final request = SuggestionRegisterRequest.fromEntity(form);

      expect(request.category, 'LOGISTICS_CLAIM');
      expect(request.accountId, 100);
      expect(request.sapAccountCode, 'SAP-0001');
      expect(request.claimType, '파손');
      expect(request.claimDate, DateTime(2026, 5, 22));
      expect(request.carNumber, '12가1234');
      expect(request.logisticsResponsibility, '운송사');
      expect(request.duplicateProposalNum, 'PROP-001');
    });
  });
}

