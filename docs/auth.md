# 인증과 인가 (6차 과제)

## API

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/signup` | X | 이메일 회원가입 |
| POST | `/api/v1/auth/login` | X | 이메일 로그인. Access Token(본문) + Refresh Token(쿠키) |
| POST | `/api/v1/auth/reissue` | 쿠키 | 두 토큰 재발급(Rotation) |
| POST | `/api/v1/auth/logout` | 쿠키 | Refresh Token 삭제, 쿠키 만료 |
| GET | `/api/v1/auth/kakao` | X | 카카오 로그인 화면으로 이동 |
| GET | `/api/v1/auth/kakao/callback` | X | 카카오 Redirect URI. 로그인 처리 후 토큰 발급 |
| POST | `/api/v1/auth/kakao/login` | X | 프론트엔드가 받은 인가 코드로 로그인 |
| GET | `/api/v1/members/me` | O | 내 정보 조회 |
| DELETE | `/api/v1/members/me` | O | 회원 탈퇴 |
| GET | `/api/v1/posts/**` | X | 게시글 목록, 상세, 검색, 필터 |
| POST | `/api/v1/posts` | O | 게시글 작성(작성자 = 토큰의 회원) |
| PATCH, DELETE | `/api/v1/posts/{postId}` | O | 작성자 본인만 가능(아니면 403) |
| POST, DELETE | `/api/v1/posts/{postId}/likes` | O | 좋아요, 좋아요 취소 |

인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 보냅니다.

### 바뀐 API (5차까지와 비교)

- `POST /api/v1/members` (이름으로 회원 등록) → `POST /api/v1/auth/signup`
- `DELETE /api/v1/members/{memberId}` → `DELETE /api/v1/members/me`
- 게시글 작성 본문의 `memberId` 삭제(보내도 무시)
- 좋아요 본문의 `memberId` 삭제, 취소는 `DELETE /api/v1/posts/{postId}/likes`

### 에러 코드

| 상태 | 코드 | 의미 |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 토큰 없이 인증이 필요한 API 호출 |
| 401 | `INVALID_TOKEN` | 서명이 틀리거나 형식이 잘못된 토큰 |
| 401 | `EXPIRED_TOKEN` | 만료된 Access Token → `/reissue` 호출 |
| 401 | `INVALID_REFRESH_TOKEN`, `EXPIRED_REFRESH_TOKEN` | 재발급 실패 → 다시 로그인 |
| 401 | `LOGIN_FAILED` | 이메일 또는 비밀번호 불일치(어느 쪽인지 알려주지 않음) |
| 403 | `NOT_POST_OWNER` | 남의 게시글 수정, 삭제 |

## 로컬 실행

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export DB_PASSWORD=...
export REFRESH_COOKIE_SECURE=false   # http://localhost 에서 Postman으로 쿠키를 주고받을 때
./gradlew bootRun
```

## Postman으로 확인하기

1. `POST /api/v1/auth/signup` `{"email": "lin@sopt.org", "password": "sopt1234!", "name": "린"}` → 201
2. `POST /api/v1/auth/login` `{"email": "lin@sopt.org", "password": "sopt1234!"}` → 본문의 `accessToken` 복사, Cookies 탭에 `refresh_token`
3. `GET /api/v1/members/me` (Authorization: Bearer 토큰) → 200
4. 토큰 없이 / 토큰 글자를 바꿔서 / 만료 후 같은 요청 → 각각 401 `UNAUTHORIZED` / `INVALID_TOKEN` / `EXPIRED_TOKEN`
   - 만료를 빨리 보려면 `JWT_ACCESS_TOKEN_EXPIRATION=10000`(10초)으로 실행합니다.
5. `POST /api/v1/auth/reissue` → 새 `accessToken`과 새 쿠키. 이전 쿠키 값으로 다시 보내면 401
6. 다른 계정으로 로그인해서 1번 계정의 게시글을 `PATCH` → 403 `NOT_POST_OWNER`
7. `POST /api/v1/auth/logout` → 쿠키가 지워지고, 이후 `/reissue`는 401

Swagger(`/swagger-ui/index.html`)에서는 오른쪽 위 **Authorize**에 Access Token을 넣고 호출할 수 있습니다.

## 카카오 로그인 설정

1. [Kakao Developers](https://developers.kakao.com) → 내 애플리케이션 → 애플리케이션 추가
2. 앱 키의 **REST API 키**를 `KAKAO_CLIENT_ID`로 설정
3. 카카오 로그인 → 활성화 ON, Redirect URI에 `http://localhost:8080/api/v1/auth/kakao/callback` 등록
   (배포 서버는 `https://{도메인}/api/v1/auth/kakao/callback`을 추가 등록하고 `KAKAO_REDIRECT_URI`도 같은 값으로 설정)
4. 동의항목 → 닉네임(profile_nickname) 설정
5. 보안 → Client Secret을 켰다면 그 값을 `KAKAO_CLIENT_SECRET`으로 설정
6. 브라우저에서 `http://localhost:8080/api/v1/auth/kakao` 접속 → 카카오 로그인 → 콜백 응답(JSON)에 `accessToken`

## 설계 메모

- **Access Token은 헤더, Refresh Token은 쿠키**: Access Token은 클라이언트가 메모리에 두고 헤더로 보내므로 CSRF 대상이 아닙니다. 오래 사는 Refresh Token은 `HttpOnly` 쿠키라 XSS로 읽을 수 없고, `SameSite=Lax`와 `Path=/api/v1/auth`로 다른 사이트의 요청이나 다른 API 요청에는 실리지 않습니다.
- **Refresh Token Rotation**: 사용한 토큰은 바로 폐기합니다. 탈취된 토큰을 공격자가 먼저 쓰면 원래 사용자의 다음 재발급이 실패하므로 다시 로그인하게 됩니다.
- **한계**: Access Token은 로그아웃, 탈퇴 후에도 만료(최대 30분)까지 유효합니다. 블랙리스트를 두면 막을 수 있지만 매 요청마다 저장소를 조회해야 해서 무상태 장점이 줄어듭니다. 대신 탈퇴한 회원은 서비스 단에서 회원 상태를 확인해 쓰기 작업을 막습니다.
