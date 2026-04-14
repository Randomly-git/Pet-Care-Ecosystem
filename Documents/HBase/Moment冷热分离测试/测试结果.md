(base) PS D:\Tongji_Projects\Pet-Care-Ecosystem\community-backend> .\mvnw test -Dtest=CommunityColdDataMigrationJobTest
[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------< petcare.example:community-backend >------------------
[INFO] Building community-backend 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[WARNING] 2 problems were encountered while building the effective model for org.apache.yetus:audience-annotations:jar:0.5.0 during dependency collection step for project (use -X to see details)
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ community-backend ---
[INFO] Copying 2 resources from src\main\resources to target\classes
[INFO] Copying 0 resource from src\main\resources to target\classes
[INFO] 
[INFO] --- compiler:3.11.0:compile (default-compile) @ community-backend ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ community-backend ---
[INFO] skip non existing resourceDirectory D:\Tongji_Projects\Pet-Care-Ecosystem\community-backend\src\test\resources
[INFO] 
[INFO] --- compiler:3.11.0:testCompile (default-testCompile) @ community-backend ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- surefire:3.1.2:test (default-test) @ community-backend ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running petcare.example.community_backend.service.CommunityColdDataMigrationJobTest
WARNING: A Java agent has been loaded dynamically (C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.10\byte-buddy-agent-1.14.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J providers.
SLF4J: Found provider [ch.qos.logback.classic.spi.LogbackServiceProvider@44550792]
SLF4J: Found provider [org.slf4j.reload4j.Reload4jServiceProvider@1835d3ed]
SLF4J: See https://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual provider is of type [ch.qos.logback.classic.spi.LogbackServiceProvider@44550792]
19:48:25.075 [main] WARN org.apache.hadoop.hbase.unsafe.HBasePlatformDependent -- java.nio.Bits#unaligned() check failed.Unsafe based read/write of primitive types won't be used
java.lang.reflect.InaccessibleObjectException: Unable to make static boolean java.nio.Bits.unaligned() accessible: module java.base does not "opens java.nio" to unnamed module @105fece7
        at java.base/java.lang.reflect.AccessibleObject.throwInaccessibleObjectException(AccessibleObject.java:391)
        at java.base/java.lang.reflect.AccessibleObject.checkCanSetAccessible(AccessibleObject.java:367)
        at java.base/java.lang.reflect.AccessibleObject.checkCanSetAccessible(AccessibleObject.java:315)
        at java.base/java.lang.reflect.Method.checkCanSetAccessible(Method.java:203)
        at java.base/java.lang.reflect.Method.setAccessible(Method.java:197)
        at org.apache.hadoop.hbase.unsafe.HBasePlatformDependent.checkUnaligned(HBasePlatformDependent.java:176)
        at org.apache.hadoop.hbase.unsafe.HBasePlatformDependent.<clinit>(HBasePlatformDependent.java:49)
        at org.apache.hadoop.hbase.util.Bytes.<clinit>(Bytes.java:129)
        at petcare.example.community_backend.service.CommunityHBaseColdStorageService.<clinit>(CommunityHBaseColdStorageService.java:54)
        at java.base/java.lang.Class.forName0(Native Method)
        at java.base/java.lang.Class.forName(Class.java:536)
        at java.base/java.lang.Class.forName(Class.java:515)
        at org.mockito.internal.creation.bytebuddy.InlineBytecodeGenerator.assureInitialization(InlineBytecodeGenerator.java:236)
        at org.mockito.internal.creation.bytebuddy.InlineBytecodeGenerator.triggerRetransformation(InlineBytecodeGenerator.java:261)
        at org.mockito.internal.creation.bytebuddy.InlineBytecodeGenerator.mockClass(InlineBytecodeGenerator.java:218)
        at org.mockito.internal.creation.bytebuddy.TypeCachingBytecodeGenerator.lambda$mockClass$0(TypeCachingBytecodeGenerator.java:78)
        at net.bytebuddy.TypeCache.findOrInsert(TypeCache.java:168)
        at net.bytebuddy.TypeCache$WithInlineExpunction.findOrInsert(TypeCache.java:399)
        at net.bytebuddy.TypeCache.findOrInsert(TypeCache.java:190)
        at net.bytebuddy.TypeCache$WithInlineExpunction.findOrInsert(TypeCache.java:410)
        at org.mockito.internal.creation.bytebuddy.TypeCachingBytecodeGenerator.mockClass(TypeCachingBytecodeGenerator.java:75)
        at org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker.createMockType(InlineDelegateByteBuddyMockMaker.java:412)
        at org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker.doCreateMock(InlineDelegateByteBuddyMockMaker.java:371)
        at org.mockito.internal.creation.bytebuddy.InlineDelegateByteBuddyMockMaker.createMock(InlineDelegateByteBuddyMockMaker.java:350)
        at org.mockito.internal.creation.bytebuddy.InlineByteBuddyMockMaker.createMock(InlineByteBuddyMockMaker.java:56)
        at org.mockito.internal.util.MockUtil.createMock(MockUtil.java:99)
        at org.mockito.internal.MockitoCore.mock(MockitoCore.java:88)
        at org.mockito.Mockito.mock(Mockito.java:2101)
        at org.mockito.internal.configuration.MockAnnotationProcessor.processAnnotationForMock(MockAnnotationProcessor.java:79)
        at org.mockito.internal.configuration.MockAnnotationProcessor.process(MockAnnotationProcessor.java:28)
        at org.mockito.internal.configuration.MockAnnotationProcessor.process(MockAnnotationProcessor.java:25)
        at org.mockito.internal.configuration.IndependentAnnotationEngine.createMockFor(IndependentAnnotationEngine.java:44)
        at org.mockito.internal.configuration.IndependentAnnotationEngine.process(IndependentAnnotationEngine.java:72)
        at org.mockito.internal.configuration.InjectingAnnotationEngine.processIndependentAnnotations(InjectingAnnotationEngine.java:62)
        at org.mockito.internal.configuration.InjectingAnnotationEngine.process(InjectingAnnotationEngine.java:47)
        at org.mockito.MockitoAnnotations.openMocks(MockitoAnnotations.java:81)
        at org.mockito.internal.framework.DefaultMockitoSession.<init>(DefaultMockitoSession.java:43)
        at org.mockito.internal.session.DefaultMockitoSessionBuilder.startMocking(DefaultMockitoSessionBuilder.java:83)
        at org.mockito.junit.jupiter.MockitoExtension.beforeEach(MockitoExtension.java:159)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeBeforeEachCallbacks$2(TestMethodTestDescriptor.java:167)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeBeforeMethodsOrCallbacksUntilExceptionOccurs$6(TestMethodTestDescriptor.java:203)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(TestMethodTestDescriptor.java:203)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeBeforeEachCallbacks(TestMethodTestDescriptor.java:166)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:133)
        at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
        at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
        at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
        at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
        at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
        at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
        at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
        at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
        at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:198)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:169)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:93)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:58)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:141)
        at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:57)
        at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
        at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
        at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
        at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
        at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:184)
        at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:148)
        at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:122)
        at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
        at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
        at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
        at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
19:48:25.205 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=1
19:48:25.213 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=1, userId=100, status=NONE, lastAccessTime=2026-04-05T19:48:25.181308900
19:48:25.214 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=1, updated=1
19:48:25.222 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始删除MySQL数据: momentId=1
19:48:25.223 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态迁移成功: momentId=1, 评论数=0, 点赞数=0
19:48:25.261 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=7
19:48:25.261 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=7, userId=700, status=NONE, lastAccessTime=2026-04-05T19:48:25.257664500
19:48:25.261 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=7, updated=1
19:48:25.264 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始删除MySQL数据: momentId=7
19:48:25.298 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态迁移成功: momentId=7, 评论数=2000, 点赞数=0
19:48:25.305 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=2
19:48:25.305 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=2, userId=200, status=NONE, lastAccessTime=2026-04-05T19:48:25.304304300
19:48:25.305 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=2, updated=1
19:48:25.305 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态评论数超过阈值，跳过迁移: momentId=2, commentCount=2001, threshold=2000
19:48:25.312 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=6
19:48:25.312 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=6, userId=600, status=MIGRATING, lastAccessTime=2026-04-13T19:48:25.312105700
19:48:25.312 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态最近被访问过，跳过迁移: momentId=6, lastAccessTime=2026-04-13T19:48:25.312105700
19:48:25.335 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=5
19:48:25.335 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=5, userId=500, status=NONE, lastAccessTime=2026-04-05T19:48:25.330711900
19:48:25.336 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=5, updated=1
19:48:25.336 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始删除MySQL数据: momentId=5
19:48:25.343 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=3
19:48:25.343 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=3, userId=300, status=NONE, lastAccessTime=2026-04-05T19:48:25.341189100
19:48:25.343 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=3, updated=1
19:48:25.349 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=9
19:48:25.350 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=9, userId=900, status=NONE, lastAccessTime=2026-04-06T18:48:25.348111500
19:48:25.350 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=9, updated=1
19:48:25.350 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始删除MySQL数据: momentId=9
19:48:25.350 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态迁移成功: momentId=9, 评论数=0, 点赞数=0
19:48:25.354 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=8
19:48:25.354 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=8, userId=800, status=NONE, lastAccessTime=2026-04-06T20:48:25.353959100
19:48:25.354 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】动态最近被访问过，跳过迁移: momentId=8, lastAccessTime=2026-04-06T20:48:25.353959100
19:48:25.358 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】开始处理动态: momentId=4
19:48:25.358 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】查询到动态: momentId=4, userId=400, status=NONE, lastAccessTime=2026-04-05T19:48:25.357111200
19:48:25.358 [main] INFO petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】尝试获取锁: momentId=4, updated=1
19:48:25.359 [main] ERROR petcare.example.community_backend.service.CommunityColdDataMigrationJob -- 【冷迁移】HBase写入验证失败: momentId=4
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 1.551 s <<< FAILURE! -- in petcare.example.community_backend.service.CommunityColdDataMigrationJobTest
[ERROR] petcare.example.community_backend.service.CommunityColdDataMigrationJobTest.testTC_DT_04_IdempotentRecovery -- Time elapsed: 0.013 s <<< FAILURE!
org.opentest4j.AssertionFailedError: 自愈迁移应当返回 true ==> expected: <true> but was: <false>
        at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
        at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
        at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
        at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
        at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
        at petcare.example.community_backend.service.CommunityColdDataMigrationJobTest.testTC_DT_04_IdempotentRecovery(CommunityColdDataMigrationJobTest.java:268)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[INFO] 
[INFO] Results:
[INFO] 
[ERROR] Failures: 
[ERROR]   CommunityColdDataMigrationJobTest.testTC_DT_04_IdempotentRecovery:268 自愈迁移应当返回 true ==> expected: <true> but was: <false>
[INFO] 
[ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.320 s
[INFO] Finished at: 2026-04-13T19:48:25+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.1.2:test (default-test) on project community-backend: There are test failures.
[ERROR] 
[ERROR] Please refer to D:\Tongji_Projects\Pet-Care-Ecosystem\community-backend\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException



1. 工具工件 (Tool Artifacts)

使用的模型：Gemini 3.1 Pro。

使用的提示词 (Prompt)：系统指令：你现在是一个资深的 Java 软件测试开发工程师。你需要使用 JUnit 5 和 Mockito 为我的 Spring Boot 项目编写单元测试代码。

背景：我们在做黑盒测试，我需要你针对 CommunityColdDataMigrationJob 类中的 migrateSingleMoment 方法编写测试用例。

测试框架规范：

使用 @ExtendWith(MockitoExtension.class)。

使用 @InjectMocks 注入被测类，使用 @Mock 模拟所有依赖的 Repository 和 Service。

不要使用 @SpringBootTest，我们只做快速的单元测试。

使用 given().willReturn() 或 when().thenReturn() 设置 Mock 行为。

当前任务：

请根据下面的【状态迁移覆盖测试用例表（STT部分）】，为前 3 个用例（TC-STT-01, TC-STT-02, TC-STT-03）生成

生成的代码：把这 9 个用例作为代表性输出展示。

2. 实验分析 (Experimental Analysis)

准确性与覆盖率：在 IDEA 里跑一次代码覆盖率（Run with Coverage），截图展示你们这 9 个用例实现了多少行（Line）和分支（Branch）的覆盖。（通常这些精挑细选的用例能达到 80% 以上的核心业务覆盖率）。

发现缺陷：
“在实践过程中，AI 生成的测试用例意外暴露出了一处源码漏洞：migrateSingleMoment 方法在执行‘遗留迁移状态自愈’时，错误地优先进行了时间阈值校验。如果崩溃遗留的动态在期间被访问过，将被永久锁定。我们通过分析 AI 测试报告，反向修正了输入条件并建议修改源码逻辑。这证明了 AI 测试不仅能验证功能，还能辅助发现设计死角。”

关于 AI 幻觉的坑：写一写最初 AI 忘记使用 ReflectionTestUtils 注入 entityManager 导致 NullPointerException 的过程。这完美回答了作业要求里“你是如何遇到 AI 的局限性，以及如何改进该工具的”这个问题。
AI 局限性分析与工具改进实践：基于依赖注入的“幻觉”陷阱
在本项目中，我们使用大语言模型（LLM）作为测试脚本的生成工具。虽然 AI 能够极其快速地理解业务需求并输出语法正确的 Mockito 测试模板，但在复杂的工程实践中，我们依然遇到了典型的“AI 幻觉”与局限性，并据此对测试生成工具（Prompt 与工作流）进行了迭代改进。

局限性发现：隐蔽的 NullPointerException
在生成状态转移（STT）测试用例的初始阶段，AI 顺利输出了 TC-STT-01 的测试代码。代码在语法检查上完全无误，但在本地执行 mvn test 时，系统却抛出了致命的空指针异常：

java.lang.NullPointerException: Cannot invoke "jakarta.persistence.EntityManager.flush()" because "this.entityManager" is null

现象分析：测试代码在执行到被测类 CommunityColdDataMigrationJob 的第 413 行 entityManager.flush() 时崩溃，说明 Mockito 的 @InjectMocks 注解并未成功将模拟的 EntityManager 对象注入到被测实例中。

根因分析：AI 对多框架协同的“语义盲区”
经过我们团队的人工排查，发现这是由于 AI 缺乏对 Spring Boot、Lombok 和 Mockito 三者底层协同机制的深度理解所导致的逻辑盲区：

Lombok 的构造策略：被测类使用了 @RequiredArgsConstructor 注解。该注解仅会为带有 final 关键字的成员变量（如各大 Repository）生成构造函数。

Spring 的特殊注入：EntityManager 属于 JPA 原生对象，在业务代码中使用了 @PersistenceContext 注解引入，并未声明为 final。

Mockito 的刻板行为：AI 生成的测试代码简单地依赖了 @InjectMocks。Mockito 的注入机制会优先尝试“构造器注入”，由于构造器中不包含 EntityManager，Mockito 直接跳过了该字段，导致其在测试运行时为 null。

局限性总结：AI 具备强大的“表层模式匹配”能力（看到 Repository 就知道要 @Mock），但在处理长上下文时，无法像人类资深工程师那样，敏锐洞察到非标准注入方式（如 @PersistenceContext）在测试环境中的特殊副作用。