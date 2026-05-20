@echo off
echo ========== Login ==========
curl.exe -s -X POST http://localhost:9000/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin\"}" > login_result.json 2>&1
type login_result.json
echo.
echo ========== Without Token (Expect 401) ==========
curl.exe -s -w "\nHTTP_CODE: %%{http_code}" http://localhost:9000/api/pets/1
echo.
echo.
echo ========== With Fake Token (Expect 401) ==========
curl.exe -s -w "\nHTTP_CODE: %%{http_code}" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.xxx" http://localhost:9000/api/pets/1
echo.
