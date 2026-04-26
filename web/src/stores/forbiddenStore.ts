import { create } from 'zustand';

interface ForbiddenState {
  forbidden: boolean;
  setForbidden: (value: boolean) => void;
}

export const useForbiddenStore = create<ForbiddenState>((set) => ({
  forbidden: false,
  setForbidden: (value: boolean) => set({ forbidden: value }),
}));
