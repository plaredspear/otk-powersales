import client from './client';

// --- Raw API response interfaces (snake_case from backend) ---

interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message?: string;
}

interface ScheduleBranchRaw {
  cost_center_code: string;
  branch_name: string;
}

// --- Frontend interfaces (camelCase) ---

export interface ScheduleBranch {
  costCenterCode: string;
  branchName: string;
}

// --- Mappers ---

function mapBranches(raw: ScheduleBranchRaw[]): ScheduleBranch[] {
  return raw.map((b) => ({
    costCenterCode: b.cost_center_code,
    branchName: b.branch_name,
  }));
}

// --- API functions ---

export async function fetchScheduleBranches(): Promise<ScheduleBranch[]> {
  const res = await client.get<ApiResponse<ScheduleBranchRaw[]>>('/api/v1/admin/schedule/branches');
  if (!res.data.success || !res.data.data) {
    throw new Error(res.data.message || '지점 목록 조회에 실패했습니다');
  }
  return mapBranches(res.data.data);
}

export async function downloadScheduleTemplate(costCenterCode: string): Promise<void> {
  const res = await client.get('/api/v1/admin/schedule/template', {
    params: { costCenterCode },
    responseType: 'blob',
  });

  // Content-Disposition 헤더에서 파일명 추출
  const contentDisposition = res.headers['content-disposition'] as string | undefined;
  let filename = `진열스케줄_양식_${costCenterCode}.xlsx`;
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
    if (match) {
      filename = decodeURIComponent(match[1]);
    }
  }

  // Blob 다운로드
  const blob = new Blob([res.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
