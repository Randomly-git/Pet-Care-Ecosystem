import os
# 1. 强制启用镜像源（解决连接问题）
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

import pandas as pd
import torch
import joblib
import numpy as np
from sklearn.model_selection import train_test_split
from transformers import BertTokenizer, BertForSequenceClassification, Trainer, TrainingArguments
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, precision_recall_fscore_support

# --- 评价指标计算函数 ---
def compute_metrics(pred):
    """计算准确率、F1、精确率和召回率"""
    labels = pred.label_ids
    preds = pred.predictions.argmax(-1)
    precision, recall, f1, _ = precision_recall_fscore_support(labels, preds, average='weighted')
    acc = accuracy_score(labels, preds)
    return {
        'accuracy': acc,
        'f1': f1,
        'precision': precision,
        'recall': recall
    }

# 1. 数据准备 (指向 V2 版本)
data_path = r'D:\petcare-ai\pet-health-symptoms-v2.csv'
if not os.path.exists(data_path):
    raise FileNotFoundError(f"找不到数据集：{data_path}，请先运行合并脚本生成 V2 数据。")

df = pd.read_csv(data_path)
le = LabelEncoder()
df['label'] = le.fit_transform(df['condition'])

# 确保模型保存文件夹存在
save_path = r'D:\petcare-ai\pet_bert_model'
if not os.path.exists(save_path):
    os.makedirs(save_path)

# 保存 LabelEncoder (包含新增的 Normal 类别)
joblib.dump(le, os.path.join(save_path, 'label_encoder.pkl'))

# 2. 加载预训练分词器
tokenizer = BertTokenizer.from_pretrained('bert-base-chinese')

# 划分数据集
train_texts, val_texts, train_labels, val_labels = train_test_split(
    df['text'].tolist(), df['label'].tolist(), test_size=0.2, random_state=42
)

# 生成编码后的 Dataset
train_encodings = tokenizer(train_texts, padding="max_length", truncation=True, max_length=128)
val_encodings = tokenizer(val_texts, padding="max_length", truncation=True, max_length=128)

class PetDataset(torch.utils.data.Dataset):
    def __init__(self, encodings, labels):
        self.encodings = encodings
        self.labels = labels
    def __getitem__(self, idx):
        item = {key: torch.tensor(val[idx]) for key, val in self.encodings.items()}
        item['labels'] = torch.tensor(self.labels[idx])
        return item
    def __len__(self):
        return len(self.labels)

train_dataset = PetDataset(train_encodings, train_labels)
val_dataset = PetDataset(val_encodings, val_labels)

# 3. 加载 BERT 模型 (num_labels 会自动设为 6)
model = BertForSequenceClassification.from_pretrained(
    'bert-base-chinese',
    num_labels=len(le.classes_)
)

# 4. 训练参数设置
training_args = TrainingArguments(
    output_dir='./results',
    num_train_epochs=5,              # 5轮训练 [cite: 441]
    per_device_train_batch_size=16,
    eval_strategy="epoch",           # 修正参数名
    save_strategy="epoch",
    logging_dir='./logs',
    logging_steps=10,
    disable_tqdm=False,
    load_best_model_at_end=True,     # 自动加载最优模型
    metric_for_best_model="accuracy", # 以准确率作为评判标准
    save_total_limit=2,              # 只保留最新的 2 个模型 [cite: 433]
    log_level="info"
)

# 5. 开启训练
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=val_dataset,
    compute_metrics=compute_metrics  # 传入指标计算函数
)

print(f"\n🚀 正在启动训练，当前类别总数: {len(le.classes_)} ({list(le.classes_)})")
trainer.train()

# 6. 最终保存
model.save_pretrained(save_path)
tokenizer.save_pretrained(save_path)
print(f"\n✅ 训练完成！模型、分词器及 LabelEncoder 已保存至: {save_path}")