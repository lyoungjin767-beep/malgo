# Malgo Backend API 명세서

> 기준 코드: Controller, DTO, `SecurityConfig`, Service 구현 (`src/main/java`)
> 검토일: 2026-08-17 · 기본 주소: `http://localhost:8081` · JSON 요청: `Content-Type: application/json`

## 1. 공통 규칙

| 표기 | JSON 타입 | 설명 |
|---|---|---|
| `Long`, `int`, `long` | number | 정수 ID 또는 수치 |
| `double` | number | 실수 |
| `LocalDateTime` | string | 타임존 없는 ISO-8601. 예: `2026-08-17T14:30:15.123456` |
| `Enum` | string | 표에 열거된 대문자 값 |
| `Set<T>` | array | 중복 없는 배열, 응답 순서 비보장 |
| `Map<String, Long/Double>` | object | 동적 문자열 키 객체 |

- `필수`는 DTO validation 기준이다. `@NotBlank`는 `null`, 빈 값, 공백을 모두 거부한다.
- `@Size`가 없는 문자열도 DB 길이를 넘으면 저장 시 실패할 수 있다.
- ID 경로 변수는 숫자여야 하나 양수 검증은 없다.
- 응답의 `nullable`은 정상 처리 중에도 `null`일 수 있는 값이다.

### 인증·회원 식별

로그인은 JWT가 아닌 서버 HTTP 세션이다. 로그인 성공 시 `JSESSIONID`를 설정하며 쿠키와 서버 세션은 30일간 유지된다. 브라우저는 보호 API 호출 시 `credentials: "include"`(Axios: `withCredentials: true`)를 사용한다. 앱을 다시 열 때 `GET /api/v1/auth/me`를 호출해 유효한 세션의 회원 ID를 복원한다. CSRF는 꺼져 있고 CORS Origin은 `http://localhost:3000`만 허용한다. 세션·쿠키 정책과 프런트 연동 예시는 [자동 로그인 API 명세서](AUTO_LOGIN_SPEC.md)를 참고한다.

| 영역 | 필터 접근 | 실제 식별/소유권 확인 |
|---|---|---|
| `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` | 공개 | 없음 |
| `GET /api/v1/auth/me` | 세션 필요 | 현재 로그인 회원 ID 반환 |
| `/api/v1/chat` | 세션 필요 | 회원 ID 비교 없음 |
| `/api/translations/**` | 세션 필요 | 요청/경로 `memberId` = 세션 회원 ID |
| `/api/partners/**` | `permitAll` | 컨트롤러에서 세션 회원 ID와 경로 `memberId` 비교 |
| `/api/conversations/**` | `permitAll` | 컨트롤러에서 세션 회원 ID와 body/path `memberId` 비교 |
| `/api/conversation-messages/**` | 공개 | 인증·메시지 소유자 검사 없음 |
| `/api/members/*/membership` | 공개 | 인증·회원 소유자 검사 없음 |
| `/api/v1/customization/**` | 세션 필요 | `X-Member-Id`와 세션 회원의 일치 여부를 검사하지 않음 |
| `/api/v1/subscription/**` | 세션 필요 | `X-Member-Id`와 세션의 로그인 회원 ID가 반드시 일치해야 함 |

AI 상대/대화방은 필터 설정상 공개지만 컨트롤러의 `MemberAuthorizationService`를 거쳐야 한다. 로그인하지 않은 요청은 컨트롤러에서 `401`으로 처리되며, 번역·구독처럼 필터가 보호하는 경로도 세션이 없으면 필터에서 `401`으로 차단한다.

`Member.membership`과 `Subscription`은 별도 상태지만, 구독 전환 API가 두 상태를 함께 갱신한다.

| 상태 | 변경 API | 사용처 |
|---|---|---|
| `membership` boolean | `POST /api/members/{memberId}/membership`, 구독 프리미엄 전환/취소 | 무료 채팅 8회, 프리미엄 언어, 커스텀 AI |
| 구독 `plan/status` | `/api/v1/subscription/me/*` | 커스터마이징 수정 권한 |

회원가입은 `FREE/ACTIVE` 구독을 생성하지만 `membership=false`로 시작한다. `PATCH /api/v1/subscription/me/premium`은 `PREMIUM/ACTIVE`와 `membership=true`를 함께 설정하고, `PATCH /api/v1/subscription/me/cancel`은 `CANCELED`와 `membership=false`를 함께 설정한다. 반면 `POST /api/members/{memberId}/membership`은 멤버십만 활성화하며 구독 상태는 변경하지 않는다.

### 구독 전환 호출 흐름

현재 서버에는 PG 결제 검증·웹훅이 없으므로, 프론트가 결제 성공으로 판단한 뒤 아래 프리미엄 전환 API를 호출하는 방식이다. `GET /api/v1/subscription/me`은 조회(구독이 없으면 FREE 생성)만 하므로 권한을 활성화하지 않는다.

```javascript
await fetch("http://localhost:8081/api/v1/subscription/me/premium", {
  method: "PATCH",
  credentials: "include",
  headers: { "X-Member-Id": String(memberId) }
});
```

성공하면 `SubscriptionResponse.plan`은 `PREMIUM`, `status`는 `ACTIVE`가 되며, 채팅·프리미엄 언어·커스텀 AI가 확인하는 `membership`도 `true`가 된다. 운영 결제 연동 시에는 이 상태 변경을 클라이언트가 직접 호출하지 않고, PG 웹훅 서명 검증 후 서버에서 실행해야 한다.

### 오류

`GlobalExceptionHandler`가 처리한 오류 형식이다. `code`는 멤버십 필요 오류에서만 존재한다.

```json
{
  "timestamp": "2026-08-17T14:30:15.123456",
  "status": 403,
  "error": "Forbidden",
  "code": "MEMBERSHIP_REQUIRED",
  "message": "커스텀 AI 생성은 멤버십이 필요합니다."
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `timestamp` | string(LocalDateTime) | 서버 오류 처리 시각 |
| `status` | number | HTTP 상태 |
| `error` | string | HTTP 오류명 |
| `code` | string, nullable | 멤버십 제한이면 `MEMBERSHIP_REQUIRED` |
| `message` | string | 상세 오류 |

| 상태 | 대표 조건 |
|---:|---|
| 400 | DTO 검증 실패, 요약할 메시지 없음 등의 `IllegalStateException` |
| 401 | 로그인 실패, 세션 없음 또는 만료 |
| 403 | 회원/리소스 소유권 불일치, 구독 API의 `X-Member-Id` 불일치, 기본 AI 변경, 멤버십·구독 제한 |
| 404 | 회원·번역·대화·AI·메모·요약 없음 (`IllegalArgumentException`도 404) |
| 500 | OpenAI, DB, 직렬화 등 미처리 오류 |

JSON 문법/enum 역직렬화/필수 헤더 누락/잘못된 HTTP 메서드 등은 Spring 기본 오류 응답일 수 있다.

## 2. API 목록

| 영역 | Method | Path | 성공 |
|---|---|---|---:|
| 인증 | POST | `/api/v1/auth/signup` | 201 |
| 인증 | POST | `/api/v1/auth/login` | 200 |
| 인증 | GET | `/api/v1/auth/me` | 200 |
| 인증 | POST | `/api/v1/auth/logout` | 204 |
| 채팅 | POST | `/api/v1/chat` | 200 |
| 번역 | POST | `/api/translations/analyze` | 200 |
| 번역 | GET | `/api/translations/member/{memberId}` | 200 |
| 번역 | GET, DELETE | `/api/translations/member/{memberId}/{id}` | 200, 204 |
| 번역 메모 | PUT, GET, DELETE | `/api/translations/member/{memberId}/{id}/memo` | 200, 200, 204 |
| 번역 | GET | `/api/translations/member/{memberId}/recent` | 200 |
| 번역 | GET | `/api/translations/member/{memberId}/statistics` | 200 |
| AI 상대 | GET, POST | `/api/partners/member/{memberId}` | 200 |
| AI 상대 | GET, PUT, DELETE | `/api/partners/member/{memberId}/{id}` | 200, 200, 204 |
| 대화 | POST | `/api/conversations` | 200 |
| 대화 | GET | `/api/conversations/member/{memberId}` | 200 |
| 대화 | GET, DELETE | `/api/conversations/member/{memberId}/{id}` | 200, 204 |
| 대화 | POST, GET | `/api/conversations/member/{memberId}/{id}/messages` | 200 |
| 대화 | POST | `/api/conversations/member/{memberId}/{id}/summary` | 200 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}/summaries` | 200 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}/summary/latest` | 200 |
| 대화 | GET | `/api/conversations/member/{memberId}/statistics` | 200 |
| 대화 메시지 메모 | PUT, GET, DELETE | `/api/conversation-messages/{messageId}/memo` | 200, 200, 204 |
| 멤버십 | POST, GET | `/api/members/{memberId}/membership` | 200 |
| 커스터마이징 | GET, PUT | `/api/v1/customization/me` | 200 |
| 구독 | GET | `/api/v1/subscription/me` | 200 |
| 구독 | PATCH | `/api/v1/subscription/me/premium` | 200 |
| 구독 | PATCH | `/api/v1/subscription/me/cancel` | 200 |

## 3. 모델 명세

### 인증·채팅

#### SignupRequest

| 요청 필드 | 타입 | 필수 | 제약 |
|---|---|---:|---|
| `username` | string | 예 | 공백 불가, 최대 30자, 중복 불가 |
| `password` | string | 예 | 공백 불가, 8~100자 |
| `passwordConfirm` | string | 예 | 공백 불가, `password`와 동일 |

#### LoginRequest

| 요청 필드 | 타입 | 필수 | 제약 |
|---|---|---:|---|
| `username` | string | 예 | 공백 불가 |
| `password` | string | 예 | 공백 불가 |

#### ChatRequest / ChatResponse

| 모델 | 필드 | 타입 | 필수/nullable | 설명 |
|---|---|---|---|---|
| 요청 | `message` | string | 필수 | 공백 불가, 사용자 메시지 |
| 응답 | `answer` | string | nullable 아님 | 선택한 말투로 생성한 AI 답변. 출력이 없으면 빈 문자열 가능 |

`POST /api/v1/chat`은 로그인한 회원의 저장된 커스터마이징 `speechStyles`를 읽어 AI 프롬프트에 적용한다. 저장된 커스터마이징이 없으면 `PLAIN`으로 답변한다.

### AI 상대

`AiPartnerCreateRequest`, `AiPartnerUpdateRequest`는 동일한 필드를 사용한다. PUT은 부분 수정이 아니다.

| 요청 필드 | 타입 | 필수 | 제약/설명 |
|---|---|---:|---|
| `name` | string | 예 | 공백 불가, DB 최대 50자 |
| `targetCountry` | string | 예 | 공백 불가, DB 최대 10자 |
| `targetLanguage` | string(enum) | 예 | `EN`, `JA`, `ZH`, `VI`, `ES`, `DE` |
| `relationshipType` | string | 예 | 공백 불가, DB 최대 30자 |
| `ageGroup` | string | 아니오 | DB 최대 30자 |
| `gender` | string | 아니오 | DB 최대 20자 |
| `speechStyle` | string(enum) | 예 | AI 답변 어투. `FORMAL`, `POLITE`, `FRIENDLY`, `WARM`, `PLAYFUL`, `PLAIN`, `SINCERE`, `EMOTIONAL`, `DIALECT` |
| `characteristic` | string | 아니오 | TEXT 특징 설명 |

#### AI 답변 말투 적용

`speechStyle`은 단순 저장값이 아니라, 메시지 전송 시 생성되는 AI 답변의 실제 어투를 제어한다.
제목을 제외한 `의미`, `맥락 설명`, `사용 예시`, `추천 표현`의 모든 자연어 문장에 선택한 어투 하나를 처음부터 끝까지 적용한다.

| 값 | AI 답변 적용 방식 |
|---|---|
| `FORMAL` | 공식적이고 전문적인 격식체 |
| `POLITE` | 부드럽고 예의 있는 정중체 |
| `FRIENDLY` | 친근하고 편안한 대화체 |
| `WARM` | 따뜻하고 배려하는 다정체 |
| `PLAYFUL` | 무례하지 않은 가볍고 유쾌한 장난체 |
| `PLAIN` | 과장 없이 간결한 담백체 |
| `SINCERE` | 진솔하고 진정성 있는 어조 |
| `EMOTIONAL` | 감정과 분위기가 느껴지는 감성체 |
| `DIALECT` | 한국어 설명을 포함한 모든 한국어 자연어 문장을 실제 한국어 사투리 어조로 작성. 지역이 지정되지 않아도 이해하기 쉬운 자연스러운 사투리를 일관되게 사용하며, 업무 상황에서도 사투리 어조는 유지 |

#### AiPartnerResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | AI 상대 ID |
| `name` | string | 아니오 | 이름 |
| `targetCountry` | string | 아니오 | 대상 국가 |
| `targetLanguage` | string(enum) | 아니오 | AI 대화 언어 |
| `relationshipType` | string | 아니오 | 관계 |
| `ageGroup` | string | 예 | 연령대 |
| `gender` | string | 예 | 성별 |
| `speechStyle` | string | 예 | 말투 |
| `characteristic` | string | 예 | 특징 |
| `custom` | boolean | 아니오 | 기본 AI `false`, 회원 생성 AI `true` |

### 대화

#### ConversationCreateRequest

| 요청 필드 | 타입 | 필수 | 제약/처리 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 세션 회원 ID와 같아야 함 |
| `aiPartnerId` | number(Long) | 아니오 | AI 상대 선택. 없으면 직접 설정 상대 |
| `situation` | string | 예 | 공백 불가, DB 최대 30자 |
| `field` | string(enum) | 예 | `IT_DEVELOPMENT`, `DESIGN`, `MARKETING`, `SALES`, `FINANCE` |
| `targetLanguage` | string(enum) | 예 | `EN`, `JA`, `ZH`, `VI`, `ES`, `DE`; AI 상대 선택 시에도 DTO상 필수이나 상대 언어가 실제로 사용됨 |
| `targetCountry` | string | 조건부 | AI 상대 미선택 시 공백 불가, DB 최대 10자 |
| `relationshipType` | string | 아니오 | DB 최대 30자 |
| `ageGroup` | string | 아니오 | DB 최대 30자 |
| `speechStyle` | string(enum) | 아니오 | 직접 설정 대화의 AI 답변 어투. `aiPartnerId`가 있으면 이 값은 무시되고 선택한 AI 상대의 `speechStyle`을 사용 |
| `characteristic` | string | 아니오 | TEXT 특징 |

비멤버십 회원의 `targetLanguage`가 `VI`, `ES`, `DE`이면 `403 MEMBERSHIP_REQUIRED`이다. AI 상대 선택 시 직접 설정 필드 5개는 사용되지 않는다.

#### ConversationResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 직접 설정이면 `null` |
| `aiPartnerName` | string | 예 | 직접 설정이면 `null` |
| `situation` | string | 아니오 | 상황 |
| `field` | string | 아니오 | 분야 |
| `targetLanguage` | string | 아니오 | 저장된 대화 언어 |

#### ConversationMessageRequest / Response

| 모델 | 필드 | 타입 | 필수/nullable | 설명 |
|---|---|---|---|---|
| 요청 | `content` | string | 필수 | 공백 불가 |
| 응답 | `id` | number(Long) | 아니오 | 메시지 ID |
| 응답 | `senderType` | string | 아니오 | `USER` 또는 `ASSISTANT` |
| 응답 | `content` | string | 아니오 | 메시지 내용 |
| 응답 | `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |

#### ConversationAnalysisResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `recommendedTranslation` | string | 아니오 | 권장 번역/표현 |
| `requestClarity` | number(int) | 아니오 | 요청 명확성 점수 |
| `businessTone` | number(int) | 아니오 | 업무 말투 점수 |
| `intentDelivery` | number(int) | 아니오 | 의도 전달 점수 |
| `culturalAppropriateness` | number(int) | 아니오 | 문화 적절성 점수 |
| `ambiguity` | number(int) | 아니오 | 모호성 점수 |

점수 범위는 코드에서 제한하지 않는다.

#### ConversationChatResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `userMessage` | ConversationMessageResponse | 아니오 | 저장된 사용자 메시지 |
| `assistantMessage` | ConversationMessageResponse | 아니오 | 자동 생성·저장된 AI 답변. 대화방 또는 선택한 AI 상대의 `speechStyle`을 적용 |
| `analysis` | ConversationAnalysisResponse | 아니오 | AI 답변 분석 |

#### ConversationMessageMemoRequest / Response

| 모델 | 필드 | 타입 | 필수/nullable | 설명 |
|---|---|---|---|---|
| 요청 | `content` | string | validation 없음 | 빈 문자열도 컨트롤러는 수용. DB 최대 1,000자, `null` 저장은 실패 가능 |
| 응답 | `id` | number(Long) | 아니오 | 메모 ID |
| 응답 | `conversationMessageId` | number(Long) | 아니오 | 연결된 AI 답변 ID |
| 응답 | `content` | string | 아니오 | 메모 내용 |
| 응답 | `createdAt` | string(LocalDateTime) | 아니오 | 최초 작성 시각 |
| 응답 | `updatedAt` | string(LocalDateTime) | 아니오 | 최근 수정 시각 |

메모는 `ASSISTANT` 메시지에만 하나씩 생성할 수 있다.

#### ConversationMessageDetailResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 메시지 ID |
| `senderType` | string | 아니오 | `USER` 또는 `ASSISTANT` |
| `content` | string | 아니오 | 메시지 |
| `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |
| `analysis` | ConversationAnalysisResponse | 예 | 보통 USER 메시지는 `null` |
| `memo` | ConversationMessageMemoResponse | 예 | AI 메모, 없으면 `null` |

#### ConversationListResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `conversationId` | number(Long) | 아니오 | 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 직접 설정이면 `null` |
| `aiPartnerName` | string | 아니오 | 직접 설정이면 `직접 설정 상대` |
| `targetCountry` | string | 예 | 대상 국가 |
| `relationshipType` | string | 예 | 관계 |
| `situation` | string | 예 | 상황 |
| `field` | string | 예 | 분야 |
| `lastMessage` | string | 예 | 최근 메시지, 없으면 `null` |
| `updatedAt` | string(LocalDateTime) | 아니오 | 최근 활동 시각 |

#### ConversationDetailResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `conversationId` | number(Long) | 아니오 | 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 직접 설정이면 `null` |
| `aiPartnerName` | string | 아니오 | 직접 설정이면 `직접 설정 상대` |
| `targetCountry`, `relationshipType`, `ageGroup`, `speechStyle`, `characteristic` | string | 예 | AI 상대 또는 직접 설정 정보 |
| `situation`, `field` | string | 예 | 대화 상황과 분야 |
| `createdAt`, `updatedAt` | string(LocalDateTime) | 아니오 | 생성/최근 활동 시각 |
| `messages` | ConversationMessageResponse[] | 아니오 | 오래된 순. 분석·메모는 미포함 |

#### ConversationSummaryResponse / StatisticsResponse

| 모델 | 응답 필드 | 타입 | nullable | 설명 |
|---|---|---|---|---|
| 요약 | `id` | number(Long) | 아니오 | 요약 ID |
| 요약 | `conversationId` | number(Long) | 아니오 | 대화방 ID |
| 요약 | `summary` | string | 아니오 | AI 요약 |
| 요약 | `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |
| 통계 | `totalCount` | number(long) | 아니오 | 전체 대화방 수 |
| 통계 | `counts` | object | 아니오 | **언어별** 수. 예: `{ "EN": 3 }` |
| 통계 | `percentages` | object | 아니오 | 언어별 0~100 비율, 소수 첫째 자리 반올림 |

### 번역

#### TranslationRequest

| 요청 필드 | 타입 | 필수 | 제약/설명 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 세션 회원 ID와 같아야 함 |
| `originalText` | string | 예 | 공백 불가, TEXT |
| `sourceLanguage`, `targetLanguage`, `targetCountry` | string | 예 | 공백 불가, DB 최대 10자 |
| `situation` | string | 예 | 공백 불가, DB 최대 30자 |
| `relationshipType`, `communicationPurpose`, `requestedTone` | string | 아니오 | DB 최대 30자 |

언어·국가·상황의 enum Pattern 검증은 없다.

#### ToneScores / CultureWarningResponse

| 모델 | 필드 | 타입 | nullable | 설명 |
|---|---|---|---|---|
| ToneScores | `friendliness`, `politeness`, `directness`, `aggression`, `burden`, `professionalism`, `naturalness` | number(int) | 아니오 | 각 말투 점수 |
| Warning | `expression` | string | 아니오 | 주의 표현 |
| Warning | `category` | string | 예 | 경고 분류 |
| Warning | `riskLevel` | string | 아니오 | 위험도 |
| Warning | `reason` | string | 아니오 | 사유 |
| Warning | `alternativeExpression` | string | 예 | 대체 표현 |
| Warning | `startIndex`, `endIndex` | number(Integer) | 예 | 원문 위치 |

#### TranslationResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `literalTranslation`, `naturalTranslation` | string | 예 | 직역, 자연스러운 번역 |
| `culturalTranslation` | string | 아니오 | 문화 반영 번역 |
| `culturalExplanation` | string | 예 | 문화 설명 |
| `overallRiskLevel` | string | 예 | 전체 위험도 |
| `toneScores` | ToneScores | 아니오 | 7개 점수 |
| `warnings` | CultureWarningResponse[] | 예 | 경고 목록. 빈 배열 또는 `null` 가능 |

#### TranslationHistoryResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 번역 ID |
| `originalText`, `sourceLanguage`, `targetLanguage`, `targetCountry`, `situation` | string | 아니오 | 저장된 번역 요청 정보 |
| `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |

#### TranslationDetailResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id`, `originalText` | number(Long), string | 아니오 | 번역 ID와 원문 |
| `literalTranslation`, `naturalTranslation`, `culturalExplanation`, `overallRiskLevel` | string | 예 | 번역/문화 결과 |
| `culturalTranslation` | string | 아니오 | 문화 반영 번역 |
| `toneScores` | ToneScores | 아니오 | 말투 점수 |
| `warnings` | CultureWarningResponse[] | 아니오 | 경고 없으면 `[]` |
| `memo` | string | 예 | 메모 없으면 `null` |
| `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |

#### TranslationMemoRequest / Response

| 모델 | 필드 | 타입 | 필수/nullable | 설명 |
|---|---|---|---|---|
| 요청 | `content` | string | 필수 | 공백 불가, TEXT |
| 응답 | `id`, `translationId` | number(Long) | 아니오 | 메모와 번역 ID |
| 응답 | `content` | string | 아니오 | 메모 |
| 응답 | `createdAt`, `updatedAt` | string(LocalDateTime) | 아니오 | 최초/최근 저장 시각 |

#### MyPageTranslationResponse / TranslationStatisticsResponse

| 모델 | 필드 | 타입 | nullable | 설명 |
|---|---|---|---|---|
| 최근 번역 | `translationId` | number(Long) | 아니오 | 번역 ID |
| 최근 번역 | `originalText`, `recommendedTranslation` | string | 아니오 | 원문, `culturalTranslation` |
| 최근 번역 | `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |
| 최근 번역 | `hasMemo` | boolean | 아니오 | 메모 존재 여부 |
| 통계 | `totalCount` | number(long) | 아니오 | 전체 번역 수 |
| 통계 | `situations` | object | 아니오 | 상황별 개수 |
| 통계 | `percentages` | object | 아니오 | 상황별 0~100 비율 |

### 멤버십·구독·커스터마이징

#### MembershipStatusResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `membership` | boolean | 아니오 | 멤버십 상태 |
| `chatCount` | number(int) | 아니오 | 성공한 무료 대화 전송 횟수 |
| `freeChatLimit` | number(int) | 아니오 | 항상 `8` |

#### SubscriptionResponse

| 응답 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id`, `memberId` | number(Long) | 아니오 | 구독/회원 ID |
| `plan` | string(enum) | 아니오 | `FREE`, `PREMIUM` |
| `status` | string(enum) | 아니오 | `ACTIVE`, `EXPIRED`, `CANCELED` |
| `startedAt` | string(LocalDateTime) | 아니오 | 현재 플랜 시작 시각 |
| `expiresAt` | string(LocalDateTime) | 예 | 종료 시각. 프리미엄 활성화도 현재 `null` 저장 |

#### CustomizationRequest / Response

| 필드 | 타입 | 요청 필수 | 응답 nullable | 값/설명 |
|---|---|---:|---:|---|
| `id` | number(Long) | - | 아니오 | 응답 전용 ID |
| `memberId` | number(Long) | - | 아니오 | 응답 전용 회원 ID |
| `aiPersona` | string(enum) | 예 | 아니오 | `TOM`, `KASH`, `SANA` |
| `expression` | string(enum) | 예 | 아니오 | `NEUTRAL`, `SMILE`, `OTHER` |
| `targetLanguage` | string(enum) | 예 | 아니오 | `EN`, `JA`, `ZH`, `VI`, `ES`, `DE` |
| `relationships` | string[] | 예, 1개+ | 아니오 | `LOVER`, `SPOUSE`, `FAMILY`, `TEACHER`, `ACQUAINTANCE`, `JUNIOR`, `US_CLIENT`, `JP_FRIEND`, `VN_BOSS` |
| `gender` | string(enum) | 예 | 아니오 | `FEMALE`, `MALE` |
| `speechStyles` | string[] | 예, 정확히 1개 | 아니오 | 일반 채팅 AI 답변 말투. `FORMAL`, `POLITE`, `FRIENDLY`, `WARM`, `PLAYFUL`, `PLAIN`, `SINCERE`, `EMOTIONAL`, `DIALECT` 중 하나 |

#### 커스터마이징 말투와 일반 채팅 연동

`speechStyles`는 JSON 배열 형식이지만 말투 선택 화면의 단일 선택과 맞추기 위해 원소를 정확히 하나만 받아야 한다. 둘 이상 보내면 `400 Bad Request`와 `말투는 하나만 선택해야 합니다.`를 반환한다.

예를 들어 사투리를 선택해 저장할 때는 아래처럼 요청한다.

```json
{
  "aiPersona": "TOM",
  "expression": "NEUTRAL",
  "targetLanguage": "EN",
  "relationships": ["ACQUAINTANCE"],
  "gender": "MALE",
  "speechStyles": ["DIALECT"]
}
```

저장한 뒤 `POST /api/v1/chat`을 호출하면 로그인한 회원의 `DIALECT` 설정을 읽어 한국어 답변 전체를 사투리 어조로 생성한다. `SINCERE`를 저장하면 진솔하고 진정성 있는 어조로, 나머지 값도 위 말투 표의 기준으로 생성한다. 커스터마이징이 아직 없으면 일반 채팅은 `PLAIN`을 사용한다.

## 4. 엔드포인트별 동작

### 인증·채팅

| Endpoint | 요청 | 성공 응답 | 비고 |
|---|---|---|---|
| `POST /api/v1/auth/signup` | SignupRequest | `201`, JSON number 회원 ID | BCrypt 저장 + FREE/ACTIVE 구독 생성. 세션 미생성 |
| `POST /api/v1/auth/login` | LoginRequest | `200`, JSON number 회원 ID | `JSESSIONID` 설정/교체. 쿠키와 세션은 30일 유지 |
| `GET /api/v1/auth/me` | 본문 없음 | `200`, JSON number 회원 ID | 자동 로그인 상태 확인. 유효 세션이 없으면 `401` |
| `POST /api/v1/auth/logout` | 본문 없음 | `204`, 본문 없음 | 세션 무효화, 쿠키 삭제 요청 |
| `POST /api/v1/chat` | ChatRequest | `200`, ChatResponse | 세션 필요, DB 저장 없음. 로그인 회원의 커스터마이징 말투를 적용하며 없으면 `PLAIN` |

회원가입 비밀번호 불일치·아이디 중복은 현재 `404`이다. 로그인 자격 증명 오류는 `401`이다.

### 번역

모든 API는 세션 + 로그인 회원과 같은 `memberId`가 필요하다.

| Endpoint | 요청/경로 | 성공 응답 | 비고 |
|---|---|---|---|
| `POST /api/translations/analyze` | TranslationRequest | `200` TranslationResponse | 이력·결과·경고 저장, 응답에 번역 ID는 없음 |
| `GET /api/translations/member/{memberId}` | `memberId` | `200` TranslationHistoryResponse[] | 최신순, 없으면 `[]` |
| `GET /api/translations/member/{memberId}/{id}` | 번역 `id` | `200` TranslationDetailResponse |  |
| `DELETE /api/translations/member/{memberId}/{id}` | 번역 `id` | `204` | 결과·경고·메모도 삭제 |
| `PUT /api/translations/member/{memberId}/{id}/memo` | TranslationMemoRequest | `200` TranslationMemoResponse | 생성 또는 갱신, 번역당 하나 |
| `GET /api/translations/member/{memberId}/{id}/memo` | 번역 `id` | `200` TranslationMemoResponse | 없으면 404 |
| `DELETE /api/translations/member/{memberId}/{id}/memo` | 번역 `id` | `204` | 없으면 404 |
| `GET /api/translations/member/{memberId}/recent` | `memberId` | `200` MyPageTranslationResponse[] | 분석 결과 없는 이력은 제외 |
| `GET /api/translations/member/{memberId}/statistics` | `memberId` | `200` TranslationStatisticsResponse | 상황별 통계 |

### AI 상대

모든 API는 세션 + 로그인 회원과 같은 경로 `memberId`가 필요하다.

| Endpoint | 요청/경로 | 성공 응답 | 비고 |
|---|---|---|---|
| `GET /api/partners/member/{memberId}` | `memberId` | `200` AiPartnerResponse[] | 기본 AI + 해당 회원 커스텀 AI, 순서 비보장 |
| `POST /api/partners/member/{memberId}` | AiPartnerCreateRequest | `200` AiPartnerResponse | 멤버십 필요, `custom=true` |
| `GET /api/partners/member/{memberId}/{id}` | AI `id` | `200` AiPartnerResponse | 기본 AI 조회 가능, 커스텀은 소유자만 |
| `PUT /api/partners/member/{memberId}/{id}` | AiPartnerUpdateRequest | `200` AiPartnerResponse | 멤버십 필요. 선택 필드 누락은 `null` 덮어쓰기 |
| `DELETE /api/partners/member/{memberId}/{id}` | AI `id` | `204` | 연결 대화·메시지·요약도 삭제 |

기본 AI나 다른 회원 AI의 수정·삭제는 403이다.

### 대화방

모든 API는 세션 + 로그인 회원과 같은 body/path `memberId`가 필요하다.

| Endpoint | 요청/경로 | 성공 응답 | 비고 |
|---|---|---|---|
| `POST /api/conversations` | ConversationCreateRequest | `200` ConversationResponse | 직접 설정 시 `targetCountry` 필수 |
| `GET /api/conversations/member/{memberId}` | `memberId` | `200` ConversationListResponse[] | 최근 활동순, 없으면 `[]` |
| `GET /api/conversations/member/{memberId}/{id}` | 대화 `id` | `200` ConversationDetailResponse |  |
| `DELETE /api/conversations/member/{memberId}/{id}` | 대화 `id` | `204` | 메시지·요약도 삭제 |
| `POST /api/conversations/member/{memberId}/{id}/messages` | ConversationMessageRequest | `200` ConversationChatResponse | USER·AI 메시지 모두 저장. AI 답변에 대화방/AI 상대의 `speechStyle` 적용 |
| `GET /api/conversations/member/{memberId}/{id}/messages` | 대화 `id` | `200` ConversationMessageDetailResponse[] | 오래된 순 |
| `POST /api/conversations/member/{memberId}/{id}/summary` | 본문 없음 | `200` ConversationSummaryResponse | 메시지 없으면 400 |
| `GET /api/conversations/member/{memberId}/{id}/summaries` | 대화 `id` | `200` ConversationSummaryResponse[] | 최신순, 없으면 `[]` |
| `GET /api/conversations/member/{memberId}/{id}/summary/latest` | 대화 `id` | `200` ConversationSummaryResponse | 요약 없으면 404 |
| `GET /api/conversations/member/{memberId}/statistics` | `memberId` | `200` ConversationStatisticsResponse | targetLanguage별 통계 |

비멤버십은 성공한 메시지 전송마다 `chatCount`가 증가하며, 8회 이후 메시지 전송은 `403 MEMBERSHIP_REQUIRED`이다. `VI`, `ES`, `DE` 언어 및 커스텀 AI도 멤버십이 필요하다.

대화방 생성 시 선택한 `targetLanguage`와 `targetCountry`는 그대로 저장되며, 직접 설정 대화와 AI 상대 대화 모두 이후 AI 응답 생성에 사용된다.

### 대화 메시지 메모

인증 및 메시지 소유자 검사가 없다.

| Endpoint | 요청/경로 | 성공 응답 | 비고 |
|---|---|---|---|
| `PUT /api/conversation-messages/{messageId}/memo` | ConversationMessageMemoRequest | `200` ConversationMessageMemoResponse | 생성 또는 갱신. USER 메시지는 404 |
| `GET /api/conversation-messages/{messageId}/memo` | `messageId` | `200` ConversationMessageMemoResponse | 없으면 404 |
| `DELETE /api/conversation-messages/{messageId}/memo` | `messageId` | `204` | 없으면 404 |

### 멤버십

두 API는 공개이며 본문이 없다. `memberId`의 로그인 회원 여부를 확인하지 않는다.

| Endpoint | 경로 | 성공 응답 | 비고 |
|---|---|---|---|
| `POST /api/members/{memberId}/membership` | `memberId` | `200`, 본문 없음 | `membership=true` 설정. 구독은 변경하지 않음 |
| `GET /api/members/{memberId}/membership` | `memberId` | `200` MembershipStatusResponse |  |

> 주의: 위 공개 멤버십 활성화 API는 결제 검증이나 소유자 검증 없이 권한을 변경한다. 실제 결제 기능에서는 공개 API로 사용하면 안 된다.

### 커스터마이징·구독

모든 API는 유효 세션과 `X-Member-Id: <Long>` 헤더가 필요하다. 커스터마이징 API는 현재 헤더와 세션 회원 ID의 일치 여부를 검사하지 않지만, 구독 API는 반드시 일치해야 하며 불일치 시 `403`이다.

| Endpoint | 요청 | 성공 응답 | 비고 |
|---|---|---|---|
| `GET /api/v1/customization/me` | Header `X-Member-Id` | `200` CustomizationResponse | 저장값 없으면 404 |
| `PUT /api/v1/customization/me` | Header + CustomizationRequest | `200` CustomizationResponse | PREMIUM/ACTIVE/미만료 구독만 가능; 없으면 생성, 있으면 전체 교체. `speechStyles`는 정확히 하나여야 하며 이후 일반 채팅 AI 답변의 말투로 적용 |
| `GET /api/v1/subscription/me` | Header `X-Member-Id` (세션 회원과 일치) | `200` SubscriptionResponse | 구독 없으면 FREE/ACTIVE 자동 생성. 조회만 수행 |
| `PATCH /api/v1/subscription/me/premium` | Header (세션 회원과 일치), 본문 없음 | `200` SubscriptionResponse | `PREMIUM/ACTIVE`, 시작시각 갱신, `expiresAt=null`, `membership=true`; 결제 검증 없음 |
| `PATCH /api/v1/subscription/me/cancel` | Header (세션 회원과 일치), 본문 없음 | `200` SubscriptionResponse | plan은 PREMIUM으로 유지, `status=CANCELED`, `membership=false` |
