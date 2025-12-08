# media_uploader.py
import requests
import os


def main():
    print("🐾 宠物媒体文件上传工具 🐾")
    print("=" * 40)

    try:
        # 获取用户输入
        pet_id = int(input("请输入宠物ID: "))
        file_path = input("请输入文件完整路径: ")

        print("\n请选择文件类型:")
        print("1. 宠物头像 (PET_AVATAR)")
        print("2. 活动记录 (ACTIVITY)")
        print("3. 状态记录 (STATUS)")
        print("4. 动态 (MOMENT)")

        choice = input("请选择 (1-4): ").strip()
        type_map = {"1": "PET_AVATAR", "2": "ACTIVITY", "3": "STATUS", "4": "MOMENT"}
        related_type = type_map.get(choice, "PET_AVATAR")

        if related_type == "PET_AVATAR":
            related_id = pet_id
        else:
            related_id = int(input("请输入关联的业务ID: "))

        # 上传文件
        print(f"\n📤 开始上传文件: {os.path.basename(file_path)}")
        upload_file(file_path, pet_id, related_type, related_id)

    except Exception as e:
        print(f"❌ 错误: {e}")
    finally:
        input("\n👋 按回车键退出...")


def upload_file(file_path, pet_id, related_type, related_id):
    if not os.path.exists(file_path):
        raise Exception("文件不存在")

    url = "http://localhost:8082/api/media/upload"

    # 添加调试信息
    print(f"🔧 调试信息:")
    print(f"  - related_type: '{related_type}'")
    print(f"  - 类型: {type(related_type)}")
    print(f"  - 长度: {len(related_type)}")

    with open(file_path, 'rb') as file:
        files = {'file': (os.path.basename(file_path), file)}
        data = {
            'petId': pet_id,
            'relatedType': related_type,
            'relatedId': related_id
        }

        response = requests.post(url, files=files, data=data)

        if response.status_code == 200:
            result = response.json()
            if result.get('success'):
                data = result.get('data', {})
                print("✅ 上传成功！")
                print(f"📁 文件ID: {data.get('mediaId')}")
                print(f"🔗 文件URL: {data.get('fileUrl')}")
                print(f"📊 文件大小: {data.get('fileSize')} bytes")
            else:
                raise Exception(f"上传失败: {result.get('message')}")
        else:
            raise Exception(f"HTTP错误: {response.status_code} - {response.text}")


if __name__ == "__main__":
    main()