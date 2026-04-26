import os

file_path = "frontend/src/views/ActivitiesView.vue"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

inject_html = """
      <!-- ===== 1.5 高级追踪：GitHub打卡热力图 & AI健康速报 ===== -->
      <div class="advanced-dash-row" id="heatmap">
        <!-- 左侧：365天 Github 风格热力矩阵 -->
        <div class="md3-card heatmap-card">
          <div class="heatmap-header">
            <h3 class="heatmap-title">年度活跃频率追踪</h3>
            <div class="heatmap-legend">
              <span>Less</span>
              <div class="legend-box level-0"></div>
              <div class="legend-box level-1"></div>
              <div class="legend-box level-2"></div>
              <div class="legend-box level-3"></div>
              <div class="legend-box level-4"></div>
              <span>More</span>
            </div>
          </div>
          <div class="heatmap-container">
            <div class="heatmap-grid">
              <div class="heatmap-col" v-for="colIndex in 52" :key="colIndex">
                <div 
                  class="heatmap-cell" 
                  v-for="rowIndex in 7" 
                  :key="rowIndex"
                  :class="'level-' + (Math.floor(Math.random() * 5))"
                  :title="`第${colIndex}周 第${rowIndex}天`"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：AI 智能评估简报 -->
        <div class="md3-card ai-report-card" id="ai-report">
          <div class="ai-report-header">
            <h3 class="heatmap-title">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6750A4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px; vertical-align: -3px;"><path d="M12 2v20M17 5v14M7 5v14M22 8v8M2 8v8"/></svg> 
              健康 AI 综合诊断
            </h3>
            <span class="report-badge">实时更新</span>
          </div>
          <div class="ai-report-body">
            <div class="report-status green">极度稳定 & 活跃</div>
            <p class="report-text">基于近 7 日的打卡密度分析：主粮喂食规律度高达 <strong>95%</strong>，排泄状态(双便)无异常。建议本周末增加约 <strong>15%</strong> 的户外互动活动量以维持骨骼活性。</p>
            <div class="report-metrics">
              <div class="metric"><div class="metric-val">98/100</div><div class="metric-label">健康评分</div></div>
              <div class="metric"><div class="metric-val">+2%</div><div class="metric-label">周活跃度</div></div>
            </div>
          </div>
        </div>
      </div>
"""

content = content.replace(
    '<!-- ===== 2. 宠物面板 + 宠物详情 双栏 ===== -->', 
    inject_html + '\n      <!-- ===== 2. 宠物面板 + 宠物详情 双栏 ===== -->'
)
content = content.replace(
    '<div class="dashboard-panel">', 
    '<div class="dashboard-panel" id="timeline">'
)

inject_css = """
/* ===== GitHub Heatmap & AI Report ===== */
.advanced-dash-row { display: flex; gap: 16px; margin-bottom: 24px; width: 100%; box-sizing: border-box; }
.heatmap-card { flex: 2; padding: 20px; background: white; border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); overflow: hidden; }
.ai-report-card { flex: 1; padding: 20px; background: linear-gradient(145deg, #ffffff, #fcfbff); border-radius: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); display: flex; flex-direction: column; }
.heatmap-header, .ai-report-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.heatmap-title { font-size: 16px; font-weight: 600; color: #1d1d1f; margin: 0; }
.heatmap-legend { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #86868b; }
.legend-box { width: 10px; height: 10px; border-radius: 2px; }
.heatmap-container { overflow-x: auto; padding-bottom: 8px; }
.heatmap-grid { display: flex; gap: 4px; width: max-content; }
.heatmap-col { display: flex; flex-direction: column; gap: 4px; }
.heatmap-cell { width: 12px; height: 12px; border-radius: 3px; background-color: #ebedf0; transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; }
.heatmap-cell:hover { transform: scale(1.2); box-shadow: 0 2px 4px rgba(0,0,0,0.1); z-index: 10; position: relative;}
.level-0 { background-color: #ebedf0; }
.level-1 { background-color: #9be9a8; }
.level-2 { background-color: #40c463; }
.level-3 { background-color: #30a14e; }
.level-4 { background-color: #216e39; }
.report-badge { background: rgba(103, 80, 164, 0.1); color: #6750A4; padding: 4px 8px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.ai-report-body { flex: 1; display: flex; flex-direction: column; }
.report-status { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
.report-status.green { color: #22c55e; }
.report-text { font-size: 13px; line-height: 1.6; color: #4b5563; margin: 0 0 16px 0; background: rgba(0,0,0,0.02); padding: 12px; border-radius: 12px; }
.report-metrics { display: flex; gap: 16px; margin-top: auto; }
.metric { flex: 1; background: #f8fafc; padding: 12px; border-radius: 12px; text-align: center; }
.metric-val { font-size: 18px; font-weight: 700; color: #1d1d1f; margin-bottom: 2px; }
.metric-label { font-size: 11px; color: #64748b; }
"""

content = content.replace("</style>", inject_css + "\n</style>")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("done")
