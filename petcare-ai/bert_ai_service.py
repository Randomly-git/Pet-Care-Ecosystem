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
NACOS_ADDR = "8.136.222.169:8848"
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
    """守护线程：每 5 秒向 Nacos 发送一次心跳"""
    while True:
        try:
            client.send_heartbeat(service_name, ip, port)
        except Exception as e:
            print(f"⚠️ 心跳发送失败: {e}")
        time.sleep(5)


def get_host_ip():
    """查询本机真实 IP 地址"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))  # 连接一个外部地址来诱导系统选择正确的网卡
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


def register_nacos():
    try:
        client = nacos.NacosClient(NACOS_ADDR, namespace="public")

        # 自动获取当前机器的真实 IP
        local_ip = get_host_ip()
        print(f"📡 检测到本机 IP: {local_ip}")

        # 使用动态获取的 IP 进行注册
        client.add_naming_instance(SERVICE_NAME, local_ip, 8000)

        # 心跳线程也要同步使用这个 local_ip
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
        credentials = pika.PlainCredentials(MQ_USER, MQ_PASS)
        parameters = pika.ConnectionParameters(
            host=MQ_HOST,
            port=MQ_PORT,
            virtual_host=MQ_VIRTUAL_HOST,
            credentials=credentials,
            heartbeat=600  # 防止推理耗时导致断连
        )

        conn = pika.BlockingConnection(parameters)
        channel = conn.channel()

        # 声明队列
        channel.queue_declare(queue=QUEUE_NAME, durable=True)
        channel.queue_declare(queue=RESULT_QUEUE, durable=True)

        def callback(ch, method, properties, body):
            try:
                data = json.loads(body)
                record_id = data.get("activityRecordId")
                text = data.get("text")

                print(f"📥 收到任务: ID={record_id}, 文本内容='{text}'")

                # 执行推理
                label, confidence = run_inference(text)

                # 构造结果回传给 Java
                result = {
                    "activityRecordId": record_id,
                    "label": label,
                    "confidence": round(confidence, 4)
                }

                channel.basic_publish(
                    exchange='',
                    routing_key=RESULT_QUEUE,
                    body=json.dumps(result)
                )
                print(f"✅ 处理完成: {label} ({round(confidence, 4)})")
                ch.basic_ack(delivery_tag=method.delivery_tag)
            except Exception as e:
                print(f"❌ 处理消息时出错: {e}")
                # 即使失败也确认，避免死循环请求
                ch.basic_ack(delivery_tag=method.delivery_tag)

        channel.basic_qos(prefetch_count=1)
        channel.basic_consume(queue=QUEUE_NAME, on_message_callback=callback)
        print(f"🚀 MQ 消费者已启动，监听队列: {QUEUE_NAME}")
        channel.start_consuming()

    except Exception as e:
        print(f"❌ MQ 连接或运行失败: {e}")


# --- 5. 启动入口 ---
if __name__ == "__main__":
    # 启动 Nacos 注册
    register_nacos()

    # 启动 MQ 消费者线程
    mq_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    mq_thread.start()

    # 启动 FastAPI 服务 (用于健康检查或手动调用)
    uvicorn.run(app, host="0.0.0.0", port=8000)