#!/bin/bash
# filepath: /home/miosha/code/bsi/ojt/coinsight/k6-tests/scripts/test-rate-limiter.sh

echo "🚀 Starting Rate Limiter Tests..."

echo "📊 Running simple rate limit test..."
k6 run k6-tests/simple-rate-limit-test.js

echo "⏱️ Waiting 30 seconds for rate limits to reset..."
sleep 30

echo "📈 Running comprehensive rate limiter test..."
k6 run k6-tests/rate-limiter-test.js

echo "✅ Rate limiter tests completed!"