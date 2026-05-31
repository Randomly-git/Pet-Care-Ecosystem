import re

# Fix 1: ActivitiesView.vue - Add scrollbar to AI Report
with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'r', encoding='utf-8') as f:
    act_content = f.read()

act_target = ".report-text { font-size: 13px; line-height: 1.6; color: #4b5563; margin: 0 0 16px 0; background: rgba(0,0,0,0.02); padding: 12px; border-radius: 12px; }"
act_replacement = ".report-text { font-size: 13px; line-height: 1.6; color: #4b5563; margin: 0 0 16px 0; background: rgba(0,0,0,0.02); padding: 12px; border-radius: 12px; max-height: 180px; overflow-y: auto; }"

if act_target in act_content:
    act_content = act_content.replace(act_target, act_replacement)
else:
    print("AI Report CSS target not found in ActivitiesView.vue")

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'w', encoding='utf-8') as f:
    f.write(act_content)

# Fix 2: PetStatusCard.vue - Change to horizontal layout
with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/petspace/PetStatusCard.vue', 'r', encoding='utf-8') as f:
    pet_content = f.read()

pet_target_items = ".status-items {\n  display: flex;\n  flex-direction: column;\n  gap: 0.75rem;\n}"
pet_replacement_items = ".status-items {\n  display: flex;\n  flex-direction: row;\n  flex-wrap: wrap;\n  gap: 0.75rem;\n}"

pet_target_item = ".status-item {\n  display: flex;\n  align-items: flex-start;\n  gap: 0.75rem;\n  padding: 0.75rem;\n  background: #f9fafb;\n  border-radius: 8px;\n  transition: all 0.2s ease;\n}"
pet_replacement_item = ".status-item {\n  display: flex;\n  align-items: flex-start;\n  gap: 0.75rem;\n  padding: 0.75rem;\n  background: #f9fafb;\n  border-radius: 8px;\n  transition: all 0.2s ease;\n  flex: 1 1 calc(33.333% - 0.75rem);\n  min-width: 140px;\n}"

pet_target_hover = ".status-item:hover {\n  background: #f3f4f6;\n  transform: translateX(4px);\n}"
pet_replacement_hover = ".status-item:hover {\n  background: #f3f4f6;\n  transform: translateY(-2px);\n}"

pet_content = pet_content.replace(pet_target_items, pet_replacement_items)
pet_content = pet_content.replace(pet_target_item, pet_replacement_item)
pet_content = pet_content.replace(pet_target_hover, pet_replacement_hover)

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/petspace/PetStatusCard.vue', 'w', encoding='utf-8') as f:
    f.write(pet_content)

# Fix 3: HomeView.vue - Refactor About Us with problem.md music player style
with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue', 'r', encoding='utf-8') as f:
    home_content = f.read()

# Replace HTML
html_target_regex = r"<!-- 5\. 关于我们 \(Glassmorphism\) -->[\s\S]*?</section>"
new_html = """<!-- 5. 关于我们 (Advanced Glassmorphism) -->
    <section class="about-section" id="about">
      <!-- Background Blobs -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>

      <div class="team-container fly-in-up">
        <h2 class="glass-title">关于我们</h2>
        <p class="glass-subtitle">同济大学软件学院</p>
        <div class="team-cards">
          <div class="glass-card" v-for="(member, index) in teamMembers" :key="index">
            <div class="glass-avatar">
              <img :src="`https://github.com/${member.github}.png`" :alt="member.github" @error="handleImageError" />
            </div>
            <h3 class="member-name">@{{ member.github }}</h3>
            <p class="member-role">Pet Care Ecosystem</p>
          </div>
        </div>
      </div>
    </section>"""
home_content = re.sub(html_target_regex, new_html, home_content)

# Replace CSS
css_target_regex = r"/\* ==========================================================================\s*5\. ABOUT SECTION \(GLASSMORPHISM\)\s*========================================================================== \*/[\s\S]*?/\* ==========================================================================\s*6\. ANIMATIONS\s*========================================================================== \*/"
new_css = """/* ==========================================================================
   5. ABOUT SECTION (ADVANCED GLASSMORPHISM)
   ========================================================================== */
.about-section {
  position: relative;
  padding: 120px 20px;
  background: #121212; /* Dark background to contrast the colorful blobs */
  overflow: hidden;
  text-align: center;
}

/* Blobs from problem.md reference */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  z-index: 0;
}
.blob-1 { width: 300px; height: 300px; background: #ff007f; top: 10%; left: 15%; opacity: 0.5; }
.blob-2 { width: 400px; height: 400px; background: #7f00ff; bottom: 20%; right: 10%; opacity: 0.4; }
.blob-3 { width: 250px; height: 250px; background: #00ffff; top: 40%; left: 50%; opacity: 0.3; }

.team-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.2);
  padding: 60px 40px;
}

.glass-title {
  font-size: 3rem;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 8px;
  text-shadow: 0 0 20px rgba(255,255,255,0.3);
  letter-spacing: 2px;
}

.glass-subtitle {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 60px;
  font-weight: 300;
  letter-spacing: 4px;
}

.team-cards {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 40px;
}

.glass-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 200px;
  transition: transform 0.3s ease;
  cursor: pointer;
}

.glass-card:hover {
  transform: translateY(-10px);
}

.glass-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  margin-bottom: 20px;
  padding: 5px;
  background: linear-gradient(135deg, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0.05) 100%);
  box-shadow: 0 10px 25px rgba(0,0,0,0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.glass-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.member-name {
  font-size: 1.1rem;
  color: #fff;
  margin: 0 0 8px 0;
  font-weight: 600;
  letter-spacing: 1px;
}

.member-role {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.5);
  margin: 0;
}

/* ==========================================================================
   6. ANIMATIONS
   ========================================================================== */"""
home_content = re.sub(css_target_regex, new_css, home_content)

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue', 'w', encoding='utf-8') as f:
    f.write(home_content)
