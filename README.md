# 🌐 Malgo Backend

> 문화적 맥락까지 고려하는 AI 글로벌 커뮤니케이션 서비스 **Malgo**의 백엔드 서버입니다.

Malgo는 단순히 문장을 다른 언어로 번역하는 것을 넘어  
**대상 국가, 관계, 상황, 대화 목적, 말투를 고려하여 자연스러운 번역과 문화적 표현 분석을 제공하는 서비스**입니다.

---

## 📌 주요 기능

### 1. AI 문화 맞춤 번역

사용자가 입력한 문장과 상황 정보를 바탕으로 OpenAI API를 호출하여 번역 결과를 제공합니다.

사용되는 정보:

- 원문 언어
- 목표 언어
- 대상 국가
- 상황
- 상대방과의 관계
- 커뮤니케이션 목적
- 요청 말투

번역 결과는 다음 3가지 형태로 제공합니다.

| 종류 | 설명 |
|---|---|
| Literal Translation | 원문의 의미와 구조를 최대한 유지한 직역 |
| Natural Translation | 현지인이 실제로 사용할 수 있는 자연스러운 번역 |
| Cultural Translation | 국가, 관계, 상황, 말투까지 고려한 문화 맞춤 번역 |

---

### 2. 문화적 위험 표현 분석

번역 과정에서 상대방에게 오해나 불쾌감을 줄 가능성이 있는 표현을 분석합니다.

위험도는 다음과 같이 구분합니다.

| 위험도 | 설명 |
|---|---|
| `SAFE` | 특별한 커뮤니케이션 위험이 없는 표현 |
| `CAUTION` | 상황이나 관계에 따라 부정적으로 받아들여질 수 있는 표현 |
| `HIGH` | 오해, 갈등 또는 관계 악화 가능성이 높은 표현 |
| `AVOID` | 대상 문화나 상황에서 사용을 강하게 피하는 것이 권장되는 표현 |

---

### 3. 위험 표현 카테고리 분석

위험 표현이 발견되면 해당 표현의 문제 유형을 함께 제공합니다.

현재 지원하는 주요 카테고리:

- `DIRECTNESS` - 지나치게 직접적인 표현
- `POLITENESS` - 예의 및 정중함 관련 표현
- `PRESSURE` - 상대방에게 압박을 줄 수 있는 표현
- `FORMALITY` - 상황에 맞지 않는 격식 수준
- `PERSONAL_ATTACK` - 상대방의 능력, 성격, 인격 등을 직접 공격하는 표현
- `SARCASM` - 비꼼 또는 반어적 표현
- `CULTURAL_TABOO` - 문화적으로 주의가 필요한 표현
- `AMBIGUITY` - 의도가 불명확해 오해할 수 있는 표현
- `SENSITIVITY` - 민감하게 받아들여질 수 있는 표현
- `OTHER` - 기타 문화적 커뮤니케이션 위험

각 위험 표현에 대해 다음 정보를 제공합니다.

- 위험 표현
- 위험 카테고리
- 위험도
- 위험한 이유
- 대체 표현
- 원문 내 표현 위치

---

### 4. 문장 Tone 분석

사용자가 작성한 원문의 분위기를 여러 항목으로 분석합니다.

```text
friendliness
politeness
directness
aggression
burden
professionalism
naturalness
```

각 항목은 `0 ~ 100` 사이의 점수로 제공됩니다.

> Tone 점수는 모든 항목이 높을수록 좋은 것을 의미하지 않습니다.  
> 관계와 상황에 따라 적절한 Tone이 달라질 수 있습니다.

---

### 5. 번역 기록 저장

사용자의 번역 요청과 AI 분석 결과를 MySQL 데이터베이스에 저장합니다.

저장되는 주요 데이터:

- 번역 요청
- AI 번역 결과
- Tone 분석 점수
- 문화적 위험 표현

번역 기록 조회 및 개별 조회/삭제 기능을 제공합니다.

---

## 🛠 Tech Stack

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Gradle

### Database

- MySQL

### AI

- OpenAI API
- GPT-5 mini
- OpenAI Responses API
- Structured Outputs (JSON Schema)

### Development Tools

- IntelliJ IDEA
- Postman
- Git / GitHub

---

## 🤖 AI Response Structure

OpenAI API의 Structured Outputs를 사용하여 AI 응답을 일정한 JSON 형식으로 제한합니다.

예시:

```json
{
  "literalTranslation": "Please send the materials quickly.",
  "naturalTranslation": "Could you please send the materials as soon as possible?",
  "culturalTranslation": "Could you please send the materials at your earliest convenience?",
  "culturalExplanation": "문화적 차이와 표현 수정 이유",
  "overallRiskLevel": "CAUTION",
  "toneScores": {
    "friendliness": 65,
    "politeness": 65,
    "directness": 80,
    "aggression": 20,
    "burden": 70,
    "professionalism": 60,
    "naturalness": 80
  },
  "warnings": [
    {
      "expression": "빨리",
      "category": "PRESSURE",
      "riskLevel": "CAUTION",
      "reason": "상대방에게 압박으로 느껴질 수 있습니다.",
      "alternativeExpression": "at your earliest convenience",
      "startIndex": 3,
      "endIndex": 5
    }
  ]
}
```

---

## 📡 API

### 번역 및 문화 분석

```http
POST /api/translations/analyze
```

Request Example:

```json
{
  "originalText": "자료 빨리 보내주세요.",
  "sourceLanguage": "ko",
  "targetLanguage": "en",
  "targetCountry": "US",
  "situation": "BUSINESS",
  "relationshipType": "CLIENT",
  "communicationPurpose": "REQUEST",
  "requestedTone": "POLITE"
}
```

---

### 번역 기록 조회

```http
GET /api/translations
```

저장된 번역 기록 목록을 조회합니다.

---

### 번역 기록 개별 조회

```http
GET /api/translations/{id}
```

특정 번역 기록의 상세 정보를 조회합니다.

---

### 번역 기록 삭제

```http
DELETE /api/translations/{id}
```

특정 번역 기록을 삭제합니다.

---

## 🗂 Project Structure

```text
src/main/java/com/malgo/backend
│
├── ai
│   └── OpenAiClient.java
│
├── controller
│   └── TranslationController.java
│
├── dto
│   ├── TranslationRequest.java
│   ├── TranslationResponse.java
│   ├── TranslationHistoryResponse.java
│   ├── CultureWarningResponse.java
│   └── ToneScores.java
│
├── entity
│   ├── Translation.java
│   ├── TranslationResult.java
│   └── CultureWarning.java
│
├── repository
│   ├── TranslationRepository.java
│   ├── TranslationResultRepository.java
│   └── CultureWarningRepository.java
│
└── service
    └── TranslationService.java
```

---

## ⚙️ Environment Variables

OpenAI API Key와 데이터베이스 정보는 GitHub에 직접 업로드하지 않습니다.

환경에 맞게 다음 값을 설정해야 합니다.

```properties
openai.api-key=${OPENAI_API_KEY}
openai.model=gpt-5-mini
```

> ⚠️ API Key와 데이터베이스 비밀번호 등의 민감한 정보는 절대 GitHub Repository에 커밋하지 않습니다.

---

## 🚀 Run

프로젝트를 clone 합니다.

```bash
git clone <repository-url>
```

프로젝트 디렉터리로 이동합니다.

```bash
cd <project-directory>
```

환경 변수와 MySQL 연결 정보를 설정한 뒤 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

Windows:

```bash
gradlew.bat bootRun
```

기본 서버 주소:

```text
http://localhost:8080
```

---

## 🧪 API Test

API 테스트는 Postman을 사용합니다.

현재 주요 테스트 항목:

- 한국어 → 영어 번역
- 한국어 → 일본어 번역
- 영어 → 일본어 번역
- 비즈니스 / 일상 상황
- CLIENT / COWORKER / FRIEND 관계
- POLITE / CASUAL 말투
- SAFE / CAUTION / HIGH 위험도
- 압박 표현 감지
- 비꼼 표현 감지
- 완곡한 거절 분석
- 복합 위험 표현 분석
- 상황과 관계에 따른 동일 문장의 위험도 변화

---

## 📈 Current Progress

- [x] Spring Boot 프로젝트 구성
- [x] MySQL 연결
- [x] 번역 관련 Entity 구성
- [x] Repository 구성
- [x] 번역 요청/결과 DB 저장
- [x] 번역 기록 조회
- [x] 번역 기록 개별 조회
- [x] 번역 기록 삭제
- [x] OpenAI API 연동
- [x] Structured Outputs 적용
- [x] 문화 맞춤 번역
- [x] Tone 분석
- [x] 문화적 위험 표현 분석
- [x] 관계/상황/말투 기반 번역
- [ ] 예외 처리 개선
- [ ] 요청 데이터 Validation
- [ ] 테스트 코드 작성
- [ ] 프론트엔드 연동
- [ ] 서버 배포

---

## 🔐 Security

다음 정보는 Repository에 업로드하지 않습니다.

```text
OpenAI API Key
Database Password
환경 변수 및 기타 Secret 정보
```

민감한 설정값이 포함된 파일은 `.gitignore`를 통해 관리합니다.

---

## 👥 Team

Malgo Team

Backend / Frontend / Design 협업으로 개발 중입니다.
