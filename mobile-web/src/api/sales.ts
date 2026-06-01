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

// --- 전산(ABC) 매출 (거래처 레벨, BE🟡) ---

/** backend `ElectronicSalesResponse.ProductSales` */
export interface ProductSalesItem {
  productCode: string;
  productName: string;
  amount: number;
  quantity: number;
}

/** backend `ElectronicSalesResponse` */
export interface ElectronicSales {
  customerId: number;
  customerName: string;
  sapAccountCode: string;
  yearMonth: string;
  items: ProductSalesItem[];
}

/**
 * 전산(ABC) 매출 조회.
 * @param customerId 거래처(Account) PK — `/mobile/accounts/my` 의 accountId 사용.
 * @param yearMonth YYYYMM
 *
 * ⚠️ 데이터: 거래처 레벨 실적. 제품/SKU 단위 정밀도는 데이터 소스 한계로 제한적(T1/데이터플랫폼).
 */
export async function fetchElectronicSales(
  customerId: number,
  yearMonth: string
): Promise<ElectronicSales> {
  const res = await client.get<ApiResponse<ElectronicSales>>('/api/v1/mobile/sales/electronic', {
    params: { customerId, yearMonth },
  });
  return unwrap(res, '전산매출 조회에 실패했습니다');
}
