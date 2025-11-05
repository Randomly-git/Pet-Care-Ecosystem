from flask import Blueprint, request, jsonify
from .services import PetService, MomentService
from .models import db, Pet, PetMoment

api_bp = Blueprint('api', __name__)

def create_sample_data():
    """创建示例数据"""
    try:
        # 检查是否已有数据
        if Pet.query.first():
            print("已有数据，跳过示例数据创建")
            return
        
        print("正在创建示例数据...")
        
        # 创建示例宠物
        pets = [
            Pet(name='咪咪', species='猫', breed='英国短毛猫', avatar_url=''),
            Pet(name='旺财', species='狗', breed='金毛犬', avatar_url=''),
            Pet(name='小白', species='猫', breed='波斯猫', avatar_url='')
        ]
        for pet in pets:
            db.session.add(pet)
        
        db.session.commit()
        print("示例宠物创建完成")
        
        # 创建示例动态
        moments = [
            PetMoment(pet_id=1, content='今天天气真好，出去散步啦！🐾', media_urls=[]),
            PetMoment(pet_id=1, content='新买的玩具超级喜欢！玩了一整天都不腻～', media_urls=[]),
            PetMoment(pet_id=2, content='今天去公园玩得很开心！遇到了好多小伙伴🎾', media_urls=[]),
            PetMoment(pet_id=3, content='优雅地晒太阳中... 这才是猫生啊☀️', media_urls=[])
        ]
        for moment in moments:
            db.session.add(moment)
        
        db.session.commit()
        print("示例动态创建完成")
        
    except Exception as e:
        print(f"创建示例数据时出错: {e}")
        db.session.rollback()

@api_bp.route('/pets', methods=['GET'])
def get_all_pets():
    """获取所有宠物"""
    pets = PetService.get_all_pets()
    return jsonify([{
        'id': p.id,
        'name': p.name,
        'species': p.species,
        'breed': p.breed,
        'avatar_url': p.avatar_url,
        'created_at': p.created_at.isoformat() if p.created_at else None
    } for p in pets])

@api_bp.route('/pets/<int:pet_id>', methods=['GET'])
def get_pet(pet_id):
    """获取特定宠物信息"""
    pet = PetService.get_pet_by_id(pet_id)
    if not pet:
        return jsonify({'error': '宠物不存在'}), 404
    
    return jsonify({
        'id': pet.id,
        'name': pet.name,
        'species': pet.species,
        'breed': pet.breed,
        'avatar_url': pet.avatar_url,
        'created_at': pet.created_at.isoformat() if pet.created_at else None
    })

@api_bp.route('/pets/<int:pet_id>/moments', methods=['GET'])
def get_pet_moments(pet_id):
    """获取宠物的动态"""
    moments = MomentService.get_moments_by_pet(pet_id)
    return jsonify([{
        'id': m.id,
        'content': m.content,
        'media_urls': m.media_urls or [],
        'created_at': m.created_at.isoformat() if m.created_at else None,
        'comment_count': 0,
        'like_count': 0
    } for m in moments])

@api_bp.route('/pets/<int:pet_id>/moments', methods=['POST'])
def create_moment(pet_id):
    """创建动态"""
    try:
        data = request.get_json()
        if not data or not data.get('content'):
            return jsonify({'error': '内容不能为空'}), 400
        
        # 检查宠物是否存在
        pet = Pet.query.get(pet_id)
        if not pet:
            return jsonify({'error': '宠物不存在'}), 404
        
        # 创建动态
        new_moment = PetMoment(
            pet_id=pet_id,
            content=data.get('content'),
            media_urls=data.get('media_urls', [])
        )
        
        db.session.add(new_moment)
        db.session.commit()
        
        return jsonify({
            'id': new_moment.id,
            'content': new_moment.content,
            'media_urls': new_moment.media_urls,
            'created_at': new_moment.created_at.isoformat() if new_moment.created_at else None,
            'pet_name': pet.name,
            'pet_avatar': pet.avatar_url
        }), 201
        
    except Exception as e:
        db.session.rollback()
        return jsonify({'error': f'发布失败: {str(e)}'}), 500

@api_bp.route('/moments/<int:moment_id>', methods=['DELETE'])
def delete_moment(moment_id):
    """删除动态"""
    moment = MomentService.get_moment_by_id(moment_id)
    if not moment:
        return jsonify({'error': '动态不存在'}), 404
    
    MomentService.delete_moment(moment_id)
    return jsonify({'message': '删除成功'})

# 初始化示例数据
@api_bp.before_app_request
def init_data():
    create_sample_data()