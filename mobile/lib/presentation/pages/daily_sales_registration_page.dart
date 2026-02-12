import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/event.dart';
import '../providers/daily_sales_form_provider.dart';
import '../providers/daily_sales_form_state.dart';
import '../widgets/daily_sales/photo_picker_widget.dart';
import '../widgets/daily_sales/product_input_form.dart';

/// 일매출 등록 화면
///
/// 행사를 선택한 후 진입하여 일매출 정보를 입력하고 등록합니다.
/// - 대표제품 또는 기타제품 정보 입력 (최소 1개 필수)
/// - 사진 첨부 (필수)
/// - 임시저장 또는 최종 등록
class DailySalesRegistrationPage extends ConsumerWidget {
  /// 선택된 행사
  final Event event;

  const DailySalesRegistrationPage({
    super.key,
    required this.event,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // 행사별 Provider 사용
    final state = ref.watch(dailySalesFormProviderFamily(event));
    final notifier = ref.read(dailySalesFormProviderFamily(event).notifier);

    // 제출 성공 시 이전 화면으로 이동
    ref.listen(
      dailySalesFormProviderFamily(event),
      (previous, next) {
        if (next.isSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('일매출이 등록되었습니다')),
          );
          Navigator.pop(context, true);
        }
      },
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('일매출 등록'),
        actions: [
          // 임시저장 버튼
          TextButton(
            onPressed: state.isSubmitting ? null : () => _saveDraft(context, notifier),
            child: const Text('임시저장'),
          ),
        ],
      ),
      body: _buildBody(context, state, notifier),
      bottomNavigationBar: _buildBottomBar(context, state, notifier),
    );
  }

  /// 본문 위젯
  Widget _buildBody(
    BuildContext context,
    DailySalesFormState state,
    DailySalesFormNotifier notifier,
  ) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 행사 정보
          _buildEventInfo(),
          const SizedBox(height: 24),

          // 대표제품 입력
          ProductInputForm(
            type: ProductType.main,
            initialPrice: state.mainProductPrice,
            initialQuantity: state.mainProductQuantity,
            initialAmount: state.mainProductAmount,
            onChanged: ({price, quantity, amount, code, name}) {
              notifier.updateMainProduct(
                price: price,
                quantity: quantity,
                amount: amount,
              );
            },
          ),
          const SizedBox(height: 16),

          // 기타제품 입력
          ProductInputForm(
            type: ProductType.sub,
            initialCode: state.subProductCode,
            initialName: state.subProductName,
            initialQuantity: state.subProductQuantity,
            initialAmount: state.subProductAmount,
            onChanged: ({price, quantity, amount, code, name}) {
              notifier.updateSubProduct(
                code: code,
                name: name,
                quantity: quantity,
                amount: amount,
              );
            },
          ),
          const SizedBox(height: 24),

          // 사진 첨부
          PhotoPickerWidget(
            photo: state.photo,
            onPhotoChanged: notifier.updatePhoto,
          ),
          const SizedBox(height: 16),

          // 에러 메시지
          if (state.isError) _buildErrorMessage(state.errorMessage!),
        ],
      ),
    );
  }

  /// 행사 정보 위젯
  Widget _buildEventInfo() {
    return Card(
      color: Colors.blue.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.event, color: Colors.blue),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    event.eventName,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${event.customerName} | ${event.eventType}',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey.shade700,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 에러 메시지 위젯
  Widget _buildErrorMessage(String message) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.red.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.red.shade200),
      ),
      child: Row(
        children: [
          Icon(Icons.error_outline, color: Colors.red.shade700),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                color: Colors.red.shade700,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 하단 버튼 바
  Widget _buildBottomBar(
    BuildContext context,
    DailySalesFormState state,
    DailySalesFormNotifier notifier,
  ) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: SafeArea(
        child: SizedBox(
          width: double.infinity,
          height: 50,
          child: ElevatedButton(
            onPressed: state.isValid && !state.isSubmitting
                ? () => _submit(context, notifier)
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              disabledBackgroundColor: Colors.grey.shade300,
            ),
            child: state.isSubmitting
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                : const Text(
                    '등록',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
          ),
        ),
      ),
    );
  }

  /// 최종 등록
  Future<void> _submit(
    BuildContext context,
    DailySalesFormNotifier notifier,
  ) async {
    final success = await notifier.submit();
    if (!success && context.mounted) {
      // 에러는 state.errorMessage로 표시됨
    }
  }

  /// 임시저장
  Future<void> _saveDraft(
    BuildContext context,
    DailySalesFormNotifier notifier,
  ) async {
    final success = await notifier.saveDraft();
    if (success && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('임시저장되었습니다')),
      );
    }
  }
}
