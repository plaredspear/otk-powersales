import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/inspection_field_type.dart';
import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../../domain/entities/inspection_theme.dart';
import '../../domain/usecases/get_field_types_usecase.dart';
import '../../domain/usecases/get_my_stores.dart';
import '../../domain/usecases/get_themes_usecase.dart';
import '../../domain/usecases/register_inspection_usecase.dart';
import 'inspection_register_state.dart';

/// 현장 점검 등록 Provider
class InspectionRegisterNotifier
    extends StateNotifier<InspectionRegisterState> {
  final GetThemesUseCase _getThemes;
  final GetFieldTypesUseCase _getFieldTypes;
  final GetMyStores _getMyStores;
  final RegisterInspectionUseCase _registerInspection;

  InspectionRegisterNotifier({
    required GetThemesUseCase getThemes,
    required GetFieldTypesUseCase getFieldTypes,
    required GetMyStores getMyStores,
    required RegisterInspectionUseCase registerInspection,
  })  : _getThemes = getThemes,
        _getFieldTypes = getFieldTypes,
        _getMyStores = getMyStores,
        _registerInspection = registerInspection,
        super(InspectionRegisterState.initial());

  /// 초기화: 테마, 현장 유형, 거래처 목록 로드
  Future<void> initialize() async {
    state = state.toLoading();

    try {
      final themes = await _getThemes.call();
      final fieldTypes = await _getFieldTypes.call();
      final storeResult = await _getMyStores.call();

      // 거래처 목록을 Map<int, String>으로 변환
      final storeMap = <int, String>{
        for (var store in storeResult.stores) store.storeId: store.storeName,
      };

      state = state.toLoaded(
        themes: themes,
        fieldTypes: fieldTypes,
        stores: storeMap,
      );
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
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
      storeId: state.form!.storeId,
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
  void selectStore(int storeId, String storeName) {
    if (state.form == null) return;

    final updatedForm = state.form!.copyWith(storeId: storeId);
    state = state.copyWith(form: updatedForm);
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
            storeId: state.form!.storeId,
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
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
      return false;
    }
  }

  /// 에러 메시지 초기화
  void clearError() {
    state = state.copyWith(clearError: true);
  }
}

/// InspectionRegisterProvider
final inspectionRegisterProvider = StateNotifierProvider<
    InspectionRegisterNotifier, InspectionRegisterState>((ref) {
  // UseCase providers (이미 다른 곳에서 정의되어 있다고 가정)
  // 실제 구현 시 해당 provider를 import하여 사용
  throw UnimplementedError(
    'inspectionRegisterProvider는 실제 UseCase provider와 연결되어야 합니다',
  );
});
