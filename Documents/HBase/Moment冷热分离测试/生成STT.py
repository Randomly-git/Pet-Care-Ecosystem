import pandas as pd
from itertools import product

# 读取 Excel 文件
file_path = "Moment_Testing_Engineering - 修改后.xlsx"  # 请根据实际路径修改
df_stt = pd.read_excel(file_path, sheet_name="逻辑模型", header=None)

# 找到状态转移表起始行（假设前几行是标题，实际需要手动定位，这里简化）
# 实际逻辑模型 sheet 中，STT 在 "当前状态" 这一列开始
# 为了通用性，我们假设读取的 df 中第 5 行开始是表头（0-index 为 4）
# 但更可靠：根据关键词 "当前状态" 定位

def find_stt_start(df):
    for idx, row in df.iterrows():
        if row.astype(str).str.contains("当前状态").any():
            return idx
    return None

start_row = find_stt_start(df_stt)
if start_row is None:
    raise ValueError("未找到状态转移表起始行")

# 读取表头
header_row = df_stt.iloc[start_row].tolist()
# 从下一行开始读取数据，直到遇到空行或 "决策表"
data_rows = []
for idx in range(start_row + 1, len(df_stt)):
    row = df_stt.iloc[idx].tolist()
    if pd.isna(row[0]) or (isinstance(row[0], str) and row[0].strip() == ""):
        break
    if "决策表" in str(row[0]):
        break
    data_rows.append(row)

# 构建原始 STT 的 DataFrame
original_stt = pd.DataFrame(data_rows, columns=header_row[:len(data_rows[0])])
original_stt = original_stt[["当前状态", "输入事件", "约束条件(Guard)", "目标状态", "动作/输出"]]
original_stt.dropna(subset=["当前状态"], inplace=True)

# 提取所有唯一的状态和输入事件
all_states = sorted(original_stt["当前状态"].unique())
all_events = sorted(original_stt["输入事件"].unique())

# 生成所有组合
all_combinations = list(product(all_states, all_events))

# 构建查找字典
stt_dict = {}
for _, row in original_stt.iterrows():
    key = (row["当前状态"], row["输入事件"])
    stt_dict[key] = {
        "约束条件(Guard)": row["约束条件(Guard)"],
        "目标状态": row["目标状态"],
        "动作/输出": row["动作/输出"]
    }

# 生成完整 STT
full_stt_rows = []
for state, event in all_combinations:
    key = (state, event)
    if key in stt_dict:
        guard = stt_dict[key]["约束条件(Guard)"]
        target = stt_dict[key]["目标状态"]
        action = stt_dict[key]["动作/输出"]
    else:
        guard = "Undefined"
        target = "Undefined"
        action = "Undefined"
    full_stt_rows.append([state, event, guard, target, action])

full_stt_df = pd.DataFrame(full_stt_rows, columns=["当前状态", "输入事件", "约束条件(Guard)", "目标状态", "动作/输出"])

# 保存为新的 Excel 文件
output_path = "Complete_STT.xlsx"
with pd.ExcelWriter(output_path, engine="openpyxl") as writer:
    full_stt_df.to_excel(writer, sheet_name="完整状态转移表", index=False)
    original_stt.to_excel(writer, sheet_name="原始STT（参考）", index=False)

print(f"完整 STT 已生成，共 {len(full_stt_df)} 行，保存至 {output_path}")