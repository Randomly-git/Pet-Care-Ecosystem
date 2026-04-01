import pandas as pd
import requests
import hashlib
import random
import time
from tqdm import tqdm

# --- 请在此处填写你的百度翻译配置 ---
APP_ID = '20260325002580426'
SECRET_KEY = 'x18bLXK1a8bBVYDMJgaf'


def baidu_translate(text, from_lang='en', to_lang='zh'):
    if not text or pd.isna(text): return ""

    endpoint = 'https://fanyi-api.baidu.com/api/trans/vip/translate'
    salt = random.randint(32768, 65536)
    sign = hashlib.md5((APP_ID + text + str(salt) + SECRET_KEY).encode('utf-8')).hexdigest()

    params = {
        'q': text, 'from': from_lang, 'to': to_lang,
        'appid': APP_ID, 'salt': salt, 'sign': sign
    }

    try:
        r = requests.get(endpoint, params=params, timeout=10)
        res = r.json()

        # 如果成功，返回结果
        if 'trans_result' in res:
            return res['trans_result'][0]['dst']
        else:
            # 如果失败，打印百度返回的错误码，方便对症下药
            print(f"\n百度返回错误: {res}")
            return f"Error: {res.get('error_code')}"

    except Exception as e:
        return f"Request Error: {e}"


# 注意：在循环里把 time.sleep 调大一点，确保每秒只发 1 个请求
# time.sleep(1.5)


# 路径配置
input_file = r'D:\petcare-ai\pet-health-symptoms-filtered.csv'
output_file = r'D:\petcare-ai\pet-health-symptoms-baidu-cn.csv'

# 读取数据并过滤
df = pd.read_csv(input_file)

print(f"开始使用百度翻译，共 {len(df)} 条...")

for i in tqdm(range(len(df))):
    df.loc[i, 'text'] = baidu_translate(df.loc[i, 'text'])

    # 每 100 条保存一次
    if (i + 1) % 100 == 0 or (i + 1) == len(df):
        df[['text', 'condition']].to_csv(output_file, index=False, encoding='utf-8-sig')

print(f"✅ 翻译完成！已保存至 {output_file}")