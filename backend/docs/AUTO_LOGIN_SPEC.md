# 자동 로그인 API 명세서

> 기준 코드: `AuthController`, `SecurityConfig`, `CorsConfig`, `application.properties`
> 기준일: 2026-08-19 · 서버 주소: `http://localhost:8081`

## 1. 개요

Malgo는 JWT나 리프레시 토큰을 사용하지 않는다. 로그인에 성공하면 Spring Security의 인증 정보를 서버 HTTP 세션에 저장하고, 브라우저에는 `JSESSIONID` 쿠키를 발급한다. 앱을 다시 열거나 새로고침할 때 `GET /api/v1/auth/me`를 호출하면 남아 있는 세션으로 로그인한 회원 ID를 복원할 수 있다.

이 명세의 자동 로그인은 **동일 브라우저에서 유효한 세션을 다시 사용하는 방식**이다. 서버가 재시작되면 현재의 메모리 세션은 사라지므로 로그인 상태도 복원되지 않는다.

## 2. 세션 및 쿠키 정책

| 항목 | 설정 | 동작 |
|---|---|---|
| 인증 방식 | 서버 HTTP 세션 | 인증 정보는 서버 세션의 `SPRING_SECURITY_CONTEXT`에 저장 |
| 쿠키 이름 | `JSESSIONID` | 브라우저가 이후 요청에 전달 |
| 서버 세션 timeout | 30일 | 마지막 요청 이후 30일 동안 사용하지 않으면 만료 |
| 쿠키 Max-Age | 30일 | 로그인 시 발급된 쿠키는 최대 30일 동안 브라우저에 보관 |
| HttpOnly | `true` | 프런트 JavaScript에서 쿠키 값을 읽을 수 없음 |
| SameSite | `Lax` | 현재 `localhost:3000` 프런트와 `localhost:8081` API 호출에 사용 가능 |
| 세션 고정 방지 | 활성화 | 로그인 성공 시 세션 ID 변경 |

세션은 서버 메모리에 저장된다. 따라서 서버 재시작, 세션 만료, 브라우저 쿠키 삭제, 또는 로그아웃 중 하나가 발생하면 자동 로그인은 해제된다.

## 3. API

### 3.1 로그인

`POST /api/v1/auth/login`

| 항목 | 값 |
|---|---|
| 인증 필요 | 아니오 |
| 요청 헤더 | `Content-Type: application/json` |
| 요청 본문 | `username`, `password` |
| 성공 응답 | `200 OK`, JSON number 회원 ID |
| 쿠키 | `JSESSIONID` 설정 또는 교체 |

```json
{
  "username": "malgo01",
  "password": "password123"
}
```

```json
1
```

자격 증명이 올바르지 않으면 `401 Unauthorized`를 반환한다. 로그인에 성공한 뒤에는 모든 세션 필요 API에 `credentials: "include"`를 사용해야 한다.

### 3.2 자동 로그인 상태 확인

`GET /api/v1/auth/me`

| 항목 | 값 |
|---|---|
| 인증 필요 | 예 |
| 요청 본문 | 없음 |
| 요청 헤더 | 필요 없음 |
| 성공 응답 | `200 OK`, JSON number 회원 ID |
| 세션 없음 또는 만료 | `401 Unauthorized` |

유효한 `JSESSIONID`가 있으면 서버는 세션의 인증 사용자명을 회원으로 조회한 뒤 회원 ID를 반환한다. 응답은 객체가 아닌 JSON number다.

```json
1
```

세션이 없거나 만료된 `401`은 Spring Security에서 처리하므로 응답 본문 형식을 계약하지 않는다. 프런트는 `response.ok` 또는 HTTP 상태 코드로만 로그인 여부를 판단해야 한다.

### 3.3 로그아웃

`POST /api/v1/auth/logout`

| 항목 | 값 |
|---|---|
| 인증 필요 | 아니오 |
| 요청 본문 | 없음 |
| 성공 응답 | `204 No Content` |
| 서버 동작 | HTTP 세션 무효화, 인증 정보 제거, `JSESSIONID` 삭제 요청 |

로그아웃 직후 `GET /api/v1/auth/me`는 `401 Unauthorized`를 반환한다.

## 4. 프런트 연동 흐름

모든 요청에 `credentials: "include"`를 지정한다. 이 옵션이 없으면 브라우저가 교차 출처 요청에 세션 쿠키를 보내거나 받지 않아 자동 로그인이 동작하지 않는다.

```javascript
const API_BASE_URL = "http://localhost:8081";

export async function restoreLogin() {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
    credentials: "include"
  });

  if (!response.ok) {
    return null;
  }

  return response.json(); // number: memberId
}
```

앱 시작 시 `restoreLogin()`의 반환값이 있으면 로그인 상태와 `memberId`를 저장하고, `null`이면 비로그인 상태로 시작한다. 로그아웃 시에는 아래 요청이 성공한 뒤 클라이언트의 로그인 상태도 비운다.

```javascript
await fetch(`${API_BASE_URL}/api/v1/auth/logout`, {
  method: "POST",
  credentials: "include"
});
```

## 5. CORS 제약

현재 서버는 `http://localhost:3000`만 CORS Origin으로 허용하며 자격 증명을 허용한다. 다른 프런트 도메인 또는 운영 도메인을 사용하면 서버의 CORS Origin 설정을 함께 변경해야 한다.

운영 환경에서 프런트와 API가 서로 다른 사이트라면 `SameSite=None`, HTTPS, `Secure` 쿠키 설정 여부를 배포 환경에 맞춰 별도로 검토해야 한다.
