from flask import Flask
from .models import db
from flask_cors import CORS
from .routes import api_bp 
import time
import logging
from sqlalchemy import text
from sqlalchemy.exc import OperationalError

def wait_for_db(db, max_retries=30, delay=2):
    """等待数据库连接就绪"""
    logging.info("开始等待数据库连接...")
    
    for i in range(max_retries):
        try:
            # 测试SQLAlchemy连接
            with db.engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logging.info("数据库连接成功！")
            return True
            
        except OperationalError as e:
            logging.warning(f"⏳ 数据库连接尝试 {i+1}/{max_retries} 失败，等待重试...")
            time.sleep(delay)
    
    logging.error("无法连接到数据库，请检查MySQL服务是否正常运行")
    return False

def create_app():
    app = Flask(__name__)
    app.config.from_object('config.Config')
    logging.basicConfig(level=logging.INFO)

    CORS(app)

    # initialize database
    db.init_app(app)

    # register blueprint (API routes)
    app.register_blueprint(api_bp, url_prefix='/api/v1/pet-space')

    # create tables within the application context
    with app.app_context():
        # wait for the database to be ready
        if not wait_for_db(db):
            logging.error("数据库连接失败，应用将继续启动但数据库功能不可用")
        else:
            try:
                db.create_all()
                from .routes import create_sample_data
                create_sample_data()
                logging.info("数据库表创建完成")
            except Exception as e:
                logging.error(f"数据库表创建失败: {e}")

    return app