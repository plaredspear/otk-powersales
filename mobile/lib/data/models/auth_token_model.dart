import '../../domain/entities/auth_token.dart';

/// 인증 토큰 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 AuthToken 엔티티로 변환합니다.
class AuthTokenModel {
  final String accessToken;
  final String refreshToken;
  final int expiresIn;

  const AuthTokenModel({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
  });

  /// snake_case JSON에서 파싱
  factory AuthTokenModel.fromJson(Map<String, dynamic> json) {
    return AuthTokenModel(
      accessToken: json['access_token'] as String,
      refreshToken: json['refresh_token'] as String,
      expiresIn: json['expires_in'] as int,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'access_token': accessToken,
      'refresh_token': refreshToken,
      'expires_in': expiresIn,
    };
  }

  /// Domain Entity로 변환
  AuthToken toEntity() {
    return AuthToken(
      accessToken: accessToken,
      refreshToken: refreshToken,
      expiresIn: expiresIn,
    );
  }

  /// Domain Entity에서 생성
  factory AuthTokenModel.fromEntity(AuthToken entity) {
    return AuthTokenModel(
      accessToken: entity.accessToken,
      refreshToken: entity.refreshToken,
      expiresIn: entity.expiresIn,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is AuthTokenModel &&
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
    return 'AuthTokenModel(accessToken: $accessToken, refreshToken: $refreshToken, expiresIn: $expiresIn)';
  }
}
