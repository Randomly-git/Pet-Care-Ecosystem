(base) PS E:\Documents\GitHub\Pet-Care-Ecosystem\community-backend> .\mvnw.cmd test -Dtest=MomentServiceLifecycleTest
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
[INFO] skip non existing resourceDirectory E:\Documents\GitHub\Pet-Care-Ecosystem\community-backend\src\test\resources
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
[INFO] Running petcare.example.community_backend.service.MomentServiceLifecycleTest
WARNING: A Java agent has been loaded dynamically (C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.10\byte-buddy-agent-1.14.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning  
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information 
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
SLF4J: Class path contains multiple SLF4J providers.
SLF4J: Found provider [ch.qos.logback.classic.spi.LogbackServiceProvider@42e22a53]
SLF4J: Found provider [org.slf4j.reload4j.Reload4jServiceProvider@57adfab0]
SLF4J: See https://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual provider is of type [ch.qos.logback.classic.spi.LogbackServiceProvider@42e22a53]
12:47:44.997 [main] WARN petcare.example.community_backend.service.MomentService -- 【冷迁移保护】拒绝修改正在迁移的动态: momentId=103
12:47:45.064 [main] INFO petcare.example.community_backend.service.MomentService -- 【内容审核】尝试通过动态: momentId=101
12:47:45.090 [main] INFO petcare.example.community_backend.service.MomentService -- 【冷热分离】MySQL 动态数量不足，需要补齐: current=0, required=10, missing=10
12:47:45.092 [main] INFO petcare.example.community_backend.service.MomentService -- 【冷热分离】HBase 中无更多冷数据可补齐
12:47:45.092 [main] INFO petcare.example.community_backend.service.MomentService -- 【冷热分离】MySQL 暂无数据，冷数据恢复中，请稍后刷新
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.545 s -- in petcare.example.community_backend.service.MomentServiceLifecycleTest
[INFO] 
[INFO] Results:
[INFO]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.418 s
[INFO] Finished at: 2026-04-18T12:47:45+08:00
[INFO] ------------------------------------------------------------------------