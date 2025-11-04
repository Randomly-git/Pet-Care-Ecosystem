from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()
class Pet(db.Model):
    __tablename__ = 'pets'

    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    species = db.Column(db.String(50), nullable=False) # e.g., Dog, Cat
    breed = db.Column(db.String(100)) # e.g., Labrador, Siamese
    birthday = db.Column(db.Date)
    avatar_url = db.Column(db.String(255)) # URL to pet's avatar image
    created_at = db.Column(db.DateTime, server_default=db.func.now())

class PetMoment(db.Model):
    __tablename__ = 'pet_moments'

    id = db.Column(db.Integer, primary_key=True)
    pet_id = db.Column(db.Integer, db.ForeignKey('pets.id'), nullable=False, index=True) # Foreign key to pets table
    content = db.Column(db.Text, nullable=False) # Text content of the moment
    media_urls = db.Column(db.JSON) # List of media URLs (images/videos) stored as JSON
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    
    # Relationship to Pet
    pet = db.relationship('Pet', backref=db.backref('moments', lazy=True))