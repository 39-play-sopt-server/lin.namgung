#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
DOMAIN=$(grep '^DOMAIN=' .env | cut -d= -f2-)
EMAIL=$(grep '^EMAIL=' .env | cut -d= -f2-)

CERT_DIR="./certbot/conf/live/${DOMAIN}"

if [ -f "${CERT_DIR}/fullchain.pem" ]; then
  echo "이미 인증서가 있습니다. 새로 발급하려면 ./certbot 디렉터리를 지우고 다시 실행하세요."
  exit 0
fi

echo "1. 임시 인증서 생성 (Nginx가 443 설정을 읽으려면 인증서 파일이 먼저 있어야 함)"
mkdir -p "${CERT_DIR}" ./certbot/www
openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
  -keyout "${CERT_DIR}/privkey.pem" \
  -out "${CERT_DIR}/fullchain.pem" \
  -subj "/CN=localhost"

echo "2. app, nginx 시작"
docker compose up -d app nginx

echo "3. 임시 인증서 삭제"
rm -rf "./certbot/conf/live/${DOMAIN}" \
       "./certbot/conf/archive/${DOMAIN}" \
       "./certbot/conf/renewal/${DOMAIN}.conf"

echo "4. Let's Encrypt 인증서 발급 (HTTP-01, webroot 방식)"
docker compose run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${DOMAIN}" \
  --email "${EMAIL}" --agree-tos --no-eff-email --non-interactive

echo "5. Nginx 재로드, certbot 갱신 컨테이너 시작"
docker compose exec nginx nginx -s reload
docker compose up -d certbot

echo "완료: https://${DOMAIN}/swagger-ui/index.html"
