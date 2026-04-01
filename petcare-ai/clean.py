import pandas as pd

# 1. 设置文件路径
input_file = r'D:\petcare-ai\pet-health-symptoms-dataset.csv'
output_file = r'D:\petcare-ai\pet-health-symptoms-filtered.csv'

try:
    # 2. 读取 CSV 文件
    # 注意：如果文件中有中文，建议加上 encoding='utf-8' 或 'gbk'
    df = pd.read_csv(input_file)

    # 3. 执行过滤：仅保留 record_type 为 'Owner Observation' 的行
    filtered_df = df[df['record_type'] == 'Owner Observation']

    # 4. 保存结果
    # index=False 表示不保存行索引
    filtered_df.to_csv(output_file, index=False, encoding='utf-8-sig')

    print(f"处理成功！")
    print(f"原始数据条数: {len(df)}")
    print(f"保留的宠主记录条数: {len(filtered_df)}")
    print(f"文件已保存至: {output_file}")

except FileNotFoundError:
    print("错误：找不到指定的 CSV 文件，请检查路径是否正确。")
except Exception as e:
    print(f"发生错误: {e}")