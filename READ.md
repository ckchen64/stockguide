markdown# 📈 주식 가이드 시뮬레이터 (Stock Guide Simulator)
> **과거 500일 실전 주가 데이터 기반 알고리즘 트레이딩 백테스팅 및 모의 투자 시스템**
> 본 프로젝트는 단순 가상 데이터 조회가 아닌, 실제 금융 데이터 가공 인프라 구축과 실시간 기술적 분석 지표(SMA/MACD) 시각화를 달성한 풀스택 모의 트레이딩 플랫폼입니다.

---

## 🚀 1. 프로젝트 개요 (Project Overview)
- **개발 기간**: 2026.05 ~ 2026.06 (풀스택 1인 개발)
- **핵심 목표**: 실전 6개 컬럼(시가/고가/저가/종가/거래량) 과거 데이터를 활용하여 1초 단위로 시장 흐름을 시뮬레이션하고, 보조지표(이동평균선) 골든크로스 알고리즘 타점에 맞춰 실시간 자산 정산 및 매매 이력 타임라인을 기록하는 독립형 샌드박스 환경 구축.

### 🛠 Tech Stacks
- **Frontend**: React 18, TypeScript, ApexCharts, Axios
- **Backend**: Spring Boot 3.x, Spring Data JPA, Spring Security
- **Database**: MySQL 8.0, Data Clean & Parsing (LOAD DATA INFILE, STR_TO_DATE)
- **DevOps/Tools**: IntelliJ IDEA, GitHub, MySQL Workbench

---

## 🏗 2. 시스템 아키텍처 및 폴더 구조 (Architecture)
도메인 중심 설계(DDD)를 지향하여 백엔드 패키지를 철저히 분리하고, 프론트엔드 역시 단일 비대화를 막기 위해 삼분할 독립 컴포넌트 구조로 설계하여 유지보수성을 극대화했습니다.

### 📁 Backend (Spring Boot) Packages
```text
com.lookuphere.stockguide
├── account       # 가상 계좌 및 자산 정산 오퍼레이션 레이어
├── config        # CORS 국경 해제 및 최신 시큐리티 필터 제어
├── dailydata     # 6대 컬럼 과거 시세 엔티티 및 리포지토리
├── dashboard     # 보조지표 연산 총괄 멀티 엔진 레이어
├── history       # 매매 실현손익 영수증 발행 및 조회 타임라인 서비스 (독립 운영)
└── order         # 클라이언트 주문 접수 및 유효성 검증 컨트롤러
```

### 📁 Frontend (React & TypeScript) Components
```text
src/components
├── AssetDashboard.tsx     # 최상단 실시간 자산 현황 평가판 바
├── CandleStickChart.tsx   # ApexCharts 기반 실시간 혼합(Mixed) 캔들 차트
├── SimulationConsole.tsx  # 1초 Interval 타이머 자동 플레이 제어 콘솔 [구역 A]
├── OrderPanel.tsx         # BUY/SELL 수량/단가 예외 처리 주문 모듈 [구역 B]
└── TradeTimeline.tsx      # DB 누적 영수증 실시간 렌더링 타임라인 표 [구역 C]
```

---

## 🔥 3. 핵심 기술적 해결 역량 (Troubleshooting & Deep Dive)
본 프로젝트 진행 과정에서 마주한 심각한 인프라 및 아키텍처적 장벽을 주도적인 디버깅으로 극복해 낸 기술적 마일스톤입니다.

### 📌 3-1. 무결성 실전 금융 데이터 수급 및 파싱 (MySQL Error 1290 / 1292 / 1411 격파)
- **문제 상황**: 외부 대용량 CSV 주가 데이터 로드 시 숫자의 쉼표(,), 문자열 따옴표(`""`), 그리고 윈도우 특유의 줄바꿈 기호(`\r\n`) 및 하이픈/슬래시 날짜 포맷 불일치로 데이터 오염 및 대량 예외 발생.
- **해결 방안**: MySQL의 보안 옵션(`secure-file-priv`)을 공식 `Uploads` 경로 지정으로 우회하고, `OPTIONALLY ENCLOSED BY '"'` 및 `IGNORE 2 LINES` 옵션을 커스텀 빌딩. 자바 연산에 알맞은 청정 데이터 상태를 확보하기 위해 `STR_TO_DATE(TRIM(@v_trade_date), '%Y/%m/%d')` 및 `REPLACE` 내장 함수를 조합한 마스터 주입 스크립트를 설계하여 **500일치 실전 주가 탄약을 1초 만에 무결성 100%로 이식 성공**.

### 📌 3-2. 변수 유효 범위(Scope) 한계 극복 및 실시간 멀티 레이어 차트 도킹
- **문제 상황**: 보조지표 연산 결과값(`ma5`, `ma20`)이 `analyzeIndicators` 내부 로컬 변수로 갇혀 리액트 통신 배달 봉투(`resultMap`)에 실리지 못해 차트 연동 실패 및 컴파일 에러 발생.
- **해결 방안**: 메인 엔진 메서드 간 파라미터 구조를 재설계하여, 호출 시 배달 주머니(`Map<String, Object> resultMap`)를 참조 레퍼런스로 직접 주입. 변수가 살아있는 스코프 방 안에서 직접 패킹하게 함으로써 객체 지향 결합도를 낮추고 응집도를 향상. 이를 프론트엔드 수신부에서 구조 분해하여 **캔들스틱 기둥 위로 5일선/20일선 실선 그래프가 실시간으로 겹쳐서 자라나는 콤보 트레이딩 차트 완벽 구현**.

### 📌 3-3. 아키텍처 책임 분산 및 TypeScript Strict 규칙 준수 (CORS 및 ts(6133) 완파)
- **문제 상황**: 단일 리액트 컴포넌트(`StockExchange`) 및 백엔드 비즈니스 로직의 비대화로 가독성 저하 및 타입스크립트 전용 strict 옵션(`verbatimModuleSyntax`) 위반 경고 발생.
- **해결 방안**: `TradeHistoryService`를 분리 독립하여 영수증 도메인 책임을 격리(클린 아키텍처 지향). 프론트엔드에서는 `type import` 문법(`import type { CandleData }`)을 도입하여 타입 세크리게이션을 달성하고, 부모-자식 간 무선 전령 프로퍼티(`onSimulationAdvanceProps`)를 바인딩하여 **컴파일러의 모든 경고등과 302 리다이렉트 국경 차단(CORS) 에러를 흔적도 없이 싹 청소하며 무결점 청정 빌드 달성**.

---

## 📊 4. 핵심 기능 시각화 (Key Features)
1. **🎮 백테스팅 콘솔**: `setInterval` 타이머 기술을 통한 1초 간격 과거 세계 무한 자동 플레이/일시정지 가동.
2. **📈 실시간 멀티 차트**: ApexCharts 콤보 모드로 구현된 실시간 이동평균선 크로스 타점 시각화 추적.
3. **🛒 자산 정산 엔진**: 매수/매도 시 취득 원금 대비 실현손익 비율(%) 연산 및 예수금(`deposit`) 실시간 장부 갱신.
4. **📜 매매 타임라인**: 주문 체결 즉시 당시의 가상 날짜 도장을 쾅 찍어 데이터베이스에 영구 보존하는 영수증 리스트.