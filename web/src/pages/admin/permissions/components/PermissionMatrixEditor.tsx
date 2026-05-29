import { useMemo, useState } from 'react';
import { Checkbox, Input, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Text } = Typography;

/**
 * Spec #837 — SObject (또는 가상 자원) × CRUD 비트 매트릭스 편집 컴포넌트.
 *
 * Object Permissions (6비트: R/C/E/D/VA/MA) 와 Custom Permissions (4비트: R/C/E/D) 양쪽에서 재사용.
 * value 는 Mutation API 와 동일한 `Record<string, Record<string, boolean>>` 형식 (DB jsonb 본문) —
 * round-trip 손실 없음.
 *
 * 검색은 client-side filter (자원 name 또는 label 기준). 변경된 row 는 강조 배경.
 */

export type PermissionBit = 'allowRead' | 'allowCreate' | 'allowEdit' | 'allowDelete' | 'viewAllRecords' | 'modifyAllRecords';

interface ResourceItem {
  /** 자원 고유 키 — Object 영역은 SF API name, Custom 영역은 resource name. value 의 key 와 일치. */
  name: string;
  /** 표시용 label (Object 영역의 entity 또는 자원 한글명). 미제공 시 name 만 표시. */
  label?: string;
}

interface Props {
  resources: ResourceItem[];
  bits: PermissionBit[];
  value: Record<string, Record<string, boolean>>;
  onChange: (next: Record<string, Record<string, boolean>>) => void;
  /** 변경 row 강조 비교용 초기 value. 미지정 시 값 변화 highlight 비활성화. */
  baselineValue?: Record<string, Record<string, boolean>>;
}

const BIT_LABEL: Record<PermissionBit, string> = {
  allowRead: 'READ',
  allowCreate: 'CREATE',
  allowEdit: 'EDIT',
  allowDelete: 'DELETE',
  viewAllRecords: 'VIEW_ALL',
  modifyAllRecords: 'MODIFY_ALL',
};

interface MatrixRow {
  key: string;
  name: string;
  label?: string;
  bits: Record<string, boolean>;
  changed: boolean;
}

export default function PermissionMatrixEditor({ resources, bits, value, onChange, baselineValue }: Props) {
  const [keyword, setKeyword] = useState('');

  const rows: MatrixRow[] = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    return resources
      .filter((r) => !k || r.name.toLowerCase().includes(k) || (r.label?.toLowerCase().includes(k) ?? false))
      .sort((a, b) => a.name.localeCompare(b.name))
      .map((r) => {
        const current = value[r.name] ?? {};
        const baseline = baselineValue?.[r.name] ?? {};
        const changed =
          baselineValue !== undefined &&
          bits.some((b) => !!current[b] !== !!baseline[b]);
        return {
          key: r.name,
          name: r.name,
          label: r.label,
          bits: current,
          changed,
        };
      });
  }, [resources, value, baselineValue, bits, keyword]);

  const toggleBit = (resourceName: string, bit: PermissionBit, next: boolean) => {
    const row = value[resourceName] ?? {};
    const updatedRow = { ...row, [bit]: next };
    // 빈 객체 정리 — 모든 비트가 false 면 키 자체 삭제 (저장 본문 가벼움).
    const allFalse = bits.every((b) => !updatedRow[b]);
    const updated = { ...value };
    if (allFalse) {
      delete updated[resourceName];
    } else {
      updated[resourceName] = updatedRow;
    }
    onChange(updated);
  };

  const columns: ColumnsType<MatrixRow> = [
    {
      title: '자원',
      key: 'resource',
      render: (_, row) =>
        row.label && row.label !== row.name ? (
          <span>
            <Text strong>{row.name}</Text> <Text type="secondary">({row.label})</Text>
          </span>
        ) : (
          <Text strong>{row.name}</Text>
        ),
    },
    ...bits.map<ColumnsType<MatrixRow>[number]>((bit) => ({
      title: BIT_LABEL[bit],
      key: bit,
      width: 100,
      align: 'center',
      render: (_, row) => (
        <Checkbox
          checked={!!row.bits[bit]}
          onChange={(e) => toggleBit(row.name, bit, e.target.checked)}
        />
      ),
    })),
  ];

  return (
    <div>
      <Input.Search
        placeholder="자원명 검색"
        allowClear
        onChange={(e) => setKeyword(e.target.value)}
        style={{ marginBottom: 12, maxWidth: 320 }}
      />
      <Table<MatrixRow>
        dataSource={rows}
        columns={columns}
        size="small"
        pagination={{ pageSize: 50, showSizeChanger: true }}
        rowClassName={(row) => (row.changed ? 'permission-matrix-row-changed' : '')}
      />
      <style>{`
        .permission-matrix-row-changed { background-color: #fff7e6; }
        .permission-matrix-row-changed:hover > td { background-color: #ffe7ba !important; }
      `}</style>
    </div>
  );
}
