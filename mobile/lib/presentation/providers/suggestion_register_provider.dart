import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/suggestion_api_datasource.dart';
import '../../data/datasources/suggestion_remote_datasource.dart';
import '../../data/repositories/suggestion_repository_impl.dart';
import '../../domain/entities/suggestion_form.dart';
import '../../domain/repositories/suggestion_repository.dart';
import '../../domain/usecases/register_suggestion_usecase.dart';
import 'suggestion_register_state.dart';

// ============================================
// 1. DataSource Provider
// ============================================

/// SuggestionRemoteDataSource Provider
final suggestionRemoteDataSourceProvider =
    Provider<SuggestionRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return SuggestionApiDataSource(dio);
});

// ============================================
// 2. Repository Provider
// ============================================

/// SuggestionRepository Provider
final suggestionRepositoryProvider = Provider<SuggestionRepository>((ref) {
  final dataSource = ref.watch(suggestionRemoteDataSourceProvider);
  return SuggestionRepositoryImpl(dataSource);
});

// ============================================
// 3. UseCase Provider
// ============================================

/// RegisterSuggestionUseCase Provider
final registerSuggestionUseCaseProvider =
    Provider<RegisterSuggestionUseCase>((ref) {
  final repository = ref.watch(suggestionRepositoryProvider);
  return RegisterSuggestionUseCase(repository);
});

/// 제안하기 등록 Provider
class SuggestionRegisterNotifier
    extends StateNotifier<SuggestionRegisterState> {
  final RegisterSuggestionUseCase _registerSuggestion;

  SuggestionRegisterNotifier({
    required RegisterSuggestionUseCase registerSuggestion,
  })  : _registerSuggestion = registerSuggestion,
        super(SuggestionRegisterState.initial());

  /// 분류 변경
  ///
  /// 카테고리 전환 시 다른 카테고리 전용 입력 필드를 초기화한다.
  /// 대표 제품은 신제품/기존제품/물류 클레임 공통 개념이라(레거시 정합) 신제품
  /// 제안으로 전환할 때만 제거하고, 기존제품 ↔ 물류 클레임 전환 시에는 유지한다.
  /// 물류 클레임 전용 필드(거래처/클레임항목/발생일자/차량번호)는 물류 클레임이
  /// 아닌 분류로 전환 시 제거한다.
  void changeCategory(SuggestionCategory category) {
    final updatedForm = state.form.copyWith(category: category);

    final clearProduct = category == SuggestionCategory.newProduct;
    final cleared = updatedForm.copyWithNull(
      productCode: clearProduct,
      productName: clearProduct,
      accountId: category != SuggestionCategory.logisticsClaim,
      accountName: category != SuggestionCategory.logisticsClaim,
      sapAccountCode: category != SuggestionCategory.logisticsClaim,
      claimType: category != SuggestionCategory.logisticsClaim,
      claimDate: category != SuggestionCategory.logisticsClaim,
      carNumber: category != SuggestionCategory.logisticsClaim,
    );

    state = state.copyWith(
      form: cleared,
      clearProductName: clearProduct,
      clearErrorMessage: true,
    );
  }

  /// 제품 선택 (신제품 제안 외 분류에서 대표 제품 지정)
  void selectProduct(String productCode, String productName) {
    if (state.isNewProduct) return;

    final updatedForm = state.form.copyWith(
      productCode: productCode,
      productName: productName,
    );

    state = state.copyWith(
      form: updatedForm,
      selectedProductName: productName,
      clearErrorMessage: true,
    );
  }

  /// 제목 변경
  void updateTitle(String title) {
    final updatedForm = state.form.copyWith(title: title);
    state = state.copyWith(
      form: updatedForm,
      clearErrorMessage: true,
    );
  }

  /// 내용 변경
  void updateContent(String content) {
    final updatedForm = state.form.copyWith(content: content);
    state = state.copyWith(
      form: updatedForm,
      clearErrorMessage: true,
    );
  }

  /// 사진 추가
  void addPhoto(File photo) {
    if (state.form.photos.length >= 2) {
      state = state.toError('사진은 최대 2장까지 첨부 가능합니다');
      return;
    }

    final updatedPhotos = [...state.form.photos, photo];
    final updatedForm = state.form.copyWith(photos: updatedPhotos);
    state = state.copyWith(
      form: updatedForm,
      clearErrorMessage: true,
    );
  }

  /// 사진 삭제
  void removePhoto(int index) {
    if (index < 0 || index >= state.form.photos.length) return;

    final updatedPhotos = [...state.form.photos];
    updatedPhotos.removeAt(index);
    final updatedForm = state.form.copyWith(photos: updatedPhotos);
    state = state.copyWith(
      form: updatedForm,
      clearErrorMessage: true,
    );
  }

  /// 거래처 선택 (물류 클레임 카테고리)
  void selectAccount({
    required int accountId,
    required String accountName,
    String? sapAccountCode,
  }) {
    if (!state.isLogisticsClaim) return;
    final updatedForm = state.form.copyWith(
      accountId: accountId,
      accountName: accountName,
      sapAccountCode: sapAccountCode,
    );
    state = state.copyWith(form: updatedForm, clearErrorMessage: true);
  }

  /// 클레임 항목 변경
  void updateClaimType(String value) {
    final updatedForm = state.form.copyWith(claimType: value);
    state = state.copyWith(form: updatedForm, clearErrorMessage: true);
  }

  /// 클레임 일자 변경
  void updateClaimDate(DateTime value) {
    final updatedForm = state.form.copyWith(claimDate: value);
    state = state.copyWith(form: updatedForm, clearErrorMessage: true);
  }

  /// 차량번호 변경
  void updateCarNumber(String value) {
    final updatedForm = state.form.copyWith(carNumber: value);
    state = state.copyWith(form: updatedForm, clearErrorMessage: true);
  }

  /// 제안 등록
  Future<void> submit() async {
    // 유효성 검증
    final validationErrors = state.form.validate();
    if (validationErrors.isNotEmpty) {
      state = state.toError(validationErrors.first);
      return;
    }

    state = state.toSubmitting();

    try {
      await _registerSuggestion.call(state.form);
      state = state.toSuccess('제안이 등록되었습니다');
    } catch (e) {
      final errorMessage = e
          .toString()
          .replaceFirst('Exception: ', '')
          .replaceFirst('Error: ', '');
      state = state.toError(errorMessage);
    }
  }

  /// 폼 초기화
  void reset() {
    state = SuggestionRegisterState.initial();
  }

  /// 에러 메시지 지우기
  void clearError() {
    state = state.copyWith(clearErrorMessage: true);
  }

  /// 성공 메시지 지우기
  void clearSuccess() {
    state = state.copyWith(clearSuccessMessage: true);
  }
}

// ============================================
// 4. StateNotifierProvider
// ============================================

/// 제안하기 등록 Provider
final suggestionRegisterProvider =
    StateNotifierProvider<SuggestionRegisterNotifier, SuggestionRegisterState>(
        (ref) {
  return SuggestionRegisterNotifier(
    registerSuggestion: ref.watch(registerSuggestionUseCaseProvider),
  );
});
