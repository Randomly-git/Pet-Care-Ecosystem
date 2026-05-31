import re

with open("d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue", "r", encoding="utf-8") as f:
    content = f.read()

html_target = r"<!-- 5\. \S+ -->\s*<section class=\"about-section\">\s*<div class=\"about-container fly-in-up\">[\s\S]*?</section>"
html_replacement = """<!-- 5. 关于我们 (Glassmorphism) -->
    <section class="about-section">
      <div class="about-container fly-in-up">
        <h2 class="glass-title">关于我们</h2>
        <p class="glass-subtitle">同济大学软件学院</p>
        <div class="team-cards">
          <div class="glass-card" v-for="(member, index) in teamMembers" :key="index">
            <div class="glass-avatar">
              <img :src="`https://github.com/${member.github}.png`" :alt="member.github" @error="handleImageError" />
            </div>
            <h3 class="member-name">@{{ member.github }}</h3>
            <p class="member-role">Pet Care Ecosystem</p>
            <div class="neon-line"></div>
          </div>
        </div>
      </div>
    </section>"""
content = re.sub(html_target, html_replacement, content)

css_target = r"/\* ==========================================================================\s*5\. ABOUT SECTION\s*========================================================================== \*/[\s\S]*?/\* ==========================================================================\s*6\. ANIMATIONS\s*========================================================================== \*/"
css_replacement = """/* ==========================================================================
   5. ABOUT SECTION (GLASSMORPHISM)
   ========================================================================== */
.about-section {
  position: relative;
  padding: 100px 20px;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%);
  overflow: hidden;
  text-align: center;
}

.about-section::before {
  content: '';
  position: absolute;
  top: -20%;
  left: -10%;
  width: 50%;
  height: 80%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.4) 0%, transparent 70%);
  filter: blur(80px);
  z-index: 0;
}

.about-section::after {
  content: '';
  position: absolute;
  bottom: -20%;
  right: -10%;
  width: 50%;
  height: 80%;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.3) 0%, transparent 70%);
  filter: blur(80px);
  z-index: 0;
}

.about-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
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
  flex-wrap: wrap;
  gap: 30px;
}

.glass-card {
  position: relative;
  width: 240px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px 20px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow: hidden;
  cursor: pointer;
}

.glass-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(180deg, rgba(255,255,255,0.1) 0%, transparent 100%);
  pointer-events: none;
}

.glass-card:hover {
  transform: translateY(-10px);
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 16px 40px 0 rgba(0, 0, 0, 0.4), 0 0 20px rgba(255, 255, 255, 0.1);
}

.glass-card:active {
  transform: scale(0.98);
}

.glass-card:hover .neon-line {
  opacity: 1;
  width: 60%;
}

.glass-avatar {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  margin: 0 auto 20px;
  padding: 4px;
  background: linear-gradient(135deg, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0.05) 100%);
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.glass-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.member-name {
  font-size: 1.2rem;
  color: #fff;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.member-role {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.6);
  margin: 0;
}

.neon-line {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  height: 3px;
  width: 0%;
  background: linear-gradient(90deg, transparent, #818cf8, #c084fc, transparent);
  opacity: 0;
  transition: all 0.4s ease;
  box-shadow: 0 0 10px #818cf8;
}

/* ==========================================================================
   6. ANIMATIONS
   ========================================================================== */"""
content = re.sub(css_target, css_replacement, content)

js_target = 'const router = useRouter()'
js_replacement = """const router = useRouter()

const teamMembers = [
  { github: 'randomly-git' },
  { github: 'FutuXer' },
  { github: 'Jeery1' },
  { github: 'lieyanzhuifeng' }
]

const handleImageError = (e) => {
  e.target.src = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}"""
content = content.replace(js_target, js_replacement)

with open("d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue", "w", encoding="utf-8") as f:
    f.write(content)
