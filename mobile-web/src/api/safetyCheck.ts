import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `SafetyCheckItemsResponse.CheckItemInfo` */
export interface SafetyCheckItem {
  seqNum: number;
  contents: string;
}

/** backend `SafetyCheckItemsResponse.CategoryInfo` */
export interface SafetyCheckCategory {
  questionNum: number;
  title: string;
  inputType: string;
  required: boolean;
  options: string[] | null;
  items: SafetyCheckItem[];
}

export interface SafetyCheckItemsData {
  categories: SafetyCheckCategory[];
}

export interface SafetyCheckTodayStatus {
  completed: boolean;
  submittedAt: string | null;
}

/** backend `SafetyCheckSubmitRequest.EquipmentAnswer` */
export interface EquipmentAnswer {
  seqNum: number;
  answer: string;
}

export interface SafetyCheckSubmitRequest {
  startTime: string; // ISO LocalDateTime
  completeTime: string;
  equipments: EquipmentAnswer[];
  precautions?: string[];
}

export interface SafetyCheckSubmitResult {
  submittedAt: string;
  safetyCheckCompleted: boolean;
}

export async function fetchSafetyCheckItems(): Promise<SafetyCheckItemsData> {
  const res = await client.get<ApiResponse<SafetyCheckItemsData>>(
    '/api/v1/mobile/safety-check/items'
  );
  return unwrap(res, '안전점검 항목 조회에 실패했습니다');
}

export async function fetchSafetyCheckToday(): Promise<SafetyCheckTodayStatus> {
  const res = await client.get<ApiResponse<SafetyCheckTodayStatus>>(
    '/api/v1/mobile/safety-check/today'
  );
  return unwrap(res, '안전점검 상태 조회에 실패했습니다');
}

export async function submitSafetyCheck(
  request: SafetyCheckSubmitRequest
): Promise<SafetyCheckSubmitResult> {
  const res = await client.post<ApiResponse<SafetyCheckSubmitResult>>(
    '/api/v1/mobile/safety-check/submit',
    request
  );
  return unwrap(res, '안전점검 제출에 실패했습니다');
}
