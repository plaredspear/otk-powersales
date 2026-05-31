import 'dart:io';

import '../entities/daily_sales_form.dart';

/// 일매출 마감/임시저장 Repository (실제 API).
///
/// 레거시 mock 기반 [DailySalesRepository] 와 별개로, 실제 promotion-employee
/// 일매출 마감 백엔드와 연동한다.
abstract class PromotionDailySalesRepository {
  Future<DailySalesForm> getForm(int promotionEmployeeId);

  Future<DailySalesCloseResult> close(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  );

  Future<DailySalesForm> saveDraft(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  );

  Future<void> deleteDraft(int promotionEmployeeId);
}
