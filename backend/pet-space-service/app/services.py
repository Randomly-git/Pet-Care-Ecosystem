from .models import db, Pet, PetMoment

class PetService:
    @staticmethod
    def get_all_pets():
        return Pet.query.all()

    @staticmethod
    def get_pet_by_id(pet_id):
        return Pet.query.get(pet_id)

    @staticmethod
    def create_pet(pet_data):
        new_pet = Pet(**pet_data)
        db.session.add(new_pet)
        db.session.commit()
        return new_pet

class MomentService:
    @staticmethod
    def get_moments_by_pet(pet_id):
        return PetMoment.query.filter_by(pet_id=pet_id).order_by(PetMoment.created_at.desc()).all()

    @staticmethod
    def get_moment_by_id(moment_id):
        return PetMoment.query.get(moment_id)

    @staticmethod
    def create_moment(pet_id, content, media_urls=None):
        if media_urls is None:
            media_urls = []
        new_moment = PetMoment(pet_id=pet_id, content=content, media_urls=media_urls)
        db.session.add(new_moment)
        db.session.commit()
        return new_moment

    @staticmethod
    def delete_moment(moment_id):
        moment = PetMoment.query.get(moment_id)
        if moment:
            db.session.delete(moment)
            db.session.commit()
            return True
        return False