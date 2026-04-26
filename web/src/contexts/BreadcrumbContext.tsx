import { createContext, useState, type ReactNode } from 'react';

interface BreadcrumbContextValue {
  dynamicTitle: string | null;
  setDynamicTitle: (title: string | null) => void;
}

// eslint-disable-next-line react-refresh/only-export-components
export const BreadcrumbContext = createContext<BreadcrumbContextValue>({
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
