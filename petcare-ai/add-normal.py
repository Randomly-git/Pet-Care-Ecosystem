import pandas as pd
import os

# 1. 路径定义
old_csv_path = r'D:\petcare-ai\pet-health-symptoms-translated.csv'
txt_path = r'D:\petcare-ai\normal_records.txt'
new_csv_path = r'D:\petcare-ai\pet-health-symptoms-v2.csv'

# 2. 读取 200 条正常记录
if os.path.exists(txt_path):
    with open(txt_path, 'r', encoding='utf-8') as f:
        # 去掉空格和换行符，过滤掉空行
        normal_texts = [line.strip() for line in f.readlines() if line.strip()]

    print(f"✅ 从 TXT 文件中成功读取了 {len(normal_texts)} 条记录。")

    # 校验是否足额（可选）
    if len(normal_texts) < 200:
        print(f"⚠️ 警告：TXT 只有 {len(normal_texts)} 条，不足 200 条。")
else:
    print(f"❌ 找不到文件：{txt_path}")
    exit()

# 3. 处理 CSV 合并
if os.path.exists(old_csv_path):
    df_old = pd.read_csv(old_csv_path)

    # 构造 Normal 类别数据
    df_normal = pd.DataFrame({
        'text': normal_texts,
        'condition': ['Normal'] * len(normal_texts),
    })

    # 合并
    df_v2 = pd.concat([df_old, df_normal], ignore_index=True)

    # 打乱顺序
    df_v2 = df_v2.sample(frac=1).reset_index(drop=True)

    # 保存结果
    df_v2.to_csv(new_csv_path, index=False, encoding='utf-8-sig')
    print(f"✅ 合并成功！新文件已生成：{new_csv_path}")
    print(f"数据总计：{len(df_v2)} 条（含 Normal 类 {len(df_normal)} 条）")
else:
    print(f"❌ 找不到原 CSV 文件：{old_csv_path}")