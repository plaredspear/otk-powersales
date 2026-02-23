import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/auth_mock_repository.dart';

void main() {
  late AuthMockRepository repository;

  setUp(() {
    repository = AuthMockRepository();
  });

  group('getGpsConsentTerms', () {
    test('약관 내용 반환 확인', () async {
      final terms = await repository.getGpsConsentTerms();

      expect(terms.agreementNumber, isNotEmpty);
      expect(terms.contents, contains('위치정보'));
      expect(terms.contents, contains('동의'));
    });
  });

  group('getGpsConsentStatus', () {
    test('미동의 계정(20020001) 로그인 후 true 반환', () async {
      await repository.login(
          _employeeIdRequiresConsent, _passwordRequiresConsent);

      final status = await repository.getGpsConsentStatus();

      expect(status.requiresGpsConsent, isTrue);
    });

    test('동의 완료 계정(20010585) 로그인 후 false 반환', () async {
      await repository.login(
          _employeeIdConsentDone, _passwordConsentDone);

      final status = await repository.getGpsConsentStatus();

      expect(status.requiresGpsConsent, isFalse);
    });
  });

  group('recordGpsConsent', () {
    test('동의 기록 후 getGpsConsentStatus false로 변경 및 결과 반환', () async {
      // Note: _mockAccounts는 static이므로 상태 변경이 다른 테스트에 영향을 줌
      // before/after를 하나의 테스트로 검증
      await repository.login(
          _employeeIdRequiresConsent, _passwordRequiresConsent);

      // 동의 전: true
      final beforeStatus = await repository.getGpsConsentStatus();
      expect(beforeStatus.requiresGpsConsent, isTrue);

      // 동의 기록
      final result = await repository.recordGpsConsent(
          agreementNumber: 'AGR-MOCK-001');

      // 결과 검증
      expect(result.accessToken, isNotEmpty);
      expect(result.expiresIn, greaterThan(0));

      // 동의 후: false
      final afterStatus = await repository.getGpsConsentStatus();
      expect(afterStatus.requiresGpsConsent, isFalse);
    });
  });
}

// --- Test Data ---

const _employeeIdRequiresConsent = '20020001';
const _passwordRequiresConsent = 'otg1';

const _employeeIdConsentDone = '20010585';
const _passwordConsentDone = 'test1234';
