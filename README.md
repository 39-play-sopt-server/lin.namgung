![banner_server.png](banner_server.png)

## 서버 배포 (4차 과제)

- AWS EC2(Ubuntu)에 Spring Boot 서버를 배포하고 RDS(MySQL)와 연결했습니다.
- 메모리 1GB 인스턴스라 4GB 스왑 파일을 설정했습니다.
- 보안 그룹 인바운드 규칙에 8080 포트를 허용했습니다.
- 빌드: `./gradlew build -x test`
- 실행: `nohup java -jar build/libs/assignment-1.0-SNAPSHOT.jar &`
- 확인: `/swagger-ui/index.html`, `/api/v1/posts`
- DB 접속 정보는 저장소에 포함하지 않고 서버에서만 설정합니다.
