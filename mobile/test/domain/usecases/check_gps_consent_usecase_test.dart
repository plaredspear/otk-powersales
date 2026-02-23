import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/auth_token.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/auth_repository.dart';
import 'package:mobile/domain/usecases/check_gps_consent_usecase.dart';

void main() {
  late FakeAuthRepository repository;
  late CheckGpsConsentUseCase useCase;

  setUp(() {
    repository = FakeAuthRepository();
    useCase = CheckGpsConsentUseCase(repository);
  });

  group('CheckGpsConsentUseCase', () {
    test('재동의 필요 시 true 반환', () async {
      repository.gpsConsentStatusToReturn =
          const GpsConsentStatus(requiresGpsConsent: true);

      final result = await useCase.call();

      expect(result, isTrue);
    });

    test('재동의 불필요 시 false 반환', () async {
      repository.gpsConsentStatusToReturn =
          const GpsConsentStatus(requiresGpsConsent: false);

      final result = await useCase.call();

      expect(result, isFalse);
    });

    test('1시간 이내 재확인 스킵 (false 반환)', () async {
      repository.gpsConsentStatusToReturn =
          const GpsConsentStatus(requiresGpsConsent: true);

      // 첫 번째 호출: API 호출 수행
      final firstResult = await useCase.call();
      expect(firstResult, isTrue);
      expect(repository.getGpsConsentStatusCallCount, 1);

      // 두 번째 호출: 1시간 이내이므로 스킵
      final secondResult = await useCase.call();
      expect(secondResult, isFalse);
      expect(repository.getGpsConsentStatusCallCount, 1); // API 재호출 없음
    });

    test('1시간 초과 후 다시 확인', () async {
      repository.gpsConsentStatusToReturn =
          const GpsConsentStatus(requiresGpsConsent: true);

      // 첫 번째 호출
      await useCase.call();
      expect(repository.getGpsConsentStatusCallCount, 1);

      // resetLastCheckTime으로 throttle 초기화 (1시간 경과 시뮬레이션)
      useCase.resetLastCheckTime();

      // 두 번째 호출: throttle 초기화됨
      final result = await useCase.call();
      expect(result, isTrue);
      expect(repository.getGpsConsentStatusCallCount, 2);
    });

    test('API 호출 실패 시 false 반환', () async {
      repository.exceptionToThrow = Exception('네트워크 오류');

      final result = await useCase.call();

      expect(result, isFalse);
    });

    test('resetLastCheckTime 후 즉시 재확인 가능', () async {
      repository.gpsConsentStatusToReturn =
          const GpsConsentStatus(requiresGpsConsent: true);

      // 첫 번째 호출
      await useCase.call();
      expect(repository.getGpsConsentStatusCallCount, 1);

      // throttle 내 호출 (스킵됨)
      await useCase.call();
      expect(repository.getGpsConsentStatusCallCount, 1);

      // 리셋 후 즉시 재확인
      useCase.resetLastCheckTime();
      final result = await useCase.call();
      expect(result, isTrue);
      expect(repository.getGpsConsentStatusCallCount, 2);
    });
  });
}

// --- Fakes ---

class FakeAuthRepository implements AuthRepository {
  /// 반환할 GPS 동의 상태
  GpsConsentStatus gpsConsentStatusToReturn =
      const GpsConsentStatus(requiresGpsConsent: false);

  /// 던질 예외 (null이면 정상 반환)
  Exception? exceptionToThrow;

  /// getGpsConsentStatus 호출 횟수
  int getGpsConsentStatusCallCount = 0;

  @override
  Future<GpsConsentStatus> getGpsConsentStatus() async {
    getGpsConsentStatusCallCount++;
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return gpsConsentStatusToReturn;
  }

  // --- 미사용 메서드 (stub) ---

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    throw UnimplementedError();
  }

  @override
  Future<AuthToken> refreshToken(String refreshToken) async {
    throw UnimplementedError();
  }

  @override
  Future<void> changePassword(
      String currentPassword, String newPassword) async {
    throw UnimplementedError();
  }

  @override
  Future<void> logout() async {
    throw UnimplementedError();
  }

  @override
  Future<GpsConsentTerms> getGpsConsentTerms() async {
    throw UnimplementedError();
  }

  @override
  Future<GpsConsentRecordResult> recordGpsConsent(
      {String? agreementNumber}) async {
    throw UnimplementedError();
  }
}
