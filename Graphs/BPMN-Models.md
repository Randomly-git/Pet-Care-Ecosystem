# Pet Care Ecosystem - BPMN业务流程模型

## 概述

本文档使用BPMN (Business Process Model and Notation) 描述Pet Care Ecosystem系统的核心业务流程。

## BPMN符号说明

### 基本元素
- **开始事件** (Start Event): 圆形，细边框
- **结束事件** (End Event): 圆形，粗边框
- **任务** (Task): 圆角矩形
- **网关** (Gateway): 菱形
- **流程线** (Sequence Flow): 带箭头的实线
- **消息流** (Message Flow): 带箭头的虚线

### 高级元素
- **泳道** (Swimlane): 分组相关活动
- **子流程** (Subprocess): 复杂活动的容器
- **数据对象** (Data Object): 文档图标
- **注释** (Annotation): 开放矩形

## 核心业务流程

### 1. 用户注册登录流程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn:process id="UserRegistrationProcess" name="用户注册流程">

    <!-- 开始事件 -->
    <bpmn:startEvent id="StartEvent_1" name="访问注册页面">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- 用户任务 -->
    <bpmn:userTask id="Task_1" name="填写注册信息">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>

    <!-- 服务任务 -->
    <bpmn:serviceTask id="Task_2" name="验证用户名唯一性">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 排他网关 -->
    <bpmn:exclusiveGateway id="Gateway_1" name="用户名是否可用?">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
      <bpmn:outgoing>Flow_5</bpmn:outgoing>
    </bpmn:exclusiveGateway>

    <!-- 创建用户任务 -->
    <bpmn:serviceTask id="Task_3" name="创建用户账户">
      <bpmn:incoming>Flow_4</bpmn:incoming>
      <bpmn:outgoing>Flow_6</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 生成JWT任务 -->
    <bpmn:serviceTask id="Task_4" name="生成JWT Token">
      <bpmn:incoming>Flow_6</bpmn:incoming>
      <bpmn:outgoing>Flow_7</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_1" name="注册成功">
      <bpmn:incoming>Flow_7</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 错误结束事件 -->
    <bpmn:endEvent id="EndEvent_2" name="用户名已存在">
      <bpmn:incoming>Flow_5</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 流程线 -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="Task_2"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="Task_2" targetRef="Gateway_1"/>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="Gateway_1" targetRef="Task_3">
      <bpmn:conditionExpression>用户名可用</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_5" sourceRef="Gateway_1" targetRef="EndEvent_2">
      <bpmn:conditionExpression>用户名已存在</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_6" sourceRef="Task_3" targetRef="Task_4"/>
    <bpmn:sequenceFlow id="Flow_7" sourceRef="Task_4" targetRef="EndEvent_1"/>

  </bpmn:process>
</bpmn:definitions>
```

**流程说明**:
1. 用户访问注册页面
2. 填写注册信息（用户名、密码）
3. 系统验证用户名唯一性
4. 如果用户名可用，创建用户账户
5. 生成JWT Token
6. 返回注册成功结果
7. 如果用户名已存在，返回错误信息

### 2. 宠物活动记录流程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn:process id="ActivityRecordingProcess" name="宠物活动记录流程">

    <!-- 开始事件 -->
    <bpmn:startEvent id="StartEvent_1" name="开始记录活动">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- 用户任务 -->
    <bpmn:userTask id="Task_1" name="选择活动类型">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>

    <!-- 子流程 -->
    <bpmn:subProcess id="SubProcess_1" name="活动信息录入">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>

      <!-- 子流程内部任务 -->
      <bpmn:userTask id="Task_2" name="填写活动描述">
        <bpmn:outgoing>Flow_Sub_1</bpmn:outgoing>
      </bpmn:userTask>

      <bpmn:userTask id="Task_3" name="选择活动时间">
        <bpmn:incoming>Flow_Sub_1</bpmn:incoming>
        <bpmn:outgoing>Flow_Sub_2</bpmn:outgoing>
      </bpmn:userTask>

      <bpmn:userTask id="Task_4" name="上传媒体文件(可选)">
        <bpmn:incoming>Flow_Sub_2</bpmn:incoming>
        <bpmn:outgoing>Flow_Sub_3</bpmn:outgoing>
      </bpmn:userTask>

      <!-- 子流程内部流程线 -->
      <bpmn:sequenceFlow id="Flow_Sub_1" sourceRef="Task_2" targetRef="Task_3"/>
      <bpmn:sequenceFlow id="Flow_Sub_2" sourceRef="Task_3" targetRef="Task_4"/>
      <bpmn:sequenceFlow id="Flow_Sub_3" sourceRef="Task_4" targetRef="SubProcess_1_End"/>
    </bpmn:subProcess>

    <!-- 媒体文件处理子流程 -->
    <bpmn:subProcess id="SubProcess_2" name="媒体文件处理">
      <bpmn:incoming>Flow_4</bpmn:incoming>
      <bpmn:outgoing>Flow_5</bpmn:outgoing>

      <bpmn:serviceTask id="Task_5" name="上传到腾讯COS">
        <bpmn:outgoing>Flow_COS_1</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:serviceTask id="Task_6" name="生成缩略图">
        <bpmn:incoming>Flow_COS_1</bpmn:incoming>
        <bpmn:outgoing>Flow_COS_2</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:serviceTask id="Task_7" name="关联活动记录">
        <bpmn:incoming>Flow_COS_2</bpmn:incoming>
        <bpmn:outgoing>Flow_COS_3</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:sequenceFlow id="Flow_COS_1" sourceRef="Task_5" targetRef="Task_6"/>
      <bpmn:sequenceFlow id="Flow_COS_2" sourceRef="Task_6" targetRef="Task_7"/>
      <bpmn:sequenceFlow id="Flow_COS_3" sourceRef="Task_7" targetRef="SubProcess_2_End"/>
    </bpmn:subProcess>

    <!-- 排他网关 -->
    <bpmn:exclusiveGateway id="Gateway_1" name="有媒体文件?">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
      <bpmn:outgoing>Flow_6</bpmn:outgoing>
    </bpmn:exclusiveGateway>

    <!-- 服务任务 -->
    <bpmn:serviceTask id="Task_8" name="保存活动记录">
      <bpmn:incoming>Flow_5</bpmn:incoming>
      <bpmn:incoming>Flow_6</bpmn:incoming>
      <bpmn:outgoing>Flow_7</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 发送任务 -->
    <bpmn:sendTask id="Task_9" name="发送统计事件">
      <bpmn:incoming>Flow_7</bpmn:incoming>
      <bpmn:outgoing>Flow_8</bpmn:outgoing>
    </bpmn:sendTask>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_1" name="活动记录完成">
      <bpmn:incoming>Flow_8</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 流程线 -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="SubProcess_1"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="SubProcess_1" targetRef="Gateway_1"/>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="Gateway_1" targetRef="SubProcess_2">
      <bpmn:conditionExpression>有媒体文件</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_5" sourceRef="SubProcess_2" targetRef="Task_8"/>
    <bpmn:sequenceFlow id="Flow_6" sourceRef="Gateway_1" targetRef="Task_8">
      <bpmn:conditionExpression>无媒体文件</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_7" sourceRef="Task_8" targetRef="Task_9"/>
    <bpmn:sequenceFlow id="Flow_8" sourceRef="Task_9" targetRef="EndEvent_1"/>

  </bpmn:process>
</bpmn:definitions>
```

**流程说明**:
1. 用户开始记录宠物活动
2. 选择活动类型（喂养、互动、清洁等）
3. 填写活动详细信息
4. 如果有媒体文件，进入文件处理流程
5. 保存活动记录到数据库
6. 发送统计事件用于数据分析

### 3. 社区动态发布流程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn:process id="MomentPublishingProcess" name="社区动态发布流程">

    <!-- 开始事件 -->
    <bpmn:startEvent id="StartEvent_1" name="点击发布动态">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- 用户任务 -->
    <bpmn:userTask id="Task_1" name="编写动态内容">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:userTask>

    <!-- 子流程：内容审核 -->
    <bpmn:subProcess id="SubProcess_1" name="内容审核">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>

      <bpmn:serviceTask id="Task_Audit_1" name="敏感词过滤">
        <bpmn:outgoing>Flow_Audit_1</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:serviceTask id="Task_Audit_2" name="内容合规检查">
        <bpmn:incoming>Flow_Audit_1</bpmn:incoming>
        <bpmn:outgoing>Flow_Audit_2</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:exclusiveGateway id="Gateway_Audit" name="内容是否合规?">
        <bpmn:incoming>Flow_Audit_2</bpmn:incoming>
        <bpmn:outgoing>Flow_Audit_Pass</bpmn:outgoing>
        <bpmn:outgoing>Flow_Audit_Fail</bpmn:outgoing>
      </bpmn:exclusiveGateway>

      <bpmn:endEvent id="EndEvent_Audit_Pass" name="审核通过">
        <bpmn:incoming>Flow_Audit_Pass</bpmn:incoming>
      </bpmn:endEvent>

      <bpmn:endEvent id="EndEvent_Audit_Fail" name="审核失败">
        <bpmn:incoming>Flow_Audit_Fail</bpmn:incoming>
      </bpmn:endEvent>

      <bpmn:sequenceFlow id="Flow_Audit_1" sourceRef="Task_Audit_1" targetRef="Task_Audit_2"/>
      <bpmn:sequenceFlow id="Flow_Audit_2" sourceRef="Task_Audit_2" targetRef="Gateway_Audit"/>
      <bpmn:sequenceFlow id="Flow_Audit_Pass" sourceRef="Gateway_Audit" targetRef="EndEvent_Audit_Pass">
        <bpmn:conditionExpression>内容合规</bpmn:conditionExpression>
      </bpmn:sequenceFlow>
      <bpmn:sequenceFlow id="Flow_Audit_Fail" sourceRef="Gateway_Audit" targetRef="EndEvent_Audit_Fail">
        <bpmn:conditionExpression>内容不合规</bpmn:conditionExpression>
      </bpmn:sequenceFlow>
    </bpmn:subProcess>

    <!-- 并行网关 - 多任务并行处理 -->
    <bpmn:parallelGateway id="Gateway_Parallel_Start" name="开始并行处理">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_Save</bpmn:outgoing>
      <bpmn:outgoing>Flow_Media</bpmn:outgoing>
      <bpmn:outgoing>Flow_Notify</bpmn:outgoing>
    </bpmn:parallelGateway>

    <!-- 并行任务 -->
    <bpmn:serviceTask id="Task_Save" name="保存动态到数据库">
      <bpmn:incoming>Flow_Save</bpmn:incoming>
      <bpmn:outgoing>Flow_Join_1</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:serviceTask id="Task_Media_Process" name="处理媒体文件">
      <bpmn:incoming>Flow_Media</bpmn:incoming>
      <bpmn:outgoing>Flow_Join_2</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:sendTask id="Task_Notify" name="发送通知给关注者">
      <bpmn:incoming>Flow_Notify</bpmn:incoming>
      <bpmn:outgoing>Flow_Join_3</bpmn:outgoing>
    </bpmn:sendTask>

    <!-- 并行网关 - 同步点 -->
    <bpmn:parallelGateway id="Gateway_Parallel_End" name="等待所有任务完成">
      <bpmn:incoming>Flow_Join_1</bpmn:incoming>
      <bpmn:incoming>Flow_Join_2</bpmn:incoming>
      <bpmn:incoming>Flow_Join_3</bpmn:incoming>
      <bpmn:outgoing>Flow_Complete</bpmn:outgoing>
    </bpmn:parallelGateway>

    <!-- 发送事件 -->
    <bpmn:sendTask id="Task_Event" name="发布社区事件">
      <bpmn:incoming>Flow_Complete</bpmn:incoming>
      <bpmn:outgoing>Flow_End</bpmn:outgoing>
    </bpmn:sendTask>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_1" name="动态发布成功">
      <bpmn:incoming>Flow_End</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 错误处理事件 -->
    <bpmn:boundaryEvent id="BoundaryEvent_1" attachedToRef="SubProcess_1">
      <bpmn:errorEventDefinition errorRef="Error_Content_Violation"/>
      <bpmn:outgoing>Flow_Error</bpmn:outgoing>
    </bpmn:boundaryEvent>

    <bpmn:endEvent id="EndEvent_Error" name="发布失败">
      <bpmn:incoming>Flow_Error</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 流程线 -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="SubProcess_1"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="SubProcess_1" targetRef="Gateway_Parallel_Start"/>
    <bpmn:sequenceFlow id="Flow_Save" sourceRef="Gateway_Parallel_Start" targetRef="Task_Save"/>
    <bpmn:sequenceFlow id="Flow_Media" sourceRef="Gateway_Parallel_Start" targetRef="Task_Media_Process"/>
    <bpmn:sequenceFlow id="Flow_Notify" sourceRef="Gateway_Parallel_Start" targetRef="Task_Notify"/>
    <bpmn:sequenceFlow id="Flow_Join_1" sourceRef="Task_Save" targetRef="Gateway_Parallel_End"/>
    <bpmn:sequenceFlow id="Flow_Join_2" sourceRef="Task_Media_Process" targetRef="Gateway_Parallel_End"/>
    <bpmn:sequenceFlow id="Flow_Join_3" sourceRef="Task_Notify" targetRef="Gateway_Parallel_End"/>
    <bpmn:sequenceFlow id="Flow_Complete" sourceRef="Gateway_Parallel_End" targetRef="Task_Event"/>
    <bpmn:sequenceFlow id="Flow_End" sourceRef="Task_Event" targetRef="EndEvent_1"/>
    <bpmn:sequenceFlow id="Flow_Error" sourceRef="BoundaryEvent_1" targetRef="EndEvent_Error"/>

  </bpmn:process>
</bpmn:definitions>
```

**流程说明**:
1. 用户点击发布动态
2. 编写动态内容
3. 系统进行内容审核（敏感词过滤、合规检查）
4. 如果审核通过，并行处理多个任务：
   - 保存动态到数据库
   - 处理媒体文件
   - 发送通知给关注者
5. 所有任务完成后，发布社区事件

### 4. LLM宠物健康分析服务流程

```puml
@startbpmn
title 宠物健康分析LLM服务流程

swimlane "前端应用" as Frontend
  start "用户请求AI分析" as StartEvent
  userTask "构建GraphQL查询" as BuildQuery
  sendTask "发送GraphQL请求" as SendRequest

swimlane "LLM微服务" as LLMService
  receiveTask "接收GraphQL查询" as ReceiveQuery
  serviceTask "解析请求参数" as ParseParams

  parallelGateway "并行数据获取" as ParallelStart
    serviceTask "获取宠物基本信息" as GetPetInfo
    serviceTask "获取健康状态记录" as GetStatusRecords
  parallelGateway "数据收集完成" as ParallelEnd

  serviceTask "构建AI提示词" as BuildPrompt
  sendTask "调用通义千问AI" as CallQwenAI
  receiveTask "接收AI响应" as ReceiveAIResponse
  serviceTask "格式化分析结果" as FormatResult
  sendTask "返回GraphQL响应" as ReturnResponse

swimlane "宠物后端服务" as PetService
  receiveTask "接收宠物信息请求" as ReceivePetRequest
  serviceTask "查询宠物数据库" as QueryPetDB
  sendTask "返回宠物信息" as ReturnPetInfo

swimlane "状态记录服务" as StatusService
  receiveTask "接收状态记录请求" as ReceiveStatusRequest
  serviceTask "查询状态记录数据库" as QueryStatusDB
  sendTask "返回状态记录" as ReturnStatusRecords

swimlane "通义千问AI服务" as QwenService
  receiveTask "接收AI推理请求" as ReceiveAIRequest
  serviceTask "执行AI推理" as ExecuteInference
  sendTask "返回AI分析结果" as ReturnAIResult

' 流程连接
StartEvent -> BuildQuery
BuildQuery -> SendQuery
SendRequest -> ReceiveQuery
ReceiveQuery -> ParseParams
ParseParams -> ParallelStart

ParallelStart -> GetPetInfo
ParallelStart -> GetStatusRecords

GetPetInfo -> ReceivePetRequest
ReceivePetRequest -> QueryPetDB
QueryPetDB -> ReturnPetInfo
ReturnPetInfo -> ParallelEnd

GetStatusRecords -> ReceiveStatusRequest
ReceiveStatusRequest -> QueryStatusDB
QueryStatusDB -> ReturnStatusRecords
ReturnStatusRecords -> ParallelEnd

ParallelEnd -> BuildPrompt
BuildPrompt -> CallQwenAI
CallQwenAI -> ReceiveAIRequest
ReceiveAIRequest -> ExecuteInference
ExecuteInference -> ReturnAIResult
ReturnAIResult -> ReceiveAIResponse
ReceiveAIResponse -> FormatResult
FormatResult -> ReturnResponse

' 边界事件 - 异常处理
boundaryError "数据获取失败" as DataError attached to ParallelEnd
boundaryError "AI调用失败" as AIError attached to ReceiveAIResponse

serviceTask "返回错误响应" as ErrorHandler
endEvent "分析失败" as ErrorEnd

DataError -> ErrorHandler
AIError -> ErrorHandler
ErrorHandler -> ErrorEnd

' 正常结束
ReturnResponse -> endEvent "分析完成" as SuccessEnd

@endbpmn
```

**流程说明**:
1. **用户发起请求**: 用户在前端应用中请求AI宠物健康分析
2. **构建查询**: 前端构建GraphQL查询，支持可选的用户特别关心的问题
3. **发送请求**: 通过HTTP POST发送GraphQL请求到LLM微服务
4. **接收处理**: LLM微服务接收并解析请求参数(petId, userRequirement)
5. **并行数据获取**: 同时调用两个微服务获取数据：
   - 从宠物后端服务获取宠物基本信息(名称、品种、物种)
   - 从状态记录服务获取宠物的历史健康状态记录
6. **构建AI提示**: 基于收集的数据构建详细的AI分析提示词
7. **AI推理**: 调用通义千问Qwen 3.5 Max模型进行智能分析
8. **格式化结果**: 将AI响应格式化为结构化的健康建议
9. **返回响应**: 通过GraphQL响应返回分析结果
10. **异常处理**: 如果数据获取或AI调用失败，返回错误信息

**关键技术特点**:
- **并行处理**: 使用CompletableFuture实现数据获取的并行化，提高响应速度
- **微服务架构**: 通过负载均衡调用其他微服务
- **AI集成**: 集成通义千问大语言模型提供智能分析
- **容错设计**: 完善的异常处理机制，确保服务稳定性

**数据流说明**:
- **输入**: 宠物ID、可选的用户特别关心的问题
- **输出**: 宠物基本信息、健康建议、历史状态记录
- **外部依赖**: 宠物后端服务、状态记录服务、通义千问AI API

### 5. 宠物健康状态监控流程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <bpmn:process id="HealthMonitoringProcess" name="宠物健康状态监控流程">

    <!-- 定时启动事件 -->
    <bpmn:startEvent id="StartEvent_1" name="每日健康检查">
      <bpmn: timerEventDefinition>
        <bpmn:timeCycle>R/PT24H</bpmn:timeCycle>
      </bpmn: timerEventDefinition>
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- 服务任务 -->
    <bpmn:serviceTask id="Task_1" name="获取所有宠物列表">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 多实例子流程 -->
    <bpmn:subProcess id="SubProcess_1" name="宠物健康分析">
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
      <bpmn:multiInstanceLoopCharacteristics>
        <bpmn:loopDataInputRef>DataInput_Pets</bpmn:loopDataInputRef>
        <bpmn:inputDataItem name="pet" itemSubjectRef="ItemDefinition_Pet"/>
      </bpmn:multiInstanceLoopCharacteristics>

      <!-- 子流程任务 -->
      <bpmn:serviceTask id="Task_Health_1" name="分析活动记录模式">
        <bpmn:outgoing>Flow_Health_1</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:serviceTask id="Task_Health_2" name="检查状态记录">
        <bpmn:incoming>Flow_Health_1</bpmn:incoming>
        <bpmn:outgoing>Flow_Health_2</bpmn:outgoing>
      </bpmn:serviceTask>

      <bpmn:exclusiveGateway id="Gateway_Health" name="需要提醒?">
        <bpmn:incoming>Flow_Health_2</bpmn:incoming>
        <bpmn:outgoing>Flow_Alert</bpmn:outgoing>
        <bpmn:outgoing>Flow_Normal</bpmn:outgoing>
      </bpmn:exclusiveGateway>

      <bpmn:sendTask id="Task_Alert" name="发送健康提醒">
        <bpmn:incoming>Flow_Alert</bpmn:incoming>
        <bpmn:outgoing>Flow_Health_End</bpmn:outgoing>
      </bpmn:sendTask>

      <bpmn:serviceTask id="Task_Normal" name="记录健康状态">
        <bpmn:incoming>Flow_Normal</bpmn:incoming>
        <bpmn:outgoing>Flow_Health_End</bpmn:outgoing>
      </bpmn:serviceTask>

      <!-- 子流程内部流程线 -->
      <bpmn:sequenceFlow id="Flow_Health_1" sourceRef="Task_Health_1" targetRef="Task_Health_2"/>
      <bpmn:sequenceFlow id="Flow_Health_2" sourceRef="Task_Health_2" targetRef="Gateway_Health"/>
      <bpmn:sequenceFlow id="Flow_Alert" sourceRef="Gateway_Health" targetRef="Task_Alert">
        <bpmn:conditionExpression>需要健康提醒</bpmn:conditionExpression>
      </bpmn:sequenceFlow>
      <bpmn:sequenceFlow id="Flow_Normal" sourceRef="Gateway_Health" targetRef="Task_Normal">
        <bpmn:conditionExpression>健康状态正常</bpmn:conditionExpression>
      </bpmn:sequenceFlow>
      <bpmn:sequenceFlow id="Flow_Health_End" sourceRef="Task_Alert" targetRef="SubProcess_1_End"/>
      <bpmn:sequenceFlow id="Flow_Health_End" sourceRef="Task_Normal" targetRef="SubProcess_1_End"/>
    </bpmn:subProcess>

    <!-- 数据处理任务 -->
    <bpmn:serviceTask id="Task_2" name="生成健康报告">
      <bpmn:incoming>Flow_3</bpmn:incoming>
      <bpmn:outgoing>Flow_4</bpmn:outgoing>
    </bpmn:serviceTask>

    <!-- 发送任务 -->
    <bpmn:sendTask id="Task_3" name="发送统计数据">
      <bpmn:incoming>Flow_4</bpmn:incoming>
      <bpmn:outgoing>Flow_5</bpmn:outgoing>
    </bpmn:sendTask>

    <!-- 结束事件 -->
    <bpmn:endEvent id="EndEvent_1" name="健康监控完成">
      <bpmn:incoming>Flow_5</bpmn:incoming>
    </bpmn:endEvent>

    <!-- 流程线 -->
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="SubProcess_1"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="SubProcess_1" targetRef="Task_2"/>
    <bpmn:sequenceFlow id="Flow_4" sourceRef="Task_2" targetRef="Task_3"/>
    <bpmn:sequenceFlow id="Flow_5" sourceRef="Task_3" targetRef="EndEvent_1"/>

  </bpmn:process>
</bpmn:definitions>
```

**流程说明**:
1. 系统每日定时启动健康监控
2. 获取所有宠物列表
3. 为每个宠物并行执行健康分析：
   - 分析活动记录模式
   - 检查状态记录
   - 判断是否需要健康提醒
4. 生成健康报告
5. 发送统计数据

## 业务规则定义

### 决策表

#### 活动记录验证规则

| 条件 | 活动名称 | 活动描述 | 活动日期 | 结果 |
|-----|---------|---------|---------|------|
| 必填 | 空 | - | - | 验证失败 |
| - | 有效 | 空 | - | 验证失败 |
| - | 有效 | 有效 | 未来日期 | 验证失败 |
| - | 有效 | 有效 | 过去日期 | 验证通过 |

#### 健康提醒触发条件

| 宠物状态 | 活动频率 | 最近体检 | 提醒类型 |
|---------|---------|---------|---------|
| 生病中 | 任何 | 任何 | 医疗提醒 |
| 健康 | 低于平均值 | 超过6个月 | 体检提醒 |
| 健康 | 正常 | 正常 | 无提醒 |
| 老年宠物 | 任何 | 任何 | 老年护理提醒 |

### 网络异常处理

```xml
<bpmn:boundaryEvent id="NetworkError" attachedToRef="ServiceCall">
  <bpmn:errorEventDefinition errorRef="NetworkTimeoutError"/>
  <bpmn:outgoing>Flow_Error_Handler</bpmn:outgoing>
</bpmn:boundaryEvent>

<bpmn:subProcess id="ErrorHandler" name="错误处理子流程">
  <bpmn:serviceTask id="LogError" name="记录错误日志"/>
  <bpmn:sendTask id="NotifyUser" name="通知用户"/>
  <bpmn:serviceTask id="RetryOrFallback" name="重试或降级"/>
</bpmn:subProcess>
```

### 业务异常处理

```xml
<bpmn:eventSubProcess id="BusinessExceptionHandler" triggeredByEvent="true">
  <bpmn:startEvent id="BusinessErrorStart">
    <bpmn:errorEventDefinition errorRef="BusinessLogicError"/>
  </bpmn:startEvent>

  <bpmn:serviceTask id="ValidateData" name="验证数据完整性"/>
  <bpmn:exclusiveGateway id="CanRecover" name="可以恢复?"/>
  <bpmn:serviceTask id="RecoverData" name="恢复数据"/>
  <bpmn:sendTask id="NotifyAdmin" name="通知管理员"/>
</bpmn:eventSubProcess>
```

## 性能监控指标

### BPMN执行指标

1. **流程实例数量**: 每分钟创建的流程实例数
2. **平均执行时间**: 流程从开始到结束的平均时间
3. **错误率**: 流程执行失败的比例
4. **资源利用率**: 流程执行期间的CPU/内存使用率

### 关键性能指标 (KPIs)

- **用户注册转化率**: 访问注册页面到成功注册的比例
- **活动记录频率**: 用户平均每天记录活动次数
- **社区活跃度**: 每日发布动态和互动的数量
- **系统响应时间**: API接口的平均响应时间

这些BPMN模型完整地描述了Pet Care Ecosystem系统的核心业务流程，为系统设计、开发和维护提供了重要参考。


