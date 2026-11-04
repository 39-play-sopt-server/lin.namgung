![banner_server.png](banner_server.png)

## 서버 배포 (4차 과제)

- AWS EC2(Ubuntu)에 Spring Boot 서버를 배포하고 RDS(MySQL)와 연결했습니다.
- 메모리 1GB 인스턴스라 4GB 스왑 파일을 설정했습니다.
- 보안 그룹 인바운드 규칙에 8080 포트를 허용했습니다.
- 빌드: `./gradlew build -x test`
- 실행: `nohup java -jar build/libs/assignment-1.0-SNAPSHOT.jar &`
- 확인: `/swagger-ui/index.html`, `/api/v1/posts`
- DB 접속 정보는 저장소에 포함하지 않고 서버에서만 설정합니다.

## HTTPS와 자동 배포 (4차 과제)

- Nginx 리버스 프록시로 HTTPS를 적용했습니다. (Let's Encrypt 인증서)
- Spring Boot, Nginx, Certbot을 Docker Compose로 구성했습니다. (`deploy/`)
- `main`에 병합되면 GitHub Actions가 테스트, 이미지 빌드, EC2 배포를 자동으로 실행합니다.
- 허용할 Origin은 환경 변수 `CORS_ALLOWED_ORIGINS`(쉼표 구분)로 설정합니다.
- 설정 방법은 [docs/deploy.md](docs/deploy.md)에 정리했습니다.
