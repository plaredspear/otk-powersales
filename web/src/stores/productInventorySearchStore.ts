import { create } from 'zustand';

export interface InventorySearchTarget {
  productCode: string;
  name: string | null;
  category1: string | null;
  category2: string | null;
  unit: string | null;
}

interface ProductInventorySearchState {
  targets: InventorySearchTarget[];
  setTargets: (targets: InventorySearchTarget[]) => void;
  clearTargets: () => void;
}

/**
 * UC-03 / UC-04 의 재고조회 모달에 전달할 선택 제품 목록.
 *
 * UC-03 — ProductPage 에서 일괄 선택 후 모달 오픈 시 N건 (1~50)
 * UC-04 — ProductDetailPage 에서 Quick Action 모달 오픈 시 1건
 */
export const useProductInventorySearchStore = create<ProductInventorySearchState>((set) => ({
  targets: [],
  setTargets: (targets) => set({ targets }),
  clearTargets: () => set({ targets: [] }),
}));
