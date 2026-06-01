import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `MonthlySalesResponse.CategorySalesInfo` */
export interface CategorySales {
  category: string;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
}

/** backend `MonthlySalesResponse` */
export interface MonthlySales {
  customerId: string;
  customerName: string;
  yearMonth: string;
  targetAmount: number;
  achievedAmount: number;
  achievementRate: number;
  categorySales: CategorySales[];
  yearComparison: {
    currentYear: number;
    previousYear: number;
  };
  monthlyAverage: {
    currentYearAverage: number;
    previousYearAverage: number;
    startMonth: number;
    endMonth: number;
  };
}

export interface MonthlySalesParams {
  /** YYYYMM */
  yearMonth: string;
  /** 거래처 ID (선택) */
  customerId?: string;
}

export async function fetchMonthlySales(params: MonthlySalesParams): Promise<MonthlySales> {
  const res = await client.get<ApiResponse<MonthlySales>>('/api/v1/mobile/sales/monthly', {
    params,
  });
  return unwrap(res, '월매출 조회에 실패했습니다');
}
