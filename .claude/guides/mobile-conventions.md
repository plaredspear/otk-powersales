# Mobile Conventions (Flutter + Riverpod)

> 이 문서는 실제 프로젝트 코드에서 추출한 패턴입니다. 새 기능 구현 시 이 패턴을 따르세요.

---

## 디렉토리 구조

```
mobile/lib/
├── app_router.dart             # Named routing (AppRouter)
├── main.dart                   # ProviderScope + 앱 초기화
├── core/
│   ├── constants/              # 메뉴, 상수 정의
│   └── theme/                  # AppColors, AppSpacing, AppTypography, AppTheme
├── domain/
│   ├── entities/               # 불변 value object (순수 Dart)
│   ├── repositories/           # abstract repository + 관련 value object
│   └── usecases/               # single-responsibility usecase
├── data/
│   ├── models/                 # JSON ↔ Entity 변환 DTO
│   ├── repositories/
│   │   ├── *_repository_impl.dart  # 실제 구현
│   │   └── mock/               # Mock 구현 (개발용)
│   └── datasources/
│       ├── *_api_datasource.dart    # Remote (Dio)
│       └── *_local_datasource.dart  # Local (Hive, SecureStorage)
└── presentation/
    ├── providers/              # Riverpod StateNotifier + State (기능별 분리)
    ├── screens/                # ConsumerStatefulWidget (주요 화면)
    ├── pages/                  # ConsumerStatefulWidget (탭 내 페이지)
    └── widgets/
        ├── common/             # PrimaryButton, LoadingIndicator, ErrorView 등
        └── [feature]/          # 기능별 위젯 (shelf_life/, order/ 등)
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

### Provider 파일 구조 (3-Section 패턴)

```dart
// shelf_life_list_provider.dart

// ============================================
// 1. Dependency Providers (Repository, UseCase)
// ============================================
final shelfLifeRepositoryProvider = Provider<ShelfLifeRepository>((ref) {
  return ShelfLifeMockRepository();  // TODO: 실제 API로 교체
});
final getShelfLifeListUseCaseProvider = Provider<GetShelfLifeList>((ref) {
  return GetShelfLifeList(ref.watch(shelfLifeRepositoryProvider));
});

// ============================================
// 2. StateNotifier Implementation
// ============================================
class ShelfLifeListNotifier extends StateNotifier<ShelfLifeListState> { ... }

// ============================================
// 3. StateNotifier Provider Definition
// ============================================
final shelfLifeListProvider = StateNotifierProvider<ShelfLifeListNotifier, ShelfLifeListState>((ref) {
  return ShelfLifeListNotifier(getShelfLifeList: ref.watch(getShelfLifeListUseCaseProvider));
});
```

### Provider 기능별 분리 (CRUD 기능)

복잡한 기능은 관심사별로 Provider를 분리합니다:

| 파일 | 역할 | 공유 |
|------|------|------|
| `shelf_life_list_provider.dart` | 목록 조회 + 필터 | Repository Provider 정의 (다른 파일에서 import) |
| `shelf_life_form_provider.dart` | 등록/수정 폼 | `shelfLifeRepositoryProvider` import해서 재사용 |
| `shelf_life_delete_provider.dart` | 일괄 삭제 | `shelfLifeRepositoryProvider` import해서 재사용 |

---

## StateNotifier + State

### State 클래스 (목록 화면)

```dart
class ShelfLifeListState {
  final bool isLoading;
  final String? errorMessage;
  final List<ShelfLifeItem> items;
  final bool hasSearched;
  final int? selectedStoreId;
  final String? selectedStoreName;
  final DateTime fromDate;
  final DateTime toDate;
  final Map<int, String> stores;  // 드롭다운용 거래처 목록

  factory ShelfLifeListState.initial() {
    final today = DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day);
    return ShelfLifeListState(
      fromDate: today.subtract(const Duration(days: 7)),
      toDate: today.add(const Duration(days: 7)),
    );
  }

  // 상태 전환 헬퍼
  ShelfLifeListState toLoading() => copyWith(isLoading: true, errorMessage: null);
  ShelfLifeListState toError(String msg) => copyWith(isLoading: false, errorMessage: msg);

  // Computed getters (UI에서 직접 사용)
  List<ShelfLifeItem> get expiredItems => items.where((i) => i.isExpired).toList()..sort(...);
  List<ShelfLifeItem> get activeItems => items.where((i) => !i.isExpired).toList()..sort(...);
  bool get isEmpty => hasSearched && items.isEmpty;

  // nullable 필드 초기화: clearXxx 플래그 패턴
  ShelfLifeListState copyWith({..., bool clearStoreFilter = false}) {
    return ShelfLifeListState(
      selectedStoreId: clearStoreFilter ? null : (selectedStoreId ?? this.selectedStoreId),
      ...
    );
  }
}
```

### State 클래스 (폼 화면)

```dart
class ShelfLifeFormState {
  final bool isLoading;
  final String? errorMessage;
  final int? selectedStoreId;
  final String? selectedProductCode;
  final DateTime expiryDate;
  final DateTime alertDate;
  final int? editId;         // null이면 등록 모드, 값 있으면 수정 모드
  final bool isSaved;        // 저장 완료 one-shot 플래그
  final bool isDeleted;      // 삭제 완료 one-shot 플래그

  // 모드 판별
  bool get isRegisterMode => editId == null;
  bool get isEditMode => editId != null;
  // 폼 유효성
  bool get isValid => isRegisterMode ? (hasStore && hasProduct) : true;
  bool get canSave => isValid && !isLoading;
}
```

### Notifier 패턴

```dart
class ShelfLifeListNotifier extends StateNotifier<ShelfLifeListState> {
  final GetShelfLifeList _getShelfLifeList;
  ShelfLifeListNotifier({required GetShelfLifeList getShelfLifeList})
      : _getShelfLifeList = getShelfLifeList, super(ShelfLifeListState.initial());

  // 초기화
  Future<void> initialize() async { ... }

  // 필터 변경 (동기)
  void selectStore(int? storeId, String? storeName) { state = state.copyWith(...); }
  void updateFromDate(DateTime date) { state = state.copyWith(fromDate: date); }

  // 비동기 액션
  Future<void> searchShelfLife() async {
    state = state.toLoading();
    try {
      final items = await _getShelfLifeList.call(filter);
      state = state.copyWith(isLoading: false, items: items, hasSearched: true);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  void clearError() => state = state.copyWith(errorMessage: null);
}
```

**규칙**:
- State 전환 헬퍼 (`toLoading()`, `toError()`), `copyWith` 필수
- nullable 필드 초기화: `clearXxx` bool 플래그 패턴
- Computed getter로 UI 로직을 State에 캡슐화
- 에러 처리: `e.toString().replaceFirst('Exception: ', '')`
- 결과 플래그: `isSaved`/`isDeleted` → UI에서 `ref.listen`으로 감지 후 화면 전환

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

## Widget (공통 + 기능별)

### 테마 상수 사용 (IMPORTANT)

```dart
// 항상 테마 상수 사용 — 하드코딩 금지
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

// Colors
AppColors.primary         // 오뚜기 Yellow
AppColors.error           // 에러 Red
AppColors.textSecondary   // 보조 텍스트
AppColors.card            // 카드 배경
AppColors.border          // 테두리

// Spacing
AppSpacing.xs / sm / md / lg    // 간격
AppSpacing.cardPadding          // 카드 내부 패딩
AppSpacing.cardBorderRadius     // 카드 모서리
AppSpacing.buttonHeight         // 버튼 높이

// Typography
AppTypography.headlineSmall     // 제목
AppTypography.bodySmall         // 본문
AppTypography.labelSmall        // 라벨/뱃지
```

### 기능별 위젯 패턴

```dart
/// 유통기한 제품 카드 위젯
class ShelfLifeProductCard extends StatelessWidget {
  final ShelfLifeItem item;       // Entity 직접 전달
  final VoidCallback? onTap;      // 콜백으로 화면 전환 위임

  const ShelfLifeProductCard({super.key, required this.item, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.xs),
      child: InkWell(
        onTap: onTap,
        borderRadius: AppSpacing.cardBorderRadius,
        child: Container(
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
          ),
          child: Column(...),
        ),
      ),
    );
  }

  Widget _buildDDayBadge() { ... }  // private helper widget
}
```

**규칙**:
- `const` 생성자 + `super.key`
- Entity를 직접 전달 (위젯이 표시 데이터 결정)
- 콜백(`onTap`, `onChanged`)으로 동작 위임
- `_buildXxx()` private 헬퍼로 하위 위젯 추출
- `AppColors`/`AppSpacing`/`AppTypography` 상수만 사용

---

## Test

### UseCase 테스트

```dart
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

### Notifier 테스트 (StateNotifier 직접 테스트)

```dart
void main() {
  group('ShelfLifeListNotifier', () {
    late ShelfLifeListNotifier notifier;
    late FakeShelfLifeRepository fakeRepository;

    setUp(() {
      fakeRepository = FakeShelfLifeRepository();
      final useCase = GetShelfLifeList(fakeRepository);
      notifier = ShelfLifeListNotifier(getShelfLifeList: useCase);
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.items, isEmpty);
    });

    group('searchShelfLife', () {
      test('검색 성공 시 items를 업데이트해야 한다', () async {
        fakeRepository.itemsToReturn = [sampleItem1, sampleItem2];
        await notifier.searchShelfLife();
        expect(notifier.state.items.length, 2);
        expect(notifier.state.hasSearched, true);
      });

      test('검색 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('최대 6개월입니다');
        await notifier.searchShelfLife();
        expect(notifier.state.errorMessage, '최대 6개월입니다');
      });
    });
  });
}
```

### Fake Repository 패턴

```dart
class FakeShelfLifeRepository implements ShelfLifeRepository {
  List<ShelfLifeItem> itemsToReturn = [];
  ShelfLifeItem? registerResult;
  int batchDeleteCount = 0;
  Exception? exceptionToThrow;        // 설정하면 모든 메서드에서 throw

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return itemsToReturn;
  }
  // ... 나머지 메서드 동일 패턴
}

// 파일 하단에 테스트 데이터
final _sampleItem1 = ShelfLifeItem(
  id: 1, productCode: 'P001', productName: '진라면', ...
);
```

**규칙**:
- `implements`로 수동 Fake (mockito 미사용)
- `group`으로 메서드별 그룹핑, `setUp`에서 Notifier 직접 생성
- `notifier.state`로 상태 직접 검증
- 한국어 테스트명
- Fake에 `exceptionToThrow` 필드로 에러 시뮬레이션
- 테스트 데이터는 파일 하단에 `_sampleXxx`로 정의

---

## API DataSource (Dio)

```dart
class AuthApiDataSource implements AuthRemoteDataSource {
  final Dio _dio;
  AuthApiDataSource(this._dio);

  @override
  Future<LoginResponseModel> login(String employeeId, String password) async {
    final response = await _dio.post(
      '/api/v1/auth/login',
      data: {'employee_id': employeeId, 'password': password},  // snake_case
    );
    return LoginResponseModel.fromJson(
      response.data['data'] as Map<String, dynamic>,  // data 필드 언래핑
    );
  }
}
```

**규칙**: Dio 직접 사용 (Retrofit 미사용), 요청/응답 snake_case, `response.data['data']`로 ApiResponse 언래핑

---

## Local Storage

```dart
class AuthLocalDataSource {
  final FlutterSecureStorage _secureStorage;

  // ─── Secure Storage (JWT 토큰) ───
  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  Future<void> saveAccessToken(String token) async =>
      await _secureStorage.write(key: _accessTokenKey, value: token);
  Future<String?> getAccessToken() async =>
      await _secureStorage.read(key: _accessTokenKey);
  Future<void> clearTokens() async {
    await _secureStorage.delete(key: _accessTokenKey);
    await _secureStorage.delete(key: _refreshTokenKey);
  }

  // ─── Hive (사용자 설정) ───
  static const String _authBoxName = 'auth_box';

  Future<void> saveEmployeeId(String id) async {
    final box = await Hive.openBox(_authBoxName);
    await box.put('saved_employee_id', id);
  }
}
```

**규칙**:
- **Secure Storage**: JWT 토큰 등 민감 데이터
- **Hive**: 사번 기억, 자동로그인 플래그 등 사용자 설정
- key는 `_xxxKey` 상수로 정의

---

## Navigation (Named Routing)

```dart
class AppRouter {
  static const String login = '/login';
  static const String main = '/';
  static const String orderDetail = '/order-detail';

  static Map<String, WidgetBuilder> get routes => {
    login: (context) => const LoginScreen(),
    main: (context) => const MainScreen(),
    orderDetail: (context) {
      final orderId = ModalRoute.of(context)!.settings.arguments as int;
      return OrderDetailPage(orderId: orderId);
    },
  };

  // Navigation helper
  static Future<T?> navigateTo<T>(BuildContext context, String routeName, {Object? arguments}) {
    return Navigator.of(context).pushNamed<T>(routeName, arguments: arguments);
  }
}
```

**규칙**: Named routing (GoRouter 미사용), 인자 전달은 `arguments`, 인증 상태 변경 시 `pushNamedAndRemoveUntil`
