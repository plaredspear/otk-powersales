import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/datasources/suggestion_remote_datasource.dart';
import '../../data/repositories/suggestion_repository_impl.dart';
import '../../domain/entities/suggestion_form.dart';
import '../../domain/repositories/suggestion_repository.dart';
import '../../domain/usecases/register_suggestion_usecase.dart';
import 'suggestion_register_state.dart';

// ============================================
// 1. DataSource Provider (Mock)
// ============================================

/// SuggestionRemoteDataSource Provider (임시 Mock)
final suggestionRemoteDataSourceProvider =
    Provider<SuggestionRemoteDataSource>((ref) {
  // TODO: 실제 구현으로 대체
  throw UnimplementedError('SuggestionRemoteDataSource not implemented yet');
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
  void changeCategory(SuggestionCategory category) {
    // 분류 변경 시 제품 정보 초기화
    final updatedForm = state.form.copyWith(category: category);

    if (category == SuggestionCategory.newProduct) {
      // 신제품으로 변경 시 제품 정보 제거
      final clearedForm = updatedForm.copyWithNull(
        productCode: true,
        productName: true,
      );
      state = state.copyWith(
        form: clearedForm,
        clearProductName: true,
        clearErrorMessage: true,
      );
    } else {
      state = state.copyWith(
        form: updatedForm,
        clearErrorMessage: true,
      );
    }
  }

  /// 제품 선택
  void selectProduct(String productCode, String productName) {
    if (!state.isExistingProduct) return;

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
