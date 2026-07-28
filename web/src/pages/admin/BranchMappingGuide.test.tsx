import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BranchMappingGuide from './BranchMappingGuide';

/**
 * 가이드는 정적 콘텐츠지만, 문구 자체가 산출물이라 회귀를 고정한다.
 *
 * 특히 "확장은 조회 단계에서만 적용" 과 "롤업만 검토 필요" 두 축은 잘못 전달되면
 * 운영자가 권한 범위를 오해하므로, 접기/펼치기 동작과 함께 명시적으로 검증한다.
 */
describe('BranchMappingGuide (지점 코드 맵핑 안내)', () => {
  it('E1 - 기본은 접힌 상태라 표 동선을 가리지 않는다', () => {
    render(<BranchMappingGuide />);
    expect(
      screen.getByText(/지점 코드 확장이 조회 결과에 미치는 영향/),
    ).toBeInTheDocument();
    expect(screen.queryByText('① 지점 셀렉터 목록')).not.toBeInTheDocument();
  });

  it('H1 - 펼치면 확장 적용 단계가 조회 단계 1곳으로만 표기된다', async () => {
    const user = userEvent.setup();
    render(<BranchMappingGuide />);
    await user.click(screen.getByText(/지점 코드 확장이 조회 결과에 미치는 영향/));

    expect(screen.getByText('① 지점 셀렉터 목록')).toBeInTheDocument();
    expect(screen.getByText('② 선택 지점 권한 판정')).toBeInTheDocument();
    expect(screen.getByText('③ 실제 데이터 조회')).toBeInTheDocument();

    // 확장 적용 = 조회 단계 1건, 미적용 = 셀렉터/판정 2건
    expect(screen.getAllByText('적용')).toHaveLength(1);
    expect(screen.getAllByText('미적용')).toHaveLength(2);
  });

  it('H2 - 검토가 필요한 유형은 롤업 1건뿐임을 표기한다', async () => {
    const user = userEvent.setup();
    render(<BranchMappingGuide />);
    await user.click(screen.getByText(/지점 코드 확장이 조회 결과에 미치는 영향/));

    expect(screen.getAllByText('사용처별 확인 필요')).toHaveLength(1);
    expect(screen.getAllByText('확인 불필요')).toHaveLength(3);
  });

  it('H3 - 롤업 합산 미표시와 화면 간 축 차이를 경고로 안내한다', async () => {
    const user = userEvent.setup();
    render(<BranchMappingGuide />);
    await user.click(screen.getByText(/지점 코드 확장이 조회 결과에 미치는 영향/));

    expect(
      screen.getByText(/롤업 유형은 합산 범위가 화면에 표시되지 않습니다/),
    ).toBeInTheDocument();
    expect(screen.getByText(/화면마다 지점 기준이 다를 수 있습니다/)).toBeInTheDocument();
  });
});
