/// 인증 토큰 엔티티
/// JWT 토큰 정보를 담는 도메인 엔티티
class AuthToken {
  /// Access Token (1시간 유효)
  final String accessToken;

  /// Refresh Token (7일 유효)
  final String refreshToken;

  /// Access Token 만료 시간 (초)
  final int expiresIn;

  const AuthToken({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
  });

  AuthToken copyWith({
    String? accessToken,
    String? refreshToken,
    int? expiresIn,
  }) {
    return AuthToken(
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
      expiresIn: expiresIn ?? this.expiresIn,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'expiresIn': expiresIn,
    };
  }

  factory AuthToken.fromJson(Map<String, dynamic> json) {
    return AuthToken(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
      expiresIn: json['expiresIn'] as int,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AuthToken &&
        other.accessToken == accessToken &&
        other.refreshToken == refreshToken &&
        other.expiresIn == expiresIn;
  }

  @override
  int get hashCode {
    return Object.hash(
      accessToken,
      refreshToken,
      expiresIn,
    );
  }

  @override
  String toString() {
    return 'AuthToken(accessToken: $accessToken, refreshToken: $refreshToken, expiresIn: $expiresIn)';
  }
}
