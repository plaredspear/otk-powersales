import '../../core/utils/error_utils.dart';
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/inspection_field_type.dart';
import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/entities/inspection_theme.dart';
import '../../domain/usecases/get_field_types_usecase.dart';
import '../../domain/usecases/get_themes_usecase.dart';
import '../../domain/usecases/register_inspection_usecase.dart';
import 'inspection_list_provider.dart';
import 'inspection_register_state.dart';

/// 현장 점검 등록 Provider
class InspectionRegisterNotifier
    extends StateNotifier<InspectionRegisterState> {
  final GetThemesUseCase _getThemes;
  final GetFieldTypesUseCase _getFieldTypes;
  final RegisterInspectionUseCase _registerInspection;

  InspectionRegisterNotifier({
    required GetThemesUseCase getThemes,
    required GetFieldTypesUseCase getFieldTypes,
    required RegisterInspectionUseCase registerInspection,
  })  : _getThemes = getThemes,
        _getFieldTypes = getFieldTypes,
        _registerInspection = registerInspection,
        super(InspectionRegisterState.initial());

  /// 초기화: 테마, 현장 유형 로드.
  /// 거래처는 [AccountSelectorSheet] 가 열릴 때 자체 로드하므로 여기서 받지 않는다.
  Future<void> initialize() async {
    state = state.toLoading();

    try {
      final themes = await _getThemes.call();
      final fieldTypes = await _getFieldTypes.call();

      state = state.toLoaded(
        themes: themes,
        fieldTypes: fieldTypes,
      );
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  /// 테마 선택
  void selectTheme(InspectionTheme theme) {
    if (state.form == null) return;

    final updatedForm = state.form!.copyWith(themeId: theme.id);
    state = state.copyWith(form: updatedForm);
  }

  /// 분류 변경 (자사/경쟁사)
  void changeCategory(InspectionCategory category) {
    if (state.form == null) return;

    // 분류 변경 시 관련 필드 초기화
    final updatedForm = InspectionRegisterForm(
      themeId: state.form!.themeId,
      category: category,
      accountId: state.form!.accountId,
      inspectionDate: state.form!.inspectionDate,
      fieldTypeCode: state.form!.fieldTypeCode,
      photos: state.form!.photos,
      // 자사/경쟁사 필드는 초기화됨
    );

    state = state.copyWith(
      form: updatedForm,
      clearProductName: true,
    );
  }

  /// 거래처 선택
  void selectAccount(int accountId, String accountName) {
    if (state.form == null) return;

    final updatedForm = state.form!.copyWith(accountId: accountId);
    state = state.copyWith(form: updatedForm, accountName: accountName);
  }

  /// 점검일 변경
  void updateInspectionDate(DateTime date) {
    if (state.form == null) return;

    final updatedForm = state.form!.copyWith(inspectionDate: date);
    state = state.copyWith(form: updatedForm);
  }

  /// 현장 유형 선택
  void selectFieldType(InspectionFieldType fieldType) {
    if (state.form == null) return;

    final updatedForm = state.form!.copyWith(fieldTypeCode: fieldType.code);
    state = state.copyWith(form: updatedForm);
  }

  /// 설명 입력 (자사)
  void updateDescription(String description) {
    if (state.form == null || !state.isOwn) return;

    final updatedForm = state.form!.copyWith(description: description);
    state = state.copyWith(form: updatedForm);
  }

  /// 제품 선택 (자사)
  void selectProduct(String productCode, String productName) {
    if (state.form == null || !state.isOwn) return;

    final updatedForm = state.form!.copyWith(productCode: productCode);
    state = state.copyWith(
      form: updatedForm,
      selectedProductName: productName,
    );
  }

  /// 경쟁사명 입력 (경쟁사)
  void updateCompetitorName(String name) {
    if (state.form == null || !state.isCompetitor) return;

    final updatedForm = state.form!.copyWith(competitorName: name);
    state = state.copyWith(form: updatedForm);
  }

  /// 경쟁사 활동 내용 입력 (경쟁사)
  void updateCompetitorActivity(String activity) {
    if (state.form == null || !state.isCompetitor) return;

    final updatedForm = state.form!.copyWith(competitorActivity: activity);
    state = state.copyWith(form: updatedForm);
  }

  /// 시식 여부 변경 (경쟁사)
  void updateCompetitorTasting(bool tasting) {
    if (state.form == null || !state.isCompetitor) return;

    // 시식=아니요로 변경 시 시식 관련 필드 초기화
    // copyWith는 null을 명시적으로 설정할 수 없으므로 새 인스턴스 생성
    final updatedForm = tasting
        ? state.form!.copyWith(competitorTasting: true)
        : InspectionRegisterForm(
            themeId: state.form!.themeId,
            category: state.form!.category,
            accountId: state.form!.accountId,
            inspectionDate: state.form!.inspectionDate,
            fieldTypeCode: state.form!.fieldTypeCode,
            photos: state.form!.photos,
            competitorName: state.form!.competitorName,
            competitorActivity: state.form!.competitorActivity,
            competitorTasting: false,
            // 시식 관련 필드는 명시적으로 null로 설정
            competitorProductName: null,
            competitorProductPrice: null,
            competitorSalesQuantity: null,
          );

    state = state.copyWith(form: updatedForm);
  }

  /// 경쟁사 상품명 입력 (시식=예)
  void updateCompetitorProductName(String productName) {
    if (state.form == null || !state.hasTasting) return;

    final updatedForm = state.form!.copyWith(competitorProductName: productName);
    state = state.copyWith(form: updatedForm);
  }

  /// 제품 가격 입력 (시식=예)
  void updateCompetitorProductPrice(int price) {
    if (state.form == null || !state.hasTasting) return;

    final updatedForm = state.form!.copyWith(competitorProductPrice: price);
    state = state.copyWith(form: updatedForm);
  }

  /// 판매 수량 입력 (시식=예)
  void updateCompetitorSalesQuantity(int quantity) {
    if (state.form == null || !state.hasTasting) return;

    final updatedForm = state.form!.copyWith(competitorSalesQuantity: quantity);
    state = state.copyWith(form: updatedForm);
  }

  /// 사진 추가
  void addPhoto(File photo) {
    if (state.form == null || !state.canAddPhoto) return;

    final updatedPhotos = [...state.form!.photos, photo];
    final updatedForm = state.form!.copyWith(photos: updatedPhotos);
    state = state.copyWith(form: updatedForm);
  }

  /// 사진 삭제
  void removePhoto(int index) {
    if (state.form == null || index >= state.photoCount) return;

    final updatedPhotos = [...state.form!.photos];
    updatedPhotos.removeAt(index);
    final updatedForm = state.form!.copyWith(photos: updatedPhotos);
    state = state.copyWith(form: updatedForm);
  }

  /// 등록 전송
  Future<bool> submit() async {
    // 유효성 검증
    final validationResult = state.validate();
    if (!validationResult.isValid) {
      state = state.toError(validationResult.firstError ?? '필수 항목을 입력해주세요');
      return false;
    }

    state = state.toLoading();

    try {
      // RegisterInspectionUseCase는 InspectionListItem을 반환
      final _ = await _registerInspection.call(state.form!);
      state = state.copyWith(isLoading: false, errorMessage: null);
      return true;
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
      return false;
    }
  }

  /// 에러 메시지 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

/// GetThemes UseCase Provider
final getThemesUseCaseProvider = Provider<GetThemesUseCase>((ref) {
  return GetThemesUseCase(ref.watch(inspectionRepositoryProvider));
});

/// GetFieldTypes UseCase Provider
final getFieldTypesUseCaseProvider = Provider<GetFieldTypesUseCase>((ref) {
  return GetFieldTypesUseCase(ref.watch(inspectionRepositoryProvider));
});

/// RegisterInspection UseCase Provider
final registerInspectionUseCaseProvider =
    Provider<RegisterInspectionUseCase>((ref) {
  return RegisterInspectionUseCase(ref.watch(inspectionRepositoryProvider));
});

/// InspectionRegisterProvider
final inspectionRegisterProvider = StateNotifierProvider<
    InspectionRegisterNotifier, InspectionRegisterState>((ref) {
  return InspectionRegisterNotifier(
    getThemes: ref.watch(getThemesUseCaseProvider),
    getFieldTypes: ref.watch(getFieldTypesUseCaseProvider),
    registerInspection: ref.watch(registerInspectionUseCaseProvider),
  );
});
