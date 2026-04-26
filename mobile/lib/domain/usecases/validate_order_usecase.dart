import '../entities/order_draft.dart';
import '../entities/validation_error.dart';
import '../repositories/order_repository.dart';

/// 주문서 유효성 검증 UseCase
///
/// 주문서의 필수 입력 및 서버 유효성 검증을 수행합니다.
class ValidateOrder {
  final OrderRepository _repository;

  ValidateOrder(this._repository);

  /// 주문서 유효성 검증 실행
  ///
  /// [orderDraft]: 검증할 주문서 초안
  /// Returns: 유효성 검증 결과
  ///
  /// 클라이언트 측 필수 입력 검증을 먼저 수행하고,
  /// 통과 시 서버 유효성 검증 API를 호출합니다.
  Future<ValidationResult> call({required OrderDraft orderDraft}) async {
    // 클라이언트 측 필수 입력 검증
    if (orderDraft.clientId == null) {
      return const ValidationResult(
        isValid: false,
        errors: {
          '_form': ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: '거래처를 선택해주세요',
          ),
        },
      );
    }

    if (orderDraft.deliveryDate == null) {
      return const ValidationResult(
        isValid: false,
        errors: {
          '_form': ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: '납기일을 선택해주세요',
          ),
        },
      );
    }

    if (orderDraft.items.isEmpty) {
      return const ValidationResult(
        isValid: false,
        errors: {
          '_form': ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: '제품을 최소 1개 이상 추가해주세요',
          ),
        },
      );
    }

    // 수량 입력 검증: 각 제품에 박스 또는 낱개 중 최소 하나 이상 입력
    for (final item in orderDraft.items) {
      if (item.quantityBoxes <= 0 && item.quantityPieces <= 0) {
        return ValidationResult(
          isValid: false,
          errors: {
            item.productCode: ValidationError(
              errorType: ValidationErrorType.minOrderQuantity,
              message: '${item.productName}의 수량을 입력해주세요',
            ),
          },
        );
      }
    }

    // 서버 유효성 검증 API 호출
    return await _repository.validateOrder(orderDraft: orderDraft);
  }
}
