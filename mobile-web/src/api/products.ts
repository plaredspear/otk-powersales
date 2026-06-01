import client from './client';
import { unwrap, type ApiResponse } from './types';

/** backend `ProductBarcodeItem` (제품 상세 내) */
export interface ProductBarcodeItem {
  barcode: string;
  [key: string]: unknown;
}

/** backend `ProductDetail` (com.otoki.powersales.product.dto.response) */
export interface ProductDetail {
  id: number;
  productCode: string | null;
  name: string | null;
  barcode: string | null;
  logisticsBarcode: string | null;
  category1: string | null;
  category2: string | null;
  category3: string | null;
  unit: string | null;
  orderingUnit: string | null;
  standardUnitPrice: number | null;
  superTax: number | null;
  launchDate: string | null;
  storageCondition: string | null;
  productStatus: string | null;
  productType: string | null;
  shelfLife: string | null;
  shelfLifeUnit: string | null;
  tasteGift: string | null;
  productFeatures: string | null;
  sellingPoint: string | null;
  purpose: string | null;
  targetAccountType: string | null;
  allergen: string | null;
  crossContamination: string | null;
  imgRefPathFront: string | null;
  imgRefPathBack: string | null;
  manufacture: string | null;
  manufactureDetail: string | null;
  claimManagement: string | null;
  createdAt: string;
  lastModifiedAt: string;
  barcodes: ProductBarcodeItem[];
}

export async function fetchProductDetail(productCode: string): Promise<ProductDetail> {
  const res = await client.get<ApiResponse<ProductDetail>>(
    `/api/v1/mobile/products/${encodeURIComponent(productCode)}`
  );
  return unwrap(res, '제품 상세 조회에 실패했습니다');
}
