import requests
import json

# --- 配置区 ---
BASE_URL = "http://127.0.0.1:9000/api/activities"  # 替换为你的 Spring Boot 后端地址
PET_ID = 1  # 现有宠物 ID
ACTIVITY_ID = 40  # 现有活动 ID (如：喂食、洗澡)
USER_ID = 2  # 执行人 ID
FILE_PATH = r"D:\test_pet.jpg"  # 想要上传的测试图片路径（可选）


def test_create_activity_record():
    url = f"{BASE_URL}/records/pet/{PET_ID}"

    # 1. 准备普通参数 (RequestParam)
    # 注意：Spring Boot 的 @RequestParam 在 requests 中通过 data 参数传递
    payload = {
        'activityId': ACTIVITY_ID,
        'userId': USER_ID,
        'description': "今天狗狗胃口不太好，只吃了很少狗粮。",  # 触发 BERT 分析的文本
        'date': "2026-03-31T15:00:00"  # 符合 LocalDateTime 格式
    }

    # 2. 准备文件 (MultipartFile)
    files = []
    file_obj = None
    try:
        # 如果文件路径存在，则打开文件
        file_obj = open(FILE_PATH, 'rb')
        files = [('file', (FILE_PATH.split('\\')[-1], file_obj, 'image/jpeg'))]
        print(f"📦 准备上传文件: {FILE_PATH}")
    except FileNotFoundError:
        print("⚠️ 未找到测试文件，将发送不带文件的请求")
        files = None

    try:
        print(f"🚀 正在发送请求到: {url} ...")

        # 发送 POST 请求
        # data 处理普通表单字段，files 处理 MultipartFile
        response = requests.post(url, data=payload, files=files)

        # 3. 结果处理
        print(f"📡 状态码: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            print("✅ 接口调用成功！")
            print(f"📝 返回记录ID: {result.get('activityRecordId')}")
            print(f"📄 响应内容: {json.dumps(result, indent=4, ensure_ascii=False)}")
            print("\n💡 提示：现在请检查 Python BERT 服务控制台和 RabbitMQ 管理界面，看是否收到了消息。")
        else:
            print(f"❌ 接口调用失败: {response.text}")

    except Exception as e:
        print(f"💥 发生异常: {e}")
    finally:
        if file_obj:
            file_obj.close()

def test_get_abnormal_records():
    """测试获取宠物异常健康记录接口"""
    url = f"{BASE_URL}/records/pet/{PET_ID}/abnormal"
    print(f"\n🔍 正在获取宠物 {PET_ID} 的异常记录...")

    try:
        response = requests.get(url)
        print(f"📡 状态码: {response.status_code}")
        if response.status_code == 200:
            records = response.json()
            print(f"✅ 成功获取到 {len(records)} 条异常记录:")
            for r in records:
                print(f"   - 记录ID: {r.get('activityRecordId')}, 描述: {r.get('activityDescription')}")
        else:
            print(f"❌ 获取失败: {response.text}")
    except Exception as e:
        print(f"💥 请求异常: {e}")

def test_ignore_abnormal_record(record_id):
    """测试忽略异常记录接口"""
    url = f"{BASE_URL}/records/{record_id}/ignore"
    print(f"\n🙈 正在尝试忽略记录 ID: {record_id} ...")

    try:
        # 使用 PUT 方法
        response = requests.put(url)
        print(f"📡 状态码: {response.status_code}")
        if response.status_code == 200:
            print(f"✅ 记录 {record_id} 已成功标记为忽略 (-1)")
        else:
            print(f"❌ 操作失败: {response.text}")
    except Exception as e:
        print(f"💥 请求异常: {e}")


if __name__ == "__main__":
    # 1. 先看看有哪些异常记录
    test_get_abnormal_records()

    # 2. 尝试忽略其中一条记录 (请确保该 ID 在数据库中确实存在)
    test_ignore_abnormal_record(30)