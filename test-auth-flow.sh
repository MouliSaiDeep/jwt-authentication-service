#!/bin/bash
set -e

API_URL="http://localhost:8080"
USERNAME="testuser_$(date +%s)"
EMAIL="${USERNAME}@example.com"
PASSWORD="Password1!"

echo "--- Starting Authentication Flow Test ---"

echo "1. Registering user..."
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $API_URL/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"email\": \"$EMAIL\", \"password\": \"$PASSWORD\"}")

HTTP_STATUS=$(echo "$REGISTER_RESPONSE" | tail -n1)
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | head -n -1)

if [ "$HTTP_STATUS" != "201" ]; then
  echo "Registration failed with status $HTTP_STATUS"
  echo "$REGISTER_BODY"
  exit 1
fi

echo "Registered user successfully."

echo "2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST $API_URL/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.access_token')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refresh_token')

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
  echo "Login failed. Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "Login successful. Received access and refresh tokens."

echo "3. Accessing protected profile route..."
PROFILE_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $API_URL/api/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN")

PROFILE_STATUS=$(echo "$PROFILE_RESPONSE" | tail -n1)
PROFILE_BODY=$(echo "$PROFILE_RESPONSE" | head -n -1)

if [ "$PROFILE_STATUS" != "200" ]; then
  echo "Profile access failed with status $PROFILE_STATUS"
  echo "$PROFILE_BODY"
  exit 1
fi

echo "Profile accessed successfully:"
echo "$PROFILE_BODY" | jq '.'

echo "4. Refreshing token..."
REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $API_URL/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\": \"$REFRESH_TOKEN\"}")

REFRESH_STATUS=$(echo "$REFRESH_RESPONSE" | tail -n1)
REFRESH_BODY=$(echo "$REFRESH_RESPONSE" | head -n -1)

if [ "$REFRESH_STATUS" != "200" ]; then
  echo "Token refresh failed with status $REFRESH_STATUS"
  echo "$REFRESH_BODY"
  exit 1
fi

NEW_ACCESS_TOKEN=$(echo "$REFRESH_BODY" | jq -r '.access_token')
if [ "$NEW_ACCESS_TOKEN" == "null" ] || [ -z "$NEW_ACCESS_TOKEN" ]; then
  echo "Failed to extract new access token"
  exit 1
fi

echo "Token refreshed successfully. Received new access token."

echo "5. Accessing profile with new token..."
NEW_PROFILE_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $API_URL/api/profile \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN")

NEW_PROFILE_STATUS=$(echo "$NEW_PROFILE_RESPONSE" | tail -n1)
if [ "$NEW_PROFILE_STATUS" != "200" ]; then
  echo "Profile access with new token failed with status $NEW_PROFILE_STATUS"
  exit 1
fi

echo "Profile accessed successfully with new token."

echo "6. Logging out..."
LOGOUT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $API_URL/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\": \"$REFRESH_TOKEN\"}")

LOGOUT_STATUS=$(echo "$LOGOUT_RESPONSE" | tail -n1)

if [ "$LOGOUT_STATUS" != "204" ]; then
  echo "Logout failed with status $LOGOUT_STATUS"
  exit 1
fi

echo "Logout successful."

echo "7. Verifying refresh token is invalid..."
INVALID_REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $API_URL/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refresh_token\": \"$REFRESH_TOKEN\"}")

INVALID_REFRESH_STATUS=$(echo "$INVALID_REFRESH_RESPONSE" | tail -n1)

if [ "$INVALID_REFRESH_STATUS" != "401" ]; then
  echo "Token was not actually invalidated! Status: $INVALID_REFRESH_STATUS"
  exit 1
fi

echo "Refresh token successfully invalidated."
echo "--- Authentication Flow Test Completed Successfully ---"
