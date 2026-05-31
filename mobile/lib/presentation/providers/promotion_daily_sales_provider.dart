import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/daily_sales_api_datasource.dart';
import '../../data/repositories/promotion_daily_sales_repository_impl.dart';
import '../../domain/entities/daily_sales_form.dart';
import '../../domain/repositories/promotion_daily_sales_repository.dart';

// ============================================
// 1. Dependency Provider
// ============================================

final promotionDailySalesRepositoryProvider =
    Provider<PromotionDailySalesRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final dataSource = DailySalesApiDataSource(dio);
  return PromotionDailySalesRepositoryImpl(remoteDataSource: dataSource);
});

// ============================================
// 2. State
// ============================================

enum DailySalesSubmitStatus { idle, submitting, success, error }

/// copyWith 에서 "전달 안 함"과 "null 로 설정"을 구분하기 위한 센티넬.
const Object _undefined = Object();

class PromotionDailySalesState {
  /// 초기 폼 로딩 중 여부.
  final bool isLoading;

  /// 로드된 폼(헤더 정보, editable, 기존 이미지 URL).
  final DailySalesForm? form;

  // 대표상품
  final int? mainPrice;
  final int? mainQuantity;
  final int? mainAmount;

  // 기타상품
  final String? subName;
  final int? subQuantity;
  final int? subAmount;

  /// 새로 첨부한 사진(없으면 기존 [form.imageUrl] 사용).
  final File? photo;

  final DailySalesSubmitStatus submitStatus;
  final String? errorMessage;

  const PromotionDailySalesState({
    this.isLoading = false,
    this.form,
    this.mainPrice,
    this.mainQuantity,
    this.mainAmount,
    this.subName,
    this.subQuantity,
    this.subAmount,
    this.photo,
    this.submitStatus = DailySalesSubmitStatus.idle,
    this.errorMessage,
  });

  bool get editable => form?.editable ?? false;

  bool get isClosed => form?.isClosed ?? false;

  bool get hasMainProduct =>
      mainPrice != null && mainQuantity != null && mainAmount != null;

  bool get hasSubProduct =>
      subName != null &&
      subName!.isNotEmpty &&
      subQuantity != null &&
      subAmount != null;

  bool get hasAnyProduct => hasMainProduct || hasSubProduct;

  /// 기존 이미지(서버) 또는 새 사진이 있는지.
  bool get hasPhoto => photo != null || (form?.imageUrl != null);

  /// 최종 마감 가능 여부.
  bool get canSubmit => editable && hasAnyProduct && hasPhoto;

  bool get isSubmitting => submitStatus == DailySalesSubmitStatus.submitting;

  PromotionDailySalesState copyWith({
    bool? isLoading,
    DailySalesForm? form,
    Object? mainPrice = _undefined,
    Object? mainQuantity = _undefined,
    Object? mainAmount = _undefined,
    Object? subName = _undefined,
    Object? subQuantity = _undefined,
    Object? subAmount = _undefined,
    Object? photo = _undefined,
    DailySalesSubmitStatus? submitStatus,
    Object? errorMessage = _undefined,
  }) {
    return PromotionDailySalesState(
      isLoading: isLoading ?? this.isLoading,
      form: form ?? this.form,
      mainPrice: identical(mainPrice, _undefined) ? this.mainPrice : mainPrice as int?,
      mainQuantity:
          identical(mainQuantity, _undefined) ? this.mainQuantity : mainQuantity as int?,
      mainAmount:
          identical(mainAmount, _undefined) ? this.mainAmount : mainAmount as int?,
      subName: identical(subName, _undefined) ? this.subName : subName as String?,
      subQuantity:
          identical(subQuantity, _undefined) ? this.subQuantity : subQuantity as int?,
      subAmount: identical(subAmount, _undefined) ? this.subAmount : subAmount as int?,
      photo: identical(photo, _undefined) ? this.photo : photo as File?,
      submitStatus: submitStatus ?? this.submitStatus,
      errorMessage:
          identical(errorMessage, _undefined) ? this.errorMessage : errorMessage as String?,
    );
  }
}

// ============================================
// 3. StateNotifier
// ============================================

class PromotionDailySalesNotifier
    extends StateNotifier<PromotionDailySalesState> {
  final PromotionDailySalesRepository _repository;
  final int promotionEmployeeId;

  PromotionDailySalesNotifier({
    required PromotionDailySalesRepository repository,
    required this.promotionEmployeeId,
  })  : _repository = repository,
        // 최초 프레임부터 로딩 상태로 시작하여 load() 이전의 빈 폼/에러뷰 깜빡임을 막는다.
        // 폼 위젯은 form 로드 완료 후 처음 mount 되므로 prefill 값이 정상 반영된다.
        super(const PromotionDailySalesState(isLoading: true));

  /// 폼 로드 + prefill.
  Future<void> load() async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final form = await _repository.getForm(promotionEmployeeId);
      state = PromotionDailySalesState(
        isLoading: false,
        form: form,
        mainPrice: form.primarySalesPrice?.toInt(),
        mainQuantity: form.primarySalesQuantity?.toInt(),
        mainAmount: form.primaryProductAmount?.toInt(),
        subName: form.description,
        subQuantity: form.otherSalesQuantity?.toInt(),
        subAmount: form.otherSalesAmount?.toInt(),
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  void updateMainProduct({int? price, int? quantity, int? amount}) {
    // ProductInputForm 은 매 입력마다 풀 스냅샷(price/quantity/amount)을 전달한다.
    // 금액은 항상 현재 단가·수량으로부터 재계산하여 단가/수량을 비웠을 때 옛 금액이
    // 되살아나지 않도록 한다(상태 단일 소스 보장). 위젯이 넘긴 amount 는 사용하지 않는다.
    final computedAmount =
        (price != null && quantity != null) ? price * quantity : null;
    state = state.copyWith(
      mainPrice: price,
      mainQuantity: quantity,
      mainAmount: computedAmount,
      errorMessage: null,
    );
  }

  void updateSubProduct({String? name, int? quantity, int? amount}) {
    state = state.copyWith(
      subName: name,
      subQuantity: quantity,
      subAmount: amount,
      errorMessage: null,
    );
  }

  void updatePhoto(File? photo) {
    state = state.copyWith(photo: photo);
  }

  void clearError() {
    state = state.copyWith(errorMessage: null);
  }

  DailySalesInput _buildInput() {
    return DailySalesInput(
      primarySalesPrice: state.mainPrice,
      primarySalesQuantity: state.mainQuantity,
      primaryProductAmount: state.mainAmount,
      otherSalesQuantity: state.subQuantity,
      otherSalesAmount: state.subAmount,
      description: state.subName,
    );
  }

  /// 최종 마감. 성공 시 true.
  Future<bool> submit() async {
    if (state.isSubmitting) return false;
    if (!state.editable) {
      state = state.copyWith(errorMessage: '마감되었거나 수정 권한이 없습니다');
      return false;
    }
    if (!state.hasAnyProduct) {
      state = state.copyWith(errorMessage: '대표상품 또는 기타상품 정보를 입력해주세요');
      return false;
    }
    if (!state.hasPhoto) {
      state = state.copyWith(errorMessage: '사진을 첨부해주세요');
      return false;
    }

    state = state.copyWith(
      submitStatus: DailySalesSubmitStatus.submitting,
      errorMessage: null,
    );
    try {
      await _repository.close(promotionEmployeeId, _buildInput(), state.photo);
      state = state.copyWith(submitStatus: DailySalesSubmitStatus.success);
      return true;
    } catch (e) {
      state = state.copyWith(
        submitStatus: DailySalesSubmitStatus.error,
        errorMessage: extractErrorMessage(e),
      );
      return false;
    }
  }

  /// 임시저장. 성공 시 true.
  Future<bool> saveDraft() async {
    if (state.isSubmitting) return false;
    if (!state.editable) {
      state = state.copyWith(errorMessage: '마감되었거나 수정 권한이 없습니다');
      return false;
    }

    state = state.copyWith(
      submitStatus: DailySalesSubmitStatus.submitting,
      errorMessage: null,
    );
    try {
      final form = await _repository.saveDraft(
        promotionEmployeeId,
        _buildInput(),
        state.photo,
      );
      // 저장 후 서버 기준 폼으로 동기화(이미지 URL 갱신, 새 사진은 소비됨).
      state = state.copyWith(
        form: form,
        submitStatus: DailySalesSubmitStatus.idle,
        photo: null,
        errorMessage: null,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        submitStatus: DailySalesSubmitStatus.error,
        errorMessage: extractErrorMessage(e),
      );
      return false;
    }
  }
}

// ============================================
// 4. Provider (family, autoDispose)
// ============================================

final promotionDailySalesProvider = StateNotifierProvider.autoDispose.family<
    PromotionDailySalesNotifier, PromotionDailySalesState, int>(
  (ref, promotionEmployeeId) {
    final repository = ref.watch(promotionDailySalesRepositoryProvider);
    return PromotionDailySalesNotifier(
      repository: repository,
      promotionEmployeeId: promotionEmployeeId,
    );
  },
);
