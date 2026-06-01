import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `DailySalesFormResponse` */
export interface DailySalesForm {
  promotionEmployeeId: number;
  promotionId: number | null;
  scheduleDate: string | null;
  employeeName: string | null;
  isClosed: boolean;
  editable: boolean;
  hasDraft: boolean;
  basePrice: number | null;
  primarySalesQuantity: number | null;
  primarySalesPrice: number | null;
  primaryProductAmount: number | null;
  otherSalesQuantity: number | null;
  otherSalesAmount: number | null;
  description: string | null;
  imageUrl: string | null;
}

/** backend `DailySalesCloseRequest` (@ModelAttribute) */
export interface DailySalesCloseRequest {
  basePrice?: number;
  primarySalesQuantity?: number;
  primarySalesPrice?: number;
  primaryProductAmount?: number;
  otherSalesQuantity?: number;
  otherSalesAmount?: number;
  description?: string;
}

export interface DailySalesResult {
  promotionEmployeeId: number;
  isClosed: boolean;
  actualAmount: number | null;
  imageUrl: string | null;
}

const base = (id: number) => `/api/v1/mobile/promotion-employees/${id}/daily-sales`;

export async function fetchDailySalesForm(promotionEmployeeId: number): Promise<DailySalesForm> {
  const res = await client.get<ApiResponse<DailySalesForm>>(base(promotionEmployeeId));
  return unwrap(res, '일매출 정보 조회에 실패했습니다');
}

function buildForm(request: DailySalesCloseRequest, image: File | null): FormData {
  const form = new FormData();
  Object.entries(request).forEach(([k, v]) => {
    if (v !== undefined && v !== null) form.append(k, String(v));
  });
  // ⚠️ T1: close/draft 의 이미지 파라미터명은 backend 확인 필요(image 가정).
  if (image) form.append('image', image);
  return form;
}

export async function closeDailySales(
  promotionEmployeeId: number,
  request: DailySalesCloseRequest,
  image: File | null
): Promise<DailySalesResult> {
  const res = await client.post<ApiResponse<DailySalesResult>>(
    base(promotionEmployeeId),
    buildForm(request, image)
  );
  return unwrap(res, '일매출 마감에 실패했습니다');
}

export async function saveDailySalesDraft(
  promotionEmployeeId: number,
  request: DailySalesCloseRequest,
  image: File | null
): Promise<DailySalesForm> {
  const res = await client.post<ApiResponse<DailySalesForm>>(
    `${base(promotionEmployeeId)}/draft`,
    buildForm(request, image)
  );
  return unwrap(res, '임시저장에 실패했습니다');
}
