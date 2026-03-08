import { createContext, useContext, useState, type ReactNode } from 'react';

interface BreadcrumbContextValue {
  dynamicTitle: string | null;
  setDynamicTitle: (title: string | null) => void;
}

const BreadcrumbContext = createContext<BreadcrumbContextValue>({
  dynamicTitle: null,
  setDynamicTitle: () => {},
});

export function BreadcrumbProvider({ children }: { children: ReactNode }) {
  const [dynamicTitle, setDynamicTitle] = useState<string | null>(null);

  return (
    <BreadcrumbContext value={{ dynamicTitle, setDynamicTitle }}>
      {children}
    </BreadcrumbContext>
  );
}

export function useBreadcrumbContext() {
  return useContext(BreadcrumbContext);
}
