import os
import joblib
import torch
import uvicorn
import nacos
import pika
import json
import threading
import time
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import BertTokenizer, BertForSequenceClassification
import socket

# --- 配置区 ---
# Python 服务不使用 Spring Cloud，因此 Nacos 和 RabbitMQ 连接都由本文件显式配置。
# 生产环境应通过环境变量或密钥管理服务注入地址和凭据，不应把真实密码提交到代码仓库。
NACOS_ADDR = "8.136.222.169:8848"
# 该名称必须与网关中的 lb://bert_ai_service 完全一致。
SERVICE_NAME = "bert_ai_service"
QUEUE_NAME = "pet_health_analysis_queue"
RESULT_QUEUE = "pet_analysis_result_queue"

# --- RabbitMQ 配置 ---
MQ_HOST = "8.136.222.169"
MQ_PORT = 5672
MQ_USER = "admin"
MQ_PASS = "Sun2004"
MQ_VIRTUAL_HOST = "/"

app = FastAPI()

# 1. 动态获取模型路径
# 获取当前脚本所在目录的绝对路径
current_dir = os.path.dirname(os.path.abspath(__file__))
# 拼接模型文件夹路径（假设 pet_bert_model 就在该脚本同级目录下）
model_path = os.path.join(current_dir, 'pet_bert_model')

print(f"⏳ 正在从路径加载 BERT 模型: {model_path}")

try:
    tokenizer = BertTokenizer.from_pretrained(model_path)
    model = BertForSequenceClassification.from_pretrained(model_path)
    # 使用 os.path.join 确保在 Linux/Windows 下路径分隔符都正确
    le = joblib.load(os.path.join(model_path, 'label_encoder.pkl'))
    model.eval()
    print("✅ 模型加载完成")
except Exception as e:
    print(f"❌ 模型加载失败，请检查项目目录下是否存在 'pet_bert_model' 文件夹。错误: {e}")
    exit(1) # 模型加载失败直接退出，防止后续报错


# --- 2. Nacos 注册与心跳逻辑 ---
def keep_alive(client, service_name, ip, port):
    """守护线程：每 5 秒向 Nacos 发送一次心跳，保持实例为健康状态。"""
    while True:
        try:
            # Nacos 临时实例需要持续心跳，否则会被注册中心判定为失效并从发现列表移除。
            client.send_heartbeat(service_name, ip, port)
        except Exception as e:
            print(f"⚠️ 心跳发送失败: {e}")
        time.sleep(5)


def get_host_ip():
    """查询本机真实 IP；不能注册 127.0.0.1，否则其他机器无法访问 AI 服务。"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # 通过 UDP socket 选择实际出网网卡；不会真正发送业务数据。
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


def register_nacos():
    try:
        # 创建 Nacos 命名服务客户端；namespace public 必须与网关使用的命名空间一致。
        client = nacos.NacosClient(NACOS_ADDR, namespace="public")

        # 自动获取当前机器的真实 IP
        local_ip = get_host_ip()
        print(f"📡 检测到本机 IP: {local_ip}")

        # 注册固定的 FastAPI 端口；Nacos 返回的实例地址会被网关用于 lb:// 转发。
        client.add_naming_instance(SERVICE_NAME, local_ip, 8000)

        # 心跳必须使用与注册相同的 service、IP 和端口，否则续租不到原实例。
        heartbeat_thread = threading.Thread(
            target=keep_alive,
            args=(client, SERVICE_NAME, local_ip, 8000),
            daemon=True
        )
        heartbeat_thread.start()
        print(f"✅ Nacos 注册成功: {SERVICE_NAME}@{local_ip}:8000")
    except Exception as e:
        print(f"❌ Nacos 注册失败: {e}")


# --- 3. BERT 推理逻辑 ---
def run_inference(text):
    inputs = tokenizer(text, return_tensors="pt", padding=True, truncation=True, max_length=128)
    with torch.no_grad():
        outputs = model(**inputs)
        probs = torch.nn.functional.softmax(outputs.logits, dim=-1)
        conf, pred_idx = torch.max(probs, dim=1)

    label = le.inverse_transform([pred_idx.item()])[0]
    return label, conf.item()


# --- 4. RabbitMQ 消费者逻辑 ---
def start_mq_consumer():
    try:
        # 使用 RabbitMQ 用户名和密码建立 Python 消费端连接；凭据应在生产环境外置。
        credentials = pika.PlainCredentials(MQ_USER, MQ_PASS)
        parameters = pika.ConnectionParameters(
            host=MQ_HOST,
            port=MQ_PORT,
            virtual_host=MQ_VIRTUAL_HOST,
            credentials=credentials,
            heartbeat=600  # 推理可能耗时较长，延长连接心跳间隔避免被误判断开。
        )

        conn = pika.BlockingConnection(parameters)
        channel = conn.channel()

        # 与 Java ActivityColdDataRabbitMQConfig 使用相同队列名，声明操作是幂等的。
        channel.queue_declare(queue=QUEUE_NAME, durable=True)
        channel.queue_declare(queue=RESULT_QUEUE, durable=True)

        def callback(ch, method, properties, body):
            try:
                data = json.loads(body)
                record_id = data.get("activityRecordId")
                text = data.get("text")

                print(f"📥 收到任务: ID={record_id}, 文本内容='{text}'")

                # 只有成功完成推理并准备好结果后才进入回传阶段。
                label, confidence = run_inference(text)

                # 结果队列是 Java AiResultListener 的输入，activityRecordId 用于关联原任务。
                result = {
                    "activityRecordId": record_id,
                    "label": label,
                    "confidence": round(confidence, 4)
                }

                # 使用默认交换机，routing_key 直接指向结果队列。
                channel.basic_publish(
                    exchange='',
                    routing_key=RESULT_QUEUE,
                    body=json.dumps(result)
                )
                print(f"✅ 处理完成: {label} ({round(confidence, 4)})")
                # 成功回传后确认原任务，RabbitMQ 才会删除该消息。
                ch.basic_ack(delivery_tag=method.delivery_tag)
            except Exception as e:
                print(f"❌ 处理消息时出错: {e}")
                # 当前策略是失败也确认，避免坏消息无限重投；如需可靠重试，应改为 nack/requeue 或死信。
                ch.basic_ack(delivery_tag=method.delivery_tag)

        # 一次只给本进程一条任务，避免 BERT 推理期间积压大量未完成消息。
        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=QUEUE_NAME, on_message_callback=callback)
        print(f"🚀 MQ 消费者已启动，监听队列: {QUEUE_NAME}")
        channel.start_consuming()

    except Exception as e:
        print(f"❌ MQ 连接或运行失败: {e}")


# --- 5. 启动入口 ---
if __name__ == "__main__":
    # 先注册 Nacos，让网关尽早发现服务实例。
    register_nacos()

    # MQ 消费者放入后台线程，不能阻塞 FastAPI 健康检查和 HTTP 推理接口。
    mq_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    mq_thread.start()

    # 监听所有网卡的 8000 端口，该端口必须与 Nacos 注册信息保持一致。
    uvicorn.run(app, host="0.0.0.0", port=8000)