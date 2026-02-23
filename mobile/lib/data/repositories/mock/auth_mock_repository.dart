import '../../../domain/entities/user.dart';
import '../../../domain/entities/auth_token.dart';
import '../../../domain/repositories/auth_repository.dart';

/// Mock 인증 Repository
///
/// Backend API 없이 로컬에서 테스트 가능하도록 하드코딩된 계정으로 동작합니다.
/// 실제 API 연동 시 AuthRepositoryImpl로 교체됩니다.
class AuthMockRepository implements AuthRepository {
  /// Mock 계정 데이터
  ///
  /// 각 계정은 사번을 키로, 사용자 정보를 값으로 가집니다.
  static final Map<String, Map<String, dynamic>> _mockAccounts = {
    '20010585': {
      'id': 1,
      'password': 'test1234',
      'name': '홍길동',
      'orgName': '부산1지점',
      'role': 'USER',
      'requiresPasswordChange': false,
      'requiresGpsConsent': false,
    },
    '20020001': {
      'id': 2,
      'password': 'otg1',
      'name': '김영업',
      'orgName': '서울2지점',
      'role': 'USER',
      'requiresPasswordChange': true,
      'requiresGpsConsent': true,
    },
    '20030117': {
      'id': 3,
      'password': 'test1234',
      'name': '김조장',
      'orgName': '부산1지점',
      'role': 'LEADER',
      'requiresPasswordChange': false,
      'requiresGpsConsent': false,
    },
    // DEVICE_MISMATCH 테스트용 계정 (다른 단말기에서 바인딩됨)
    '20040001': {
      'id': 4,
      'password': 'test1234',
      'name': '박테스트',
      'orgName': '서울1지점',
      'role': 'USER',
      'requiresPasswordChange': false,
      'requiresGpsConsent': false,
      'deviceBound': true,
    },
  };

  /// Mock 토큰 상수
  static const String _mockAccessToken = 'mock_access_token_1234567890';
  static const String _mockRefreshToken = 'mock_refresh_token_0987654321';

  /// 현재 로그인된 사번 (비밀번호 변경 시 사용)
  String? _currentEmployeeId;

  /// 네트워크 지연 시뮬레이션 (500ms)
  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    await _simulateDelay();

    // 계정 조회
    final account = _mockAccounts[employeeId];
    if (account == null) {
      throw Exception('사번 또는 비밀번호가 올바르지 않습니다');
    }

    // 비밀번호 검증
    if (account['password'] != password) {
      throw Exception('사번 또는 비밀번호가 올바르지 않습니다');
    }

    // 단말기 바인딩 검증 (DEVICE_MISMATCH 시뮬레이션)
    if (account['deviceBound'] == true) {
      throw Exception('등록된 단말기와 다른 기기입니다. 관리자에게 문의하세요');
    }

    // 현재 로그인된 사번 저장
    _currentEmployeeId = employeeId;

    // User 엔티티 생성
    final user = User(
      id: account['id'] as int,
      employeeId: employeeId,
      name: account['name'] as String,
      orgName: account['orgName'] as String?,
      role: account['role'] as String,
    );

    // AuthToken 생성
    const token = AuthToken(
      accessToken: _mockAccessToken,
      refreshToken: _mockRefreshToken,
      expiresIn: 3600,
    );

    return LoginResult(
      user: user,
      token: token,
      requiresPasswordChange: account['requiresPasswordChange'] as bool,
      requiresGpsConsent: account['requiresGpsConsent'] as bool,
    );
  }

  @override
  Future<AuthToken> refreshToken(String refreshToken) async {
    await _simulateDelay();

    // Mock refresh token 검증
    if (!refreshToken.startsWith('mock_')) {
      throw Exception('세션이 만료되었습니다. 다시 로그인해주세요');
    }

    // 새로운 Access Token 발급 (Refresh Token은 재사용)
    return AuthToken(
      accessToken:
          '${_mockAccessToken}_refreshed_${DateTime.now().millisecondsSinceEpoch}',
      refreshToken: refreshToken,
      expiresIn: 3600,
    );
  }

  @override
  Future<void> changePassword(
      String currentPassword, String newPassword) async {
    await _simulateDelay();

    // 현재 로그인된 사용자의 비밀번호 검증
    if (_currentEmployeeId == null) {
      throw Exception('인증이 필요합니다');
    }

    final account = _mockAccounts[_currentEmployeeId];
    if (account == null || account['password'] != currentPassword) {
      throw Exception('현재 비밀번호가 올바르지 않습니다');
    }

    // Mock에서는 비밀번호를 실제로 변경 (테스트 편의성)
    account['password'] = newPassword;
    account['requiresPasswordChange'] = false;
  }

  @override
  Future<void> logout() async {
    await _simulateDelay();
    _currentEmployeeId = null;
  }

  @override
  Future<GpsConsentTerms> getGpsConsentTerms() async {
    await _simulateDelay();
    return const GpsConsentTerms(
      agreementNumber: 'AGR-MOCK-001',
      contents: '개인정보, 위치정보의 수집 및 이용에 대한 동의서\n\n'
          '회사는 영업활동 관리 목적으로 GPS 위치정보를 수집합니다.\n'
          '수집된 위치정보는 근태관리 및 영업활동 확인에 활용됩니다.\n\n'
          '동의를 거부할 수 있으나, 거부 시 앱 사용이 제한됩니다.',
    );
  }

  @override
  Future<GpsConsentStatus> getGpsConsentStatus() async {
    await _simulateDelay();
    if (_currentEmployeeId != null) {
      final account = _mockAccounts[_currentEmployeeId];
      if (account != null) {
        return GpsConsentStatus(
          requiresGpsConsent: account['requiresGpsConsent'] as bool,
        );
      }
    }
    return const GpsConsentStatus(requiresGpsConsent: false);
  }

  @override
  Future<GpsConsentRecordResult> recordGpsConsent({String? agreementNumber}) async {
    await _simulateDelay();

    // Mock에서는 GPS 동의 상태 변경
    if (_currentEmployeeId != null) {
      final account = _mockAccounts[_currentEmployeeId];
      if (account != null) {
        account['requiresGpsConsent'] = false;
      }
    }

    return GpsConsentRecordResult(
      accessToken:
          '${_mockAccessToken}_consented_${DateTime.now().millisecondsSinceEpoch}',
      expiresIn: 86400,
    );
  }
}
