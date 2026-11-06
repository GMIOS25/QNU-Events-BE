#!/usr/bin/env bash
set -euo pipefail

chmod +x ./mvnw
./mvnw -B -ntp -Pintegration verify
printf '\nBackend ready. Run: ./mvnw spring-boot:run\n'
