import torch
from transformers import BertTokenizer, BertForSequenceClassification
import joblib
import torch.nn.functional as F

# 1. 加载训练好的模型、分词器和标签转换器
model_path = r'D:\petcare-ai\pet_bert_model'
tokenizer = BertTokenizer.from_pretrained(model_path)
model = BertForSequenceClassification.from_pretrained(model_path)
le = joblib.load( model_path + r'\label_encoder.pkl')


def predict(text):
    # 将模型设置为评估模式
    model.eval()

    # 2. 对输入文字进行分词
    inputs = tokenizer(text, return_tensors="pt", padding=True, truncation=True, max_length=128)

    # 3. 推理（不计算梯度，节省内存）
    with torch.no_grad():
        outputs = model(**inputs)
        logits = outputs.logits

        # 4. 核心：使用 Softmax 将原始分数转为百分比概率
        probabilities = F.softmax(logits, dim=1).flatten()

    # 5. 组合结果
    results = []
    for i, prob in enumerate(probabilities):
        condition_name = le.inverse_transform([i])[0]
        results.append((condition_name, prob.item()))

    # 按置信度从高到低排序
    results = sorted(results, key=lambda x: x[1], reverse=True)

    print(f"\n诊断描述: {text}")
    print("-" * 30)
    for condition, confidence in results:
        print(f"{condition}: {confidence:.2%}")


# 测试一下
if __name__ == "__main__":
    test_text = "我家的狗粪便中有白色条状物，会动。"
    predict(test_text)