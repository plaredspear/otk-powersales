# Mobile Conventions (Flutter + Riverpod)

> 이 문서는 실제 프로젝트 코드에서 추출한 패턴입니다. 새 기능 구현 시 이 패턴을 따르세요.

---

## 디렉토리 구조

```
mobile/lib/
├── domain/
│   ├── entities/       # 불변 value object
│   ├── repositories/   # abstract repository + 관련 value object
│   └── usecases/       # single-responsibility usecase
├── data/
│   ├── models/         # JSON ↔ Entity 변환 DTO
│   ├── repositories/   # repository 구현체
│   └── datasources/    # Remote (API) + Local (secure storage)
└── presentation/
    ├── providers/      # Riverpod provider + StateNotifier + State
    ├── screens/        # ConsumerStatefulWidget (주요 화면)
    ├── pages/          # ConsumerStatefulWidget (탭 내 페이지)
    └── widgets/        # 재사용 위젯
```

---

## Entity (Domain)

```dart
class User {
  final int id;
  final String employeeId;
  final String name;
  final String department;
  final String branchName;
  final String role;

  const User({
    required this.id,
    required this.employeeId,
    required this.name,
    required this.department,
    required this.branchName,
    required this.role,
  });

  User copyWith({int? id, String? name, ...}) => User(id: id ?? this.id, ...);
  Map<String, dynamic> toJson() => {'id': id, 'employee_id': employeeId, ...};
  factory User.fromJson(Map<String, dynamic> json) => User(id: json['id'], ...);

  @override
  bool operator ==(Object other) => identical(this, other) ||
      other is User && id == other.id && employeeId == other.employeeId;
  @override
  int get hashCode => id.hashCode ^ employeeId.hashCode;
}
```

**규칙**: `const` 생성자, `final` 필드, `copyWith()`, `==`/`hashCode` 오버라이드, `toJson()`/`fromJson()`

---

## Model (Data)

```dart
class UserModel {
  final int id;
  final String employeeId;
  // ... 동일 필드

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      employeeId: json['employee_id'] as String,  // snake_case API → camelCase
      branchName: json['branch_name'] as String,
    );
  }

  User toEntity() => User(id: id, employeeId: employeeId, ...);
  factory UserModel.fromEntity(User entity) => UserModel(id: entity.id, ...);
}
```

**규칙**: API JSON은 snake_case. Model이 snake↔camel 변환 담당. `toEntity()`/`fromEntity()` 제공.

---

## Repository Interface (Domain)

```dart
// 관련 value object도 같은 파일에 정의
class LoginResult {
  final User user;
  final AuthToken token;
  final bool requiresPasswordChange;
  final bool requiresGpsConsent;
  const LoginResult({required this.user, required this.token, ...});
}

abstract class AuthRepository {
  Future<LoginResult> login(String employeeId, String password);
  Future<AuthToken> refreshToken(String refreshToken);
  Future<void> changePassword(String currentPassword, String newPassword);
  Future<void> logout();
}
```

**규칙**: `abstract class`, 반환은 domain entity, 관련 value object는 같은 파일 상단에 정의

---

## Repository Implementation (Data)

```dart
class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;

  AuthRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
    required AuthLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    final response = await _remoteDataSource.login(employeeId, password);
    final result = response.toLoginResult();
    await _localDataSource.saveAccessToken(result.token.accessToken);
    await _localDataSource.saveRefreshToken(result.token.refreshToken);
    return result;
  }
}
```

**규칙**: DataSource 조합, Model → Entity 변환, 로컬 저장소 부수효과 처리

---

## UseCase (Domain)

```dart
class LoginUseCase {
  final AuthRepository _repository;
  LoginUseCase(this._repository);

  Future<LoginResult> call({
    required String employeeId,
    required String password,
    required bool rememberEmployeeId,
    required bool autoLogin,
  }) async {
    if (employeeId.isEmpty) throw ArgumentError('사번을 입력해주세요');
    if (password.isEmpty) throw ArgumentError('비밀번호를 입력해주세요');
    return await _repository.login(employeeId, password);
  }
}
```

**규칙**: `call()` 메서드, named parameters, 입력 validation, repository 위임

---

## Provider (Riverpod)

```dart
// 1. 싱글톤 Provider (Repository, UseCase)
final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    remoteDataSource: ref.watch(authRemoteDataSourceProvider),
    localDataSource: ref.watch(authLocalDataSourceProvider),
  );
});

final loginUseCaseProvider = Provider<LoginUseCase>((ref) {
  return LoginUseCase(ref.watch(authRepositoryProvider));
});

// 2. StateNotifierProvider (복잡한 상태 관리)
final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    loginUseCase: ref.watch(loginUseCaseProvider),
    logoutUseCase: ref.watch(logoutUseCaseProvider),
    localDataSource: ref.watch(authLocalDataSourceProvider),
  );
});
```

**규칙**: `Provider<T>` = 싱글톤, `StateNotifierProvider` = 복잡한 상태, `ref.watch()`로 의존성 주입

---

## StateNotifier + State

```dart
class AuthState {
  final bool isLoading;
  final User? user;
  final String? errorMessage;
  final bool requiresPasswordChange;
  const AuthState({this.isLoading = false, this.user, this.errorMessage, ...});

  factory AuthState.initial() => const AuthState();
  AuthState toLoading() => copyWith(isLoading: true, errorMessage: null);
  AuthState toAuthenticated(User user) => copyWith(isLoading: false, user: user);
  AuthState toError(String msg) => copyWith(isLoading: false, errorMessage: msg);
  bool get isAuthenticated => user != null && !requiresPasswordChange;

  AuthState copyWith({bool? isLoading, User? user, String? errorMessage, ...}) {
    return AuthState(isLoading: isLoading ?? this.isLoading, ...);
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final LoginUseCase _loginUseCase;
  AuthNotifier({required LoginUseCase loginUseCase, ...})
      : _loginUseCase = loginUseCase, super(AuthState.initial());

  Future<void> login({required String employeeId, ...}) async {
    state = state.toLoading();
    try {
      final result = await _loginUseCase.call(...);
      state = state.toAuthenticated(result.user);
    } on ArgumentError catch (e) {
      state = state.toError(e.message as String);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  void clearError() => state = state.copyWith(errorMessage: null);
}
```

**규칙**: State 전환 헬퍼 (`toLoading()`, `toError()`), 에러 처리는 Notifier에서, `copyWith` 필수

---

## Screen (ConsumerStatefulWidget)

```dart
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _employeeIdController = TextEditingController();

  @override
  void dispose() {
    _employeeIdController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;
    ref.read(authProvider.notifier).login(
      employeeId: _employeeIdController.text.trim(),
      password: _passwordController.text,
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);  // rebuild on change

    ref.listen<String?>(
      authProvider.select((s) => s.errorMessage),  // watch specific field
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(next)));
          ref.read(authProvider.notifier).clearError();
        }
      },
    );

    return Scaffold(body: Form(key: _formKey, child: ...));
  }
}
```

**규칙**:
- `ref.watch()` = rebuild, `ref.read()` = 1회 읽기, `ref.listen()` = side effect
- `ref.read(provider.notifier)` = Notifier 메서드 호출
- `.select()` = 특정 필드만 watch

---

## Page (데이터 로딩 패턴)

```dart
class _HomePageState extends ConsumerState<HomePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(homeProvider.notifier).fetchHomeData();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(homeProvider);

    if (state.homeData == null && !state.isError) {
      return const LoadingIndicator(message: '홈 데이터를 불러오는 중...');
    }
    if (state.isError && state.homeData == null) {
      return ErrorView(message: '데이터를 불러올 수 없습니다', onRetry: ...);
    }
    return RefreshIndicator(onRefresh: _onRefresh, child: ...);
  }
}
```

**규칙**: `initState` + `addPostFrameCallback`으로 초기 데이터 로드, loading/error/success 3상태 처리

---

## Test

```dart
class MockAuthRepository implements AuthRepository {
  LoginResult? loginResult;
  Exception? exceptionToThrow;
  String? lastLoginEmployeeId;

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    lastLoginEmployeeId = employeeId;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return loginResult!;
  }
  // ... 나머지 메서드 stub
}

void main() {
  group('LoginUseCase', () {
    late LoginUseCase useCase;
    late MockAuthRepository mockRepo;

    setUp(() {
      mockRepo = MockAuthRepository();
      useCase = LoginUseCase(mockRepo);
    });

    test('정상 로그인 시 LoginResult 반환', () async {
      mockRepo.loginResult = const LoginResult(user: testUser, token: testToken, ...);
      final result = await useCase.call(employeeId: '20010585', password: 'test1234', ...);
      expect(result.user.employeeId, equals('20010585'));
    });

    test('빈 사번이면 ArgumentError 발생', () async {
      expect(
        () => useCase.call(employeeId: '', password: 'test1234', ...),
        throwsA(isA<ArgumentError>().having((e) => e.message, 'message', '사번을 입력해주세요')),
      );
    });
  });
}
```

**규칙**: `implements`로 수동 Mock, `group`/`setUp`/`test`, `expect`/`throwsA`/`isA<T>().having()`, 한국어 테스트명
