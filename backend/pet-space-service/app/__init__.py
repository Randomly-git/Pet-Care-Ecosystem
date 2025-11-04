from flask import Flask
from .models import db
from flask_cors import CORS
from .routes import api_bp 

def create_app():
    app = Flask(__name__)
    app.config.from_object('config.Config')

    CORS(app)

    # initialize database
    db.init_app(app)

    # register blueprint (API routes)
    app.register_blueprint(api_bp, url_prefix='/api/v1/pet-space')

    # create tables within the application context
    with app.app_context():
        db.create_all()
        from .routes import create_sample_data
        create_sample_data()

    return app