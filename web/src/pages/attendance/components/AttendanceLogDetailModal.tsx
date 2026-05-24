import { Button, Descriptions, Modal, Space, Spin, Tag } from 'antd';
import dayjs from 'dayjs';
import type { AttendanceTypeCode } from '@/api/attendanceLog';
import { useAttendanceLogDetail } from '@/hooks/attendance-log/useAttendanceLog';

interface AttendanceLogDetailModalProps {
  attendanceLogId: number;
  onClose: () => void;
}

const ATTENDANCE_TYPE_LABEL: Record<AttendanceTypeCode, string> = {
  REGULAR: '일반',
  DISPLAY: '진열',
  EVENT: '행사',
};

const ATTENDANCE_TYPE_COLOR: Record<AttendanceTypeCode, string> = {
  REGULAR: 'blue',
  DISPLAY: 'green',
  EVENT: 'purple',
};

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value;
}

export default function AttendanceLogDetailModal({
  attendanceLogId,
  onClose,
}: AttendanceLogDetailModalProps) {
  const { data, isLoading } = useAttendanceLogDetail(attendanceLogId);

  return (
    <Modal
      title={`근무 등록 상세 (id=${attendanceLogId})`}
      open
      onCancel={onClose}
      footer={<Button onClick={onClose}>닫기</Button>}
      width={720}
    >
      {isLoading || !data ? (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Spin />
        </div>
      ) : (
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="등록번호 (Name)">{data.name ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="출근 종류">
            {data.attendanceType ? (
              <Tag color={ATTENDANCE_TYPE_COLOR[data.attendanceType]}>
                {ATTENDANCE_TYPE_LABEL[data.attendanceType]}
              </Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>

          <Descriptions.Item label="사원">
            <Space>
              {data.employeeName ?? '-'}
              {data.employeeCode && (
                <span style={{ color: '#888' }}>({data.employeeCode})</span>
              )}
              {data.employeeJobCode && <Tag color="geekblue">{data.employeeJobCode}</Tag>}
            </Space>
          </Descriptions.Item>

          <Descriptions.Item label="거래처">
            <Space>
              {data.accountName ?? '-'}
              {data.accountCode && <span style={{ color: '#888' }}>({data.accountCode})</span>}
            </Space>
          </Descriptions.Item>

          <Descriptions.Item label="출근일시">
            {formatDateTime(data.attendanceDate)}
          </Descriptions.Item>
          <Descriptions.Item label="근무유형 (2nd)">
            {data.secondWorkTypeName ?? '-'}
          </Descriptions.Item>

          <Descriptions.Item label="사유" span={2}>
            {data.reason ?? '-'}
          </Descriptions.Item>

          <Descriptions.Item label="등록자">
            {data.createdByName ?? <span style={{ color: '#bbb' }}>(SF 미연동)</span>}
          </Descriptions.Item>
          <Descriptions.Item label="최종 수정자">
            {data.lastModifiedByName ?? '-'}
          </Descriptions.Item>

          <Descriptions.Item label="등록일시">{formatDateTime(data.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="최종 수정일시">
            {formatDateTime(data.updatedAt)}
          </Descriptions.Item>

          <Descriptions.Item label="삭제 여부">
            {data.isDeleted === true ? (
              <Tag color="red">삭제됨</Tag>
            ) : data.isDeleted === false ? (
              <Tag color="default">정상</Tag>
            ) : (
              '-'
            )}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
}
