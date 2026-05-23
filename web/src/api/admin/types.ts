
/**
 * 시스템 관리자 수동 등록 (Spec #579).
 *
 * - Backend `POST /api/v1/admin/employees` 의 요청·응답 본문 타입.
 * - 응답 본문은 백엔드 `ApiResponse<AdminEmployeeRegisterResponse>` 의 `data` 필드.
 */

export interface AdminAccountRegisterRequest {
  employeeCode: string;
  name: string;
  password: string;
  passwordConfirm: string;
  workEmail?: string | null;
  workPhone?: string | null;
  orgName?: string | null;
  costCenterCode?: string | null;
}

export interface AdminAccountRegisterResponse {
  employeeId: number;
  employeeCode: string;
  name: string;
  /** SF DKRetail__AppAuthority__c picklist value 또는 null (시스템 관리자는 null). */
  role: string | null;
  origin: 'SAP' | 'MANUAL';
  appLoginActive: boolean;
  passwordChangeRequired: boolean;
  createdAt: string;
}
