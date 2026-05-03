import type { UserRole } from '@/constants/userRole';

/**
 * 시스템 관리자 수동 등록 (Spec #579).
 *
 * - Backend `POST /api/v1/admin/employees` 의 요청·응답 본문 타입.
 * - 응답 본문은 백엔드 `ApiResponse<AdminEmployeeRegisterResponse>` 의 `data` 필드.
 */

export interface AdminAccountRegisterRequest {
  employee_code: string;
  name: string;
  password: string;
  password_confirm: string;
  work_email?: string | null;
  work_phone?: string | null;
  org_name?: string | null;
  cost_center_code?: string | null;
}

export interface AdminAccountRegisterResponse {
  employee_id: number;
  employee_code: string;
  name: string;
  role: UserRole;
  origin: 'SAP' | 'MANUAL';
  app_login_active: boolean;
  password_change_required: boolean;
  created_at: string;
}
