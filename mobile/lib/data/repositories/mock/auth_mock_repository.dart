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
      'department': '영업1팀',
      'branchName': '부산1지점',
      'role': 'USER',
      'requiresPasswordChange': false,
      'requiresGpsConsent': false,
    },
    '20020001': {
      'id': 2,
      'password': 'otg1',
      'name': '김영업',
      'department': '영업2팀',
      'branchName': '서울2지점',
      'role': 'USER',
      'requiresPasswordChange': true,
      'requiresGpsConsent': true,
    },
    '20030117': {
      'id': 3,
      'password': 'test1234',
      'name': '김조장',
      'department': '영업1팀',
      'branchName': '부산1지점',
      'role': 'LEADER',
      'requiresPasswordChange': false,
      'requiresGpsConsent': false,
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

    // 현재 로그인된 사번 저장
    _currentEmployeeId = employeeId;

    // User 엔티티 생성
    final user = User(
      id: account['id'] as int,
      employeeId: employeeId,
      name: account['name'] as String,
      department: account['department'] as String,
      branchName: account['branchName'] as String,
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
  Future<void> recordGpsConsent() async {
    await _simulateDelay();

    // Mock에서는 GPS 동의 상태 변경
    if (_currentEmployeeId != null) {
      final account = _mockAccounts[_currentEmployeeId];
      if (account != null) {
        account['requiresGpsConsent'] = false;
      }
    }
  }
}
