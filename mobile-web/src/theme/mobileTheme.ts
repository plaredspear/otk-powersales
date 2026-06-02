import type { ThemeConfig } from 'antd';

/**
 * 모바일앱용 AntD 테마 — Heroku 레거시(PowerSales) 디자인 정합.
 *
 * 레거시 common.css 토큰:
 * - 주색 빨강 #DC2C34 (.btn_red), 보조 네이비 #0B3F8C, 노랑 #FFE40C, 슬레이트 #425563
 * - 배경 #F7F7F7, 경계 #E5E5E5/#CCC, 탭 인디케이터 #003CFF
 * - 폰트 NanumSquareNeo Bold(700) / 제목 800, 15px, letter-spacing -0.25px
 * - 버튼 height 48, border-radius 2(거의 직각)
 */
export const LEGACY = {
  red: '#dc2c34',
  navy: '#0b3f8c',
  yellow: '#ffe40c',
  slate: '#425563',
  tabBlue: '#003cff',
  bg: '#f7f7f7',
  border: '#e5e5e5',
} as const;

const FONT_STACK =
  "'NanumSquareNeo', 'NanumSquare', -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif";

export const mobileTheme: ThemeConfig = {
  token: {
    colorPrimary: LEGACY.red,
    colorInfo: LEGACY.navy,
    colorLink: LEGACY.navy,
    colorLinkHover: LEGACY.red,
    colorBgLayout: LEGACY.bg,
    colorBorder: '#cccccc',
    colorBorderSecondary: LEGACY.border,
    fontFamily: FONT_STACK,
    fontSize: 15,
    controlHeight: 44,
    borderRadius: 6,
    borderRadiusLG: 8,
    borderRadiusSM: 4,
  },
  components: {
    Card: { paddingLG: 16, borderRadiusLG: 8 },
    List: { itemPadding: '12px 0' },
    Button: { controlHeight: 48, controlHeightLG: 50, borderRadius: 4, fontSize: 16 },
    Input: { controlHeight: 46 },
    Select: { controlHeight: 46 },
    Tabs: { inkBarColor: LEGACY.tabBlue, itemSelectedColor: LEGACY.tabBlue },
    Tag: { defaultBg: '#f7f7f7' },
  },
};
