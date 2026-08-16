# Malgo Backend API 명세서

> 기준 코드: `src/main/java`의 Controller, Request/Response DTO, Service, Entity, Repository, 보안 설정, 전역 예외 처리기  
> 검토 기준: 2026-08-15, 현재 작업 트리(세션 인증 변경 포함)  
> 서버 기본 주소: `http://localhost:8081`  
> Content-Type: 요청 본문이 있는 API는 `application/json`

## 1. 공통 규칙

### 1.1 인증 및 회원 식별

현재 `SecurityConfig`의 **실제 접근 정책**은 다음과 같다.

| 대상 | 접근 정책 |
|---|---|
| 모든 `OPTIONS` 요청 | 공개 |
| `/api/v1/auth/**` | 공개 |
| 그 밖의 모든 API | 유효한 HTTP 세션 인증 필요 |

인증은 JWT가 아닌 **서버 HTTP 세션 방식**이다.

- 로그인 성공 시 Spring Security `Authentication`을 생성하고 `SecurityContext`를 HTTP 세션에 저장한다.
- 서버는 세션 쿠키 `JSESSIONID`를 발급한다. JWT와 `Authorization: Bearer` 헤더는 사용하지 않는다.
- 프론트는 로그인 요청과 이후 모든 보호 API 요청에 `credentials: "include"`(Axios는 `withCredentials: true`)를 사용해야 한다.
- 이미 세션이 있으면 로그인 성공 시 세션 ID를 변경해 세션 고정 공격을 방지한다.
- 회원가입만으로는 로그인 세션이 생성되지 않는다. 회원가입 후 로그인 API를 별도로 호출해야 한다.
- 세션이 없거나 만료되면 보호 API는 `401 Unauthorized`를 반환한다.
- 로그아웃은 세션을 무효화하고 인증 정보를 지우며 `JSESSIONID` 쿠키 삭제를 요청한다.
- 세션 만료 시간과 `JSESSIONID`의 `Secure`, `SameSite`, `HttpOnly`, Path 같은 쿠키 속성은 애플리케이션 코드에서 별도로 고정하지 않았으므로 서버·서블릿 컨테이너 설정을 따른다.

- `memberId`가 있는 API는 세션의 로그인 회원 ID와 URL 또는 요청 본문의 `memberId`가 반드시 같아야 한다. 다르면 `403 Forbidden`이다.
- 일치 여부를 통과한 뒤에도 번역, 대화방, 커스텀 AI 상대의 실제 소유권을 서비스에서 다시 검사한다.
- CSRF는 비활성화되어 있다.

#### 프론트 호출 예시

```javascript
const loginResponse = await fetch("http://localhost:8081/api/v1/auth/login", {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username, password })
});

const memberId = await loginResponse.json();

const historyResponse = await fetch(
  `http://localhost:8081/api/translations/member/${memberId}`,
  { credentials: "include" }
);
```

### 1.2 데이터 표기

| 표기 | JSON 타입 | 설명 |
|---|---|---|
| `Long` | number | 64비트 정수 ID |
| `String` | string | 문자열 |
| `boolean` | boolean | `true` 또는 `false` |
| `LocalDateTime` | string | 타임존이 없는 ISO-8601 형식. 예: `2026-08-15T13:25:10.123456` |
| `Map<String, Long>` | object | 동적인 문자열 키와 정수 값 |
| `Map<String, Double>` | object | 동적인 문자열 키와 실수 값 |

- 필수 여부는 컨트롤러의 `@Valid`와 DTO의 Bean Validation을 기준으로 한다.
- `필수` 문자열은 누락, `null`, 빈 문자열 `""`, 공백 문자열이 허용되지 않는다.
- `선택` 필드는 별도 서비스 검증이 없다면 누락하거나 `null`로 보낼 수 있다.
- 숫자 ID에는 양수 검증이 없지만, 존재하지 않는 값은 대부분 조회 과정에서 `404`가 된다.
- DTO에 `@Size`가 없어도 DB 컬럼 길이를 초과하면 저장 단계에서 DB 오류(`500` 가능)가 발생할 수 있다. 각 요청 표에 유효 DB 길이를 함께 기재했다.
- 응답의 선택 필드는 Jackson 기본 설정상 값이 없더라도 `null`로 포함될 수 있다.
- 목록 API는 결과가 없을 때 `null`이 아니라 빈 배열 `[]`을 반환한다.

### 1.3 공통 오류 응답

```json
{
  "timestamp": "2026-08-15T13:25:10.123456",
  "status": 404,
  "error": "Not Found",
  "message": "번역 기록을 찾을 수 없습니다. id=10"
}
```

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `timestamp` | string(LocalDateTime) | 아니오 | 오류 발생 서버 시각 |
| `status` | number | 아니오 | HTTP 상태 코드 |
| `error` | string | 아니오 | HTTP 오류 이름 |
| `message` | string | 아니오 | 상세 오류 메시지 |

| HTTP 상태 | 발생 조건 |
|---|---|
| `400 Bad Request` | DTO 검증 실패, 요약할 메시지가 없음, OpenAI 응답 본문/구조 해석 실패 등 `IllegalStateException` 발생 |
| `401 Unauthorized` | 아이디/비밀번호 불일치, 세션 없음 또는 만료 |
| `403 Forbidden` | 세션 회원 ID와 요청 `memberId` 불일치, 다른 회원의 리소스 접근, 기본 AI 상대 수정/삭제 시도 |
| `404 Not Found` | 회원/번역/대화방/AI 상대/메모/요약 없음. 현재 구현에서는 회원가입 중복, 비밀번호 확인 불일치, 직접 설정 국가 누락 같은 `IllegalArgumentException`도 404로 처리됨 |
| `415 Unsupported Media Type` | JSON 본문 API를 `application/json`이 아닌 타입으로 호출 |
| `500 Internal Server Error` | 별도 처리되지 않은 DB, OpenAI, 직렬화, 서버 오류 |

위 JSON 형식은 `GlobalExceptionHandler`가 처리한 오류에만 적용된다. 로그인 자격 증명 오류는 위 형식의 `401` JSON을 반환한다. 반면 세션이 없어 Spring Security가 컨트롤러 앞에서 차단한 `401`은 `HttpStatusEntryPoint` 응답이므로 본문이 없을 수 있다. JSON 문법 오류, 타입 불일치, 잘못된 HTTP 메서드, 지원하지 않는 Content-Type 등도 Spring/Spring Boot 기본 오류 형식일 수 있다. 검증 오류는 실패한 필드 중 첫 번째 메시지만 반환한다.

대표 인증·인가 오류:

| 조건 | 상태 | 응답 본문/`message` |
|---|---:|---|
| 아이디 없음 또는 비밀번호 오류 | 401 | 공통 오류 JSON, `아이디 또는 비밀번호가 올바르지 않습니다.` |
| 보호 API에 유효한 세션 없음 | 401 | Spring Security 필터 응답. 공통 오류 JSON 형식은 보장되지 않음 |
| 요청 `memberId`와 로그인 회원 ID 불일치 | 403 | 공통 오류 JSON, `로그인한 회원과 요청 회원이 일치하지 않습니다.` |
| 세션 principal에 대응하는 회원 정보 없음 | 403 | 공통 오류 JSON, `로그인한 회원 정보를 찾을 수 없습니다.` |

공통 오류 JSON 객체의 필드 순서는 계약하지 않는다.

### 1.4 CORS

- 허용 Origin: `http://localhost:3000`만 허용
- 허용 Method/Header: 모두 허용
- 자격 증명 허용: `true`. 브라우저가 `JSESSIONID`를 주고받을 수 있다.

## 2. API 요약

| 영역 | Method | Path | 접근 | 성공 | 설명 |
|---|---|---|---|---:|---|
| 인증 | POST | `/api/v1/auth/signup` | 공개 | 201 | 회원가입 |
| 인증 | POST | `/api/v1/auth/login` | 공개 | 200 | 로그인, 세션 생성 및 회원 ID 반환 |
| 인증 | POST | `/api/v1/auth/logout` | 공개 | 204 | 세션 로그아웃 |
| 채팅 | POST | `/api/v1/chat` | 세션 필요 | 200 | 저장하지 않는 단순 AI 채팅 |
| AI 상대 | GET | `/api/partners/member/{memberId}` | 세션 필요 | 200 | 사용 가능한 AI 상대 목록 |
| AI 상대 | POST | `/api/partners/member/{memberId}` | 세션 필요 | 200 | 커스텀 AI 상대 생성 |
| AI 상대 | GET | `/api/partners/member/{memberId}/{id}` | 세션 필요 | 200 | AI 상대 상세 |
| AI 상대 | PUT | `/api/partners/member/{memberId}/{id}` | 세션 필요 | 200 | 커스텀 AI 상대 수정 |
| AI 상대 | DELETE | `/api/partners/member/{memberId}/{id}` | 세션 필요 | 204 | 커스텀 AI 상대 삭제 |
| 대화 | POST | `/api/conversations` | 세션 필요 | 200 | 대화방 생성 |
| 대화 | GET | `/api/conversations/member/{memberId}` | 세션 필요 | 200 | 대화방 목록 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}` | 세션 필요 | 200 | 대화방 상세 |
| 대화 | DELETE | `/api/conversations/member/{memberId}/{id}` | 세션 필요 | 204 | 대화방 삭제 |
| 대화 | POST | `/api/conversations/member/{memberId}/{id}/messages` | 세션 필요 | 200 | 메시지 전송 및 AI 응답 생성 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}/messages` | 세션 필요 | 200 | 메시지 목록 |
| 대화 | POST | `/api/conversations/member/{memberId}/{id}/summary` | 세션 필요 | 200 | 대화 요약 생성 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}/summaries` | 세션 필요 | 200 | 저장된 요약 목록 |
| 대화 | GET | `/api/conversations/member/{memberId}/{id}/summary/latest` | 세션 필요 | 200 | 최근 요약 |
| 대화 | GET | `/api/conversations/member/{memberId}/statistics` | 세션 필요 | 200 | 상황별 대화 통계 |
| 번역 | POST | `/api/translations/analyze` | 세션 필요 | 200 | 번역 및 문화 분석 |
| 번역 | GET | `/api/translations/member/{memberId}` | 세션 필요 | 200 | 번역 기록 목록 |
| 번역 | GET | `/api/translations/member/{memberId}/{id}` | 세션 필요 | 200 | 번역 상세 |
| 번역 | DELETE | `/api/translations/member/{memberId}/{id}` | 세션 필요 | 204 | 번역 기록 삭제 |
| 번역 | PUT | `/api/translations/member/{memberId}/{id}/memo` | 세션 필요 | 200 | 메모 저장/수정 |
| 번역 | GET | `/api/translations/member/{memberId}/{id}/memo` | 세션 필요 | 200 | 메모 조회 |
| 번역 | DELETE | `/api/translations/member/{memberId}/{id}/memo` | 세션 필요 | 204 | 메모 삭제 |
| 번역 | GET | `/api/translations/member/{memberId}/recent` | 세션 필요 | 200 | 마이페이지 최근 번역 |
| 번역 | GET | `/api/translations/member/{memberId}/statistics` | 세션 필요 | 200 | 상황별 번역 통계 |

총 28개 경로다. 이 중 27개는 Controller 매핑이고, 로그아웃 1개는 Spring Security LogoutFilter가 처리한다.

## 3. 인증 API

### 3.1 회원가입

`POST /api/v1/auth/signup`

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `username` | string | 예 | 공백 불가, 최대 30자. 중복 불가 |
| `password` | string | 예 | 공백 불가, 8~100자. BCrypt로 해시해 저장 |
| `passwordConfirm` | string | 예 | 공백 불가, `password`와 정확히 일치해야 함 |

```json
{
  "username": "malgo01",
  "password": "password123!",
  "passwordConfirm": "password123!"
}
```

#### 성공 응답 — `201 Created`

응답 본문은 객체가 아니라 생성된 회원 ID 하나를 나타내는 JSON number다.
회원가입 성공만으로 로그인 세션은 생성되지 않는다.

```json
1
```

| 조건 | 상태 | `message` |
|---|---:|---|
| 요청 필드 검증 실패 | 400 | 해당 DTO 검증 메시지 |
| 비밀번호 확인 불일치 | 404 | `비밀번호와 비밀번호 확인이 일치하지 않습니다.` |
| 아이디 중복 | 404 | `이미 사용 중인 아이디입니다.` |

### 3.2 로그인

`POST /api/v1/auth/login`

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `username` | string | 예 | 공백 불가. 로그인 DTO 자체에는 최대 길이 검증이 없음 |
| `password` | string | 예 | 공백 불가 |

```json
{
  "username": "malgo01",
  "password": "password123!"
}
```

#### 성공 응답 — `200 OK`

응답 본문은 객체가 아니라 로그인한 회원 ID 하나를 나타내는 JSON number다. 동시에 서버가 `JSESSIONID` 세션 쿠키를 발급하거나 기존 세션의 ID를 교체한다.

```json
1
```

| 항목 | 값 |
|---|---|
| 응답 쿠키 | `JSESSIONID` |
| 인증 저장 위치 | 서버 HTTP 세션의 Spring Security `SecurityContext` |
| 클라이언트 후속 처리 | 모든 보호 요청에 `credentials: "include"` 또는 `withCredentials: true` 적용 |
| JWT/Authorization 헤더 | 사용하지 않음 |

아이디가 없거나 비밀번호가 틀리면 `401 Unauthorized`를 반환한다.

```json
{
  "timestamp": "2026-08-15T18:40:00.123456",
  "status": 401,
  "error": "Unauthorized",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

### 3.3 로그아웃

`POST /api/v1/auth/logout`

- 요청 본문 없음.
- 프론트는 로그인 때와 마찬가지로 `credentials: "include"` 또는 `withCredentials: true`를 사용해 현재 `JSESSIONID`를 전송해야 한다.
- 서버는 HTTP 세션을 무효화하고 Spring Security 인증을 지우며 `JSESSIONID` 쿠키 삭제를 요청한다.
- 성공: `204 No Content`, 응답 본문 없음.
- 세션이 이미 없더라도 성공 응답은 `204`이다.
- 보안 설정에서 POST 요청만 로그아웃으로 처리한다.

## 4. 단순 채팅 API

### 4.1 AI 채팅

`POST /api/v1/chat`

대화방이나 메시지를 DB에 저장하지 않고 입력 문자열을 OpenAI에 전달한다.
유효한 로그인 세션이 필요하다. `memberId`를 받지 않으므로 로그인한 모든 회원이 호출할 수 있다.

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `message` | string | 예 | 공백 불가, 사용자 입력 |

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `answer` | string | 아니오 | AI 응답. 출력 텍스트가 없으면 빈 문자열 가능 |

```json
{
  "answer": "안녕하세요. 무엇을 도와드릴까요?"
}
```

## 5. AI 대화 상대 API

### 5.1 공통 Path Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 요청 회원 ID. 로그인 세션의 회원 ID와 같아야 하며, 다르면 `403` |
| `id` | number(Long) | 해당 API만 | AI 상대 ID |

### 5.2 AI 상대 응답 객체

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | AI 상대 ID |
| `name` | string | 아니오 | 표시 이름, DB 최대 50자 |
| `targetCountry` | string | 아니오 | 국가 코드/값, DB 최대 10자. 예: `US`, `JP`, `VN` |
| `relationshipType` | string | 아니오 | 관계, DB 최대 30자. 예: `CLIENT`, `FRIEND`, `BOSS` |
| `ageGroup` | string | 예 | 연령대, DB 최대 30자. 예: `CHILD`, `TEENAGER`, `COLLEGE_STUDENT`, `WORKER`, `SENIOR` |
| `gender` | string | 예 | 성별, DB 최대 20자. 예: `FEMALE`, `MALE` |
| `speechStyle` | string | 예 | 말투, DB 최대 30자. 예: `CASUAL`, `POLITE`, `FRIENDLY` |
| `characteristic` | string | 예 | 자유 형식 특징 설명 |
| `custom` | boolean | 아니오 | 기본 제공이면 `false`, 회원 생성 상대이면 `true` |

```json
{
  "id": 4,
  "name": "Alex",
  "targetCountry": "US",
  "relationshipType": "CLIENT",
  "ageGroup": "WORKER",
  "gender": null,
  "speechStyle": "POLITE",
  "characteristic": "간결한 설명을 선호함",
  "custom": true
}
```

### 5.3 사용 가능한 AI 상대 목록 조회

`GET /api/partners/member/{memberId}`

- 요청 본문 없음.
- 성공 응답은 `200 OK`, 본문은 **AI 상대 응답 객체 배열**이다.
- 기본 AI(`member=null`)와 해당 회원이 생성한 커스텀 AI를 함께 반환한다.
- 정렬 조건은 Repository에 지정되어 있지 않으므로 반환 순서를 계약으로 가정하면 안 된다.
- URL의 `memberId`가 로그인 회원 ID와 다르거나 로그인 회원 정보를 더 이상 찾을 수 없으면 `403`.

### 5.4 커스텀 AI 상대 생성

`POST /api/partners/member/{memberId}`

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `name` | string | 예 | 공백 불가, AI 상대 이름. 유효 DB 최대 50자 |
| `targetCountry` | string | 예 | 공백 불가, 대상 국가. 유효 DB 최대 10자 |
| `relationshipType` | string | 예 | 공백 불가, 관계. 유효 DB 최대 30자 |
| `ageGroup` | string | 아니오 | 연령대. 유효 DB 최대 30자 |
| `gender` | string | 아니오 | 성별. 유효 DB 최대 20자 |
| `speechStyle` | string | 아니오 | 말투/대화 스타일. 유효 DB 최대 30자 |
| `characteristic` | string | 아니오 | 특징 설명 |

문자열 값에 대한 enum 검증은 없으므로 예시 외 값도 DTO 검증을 통과한다. 위 최대 길이는 Bean Validation이 아니라 DB 스키마 제약이다.

```json
{
  "name": "Alex",
  "targetCountry": "US",
  "relationshipType": "CLIENT",
  "ageGroup": "WORKER",
  "gender": null,
  "speechStyle": "POLITE",
  "characteristic": "간결한 설명을 선호함"
}
```

#### 성공 응답 — `200 OK`

생성된 **AI 상대 응답 객체**를 반환하며 `custom`은 `true`이다. URL의 `memberId`가 로그인 회원 ID와 다르거나 로그인 회원 정보를 더 이상 찾을 수 없으면 `403`.

### 5.5 AI 상대 상세 조회

`GET /api/partners/member/{memberId}/{id}`

- 요청 본문 없음.
- 성공 응답은 `200 OK`, 본문은 **AI 상대 응답 객체**이다.
- 기본 AI는 모든 로그인 회원이 사용할 수 있지만, URL의 `memberId`는 항상 로그인 세션의 회원 ID여야 한다.
- 커스텀 AI가 다른 회원 소유이면 `403`, AI 상대가 없으면 `404`.

### 5.6 커스텀 AI 상대 수정

`PUT /api/partners/member/{memberId}/{id}`

요청 필드와 성공 응답은 생성 API와 동일하다. PUT이므로 필수 3개 필드(`name`, `targetCountry`, `relationshipType`)를 모두 보내야 한다.

선택 필드를 누락하면 기존 값을 유지하지 않고 `null`로 덮어쓴다.

- 성공: `200 OK`, 수정된 **AI 상대 응답 객체**
- 기본 AI 수정 시도: `403`
- 다른 회원의 커스텀 AI 수정 시도: `403`
- AI 상대 없음: `404`

### 5.7 커스텀 AI 상대 삭제

`DELETE /api/partners/member/{memberId}/{id}`

- 성공: `204 No Content`, 응답 본문 없음.
- 연결된 대화방과 그 메시지·요약도 함께 삭제한다.
- 기본 AI 또는 다른 회원의 커스텀 AI는 `403`, AI 상대가 없으면 `404`.

## 6. 대화방 API

### 6.1 공통 Path Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 대화방 소유 회원 ID. 로그인 세션의 회원 ID와 같아야 하며, 다르면 `403` |
| `id` | number(Long) | 해당 API만 | 대화방 ID |

### 6.2 대화방 생성

`POST /api/conversations`

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 회원 ID, `null` 불가. 로그인 세션의 회원 ID와 같아야 하며, 다르면 `403` |
| `aiPartnerId` | number(Long) | 아니오 | 선택할 AI 상대 ID. 직접 설정이면 누락 또는 `null` |
| `situation` | string | 예 | 공백 불가, 대화 상황/주제. 유효 DB 최대 30자 |
| `field` | string | 예 | `IT_DEVELOPMENT`, `DESIGN`, `MARKETING`, `SALES`, `FINANCE` 중 하나 |
| `targetCountry` | string | 조건부 | `aiPartnerId`가 없으면 공백이 아닌 값 필수, 유효 DB 최대 10자. AI 상대 선택 시 요청값은 사용하지 않음 |
| `relationshipType` | string | 아니오 | 직접 설정 관계, 유효 DB 최대 30자. AI 상대 선택 시 요청값은 사용하지 않음 |
| `ageGroup` | string | 아니오 | 직접 설정 연령대, 유효 DB 최대 30자. AI 상대 선택 시 요청값은 사용하지 않음 |
| `speechStyle` | string | 아니오 | 직접 설정 말투, 유효 DB 최대 30자. AI 상대 선택 시 요청값은 사용하지 않음 |
| `characteristic` | string | 아니오 | 직접 설정 특징. AI 상대 선택 시 요청값은 사용하지 않음 |

AI 상대 선택 예시:

```json
{
  "memberId": 1,
  "aiPartnerId": 2,
  "situation": "BUSINESS",
  "field": "IT_DEVELOPMENT"
}
```

직접 설정 예시:

```json
{
  "memberId": 1,
  "aiPartnerId": null,
  "situation": "INTERVIEW",
  "field": "MARKETING",
  "targetCountry": "US",
  "relationshipType": "INTERVIEWER",
  "ageGroup": "WORKER",
  "speechStyle": "POLITE",
  "characteristic": "간결한 답변을 선호함"
}
```

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 생성된 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 선택한 AI 상대 ID. 직접 설정이면 `null` |
| `aiPartnerName` | string | 예 | 선택한 AI 상대 이름. 직접 설정이면 `null` |
| `situation` | string | 아니오 | 대화 상황 |
| `field` | string | 아니오 | 업무 분야 |

```json
{
  "id": 21,
  "aiPartnerId": 2,
  "aiPartnerName": "kash",
  "situation": "BUSINESS",
  "field": "IT_DEVELOPMENT"
}
```

요청 본문의 `memberId`가 로그인 회원 ID와 다르면 `403`, AI 상대가 없으면 `404`, 다른 회원의 커스텀 AI를 선택하면 `403`, 직접 설정에서 국가가 없으면 현재 구현상 `404`이다.

### 6.3 회원 대화방 목록 조회

`GET /api/conversations/member/{memberId}`

- 성공: `200 OK`, 최근 활동(`updatedAt`) 내림차순의 대화방 목록.
- 페이지네이션과 개수 제한은 없다.
- URL의 `memberId`가 로그인 회원 ID와 다르거나 로그인 회원 정보를 더 이상 찾을 수 없으면 `403`.
- 연결된 AI 상대가 있으면 현재 `AiPartner`의 `name`, `targetCountry`, `relationshipType`을 사용한다. 따라서 AI 상대를 수정하면 기존 대화방 목록의 표시값도 바뀐다.

배열 항목:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `conversationId` | number(Long) | 아니오 | 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 직접 설정이면 `null` |
| `aiPartnerName` | string | 아니오 | 직접 설정이면 `직접 설정 상대` |
| `targetCountry` | string | 아니오 | 상대 국가 |
| `relationshipType` | string | 예 | 관계 |
| `situation` | string | 아니오 | 대화 상황 |
| `field` | string | 아니오 | 업무 분야 |
| `lastMessage` | string | 예 | 가장 최근 메시지. 메시지가 없으면 `null` |
| `updatedAt` | string(LocalDateTime) | 아니오 | 최근 활동 시각 |

### 6.4 대화방 상세 조회

`GET /api/conversations/member/{memberId}/{id}`

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `conversationId` | number(Long) | 아니오 | 대화방 ID |
| `aiPartnerId` | number(Long) | 예 | 직접 설정이면 `null` |
| `aiPartnerName` | string | 아니오 | 직접 설정이면 `직접 설정 상대` |
| `targetCountry` | string | 아니오 | 대상 국가 |
| `relationshipType` | string | 예 | 관계 |
| `ageGroup` | string | 예 | 연령대 |
| `speechStyle` | string | 예 | 말투 |
| `characteristic` | string | 예 | 특징 |
| `situation` | string | 아니오 | 대화 상황 |
| `field` | string | 아니오 | 업무 분야 |
| `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |
| `updatedAt` | string(LocalDateTime) | 아니오 | 최근 활동 시각 |
| `messages` | array | 아니오 | 오래된 순의 **메시지 응답 객체** 목록 |

메시지가 없으면 `messages`는 `[]`이다. AI 상대를 선택해 만든 대화방은 조회 시 대화방에 복사 저장된 값보다 현재 `AiPartner`의 이름·국가·관계·연령·말투·특징을 우선 사용한다.

대화방이 없으면 `404`, 로그인 회원 소유가 아니면 `403`이다.

### 6.5 대화방 삭제

`DELETE /api/conversations/member/{memberId}/{id}`

- 성공: `204 No Content`, 응답 본문 없음.
- 메시지와 저장된 요약도 함께 삭제한다.
- 대화방이 없으면 `404`, 다른 회원 소유이면 `403`.

### 6.6 메시지 전송 및 AI 응답 생성

`POST /api/conversations/member/{memberId}/{id}/messages`

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `content` | string | 예 | 공백 불가, 사용자 메시지. DB `TEXT` 컬럼 |

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `userMessage` | object | 아니오 | 저장된 사용자 **메시지 응답 객체** |
| `assistantMessage` | object | 아니오 | 생성 및 저장된 AI **메시지 응답 객체** |

```json
{
  "userMessage": {
    "id": 10,
    "senderType": "USER",
    "content": "프로젝트 일정을 영어로 물어봐 줘.",
    "createdAt": "2026-08-15T13:30:00"
  },
  "assistantMessage": {
    "id": 11,
    "senderType": "ASSISTANT",
    "content": "Could you share the project timeline?",
    "createdAt": "2026-08-15T13:30:02"
  }
}
```

메시지 응답 객체:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 메시지 ID |
| `senderType` | string | 아니오 | 사용자 메시지 `USER`, AI 메시지 `ASSISTANT` |
| `content` | string | 아니오 | 메시지 내용 |
| `createdAt` | string(LocalDateTime) | 아니오 | 저장 시각 |

대화방이 없으면 `404`, 로그인 회원 소유가 아니면 `403`이다.

### 6.7 메시지 목록 조회

`GET /api/conversations/member/{memberId}/{id}/messages`

- 성공: `200 OK`, **메시지 응답 객체 배열**.
- `createdAt` 오름차순(오래된 메시지부터)이다.
- 페이지네이션과 개수 제한은 없으며, 메시지가 없으면 `[]`이다.
- 대화방이 없으면 `404`, 다른 회원 소유이면 `403`.

### 6.8 대화 요약 생성

`POST /api/conversations/member/{memberId}/{id}/summary`

- 요청 본문 없음.
- 전체 메시지를 오래된 순으로 결합해 AI 요약을 생성하고 DB에 새 요약 레코드로 저장한다.
- 호출할 때마다 기존 요약을 갱신하지 않고 새 요약 레코드를 추가한다.
- 메시지가 없으면 `400 Bad Request`.
- 대화방이 없으면 `404`, 로그인 회원 소유가 아니면 `403`.

성공 응답 `200 OK`:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 요약 ID |
| `conversationId` | number(Long) | 아니오 | 대화방 ID |
| `summary` | string | 아니오 | AI가 생성한 요약 |
| `createdAt` | string(LocalDateTime) | 아니오 | 요약 저장 시각 |

### 6.9 저장된 요약 목록 조회

`GET /api/conversations/member/{memberId}/{id}/summaries`

- 성공: `200 OK`, 위 **요약 응답 객체 배열**.
- 최신 요약부터 반환한다.
- 요약이 없으면 빈 배열을 반환한다.
- 대화방이 없으면 `404`, 로그인 회원 소유가 아니면 `403`.

### 6.10 가장 최근 요약 조회

`GET /api/conversations/member/{memberId}/{id}/summary/latest`

- 성공: `200 OK`, 위 **요약 응답 객체**.
- 대화방 또는 저장된 요약이 없으면 `404`, 대화방이 로그인 회원 소유가 아니면 `403`.

### 6.11 상황별 대화 통계 조회

`GET /api/conversations/member/{memberId}/statistics`

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `totalCount` | number | 아니오 | 회원의 전체 대화방 수 |
| `counts` | object | 아니오 | 상황별 대화방 수. 빈 상황은 집계에서 제외 |
| `percentages` | object | 아니오 | `전체 대화방 수` 기준 상황별 비율(%), 소수점 첫째 자리 반올림 |

```json
{
  "totalCount": 5,
  "counts": {
    "BUSINESS": 3,
    "DAILY": 2
  },
  "percentages": {
    "BUSINESS": 60.0,
    "DAILY": 40.0
  }
}
```

대화방이 하나도 없으면 `totalCount`는 `0`, `counts`와 `percentages`는 모두 빈 객체 `{}`이다. 로그인 회원 확인에 실패하면 `403`이다.

## 7. 번역 API

### 7.1 번역 및 문화 분석

`POST /api/translations/analyze`

요청과 AI 분석 결과를 DB에 저장한 뒤 분석 결과를 반환한다.

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `memberId` | number(Long) | 예 | 회원 ID, `null` 불가. 로그인 세션의 회원 ID와 같아야 하며, 다르면 `403` |
| `originalText` | string | 예 | 공백 불가, 번역할 원문. DB `TEXT` 컬럼 |
| `sourceLanguage` | string | 예 | 공백 불가, 원문 언어. 유효 DB 최대 10자 |
| `targetLanguage` | string | 예 | 공백 불가, 번역 언어. 유효 DB 최대 10자 |
| `targetCountry` | string | 예 | 공백 불가, 문화 맥락 대상 국가. 유효 DB 최대 10자 |
| `situation` | string | 예 | 공백 불가, 사용 상황. 유효 DB 최대 30자 |
| `relationshipType` | string | 아니오 | 발신자와 수신자의 관계. 유효 DB 최대 30자 |
| `communicationPurpose` | string | 아니오 | 의사소통 목적. 유효 DB 최대 30자 |
| `requestedTone` | string | 아니오 | 요청 말투. 유효 DB 최대 30자 |

언어·국가·상황·관계·목적·말투 값에 DTO enum 검증은 없다. 다만 번역 프롬프트가 설명 언어를 명시적으로 분기하는 코드는 `sourceLanguage`의 소문자 `ko`, `en`, `ja`이므로 이 값을 사용하는 편이 안전하다.

```json
{
  "memberId": 1,
  "originalText": "이번 주까지 가능할까요?",
  "sourceLanguage": "ko",
  "targetLanguage": "en",
  "targetCountry": "US",
  "situation": "BUSINESS",
  "relationshipType": "CLIENT",
  "communicationPurpose": "SCHEDULE_CHECK",
  "requestedTone": "POLITE"
}
```

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `literalTranslation` | string | 아니오 | 직역 |
| `naturalTranslation` | string | 아니오 | 자연스러운 번역 |
| `culturalTranslation` | string | 아니오 | 문화 맥락을 반영한 추천 번역 |
| `culturalExplanation` | string | 아니오 | 문화적 조정 설명 |
| `overallRiskLevel` | string | 아니오 | `SAFE`, `CAUTION`, `HIGH`, `AVOID` 중 하나 |
| `toneScores` | object | 아니오 | 말투 점수 객체 |
| `warnings` | array | 아니오 | 문화적 위험 표현 배열. 위험 표현이 없으면 빈 배열 `[]` |

말투 점수 객체:

| 필드 | 타입 | 설명 |
|---|---|---|
| `friendliness` | number(int) | 친근함 점수, 0~100 |
| `politeness` | number(int) | 정중함 점수, 0~100 |
| `directness` | number(int) | 직접성 점수, 0~100 |
| `aggression` | number(int) | 공격성 점수, 0~100 |
| `burden` | number(int) | 부담감 점수, 0~100 |
| `professionalism` | number(int) | 전문성 점수, 0~100 |
| `naturalness` | number(int) | 자연스러움 점수, 0~100 |

점수 범위는 Java DTO에서 검증하지 않지만, OpenAI 호출에 전달하는 strict JSON Schema가 각 값을 0~100 정수로 제한한다.

문화적 경고 객체:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `expression` | string | 아니오 | 위험 가능성이 있는 원문 표현 |
| `category` | string | 아니오 | `DIRECTNESS`, `POLITENESS`, `PRESSURE`, `FORMALITY`, `PERSONAL_ATTACK`, `SARCASM`, `CULTURAL_TABOO`, `AMBIGUITY`, `SENSITIVITY`, `OTHER` 중 하나 |
| `riskLevel` | string | 아니오 | `SAFE`, `CAUTION`, `HIGH`, `AVOID` 중 하나 |
| `reason` | string | 아니오 | 위험한 이유 |
| `alternativeExpression` | string | 아니오 | `targetLanguage`로 작성된 대체 표현 |
| `startIndex` | number(int) | 아니오 | 원문에서 `expression`이 시작하는 0-based 인덱스 |
| `endIndex` | number(int) | 아니오 | 원문에서 `expression`이 끝난 직후의 exclusive 인덱스 |

위 응답 루트 필드, 점수 7개, 경고 객체 필드는 OpenAI strict JSON Schema에서 모두 `required`다. 따라서 정상적인 성공 응답에서는 누락되거나 `null`이 되지 않는다.
다만 `startIndex`와 `endIndex`는 정수 여부만 스키마로 제한하며, 서버가 원문 범위나 `expression`과의 substring 일치를 다시 검증하지는 않는다.

```json
{
  "literalTranslation": "Can it be done by this week?",
  "naturalTranslation": "Would it be possible to complete this by the end of the week?",
  "culturalTranslation": "Would you be able to complete this by the end of the week?",
  "culturalExplanation": "상대방의 가능 여부를 정중하게 묻는 표현입니다.",
  "overallRiskLevel": "SAFE",
  "toneScores": {
    "friendliness": 65,
    "politeness": 90,
    "directness": 45,
    "aggression": 5,
    "burden": 30,
    "professionalism": 85,
    "naturalness": 90
  },
  "warnings": []
}
```

### 7.2 번역 기록 목록 조회

`GET /api/translations/member/{memberId}`

- 성공: `200 OK`, 생성 시각 내림차순의 배열.
- URL의 `memberId`가 로그인 회원 ID와 다르거나 로그인 회원 정보를 더 이상 찾을 수 없으면 `403`.

배열 항목:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 번역 기록 ID |
| `originalText` | string | 아니오 | 원문 |
| `sourceLanguage` | string | 아니오 | 원문 언어 |
| `targetLanguage` | string | 아니오 | 번역 언어 |
| `targetCountry` | string | 아니오 | 대상 국가 |
| `situation` | string | 아니오 | 사용 상황 |
| `createdAt` | string(LocalDateTime) | 아니오 | 생성 시각 |

### 7.3 번역 기록 상세 조회

`GET /api/translations/member/{memberId}/{id}`

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 번역 기록 ID |
| `originalText` | string | 아니오 | 원문 |
| `literalTranslation` | string | 아니오 | 직역 |
| `naturalTranslation` | string | 아니오 | 자연스러운 번역 |
| `culturalTranslation` | string | 아니오 | 문화 맥락 추천 번역 |
| `culturalExplanation` | string | 아니오 | 문화적 조정 설명 |
| `overallRiskLevel` | string | 아니오 | 전체 위험 수준 |
| `toneScores` | object | 아니오 | 7개 필드의 **말투 점수 객체** |
| `warnings` | array | 아니오 | **문화적 경고 객체** 배열. 저장된 경고가 없으면 `[]` |
| `memo` | string | 예 | 저장된 메모 내용. 없으면 `null` |
| `createdAt` | string(LocalDateTime) | 아니오 | 번역 생성 시각 |

번역이 없으면 `404`, 다른 회원 소유이면 `403`, 연결된 번역 결과가 없으면 `404`.

위 nullable 표시는 정상 분석 API를 통해 생성된 데이터 기준이다. DB 스키마에서는 일부 번역 결과·문화 경고·점수 컬럼이 nullable이므로 레거시 또는 수동 입력 데이터는 이 계약을 만족하지 않을 수 있다. 특히 점수 컬럼이 `null`이면 primitive `int` 변환 중 상세 조회가 `500`이 될 수 있다.

### 7.4 번역 기록 삭제

`DELETE /api/translations/member/{memberId}/{id}`

- 성공: `204 No Content`, 응답 본문 없음.
- 연결된 문화적 경고, 분석 결과, 메모를 함께 삭제한다.
- 번역이 없으면 `404`, 다른 회원 소유이면 `403`.

### 7.5 메모 저장 또는 수정

`PUT /api/translations/member/{memberId}/{id}/memo`

메모가 없으면 생성하고 이미 있으면 내용을 수정한다.

#### 요청 본문

| 필드 | 타입 | 필수 | 제약 및 설명 |
|---|---|---:|---|
| `content` | string | 예 | 공백 불가, 메모 내용 |

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `id` | number(Long) | 아니오 | 메모 ID |
| `translationId` | number(Long) | 아니오 | 번역 기록 ID |
| `content` | string | 아니오 | 메모 내용 |
| `createdAt` | string(LocalDateTime) | 아니오 | 최초 생성 시각 |
| `updatedAt` | string(LocalDateTime) | 아니오 | 최근 수정 시각 |

기존 메모를 수정하는 경우 JPA `@PreUpdate`가 트랜잭션 flush 때 실행되므로, 수정 직후 이 응답의 `updatedAt`에는 이전 값이 들어갈 수 있다. 이후 조회 응답에는 갱신된 시각이 반영된다.

### 7.6 메모 조회

`GET /api/translations/member/{memberId}/{id}/memo`

- 성공: `200 OK`, 위 **메모 응답 객체**.
- 메모가 없으면 `404`, 번역이 없으면 `404`, 다른 회원 소유이면 `403`.

### 7.7 메모 삭제

`DELETE /api/translations/member/{memberId}/{id}/memo`

- 성공: `204 No Content`, 응답 본문 없음.
- 메모가 없으면 `404`, 번역이 없으면 `404`, 다른 회원 소유이면 `403`.

### 7.8 마이페이지 최근 번역 기록 조회

`GET /api/translations/member/{memberId}/recent`

- 성공: `200 OK`, 생성 시각 내림차순의 배열.
- API 이름은 `recent`지만 현재 개수 제한은 없어 번역 결과가 존재하는 모든 기록을 반환한다.
- URL의 `memberId`가 로그인 회원 ID와 다르거나 로그인 회원 정보를 더 이상 찾을 수 없으면 `403`.

배열 항목:

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `translationId` | number(Long) | 아니오 | 번역 기록 ID |
| `originalText` | string | 아니오 | 원문 |
| `recommendedTranslation` | string | 아니오 | 문화 맥락 추천 번역(`culturalTranslation`) |
| `createdAt` | string(LocalDateTime) | 아니오 | 번역 생성 시각 |
| `hasMemo` | boolean | 아니오 | 메모 존재 여부 |

### 7.9 상황별 번역 통계 조회

`GET /api/translations/member/{memberId}/statistics`

#### 성공 응답 — `200 OK`

| 필드 | 타입 | nullable | 설명 |
|---|---|---:|---|
| `totalCount` | number | 아니오 | 전체 번역 기록 수 |
| `situations` | object | 아니오 | 상황별 번역 수. 키는 저장된 `situation` 문자열 |
| `percentages` | object | 아니오 | 전체 번역 수 기준 상황별 비율(%). 별도 반올림하지 않음 |

```json
{
  "totalCount": 4,
  "situations": {
    "BUSINESS": 3,
    "TRAVEL": 1
  },
  "percentages": {
    "BUSINESS": 75.0,
    "TRAVEL": 25.0
  }
}
```

번역 기록이 하나도 없으면 `totalCount`는 `0`, `situations`와 `percentages`는 모두 빈 객체 `{}`이다. 로그인 회원 확인에 실패하면 `403`이다.

## 8. 요청 검증 메시지

`@Valid` 검증이 실패하면 아래 메시지 중 첫 번째 한 건만 공통 오류 응답의 `message`로 반환된다. 둘 이상의 필드가 동시에 실패했을 때 어느 메시지가 먼저 선택되는지는 클라이언트 계약으로 가정하지 않는다.

| 요청 DTO | 필드/조건 | 메시지 |
|---|---|---|
| `SignupRequest` | `username` 공백 | `아이디는 필수입니다.` |
| `SignupRequest` | `username` 30자 초과 | `아이디는 30자 이하여야 합니다.` |
| `SignupRequest` | `password` 공백 | `비밀번호는 필수입니다.` |
| `SignupRequest` | `password` 8자 미만 또는 100자 초과 | `비밀번호는 8자 이상 100자 이하여야 합니다.` |
| `SignupRequest` | `passwordConfirm` 공백 | `비밀번호 확인은 필수입니다.` |
| `LoginRequest` | `username` 공백 | `아이디는 필수입니다.` |
| `LoginRequest` | `password` 공백 | `비밀번호는 필수입니다.` |
| `ChatRequest` | `message` 공백 | `메시지를 입력해주세요.` |
| AI 상대 생성/수정 | `name` 공백 | `AI 상대 이름은 필수입니다.` |
| AI 상대 생성/수정 | `targetCountry` 공백 | `대상 국가는 필수입니다.` |
| AI 상대 생성/수정 | `relationshipType` 공백 | `관계는 필수입니다.` |
| `ConversationCreateRequest` | `memberId`가 `null` | `회원 ID는 필수입니다.` |
| `ConversationCreateRequest` | `situation` 공백 | `대화 상황은 필수입니다.` |
| `ConversationCreateRequest` | `field` 공백 | `분야 선택은 필수입니다.` |
| `ConversationCreateRequest` | `field` 허용값 위반 | `분야는 IT_DEVELOPMENT, DESIGN, MARKETING, SALES, FINANCE 중 하나여야 합니다.` |
| `ConversationMessageRequest` | `content` 공백 | `메시지 내용은 필수입니다.` |
| `TranslationRequest` | `memberId`가 `null` | `회원 ID는 필수입니다.` |
| `TranslationRequest` | `originalText` 공백 | `원문은 필수입니다.` |
| `TranslationRequest` | `sourceLanguage` 공백 | `원문 언어는 필수입니다.` |
| `TranslationRequest` | `targetLanguage` 공백 | `번역 언어는 필수입니다.` |
| `TranslationRequest` | `targetCountry` 공백 | `대상 국가는 필수입니다.` |
| `TranslationRequest` | `situation` 공백 | `상황은 필수입니다.` |
| `TranslationMemoRequest` | `content` 공백 | `메모 내용은 필수입니다.` |

`aiPartnerId` 없이 대화방을 만들면서 `targetCountry`를 비우면 Bean Validation이 아니라 서비스에서 `IllegalArgumentException`이 발생한다. 따라서 메시지는 `AI 상대를 선택하지 않은 경우 대상 국가는 필수입니다.`이지만 상태 코드는 현재 `404`다.

## 9. 구현 상태 및 연동 주의사항

1. 로그인 성공 시 Spring Security 인증 정보가 HTTP 세션에 저장된다. 브라우저는 로그인 요청과 이후 모든 보호 API 요청에 `credentials: "include"` 또는 Axios `withCredentials: true`를 적용해야 한다.
2. 회원가입과 로그인 성공 응답은 `{ "memberId": 1 }` 형태가 아니라 숫자 `1` 하나다. 프론트 역직렬화 타입도 number/Long이어야 한다.
3. `memberId`를 받는 AI 상대·대화·번역 API는 로그인 principal의 username으로 조회한 회원 ID와 요청 `memberId`를 대조한다. 다른 회원 ID를 보내면 비즈니스 서비스 호출 전에 `403` 응답이 발생한다. 단, 본문 기반 API의 JSON 역직렬화와 Bean Validation은 컨트롤러 호출 전에 수행되므로 잘못된 본문은 `400`이 먼저다.
4. `IllegalArgumentException`을 전부 `404`로 처리하므로 중복 가입, 비밀번호 확인 불일치, 직접 설정 국가 누락 등 의미상 `400`/`409`에 가까운 오류도 현재는 `404`다.
5. AI 상대 및 여러 번역 입력 문자열에는 `@Size`가 없고 DB 길이만 존재한다. 초과 입력은 validation `400`이 아니라 저장 시 DB 오류가 될 수 있다.
6. 번역 AI 응답은 strict JSON Schema로 enum과 0~100 점수 범위를 제한한다. 반면 Java DTO에는 별도 enum/범위 검증이 없으므로 레거시 DB 데이터나 다른 생성 경로가 생기면 알 수 없는 값에 대비해야 한다.
7. 커스텀 AI 상대 삭제는 연결된 대화방과 그 메시지·요약까지 삭제한다. 번역 기록 삭제도 결과·문화 경고·메모를 함께 삭제한다.
8. `/recent`는 이름과 달리 반환 개수 제한이 없고, 목록·메시지·요약 API에도 페이지네이션이 없다.
9. 테스트 코드의 `MemoControllerTests`는 현재 존재하지 않는 `/api/memo` 엔드포인트를 호출한다. 이 테스트는 현재 컨트롤러 명세와 일치하지 않는다.
10. 세션 만료 시간과 세션 쿠키의 세부 속성은 코드에 명시되어 있지 않다. 운영 배포 전 서버 설정을 확정하고, 세션 쿠키 인증을 사용하는 만큼 현재 비활성화된 CSRF 보호도 함께 검토해야 한다.
