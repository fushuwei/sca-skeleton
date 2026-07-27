#!/bin/bash
echo "=== /admin redirect (via cloudflared) ==="
curl -sI --max-time 15 https://newease.cloud/admin 2>&1 | head -6
echo ""
echo "=== .ttf Content-Type (local nginx) ==="
curl -sI --max-time 5 "http://localhost:8080/fonts/OPPO_Sans_4.0/OPPO%20Sans%204.0.ttf" 2>&1 | head -5
echo ""
echo "=== .woff2 Content-Type (should still be font/woff2) ==="
curl -sI --max-time 5 "http://localhost:8080/fonts/JetBrains_Mono/JetBrainsMono-Regular.woff2" 2>&1 | head -4
echo ""
echo "=== Portal homepage ==="
curl -sI --max-time 15 https://newease.cloud/ 2>&1 | head -4
