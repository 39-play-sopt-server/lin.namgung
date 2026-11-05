# 배포 가이드 (HTTPS, Docker Compose, 자동 배포)

## 0. 공통 준비

- 도메인의 A 레코드가 EC2 퍼블릭 IP를 가리켜야 합니다. (탄력적 IP 권장)
- EC2 보안 그룹 인바운드: **80, 443 허용**. HTTPS를 적용한 뒤에는 8080 규칙을 지웁니다.
  앱 포트가 열려 있으면 Nginx를 거치지 않고 접근할 수 있습니다.
- 기존에 `nohup java -jar`로 띄운 프로세스가 있으면 먼저 종료합니다. (`pgrep -f assignment` -> `kill`)

## 1. 방법 A: 호스트의 Nginx + Certbot (필수 과제)

앱은 기존처럼 8080에서 실행하고, 앞에 Nginx를 두어 HTTPS를 받습니다.

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
```

`/etc/nginx/sites-available/app` (도메인은 본인 것으로):

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/app /etc/nginx/sites-enabled/app
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d api.example.com
```

Certbot이 인증서를 발급하고 443 설정과 HTTP -> HTTPS 리다이렉트를 Nginx 설정에 추가합니다.
갱신은 systemd 타이머가 자동으로 합니다. (`sudo certbot renew --dry-run`으로 확인)

확인: `https://api.example.com/swagger-ui/index.html`

## 2. 방법 B: Docker Compose (심화 과제)

방법 A의 Nginx와 80/443 포트가 겹치므로 먼저 멈춥니다.

```bash
sudo systemctl disable --now nginx
```

```bash
cd deploy
cp .env.example .env
vi .env
./init-letsencrypt.sh
```

- 처음 한 번만 `init-letsencrypt.sh`로 인증서를 발급합니다. 이후에는 `docker compose up -d`만 쓰면 됩니다.
- 구성 요소: `app`(Spring Boot, 외부 비공개), `nginx`(80/443), `certbot`(갱신)
- 로그: `docker compose logs -f app`

## 3. GitHub Actions 자동 배포

`main`에 병합되면 `.github/workflows/deploy.yml`이 실행됩니다.
테스트 -> 이미지 빌드/GHCR push -> EC2에서 `app` 컨테이너 교체 -> 응답 확인 순서입니다.

저장소 Settings > Secrets and variables > Actions에 등록합니다.

| 이름 | 값 |
| --- | --- |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | SSH 사용자 (Ubuntu는 `ubuntu`) |
| `EC2_SSH_KEY` | 접속용 프라이빗 키 전체 내용 |
| `EC2_PROJECT_PATH` | EC2에 clone한 저장소 경로 |

EC2에는 Docker와 Docker Compose 플러그인이 설치되어 있어야 하고, `deploy/.env`가 만들어져 있어야 합니다.

### 롤백

```bash
cd deploy
APP_IMAGE=ghcr.io/39-play-sopt-server/lin.namgung:<이전-커밋-SHA> docker compose up -d app
```
