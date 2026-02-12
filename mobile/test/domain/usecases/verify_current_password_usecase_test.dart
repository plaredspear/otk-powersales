import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/repositories/password_repository.dart';
import 'package:mobile/domain/usecases/verify_current_password_usecase.dart';

// Mock PasswordRepository
class MockPasswordRepository implements PasswordRepository {
  bool shouldReturnValid = true;
  String? lastCurrentPassword;

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    lastCurrentPassword = currentPassword;
    return shouldReturnValid;
  }

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    // Not used in this test
  }
}

void main() {
  late VerifyCurrentPasswordUseCase useCase;
  late MockPasswordRepository mockRepository;

  setUp(() {
    mockRepository = MockPasswordRepository();
    useCase = VerifyCurrentPasswordUseCase(mockRepository);
  });

  group('VerifyCurrentPasswordUseCase', () {
    test('현재 비밀번호가 일치하면 true를 반환한다', () async {
      mockRepository.shouldReturnValid = true;

      final result = await useCase('1234');

      expect(result, true);
      expect(mockRepository.lastCurrentPassword, '1234');
    });

    test('현재 비밀번호가 불일치하면 false를 반환한다', () async {
      mockRepository.shouldReturnValid = false;

      final result = await useCase('wrong');

      expect(result, false);
      expect(mockRepository.lastCurrentPassword, 'wrong');
    });

    test('빈 비밀번호는 ArgumentError를 던진다', () async {
      expect(
        () => useCase(''),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('ArgumentError에 적절한 메시지가 포함된다', () async {
      try {
        await useCase('');
        fail('ArgumentError가 발생해야 합니다');
      } catch (e) {
        expect(e, isA<ArgumentError>());
        expect((e as ArgumentError).message, contains('비밀번호를 입력해주세요'));
      }
    });
  });
}
