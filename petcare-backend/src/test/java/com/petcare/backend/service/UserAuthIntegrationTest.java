package com.petcare.backend.service;

import com.petcare.backend.dto.request.LoginRequest;
import com.petcare.backend.dto.request.RegisterRequest;
import com.petcare.backend.dto.response.LoginResponse;
import com.petcare.backend.dto.response.RegisterResponse;
import com.petcare.backend.exception.InvalidCredentialsException;
import com.petcare.backend.exception.UserAlreadyExistsException;
import com.petcare.backend.util.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserAuthIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Test
    void register_Success() {
        System.out.println("=== 测试用户注册 ===");

        RegisterRequest request = new RegisterRequest();
        request.setName("testuser");
        request.setPassword("password123");

        RegisterResponse response = userService.register(request);

        assertNotNull(response.getUserId());
        assertEquals("testuser", response.getName());
        assertEquals("注册成功", response.getMessage());

        System.out.println("用户注册成功: " + response.getName() + ", ID: " + response.getUserId());
    }

    @Test
    void register_UserAlreadyExists() {
        System.out.println("=== 测试重复用户注册 ===");

        // 先注册一个用户
        RegisterRequest request1 = new RegisterRequest();
        request1.setName("existinguser");
        request1.setPassword("password123");
        userService.register(request1);

        // 尝试注册同名用户
        RegisterRequest request2 = new RegisterRequest();
        request2.setName("existinguser");
        request2.setPassword("differentpassword");

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(request2)
        );

        assertTrue(exception.getMessage().contains("已存在"));
        System.out.println("重复注册异常: " + exception.getMessage());
    }

    @Test
    void login_Success() {
        System.out.println("=== 测试用户登录 ===");

        // 先注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("loginuser");
        registerRequest.setPassword("mypassword");
        RegisterResponse registerResponse = userService.register(registerRequest);

        // 然后登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("loginuser");
        loginRequest.setPassword("mypassword");

        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response.getUserId());
        assertEquals("loginuser", response.getName());
        assertNotNull(response.getToken());
        assertEquals("登录成功", response.getMessage());

        // 验证JWT token
        assertTrue(jwtTokenUtil.validateToken(response.getToken()));
        assertEquals(registerResponse.getUserId(), jwtTokenUtil.getUserIdFromToken(response.getToken()));
        assertEquals("loginuser", jwtTokenUtil.getUsernameFromToken(response.getToken()));

        System.out.println("登录成功，Token: " + response.getToken());
        System.out.println("用户ID: " + response.getUserId() + ", 用户名: " + response.getName());
    }

    @Test
    void login_InvalidUsername() {
        System.out.println("=== 测试用户名错误登录 ===");

        LoginRequest request = new LoginRequest();
        request.setName("nonexistentuser");
        request.setPassword("password");

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        System.out.println("用户名错误异常: " + exception.getMessage());
    }

    @Test
    void login_InvalidPassword() {
        System.out.println("=== 测试密码错误登录 ===");

        // 先注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("passworduser");
        registerRequest.setPassword("correctpassword");
        userService.register(registerRequest);

        // 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("passworduser");
        loginRequest.setPassword("wrongpassword");

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(loginRequest)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        System.out.println("密码错误异常: " + exception.getMessage());
    }

    @Test
    void registerAndLogin_CompleteWorkflow() {
        System.out.println("=== 测试完整注册登录流程 ===");

        // 1. 注册
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("workflowuser");
        registerRequest.setPassword("workflowpass");
        RegisterResponse registerResponse = userService.register(registerRequest);

        assertNotNull(registerResponse.getUserId());
        assertEquals("注册成功", registerResponse.getMessage());
        System.out.println("步骤1 - 注册成功: " + registerResponse.getName());

        // 2. 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("workflowuser");
        loginRequest.setPassword("workflowpass");
        LoginResponse loginResponse = userService.login(loginRequest);

        assertNotNull(loginResponse.getUserId());
        assertEquals(registerResponse.getUserId(), loginResponse.getUserId());
        assertNotNull(loginResponse.getToken());
        assertEquals("登录成功", loginResponse.getMessage());
        System.out.println("步骤2 - 登录成功");

        // 3. 验证JWT token
        assertTrue(jwtTokenUtil.validateToken(loginResponse.getToken()));
        Long userIdFromToken = jwtTokenUtil.getUserIdFromToken(loginResponse.getToken());
        String usernameFromToken = jwtTokenUtil.getUsernameFromToken(loginResponse.getToken());

        assertEquals(registerResponse.getUserId(), userIdFromToken);
        assertEquals("workflowuser", usernameFromToken);
        System.out.println("步骤3 - JWT验证成功");

        // 4. 验证token包含过期时间
        assertFalse(jwtTokenUtil.isTokenExpired(loginResponse.getToken()));
        System.out.println("步骤4 - Token未过期");

        System.out.println("完整流程测试完成");
    }

    @Test
    void jwtToken_ValidationTests() {
        System.out.println("=== 测试JWT Token功能 ===");

        // 注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("jwttestuser");
        registerRequest.setPassword("jwttestpass");
        RegisterResponse registerResponse = userService.register(registerRequest);

        // 登录获取JWT
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("jwttestuser");
        loginRequest.setPassword("jwttestpass");
        LoginResponse loginResponse = userService.login(loginRequest);

        String token = loginResponse.getToken();
        assertNotNull(token);
        System.out.println("获取到JWT Token: " + token);

        // 测试JWT解析
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        String username = jwtTokenUtil.getUsernameFromToken(token);

        assertEquals(registerResponse.getUserId(), userId);
        assertEquals("jwttestuser", username);
        System.out.println("JWT解析成功 - 用户ID: " + userId + ", 用户名: " + username);

        // 测试token验证
        assertTrue(jwtTokenUtil.validateToken(token));
        System.out.println("Token验证成功");

        // 测试无效token
        assertFalse(jwtTokenUtil.validateToken("invalid.token.here"));
        System.out.println("无效Token验证失败（符合预期）");
    }

    @Test
    void password_EncryptionTest() {
        System.out.println("=== 测试密码加密 ===");

        String rawPassword = "mySecurePassword123";

        // 注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("encryptionuser");
        registerRequest.setPassword(rawPassword);
        userService.register(registerRequest);

        // 使用相同密码登录应该成功
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("encryptionuser");
        loginRequest.setPassword(rawPassword);

        LoginResponse response = userService.login(loginRequest);
        assertNotNull(response.getToken());
        System.out.println("相同密码登录成功");

        // 使用不同密码登录应该失败
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setName("encryptionuser");
        wrongPasswordRequest.setPassword("differentPassword");

        assertThrows(InvalidCredentialsException.class, () -> {
            userService.login(wrongPasswordRequest);
        });
        System.out.println("不同密码登录失败（符合预期）");
    }

    @Test
    void multipleUsers_CanRegisterAndLogin() {
        System.out.println("=== 测试多用户注册登录 ===");

        String[] users = {
                "user1", "user2", "user3", "user4", "user5"
        };

        for (int i = 0; i < users.length; i++) {
            String username = users[i];
            String password = "pass" + (i + 1);

            // 注册
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setName(username);
            registerRequest.setPassword(password);
            RegisterResponse registerResponse = userService.register(registerRequest);

            assertNotNull(registerResponse.getUserId());
            System.out.println("注册用户 " + (i+1) + ": " + username);

            // 登录
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setName(username);
            loginRequest.setPassword(password);
            LoginResponse loginResponse = userService.login(loginRequest);

            assertNotNull(loginResponse.getToken());
            assertTrue(jwtTokenUtil.validateToken(loginResponse.getToken()));
            System.out.println("登录用户 " + (i+1) + " 成功");
        }

        System.out.println("所有用户注册登录测试完成");
    }


    @Test
    void token_ContainsCorrectInformation() {
        System.out.println("=== 测试Token信息完整性 ===");

        // 注册并登录
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("tokeninfouser");
        registerRequest.setPassword("tokeninfopass");
        RegisterResponse registerResponse = userService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setName("tokeninfouser");
        loginRequest.setPassword("tokeninfopass");
        LoginResponse loginResponse = userService.login(loginRequest);

        String token = loginResponse.getToken();

        // 验证token包含的信息
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        String username = jwtTokenUtil.getUsernameFromToken(token);

        assertEquals(registerResponse.getUserId(), userId);
        assertEquals("tokeninfouser", username);

        // 验证token未过期
        assertFalse(jwtTokenUtil.isTokenExpired(token));

        System.out.println("Token信息验证成功:");
        System.out.println("  - 用户ID: " + userId);
        System.out.println("  - 用户名: " + username);
        System.out.println("  - 是否过期: " + jwtTokenUtil.isTokenExpired(token));
    }
}