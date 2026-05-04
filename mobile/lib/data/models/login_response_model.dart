import '../../domain/repositories/auth_repository.dart';
import 'user_model.dart';
import 'auth_token_model.dart';

/// 로그인 응답 모델 (DTO)
///
/// Backend API 응답:
/// ```json
/// {
///   "success": true,
///   "data": {
///     "user": { ... },
///     "token": { ... },
///     "passwordChangeRequired": true,
///     "requiresGpsConsent": false
///   },
///   "message": "로그인 성공"
/// }
/// ```
class LoginResponseModel {
  final UserModel user;
  final AuthTokenModel token;
  final bool passwordChangeRequired;
  final bool requiresGpsConsent;

  const LoginResponseModel({
    required this.user,
    required this.token,
    required this.passwordChangeRequired,
    required this.requiresGpsConsent,
  });

  factory LoginResponseModel.fromJson(Map<String, dynamic> json) {
    return LoginResponseModel(
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>),
      token: AuthTokenModel.fromJson(json['token'] as Map<String, dynamic>),
      passwordChangeRequired: json['passwordChangeRequired'] as bool,
      requiresGpsConsent: json['requiresGpsConsent'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'user': user.toJson(),
      'token': token.toJson(),
      'passwordChangeRequired': passwordChangeRequired,
      'requiresGpsConsent': requiresGpsConsent,
    };
  }

  LoginResult toLoginResult() {
    return LoginResult(
      user: user.toEntity(),
      token: token.toEntity(),
      passwordChangeRequired: passwordChangeRequired,
      requiresGpsConsent: requiresGpsConsent,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LoginResponseModel &&
        other.user == user &&
        other.token == token &&
        other.passwordChangeRequired == passwordChangeRequired &&
        other.requiresGpsConsent == requiresGpsConsent;
  }

  @override
  int get hashCode {
    return Object.hash(
      user,
      token,
      passwordChangeRequired,
      requiresGpsConsent,
    );
  }

  @override
  String toString() {
    return 'LoginResponseModel(user: $user, token: $token, passwordChangeRequired: $passwordChangeRequired, requiresGpsConsent: $requiresGpsConsent)';
  }
}
