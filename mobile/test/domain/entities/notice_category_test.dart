import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice_category.dart';

void main() {
  group('NoticeCategory', () {
    test('displayName이 올바르게 반환된다', () {
      // Given & When & Then
      expect(NoticeCategory.company.displayName, '회사공지');
      expect(NoticeCategory.branch.displayName, '지점공지');
    });

    test('code가 올바르게 반환된다', () {
      // Given & When & Then
      expect(NoticeCategory.company.code, 'COMPANY');
      expect(NoticeCategory.branch.code, 'BRANCH');
    });

    test('fromCode로 정확한 enum 값을 생성한다', () {
      // Given & When & Then
      expect(NoticeCategory.fromCode('COMPANY'), NoticeCategory.company);
      expect(NoticeCategory.fromCode('BRANCH'), NoticeCategory.branch);
      expect(NoticeCategory.fromCode('company'), NoticeCategory.company); // 대소문자 무관
      expect(NoticeCategory.fromCode('branch'), NoticeCategory.branch);
    });

    test('fromCode에 잘못된 코드를 전달하면 ArgumentError가 발생한다', () {
      // When & Then
      expect(
        () => NoticeCategory.fromCode('INVALID'),
        throwsA(isA<ArgumentError>()),
      );
    });
  });
}
