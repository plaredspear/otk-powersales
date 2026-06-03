import { useMemo, useState } from 'react';
import { Select, Button, Space } from 'antd';
import { SaveOutlined, SettingOutlined } from '@ant-design/icons';
import { useSavedSearches } from '@/hooks/savedSearch/useSavedSearches';
import { usePermission } from '@/hooks/usePermission';
import type { SavedSearch } from '@/api/savedSearch';
import SavedSearchSaveModal from './SavedSearchSaveModal';
import SavedSearchManageModal from './SavedSearchManageModal';

interface FilterPreview {
  label: string;
  value: string;
}

interface SavedSearchBarProps {
  /** 화면 식별자 (예: "promotion"). */
  resourceKey: string;
  /** 현재 화면의 필터 상태 (저장 대상). */
  filters: Record<string, string>;
  /** 저장할 조건의 사람이 읽을 수 있는 미리보기. */
  preview: FilterPreview[];
  /** 저장된 검색 선택 시 호출 — 해당 filters 를 화면에 주입. */
  onApply: (filters: Record<string, string>) => void;
}

/**
 * 저장된 검색 바 (Spec #852).
 *
 * 드롭다운(공용/개인 그룹) + "현재 조건 저장" + "관리" 를 묶은 범용 컴포넌트.
 * resourceKey 만 바꾸면 행사마스터 외 타 목록 화면에서도 재사용 가능하다.
 */
export default function SavedSearchBar({
  resourceKey,
  filters,
  preview,
  onApply,
}: SavedSearchBarProps) {
  const { hasEntityPermission } = usePermission();
  const canSaveShared = hasEntityPermission('saved_search', 'EDIT');
  const { data: searches } = useSavedSearches(resourceKey);

  const [selectedId, setSelectedId] = useState<number | undefined>(undefined);
  const [saveOpen, setSaveOpen] = useState(false);
  const [manageOpen, setManageOpen] = useState(false);

  const options = useMemo(() => {
    const list = searches ?? [];
    const shared = list.filter((s) => s.scope === 'SHARED');
    const personal = list.filter((s) => s.scope === 'PRIVATE');
    const toOption = (s: SavedSearch) => ({ label: s.name, value: s.id });
    const groups = [];
    if (shared.length > 0) groups.push({ label: '공용', options: shared.map(toOption) });
    if (personal.length > 0) groups.push({ label: '개인', options: personal.map(toOption) });
    return groups;
  }, [searches]);

  const handleSelect = (id: number) => {
    setSelectedId(id);
    const target = (searches ?? []).find((s) => s.id === id);
    if (target) onApply(target.filters);
  };

  return (
    <Space wrap>
      <Select
        style={{ width: 220 }}
        placeholder="저장된 검색"
        allowClear
        value={selectedId}
        options={options}
        onChange={(v) => (v == null ? setSelectedId(undefined) : handleSelect(v))}
        notFoundContent="저장된 검색이 없습니다"
      />
      <Button icon={<SaveOutlined />} onClick={() => setSaveOpen(true)}>
        현재 조건 저장
      </Button>
      <Button icon={<SettingOutlined />} onClick={() => setManageOpen(true)}>
        관리
      </Button>

      <SavedSearchSaveModal
        open={saveOpen}
        resourceKey={resourceKey}
        filters={filters}
        preview={preview}
        canSaveShared={canSaveShared}
        onClose={() => setSaveOpen(false)}
        onSaved={(id) => setSelectedId(id)}
      />
      <SavedSearchManageModal
        open={manageOpen}
        resourceKey={resourceKey}
        onClose={() => setManageOpen(false)}
      />
    </Space>
  );
}
