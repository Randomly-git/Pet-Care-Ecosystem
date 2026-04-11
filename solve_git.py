import subprocess
import time

try:
    with open('log.txt', 'w') as f:
        # Add files
        r1 = subprocess.run(['git', 'add', 'frontend/src/api/media.js', 'frontend/src/views/ActivitiesView.vue', 'petcare-backend/src/main/java/com/petcare/backend/dto/response/StatusRecordDTO.java'], capture_output=True, text=True)
        f.write(f"Add stdout: {r1.stdout}\nAdd stderr: {r1.stderr}\n")

        # Status
        r2 = subprocess.run(['git', 'status'], capture_output=True, text=True)
        f.write(f"Status stdout: {r2.stdout}\n")

        # Commit
        r3 = subprocess.run(['git', 'commit', '-m', 'Merge new-version, resolve conflicts'], capture_output=True, text=True)
        f.write(f"Commit stdout: {r3.stdout}\nCommit stderr: {r3.stderr}\n")

except Exception as e:
    with open('log.txt', 'a') as f:
        f.write(str(e))
