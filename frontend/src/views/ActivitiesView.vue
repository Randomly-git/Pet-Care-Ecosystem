<template>
  <div class="activities-page">
    <AppHeader />

    <!-- Hero Banner (保持不变) -->
    <section class="banner-section hero-section">
      <div class="banner-bg hero-bg"></div>
      <div class="banner-content hero-content">
        <h2 class="banner-title">宠物活动中心</h2>
        <p class="banner-subtitle">随时掌握它们的生活节奏。记录打卡、多宠对比、时光轴回溯。</p>
      </div>
    </section>

    <div class="md3-page-body">

      <!-- ===== 1. 统计卡片行 ===== -->
      <div class="stats-row">
        <div class="md3-stat-card">
          <div class="stat-icon-wrap" style="background: rgba(103,80,164,0.12); color: #6750A4;">🐾</div>
          <div class="stat-info">
            <div class="stat-number">{{ userPets.length }}</div>
            <div class="stat-label">我的宠物</div>
          </div>
        </div>
        <div class="md3-stat-card">
          <div class="stat-icon-wrap" style="background: rgba(98,91,113,0.12); color: #625B71;">📅</div>
          <div class="stat-info">
            <div class="stat-number">{{ activityRecords.filter(r => r.activityDate && r.activityDate.startsWith(new Date().toISOString().slice(0,7))).length }}</div>
            <div class="stat-label">本月活动</div>
          </div>
        </div>
        <div class="md3-stat-card">
          <div class="stat-icon-wrap" style="background: rgba(125,82,96,0.12); color: #7D5260;">🕐</div>
          <div class="stat-info">
            <div class="stat-number">{{ activityRecords.length > 0 ? formatTime(activityRecords.sort((a,b) => new Date(b.activityDate)-new Date(a.activityDate))[0].activityDate) : '--' }}</div>
            <div class="stat-label">最近打卡</div>
          </div>
        </div>
        <div class="md3-stat-card">
          <div class="stat-icon-wrap" style="background: rgba(176,200,120,0.15); color: #4a7c3f;">💚</div>
          <div class="stat-info">
            <div class="stat-number" style="font-size: 18px;">{{ activityRecords.length > 0 ? '良好' : '待记录' }}</div>
            <div class="stat-label">健康状态</div>
          </div>
        </div>
      </div>

      
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
          <div class="ai-report-body" style="position: relative; min-height: 120px;">
            <div v-if="aiReportLoading" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #6b7280; font-size: 13px;">
              <el-icon class="is-loading" style="font-size: 24px; margin-bottom: 8px;"><Loading /></el-icon>
              正在分析近期健康数据...
            </div>
            <div v-else-if="aiReportError" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #ef4444; font-size: 13px;">
              <el-icon style="font-size: 24px; margin-bottom: 8px;"><Warning /></el-icon>
              生成诊断报告失败，请稍后重试
              <el-button link size="small" type="primary" @click="fetchAiReport" style="margin-top: 8px;">重试</el-button>
            </div>
            <template v-else-if="aiReportData">
              <div class="report-status green">诊断类型：{{ aiReportData.analysisType === 'RAG' ? '深度评估 (基于文献库)' : '基础评估' }}</div>
              <p class="report-text" style="white-space: pre-wrap;">{{ aiReportData.analysis }}</p>
              
              <div v-if="aiReportData.knowledgeSources && aiReportData.knowledgeSources.length > 0" style="margin-top: auto; padding-top: 12px; border-top: 1px dashed #e5e7eb;">
                <div style="font-size: 11px; color: #6b7280; margin-bottom: 6px;">📚 参考文献支持：</div>
                <div v-for="(source, idx) in aiReportData.knowledgeSources.slice(0, 2)" :key="idx" style="font-size: 11px; color: #4b5563; display: flex; align-items: center; gap: 4px; margin-bottom: 4px;">
                  <el-icon style="color: #6750A4"><Document /></el-icon> {{ source.title }} (置信度: {{(source.score * 100).toFixed(0)}}%)
                </div>
              </div>
            </template>
            <div v-else style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #6b7280; font-size: 13px;">
              <p style="margin-bottom: 12px; text-align: center;">基于宠物的近期活动、饮食与医疗记录，利用 Qwen 模型与知识库生成深度健康分析。</p>
              <el-button type="primary" round @click="fetchAiReport" style="background-color: #6750A4; border-color: #6750A4; box-shadow: 0 4px 12px rgba(103, 80, 164, 0.3);">
                <el-icon style="margin-right: 6px;"><MagicStick /></el-icon>
                生成健康简报
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 2. 宠物面板 + 宠物详情 双栏 ===== -->
      <div class="dashboard-panel" id="timeline">

        <!-- 左栏：宠物列表 -->
        <div class="md3-card pet-list-panel">
          <div class="panel-header">
            <span class="panel-title">🐾 我的宠物</span>
            <span class="pets-count-badge">{{ userPets.length }}</span>
          </div>

          <div class="pets-list">
            <!-- 宠物卡片 -->
            <div v-for="pet in userPets" :key="pet.id || pet.petId" class="md3-pet-item"
              :class="{ 'active': selectedPetId === (pet.id || pet.petId) }"
              @click="selectPet(pet.id || pet.petId)">
              <div class="pet-item-left">
                <!-- 多选 Checkbox -->
                <div @click.stop>
                  <el-checkbox :model-value="selectedPetIds.includes(pet.id || pet.petId)"
                    @change="togglePetSelection(pet.id || pet.petId)" />
                </div>
                <el-avatar :size="40" :src="pet.avatar_url">
                  {{ pet.name.charAt(0) }}
                </el-avatar>
              </div>
              <div class="pet-item-info">
                <div class="pet-item-name">{{ pet.name }}</div>
                <div class="pet-item-sub">
                  {{ pet.species || pet.type }}
                  <span v-if="pet.breed"> · {{ pet.breed }}</span>
                  <span v-if="pet.gender === 'male'"> · ♂</span>
                  <span v-if="pet.gender === 'female'"> · ♀</span>
                </div>
                <div class="pet-activity-count">{{ getPetActivityCount(pet.id || pet.petId) }} 条活动记录</div>
              </div>
              <div class="pet-item-actions" @click.stop>
                <el-dropdown trigger="click">
                  <div class="more-options-btn">
                    <el-icon><MoreFilled /></el-icon>
                  </div>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="getAIStatusSummary(pet.id || pet.petId, pet.name)">
                        <el-icon><MagicStick /></el-icon> AI 总结
                      </el-dropdown-item>
                      <el-dropdown-item @click="getPetStats(pet.id || pet.petId, pet.name)">
                        分析统计
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <!-- 添加宠物 -->
            <div class="md3-add-pet" @click="showAddPetDialog = true">
              <el-icon><Plus /></el-icon>
              <span>添加宠物</span>
            </div>
          </div>
        </div>

        <!-- 右栏：选中宠物详情 / 活动摘要 -->
        <div class="md3-card pet-detail-panel">
          <div v-if="currentSelectedPet">
            <!-- 宠物头像信息区 -->
            <div class="pet-detail-header">
              <el-avatar :size="64" :src="currentSelectedPet.avatar_url" class="detail-avatar">
                {{ currentSelectedPet.name.charAt(0) }}
              </el-avatar>
              <div class="pet-detail-meta">
                <div class="pet-detail-name">{{ currentSelectedPet.name }}</div>
                <div class="pet-detail-sub">
                  {{ currentSelectedPet.species || currentSelectedPet.type }}
                  <span v-if="currentSelectedPet.breed"> · {{ currentSelectedPet.breed }}</span>
                  <el-tag size="small" style="margin-left: 8px;"
                    :type="currentSelectedPet.gender === 'male' ? 'primary' : 'danger'">
                    {{ currentSelectedPet.gender === 'male' ? '♂ 雄' : currentSelectedPet.gender === 'female' ? '♀ 雌' : '未知' }}
                  </el-tag>
                </div>
              </div>
              <div class="pet-detail-actions">
                <el-button type="primary" round size="small" @click="openAddDialog">
                  <el-icon><Plus /></el-icon> 记录活动
                </el-button>
                <el-button round size="small"
                  @click="getAIStatusSummary(currentSelectedPet.id || currentSelectedPet.petId, currentSelectedPet.name)">
                  <el-icon><MagicStick /></el-icon> AI 总结
                </el-button>
              </div>
            </div>

            <!-- 宠物状态卡片组件 -->
            <div class="pet-detail-status">
              <PetStatusCard :pet-id="selectedPetId" :pet-info="getPetInfo(selectedPetId)" />
            </div>

            <!-- 最近活动 mini 时间线 -->
            <div class="pet-detail-recent">
              <div class="recent-header">最近活动</div>
              <div v-if="filteredRecords.slice(0,5).length === 0" class="recent-empty">暂无活动记录</div>
              <div v-for="record in filteredRecords.slice(0,5)" :key="record.activityRecordId" class="recent-item">
                <div class="recent-dot" :class="getActivityTypeClass(record.activityId)"></div>
                <div class="recent-content">
                  <span class="recent-type">{{ getActivityTypeName(record.activityId) }}</span>
                  <span class="recent-time">{{ formatTime(record.activityDate) }} · {{ formatDate(record.activityDate) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 未选中宠物时的空状态 -->
          <div v-else class="no-pet-selected">
            <div class="no-pet-icon">🐾</div>
            <div class="no-pet-text">从左侧选择一只宠物</div>
            <div class="no-pet-sub">查看详细信息和活动记录</div>
          </div>
        </div>
      </div>

      <!-- ===== 3. 时间轴 + 筛选 + 记录区 ===== -->
      <div class="timeline-area">

        <!-- 活动跨度视图 (横向日历时间轴) -->
        <div class="md3-card">
          <div class="scrubber-header">
            <span class="panel-title">活动跨度视图</span>
            <div class="scrubber-actions">
              <el-date-picker v-model="dateRange" type="daterange" range-separator="-"
                start-placeholder="开始" end-placeholder="结束"
                format="YYYY-MM-DD" value-format="YYYY-MM-DD"
                @change="handleDateRangeChange" size="small" style="width: 220px; margin-right: 12px;" />
              <el-button type="primary" round @click="openAddDialog">
                <el-icon><Plus /></el-icon> 记录活动
              </el-button>
            </div>
          </div>
          <div class="scrubber-track" @wheel.prevent="handleScrubberWheel">
            <div v-for="day in recentDays" :key="day.dateStr" class="scrubber-day"
              :class="{ 'active': isDateInRange(day.dateStr) }"
              @click="selectScrubberDate(day.dateStr)">
              <div class="day-name">{{ day.monthAndDayName }}</div>
              <div class="day-num">{{ day.dayNum }}</div>
              <div class="activity-indicator" :class="{ 'has-activity': groupedRecords[day.dateStr] }"></div>
            </div>
          </div>
        </div>

        <!-- 活动筛选 -->
        <div class="md3-card filter-card">
          <div class="filter-header">
            <span class="panel-title">活动筛选</span>
            <div>
              <el-button link type="primary" size="small"
                @click="selectedActivityTypes = activityTypes.map(t => t.value); loadActivityRecords()">全选</el-button>
              <el-button link size="small"
                @click="selectedActivityTypes = []; loadActivityRecords()">清空</el-button>
            </div>
          </div>
          <div class="filter-content">
            <el-checkbox-group v-model="selectedActivityTypes" @change="loadActivityRecords" class="custom-checkbox-group">
              <el-checkbox v-for="type in activityTypes" :key="type.value" :label="type.value" :value="type.value" border>
                {{ type.label }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <!-- 活动记录时间线 -->
        <div class="md3-card timeline-card">
          <div class="panel-title" style="margin-bottom: 20px;">活动记录</div>
          <div v-if="loading">
            <el-skeleton :rows="5" animated />
          </div>
          <div v-else-if="filteredRecords.length === 0" class="empty-state">
            <el-empty description="暂无活动记录">
              <el-button type="primary" @click="openAddDialog">创建第一条记录</el-button>
            </el-empty>
          </div>
          <div v-else class="activity-timeline">
            <div v-for="(group, date) in groupedRecords" :key="date" class="timeline-group">
              <div class="timeline-date">
                <div class="date-badge">{{ formatDate(date) }}</div>
              </div>
              <div class="timeline-items">
                <div v-for="record in group" :key="record.activityRecordId" class="timeline-item"
                  @click="editRecord(record)">
                  <div class="timeline-marker">
                    <div class="marker-dot" :class="getActivityTypeClass(record.activityId)"></div>
                    <div class="marker-line"></div>
                  </div>
                  <div class="timeline-content">
                    <div class="record-card">
                      <div class="record-header">
                        <div class="pet-info">
                          <el-avatar :size="32" :src="getPetInfo(record.petId).avatar_url">
                            {{ getPetInfo(record.petId).name.charAt(0) }}
                          </el-avatar>
                          <div class="pet-details">
                            <div class="pet-name">{{ getPetInfo(record.petId).name }}</div>
                            <div class="activity-type">{{ getActivityTypeName(record.activityId) }}</div>
                          </div>
                        </div>
                        <div class="record-time">{{ formatTime(record.activityDate) }}</div>
                      </div>
                      <div class="record-description">{{ record.activityDescription }}</div>
                      <div class="record-media" v-if="record.mediaFiles && record.mediaFiles.length > 0">
                        <div class="media-preview">
                          <div v-for="media in record.mediaFiles.slice(0, 4)" :key="media.mediaId"
                            class="media-item" @click.stop="previewMedia(media)">
                            <img v-if="media.fileType.startsWith('image/')" :src="media.fileUrl" :alt="media.fileName" />
                            <div v-else class="file-icon"><i class="fas fa-file"></i></div>
                          </div>
                          <div v-if="record.mediaFiles.length > 4" class="more-media">
                            +{{ record.mediaFiles.length - 4 }}
                          </div>
                        </div>
                      </div>
                      <div class="record-actions">
                        <el-button size="small" @click.stop="editRecord(record)">编辑</el-button>
                        <el-button size="small" type="danger" @click.stop="deleteRecord(record)">删除</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- ===== 所有对话框 (完整保留) ===== -->

    <!-- 添加活动记录对话框 -->
    <el-dialog v-model="showAddDialog" title="添加活动记录" width="600px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="100px">
        <el-form-item label="当前宠物">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-avatar :size="32" :src="currentSelectedPet?.avatar_url">
              {{ currentSelectedPet?.name?.charAt(0) }}
            </el-avatar>
            <span>{{ currentSelectedPet?.name || '未选择宠物' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="活动类别" prop="activityKindId">
          <el-select v-model="addForm.activityKindId" placeholder="选择活动类别" style="width: 100%"
            @change="handleActivityKindChange">
            <el-option v-for="type in activityTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="addForm.activityKindId" label="具体活动" prop="activityId">
          <div style="display: flex; gap: 8px;">
            <el-select v-model="addForm.activityId" placeholder="选择具体活动" style="flex: 1" filterable
              no-data-text="该类别下暂无活动，请新建活动">
              <el-option v-for="activity in getActivitiesByKind(addForm.activityKindId)" :key="activity.activityId"
                :label="activity.activityName" :value="activity.activityId" />
            </el-select>
            <el-button type="primary" plain @click="showCreateActivityDialog = true"
              :disabled="!addForm.activityKindId">新建活动</el-button>
          </div>
          <div v-if="addForm.activityKindId"
            style="margin-top: 8px; padding: 8px; background: #f0f9ff; border: 1px solid #bfdbfe; border-radius: 4px; font-size: 12px; color: #1e40af;">
            <div v-if="getActivitiesByKind(addForm.activityKindId).length === 0">
              该类别下还没有具体的活动。您可以选择：
              <ul style="margin: 4px 0; padding-left: 16px;">
                <li><strong>直接提交</strong>：系统将使用活动类别直接创建记录</li>
                <li><strong>新建活动</strong>：点击右侧"新建活动"按钮创建具体活动</li>
              </ul>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker v-model="addForm.activityDate" type="datetime" placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="4" placeholder="请输入活动描述..." />
        </el-form-item>
        <el-form-item v-if="addForm.activityKindId === 6" label="用药量" prop="dosage">
          <el-input v-model="addForm.dosage" placeholder="例如：2片、5ml、1支" />
        </el-form-item>
        <el-form-item v-if="addForm.activityKindId === 6" label="用药量" prop="dosage">
          <el-input v-model="addForm.dosage" placeholder="例如：2片、5ml、1支" />
        </el-form-item>
        <el-form-item label="上传媒体文件">
          <el-upload ref="activityUploadRef" :auto-upload="false" :on-change="handleActivityFileChange" :limit="5"
            :file-list="activityFileList" action="#" :accept="'image/*,video/*,.pdf,.doc,.docx'" multiple>
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持图片、视频、PDF、Word文档，最多5个文件，每个文件不超过10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑活动记录对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑活动记录" width="600px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="100px">
        <el-form-item label="宠物名称" prop="petId">
          <el-input v-model="editForm.petName" disabled placeholder="宠物名称" />
        </el-form-item>
        <el-form-item label="活动类型" prop="activityId">
          <el-select v-model="editForm.activityId" placeholder="选择活动类型" style="width: 100%">
            <el-option v-for="type in activityTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker v-model="editForm.activityDate" type="datetime" placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入活动描述..." />
        </el-form-item>
        <el-form-item v-if="editForm.activityId === 6" label="用药量" prop="dosage">
          <el-input v-model="editForm.dosage" placeholder="例如：2片、5ml、1支" />
        </el-form-item>
        <el-form-item label="上传媒体文件">
          <el-upload ref="editActivityUploadRef" :auto-upload="false" :on-change="handleEditActivityFileChange"
            :limit="5" :file-list="editActivityFileList" action="#" :accept="'image/*,video/*,.pdf,.doc,.docx'" multiple>
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">上传新文件将替换现有文件，支持图片、视频、PDF、Word文档</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="editForm.mediaFiles && editForm.mediaFiles.length > 0" label="已上传文件">
          <div class="existing-media">
            <div v-for="media in editForm.mediaFiles" :key="media.mediaId" class="media-item-small">
              <img v-if="media.fileType.startsWith('image/')" :src="media.fileUrl" :alt="media.fileName"
                @click="previewMedia(media)" />
              <div v-else class="file-icon-small" @click="previewMedia(media)">
                <i class="fas fa-file"></i>
              </div>
              <el-button size="small" type="danger" @click="removeMediaFromEdit(media)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm" :loading="submitting">更新</el-button>
      </template>
    </el-dialog>

    <!-- 添加宠物对话框 -->
    <el-dialog v-model="showAddPetDialog" title="添加宠物" width="500px" :close-on-click-modal="false">
      <el-form ref="petFormRef" :model="petForm" :rules="petFormRules" label-width="100px">
        <el-form-item label="宠物名称" prop="name">
          <el-input v-model="petForm.name" placeholder="请输入宠物名称" />
        </el-form-item>
        <el-form-item label="宠物类型" prop="type">
          <el-select v-model="petForm.type" placeholder="选择宠物类型" style="width: 100%">
            <el-option label="狗" value="dog" />
            <el-option label="猫" value="cat" />
            <el-option label="鸟" value="bird" />
            <el-option label="鱼" value="fish" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种" prop="breed">
          <el-input v-model="petForm.breed" placeholder="请输入宠物品种" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="petForm.gender" placeholder="选择性别" style="width: 100%">
            <el-option label="雄性" value="male" />
            <el-option label="雌性" value="female" />
            <el-option label="未知" value="unknown" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker v-model="petForm.birthday" type="date" placeholder="选择宠物生日" format="YYYY-MM-DD"
            value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddPetDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPetForm" :loading="submittingPet">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建新活动对话框 -->
    <el-dialog v-model="showCreateActivityDialog" title="创建新活动" width="500px" :close-on-click-modal="false">
      <el-form ref="newActivityFormRef" :model="newActivityForm" :rules="newActivityFormRules" label-width="100px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="newActivityForm.activityName" placeholder="请输入活动名称，如：吃零食、散步、洗澡等" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="活动类别">
          <el-input :value="getActivityKindName(addForm.activityKindId)" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateActivityDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreateActivity" :loading="submittingNewActivity">创建活动</el-button>
      </template>
    </el-dialog>

    <!-- 活动统计对话框 -->
    <el-dialog v-model="showStatsDialog" :title="`📊 ${currentStatsPetName}的活动统计`" width="700px" :close-on-click-modal="false">
      <div v-loading="statsLoading" element-loading-text="加载统计数据中...">
        <div class="stats-period-selector">
          <el-radio-group v-model="statsPeriod" @change="handleStatsPeriodChange">
            <el-radio-button label="MONTHLY">月度统计</el-radio-button>
            <el-radio-button label="WEEKLY">周度统计</el-radio-button>
          </el-radio-group>
        </div>
        <div class="stats-overview">
          <div class="stat-card">
            <div class="stat-value">{{ statsData.totalActivities || 0 }}</div>
            <div class="stat-label">总活动数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statsData.uniqueActivityTypes || 0 }}</div>
            <div class="stat-label">活动类型</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ statsData.uniqueActivityKinds || 0 }}</div>
            <div class="stat-label">活动大类</div>
          </div>
        </div>
        <div class="stats-frequent" v-if="statsData.mostFrequentActivity">
          <div class="frequent-item">
            <span class="frequent-label">最频繁活动:</span>
            <span class="frequent-value">{{ statsData.mostFrequentActivity }}</span>
            <span class="frequent-count">{{ statsData.mostFrequentCount || 0 }}次</span>
          </div>
          <div class="frequent-item" v-if="statsData.mostFrequentKind">
            <span class="frequent-label">最频繁大类:</span>
            <span class="frequent-value">{{ statsData.mostFrequentKind }}</span>
            <span class="frequent-count">{{ statsData.mostFrequentKindCount || 0 }}次</span>
          </div>
        </div>
        <div class="stats-detail">
          <h4>{{ statsPeriod === 'MONTHLY' ? '月度详情' : '周度详情' }}</h4>
          <div v-if="statsPeriod === 'MONTHLY' && statsData.monthlyStats && statsData.monthlyStats.length > 0" class="stats-list">
            <div v-for="(month, index) in statsData.monthlyStats.slice().reverse()" :key="index" class="stat-month-item">
              <div class="month-header">
                <span class="month-name">{{ month.yearMonth }}</span>
                <span class="month-total">{{ month.totalActivities }}次活动</span>
              </div>
              <div class="month-activities" v-if="month.activityTypeCounts">
                <div v-for="(count, type) in month.activityTypeCounts" :key="type" class="activity-tag">
                  {{ type }}: {{ count }}
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="statsPeriod === 'WEEKLY' && statsData.weeklyStats && statsData.weeklyStats.length > 0" class="stats-list">
            <div v-for="(week, index) in statsData.weeklyStats.slice().reverse()" :key="index" class="stat-week-item">
              <div class="week-header">
                <span class="week-name">{{ week.weekRange }}</span>
                <span class="week-total">{{ week.totalActivities }}次活动</span>
              </div>
              <div class="week-activities" v-if="week.activityTypeCounts">
                <div v-for="(count, type) in week.activityTypeCounts" :key="type" class="activity-tag">
                  {{ type }}: {{ count }}
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无统计数据" :image-size="80" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showStatsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import apiService from '@/api/modules'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import PetStatusCard from '@/components/petspace/PetStatusCard.vue'
import {
  Plus,
  Refresh,
  MagicStick,
  EditPen,
  Fold,
  Expand,
  MoreFilled
} from '@element-plus/icons-vue'
import * as statusApi from '@/api/status'
import { getPetStatusSummary } from '@/api/llm'

const authStore = useAuthStore()

const getHeatmapLevel = (col, row) => {
  const seed = (col * 7 + row) * 11
  // Mock a dynamic pattern that mimics real logs
  if (seed % 5 === 0) return 4
  if (seed % 3 === 0) return 2
  if (seed % 2 === 0) return 1
  return 0
}


// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const showAddPetDialog = ref(false)
const submittingPet = ref(false)

// AI状态总结相关
const showAISummaryDialog = ref(false)
const aiSummaryLoading = ref(false)
const aiSummaryContent = ref('')
const currentPetName = ref('')
const currentAnalyzingPetId = ref(null)
const userPrompt = ref('')
const useCustomPrompt = ref(false)

// 活动统计相关
const showStatsDialog = ref(false)
const statsLoading = ref(false)
const statsData = ref(null)
const statsPeriod = ref('MONTHLY') // MONTHLY 或 WEEKLY
const currentStatsPetId = ref(null)
const currentStatsPetName = ref('')

// 创建新活动相关
const showCreateActivityDialog = ref(false)
const submittingNewActivity = ref(false)
const newActivityFormRef = ref()
const newActivityForm = ref({
  activityName: ''
})

const userPets = ref([])
const activityRecords = ref([])
const userActivities = ref([])
const allActivities = ref([])
const selectedPetId = ref(null) // 当前选中的主操作宠物ID
const selectedPetIds = ref([]) // 用于多选查看时间轴的宠物集合
const isSidebarCollapsed = ref(false) // macOS风格侧边栏折叠状态
const isStatusSidebarCollapsed = ref(false) // 宠物状态侧边栏折叠状态
const selectedActivityTypes = ref([1, 2, 3, 4, 5, 6, 7, 8, 9]) // 默认选择所有活动类型
const dateRange = ref([])

// AI Report variables
const aiReportData = ref(null)
const aiReportLoading = ref(false)
const aiReportError = ref(false)

const formatAiAnalysisText = (text) => {
  if (!text) return ''
  // Basic markdown bold to HTML
  return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

const fetchAiReport = async () => {
  if (!selectedPetId.value) return
  
  aiReportLoading.value = true
  aiReportError.value = false
  
  try {
    const { getActivityHealthAnalysis } = await import('@/api/ai')
    aiReportData.value = await getActivityHealthAnalysis(selectedPetId.value, 7, '')
  } catch (error) {
    console.error('Failed to fetch AI report:', error)
    aiReportError.value = true
    aiReportData.value = null
  } finally {
    aiReportLoading.value = false
  }
}



// 存储每个宠物的总活动记录数（用于卡片显示）
const petActivityStats = ref({})

// 记录类型切换
const currentRecordType = ref('activity') // 'activity' 或 'status'

// 状态记录相关数据
const statusLoading = ref(false)
const submittingStatus = ref(false)
const showAddStatusDialog = ref(false)
const showEditStatusDialog = ref(false)
const statusRecords = ref([])
const userStatuses = ref([])
const statusDateRange = ref([])
const statusFileList = ref([])
const editStatusFileList = ref([])

// 状态记录表单数据
const addStatusFormRef = ref()
const editStatusFormRef = ref()
const addStatusForm = ref({
  petId: null,
  statusId: null,
  startDate: '',
  description: '',
  file: null
})

const editStatusForm = ref({
  statusRecordId: null,
  petId: null,
  petName: '',
  statusId: null,
  startDate: '',
  endDate: null,
  description: '',
  file: null
})

// 表单数据
const addFormRef = ref()
const editFormRef = ref()

// 格式化日期为本地时间 (北京时间 UTC+8)
// 返回格式: yyyy-MM-dd'T'HH:mm:ss (API要求的格式)
const formatDateToLocal = (date) => {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

const addForm = ref({
  petId: null,
  activityKindId: null,
  activityId: null,
  activityDate: '',
  description: '',
  dosage: ''
})

const editForm = ref({
  activityRecordId: null,
  petId: null,
  petName: '',
  activityId: null,
  activityDate: '',
  description: '',
  mediaFiles: [],
  dosage: ''
})

// 活动记录媒体文件相关数据
const activityFileList = ref([])
const editActivityFileList = ref([])
const activityUploadRef = ref()
const editActivityUploadRef = ref()

// 宠物表单数据
const petFormRef = ref()
const petForm = ref({
  name: '',
  type: '',
  breed: '',
  gender: '',
  birthday: ''
})

// 表单验证规则
const addFormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  activityKindId: [{ required: true, message: '请选择活动类别', trigger: 'change' }],
  activityDate: [
    {
      validator: (rule, value, callback) => {
        // 现在日期是可选字段，如果不提供则使用当前时间
        if (!value) {
          callback()
        } else {
          // 验证日期格式
          try {
            new Date(value)
            callback()
          } catch (error) {
            callback(new Error('日期格式不正确'))
          }
        }
      },
      trigger: 'change'
    }
  ],
  // description 现在是可选字段，不需要必填验证
  activityId: [
    {
      validator: (rule, value, callback) => {
        // 现在允许直接使用活动种类创建记录，所以具体活动不是必需的
        // 只要选择了活动类别就可以通过验证
        if (addForm.value.activityKindId) {
          callback()
        } else {
          callback(new Error('请先选择活动类别'))
        }
      },
      trigger: 'change'
    }
  ]
}

// 编辑表单验证规则
const editFormRules = {
  activityId: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  activityDate: [
    {
      validator: (rule, value, callback) => {
        // 日期是可选字段，如果不提供则使用当前时间
        if (!value) {
          callback()
        } else {
          // 验证日期格式
          try {
            new Date(value)
            callback()
          } catch (error) {
            callback(new Error('日期格式不正确'))
          }
        }
      },
      trigger: 'change'
    }
  ]
}

// 宠物表单验证规则
const petFormRules = {
  name: [{ required: true, message: '请输入宠物名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择宠物类型', trigger: 'change' }],
  breed: [{ required: true, message: '请输入宠物品种', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择宠物性别', trigger: 'change' }],
  birthday: [
    { required: true, message: '请选择宠物生日', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const selectedDate = new Date(value)
        const today = new Date()
        if (selectedDate > today) {
          callback(new Error('生日不能是未来日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 新建活动表单验证规则
const newActivityFormRules = {
  activityName: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 1, max: 100, message: '活动名称长度应在1-100个字符之间', trigger: 'blur' }
  ]
}

// 状态记录表单验证规则
const addStatusFormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  statusId: [{ required: true, message: '请选择状态类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }]
}

const editStatusFormRules = {
  statusId: [{ required: true, message: '请选择状态类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }]
}

// 活动类型 - 匹配数据库中的实际活动种类
const activityTypes = ref([
  { label: '喂养', value: 1 },
  { label: '互动', value: 2 },
  { label: '清洁', value: 3 },
  { label: '外出', value: 4 },
  { label: '运动', value: 5 },
  { label: '医疗', value: 6 },
  { label: '生育', value: 7 },
  { label: '异常', value: 8 },
  { label: '其他', value: 9 }
])

// 计算属性
const currentUserId = computed(() => authStore.userId)

const totalActivities = computed(() => activityRecords.value.length)

const completedToday = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return activityRecords.value.filter(record =>
    record.activityDate.startsWith(today)
  ).length
})

const selectedPetsCount = computed(() => selectedPetId.value ? 1 : 0)

// 当前选中的宠物对象
const currentSelectedPet = computed(() => {
  if (!selectedPetId.value) return null
  return userPets.value.find(pet => (pet.id || pet.petId) === selectedPetId.value)
})

const filteredRecords = computed(() => {
  let filtered = activityRecords.value

  // 按选择的宠物过滤
  if (selectedPetId.value) {
    filtered = filtered.filter(record => record.petId === selectedPetId.value)
  }

  // 按日期范围过滤
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    filtered = filtered.filter(record => {
      const recordDate = record.activityDate.split(' ')[0]
      return recordDate >= startDate && recordDate <= endDate
    })
  }

    // 按活动类型筛选
    if (selectedActivityTypes.value.length > 0) {
      filtered = filtered.filter(record => {
        const recordKindId = String(record.activityKindId)
        const shouldInclude = selectedActivityTypes.value.some(type => String(type) === recordKindId)
        return shouldInclude
      })
    }

  return filtered.sort((a, b) => new Date(b.activityDate) - new Date(a.activityDate))
})

const groupedRecords = computed(() => {
  const groups = {}
  filteredRecords.value.forEach(record => {
    const date = record.activityDate.split(' ')[0]
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(record)
  })
  return groups
})

// 状态记录相关计算属性
const activityCount = computed(() => activityRecords.value.length)

const statusCount = computed(() => statusRecords.value.length)

const filteredStatusRecords = computed(() => {
  let filtered = statusRecords.value

  // 按选择的宠物过滤
  if (selectedPetId.value) {
    filtered = filtered.filter(record => record.petId === selectedPetId.value)
  }

  // 按日期范围过滤
  if (statusDateRange.value && statusDateRange.value.length === 2) {
    const [startDate, endDate] = statusDateRange.value
    filtered = filtered.filter(record => {
      const recordDate = record.startDate
      return recordDate >= startDate && recordDate <= endDate
    })
  }

  return filtered.sort((a, b) => new Date(b.startDate) - new Date(a.startDate))
})

const groupedStatusRecords = computed(() => {
  const groups = {}
  filteredStatusRecords.value.forEach(record => {
    const date = record.startDate
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(record)
  })
  return groups
})

// 方法
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  } else {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
}

const formatTime = (dateTimeStr) => {
  const date = new Date(dateTimeStr)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// Apple Health 日历时间带
const recentDays = computed(() => {
  const days = [];
  const today = new Date();
  for (let i = 29; i >= 0; i--) { // 扩展为30天滑动跨度
    const d = new Date(today);
    d.setDate(today.getDate() - i);

    // 手动格式化避免时区问题
    const year = d.getFullYear();
    const monthNum = d.getMonth() + 1;
    const month = String(monthNum).padStart(2, '0');
    const dayNameStr = String(d.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${dayNameStr}`;
    const weekName = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()];

    days.push({
      dateStr,
      monthAndDayName: `${monthNum}月 · ${weekName}`, // 清晰展示年月
      dayNum: d.getDate()
    });
  }
  return days;
});

const handleScrubberWheel = (e) => {
  if (e.currentTarget) {
    e.currentTarget.scrollLeft += (e.deltaY * 0.8);
  }
};

const isDateInRange = (dateStr) => {
  if (!dateRange.value || dateRange.value.length < 2) return false;
  return dateStr >= dateRange.value[0] && dateStr <= dateRange.value[1];
};

const selectScrubberDate = (dateStr) => {
  if (!dateRange.value || dateRange.value.length === 0) {
    // 第一次点击，默认选中单天
    dateRange.value = [dateStr, dateStr];
  } else {
    // 之前已经有选择范围了
    const currentStart = dateRange.value[0];
    const currentEnd = dateRange.value[1];

    if (currentStart === currentEnd) {
      if (dateStr === currentStart) {
        // 点击已经被选取的同一天，取消选取，展示所有
        dateRange.value = [];
      } else {
        // 点击不同天，形成范围跨度
        const d1 = new Date(currentStart);
        const d2 = new Date(dateStr);
        if (d1 > d2) {
          dateRange.value = [dateStr, currentStart];
        } else {
          dateRange.value = [currentStart, dateStr];
        }
      }
    } else {
      // 当前是跨度模式，若是重新点击则视为重置单天
      dateRange.value = [dateStr, dateStr];
    }
  }
  loadActivityRecords();
};

const toggleActivityType = (typeValue) => {
  const index = selectedActivityTypes.value.indexOf(typeValue);
  if (index > -1) {
    selectedActivityTypes.value.splice(index, 1);
  } else {
    selectedActivityTypes.value.push(typeValue);
  }
  loadActivityRecords();
};

const getPetInfo = (petId) => {
  const pet = userPets.value.find(p => (p.id || p.petId) === petId)
  return pet || { name: '未知宠物', avatar_url: '' }
}

// 处理活动类别变化
const handleActivityKindChange = () => {
  // 当活动类别变化时，清空具体活动选择
  addForm.value.activityId = null
}

// 根据活动类别ID获取用户的具体活动列表
const getActivitiesByKind = (activityKindId) => {
  if (!activityKindId) return []

  return userActivities.value.filter(activity =>
    activity.activityKindId === activityKindId
  )
}

const getActivityTypeName = (activityId) => {
  // 首先在用户活动列表中找到对应的活动，获取activityKindId
  const activity = userActivities.value.find(a => a.activityId === activityId)
  if (!activity) return '未知活动'

  // 然后根据activityKindId找到类型名称
  const type = activityTypes.value.find(t => t.value === activity.activityKindId)
  return type ? type.label : '未知活动'
}

// 根据活动种类ID获取活动种类名称
const getActivityKindName = (activityKindId) => {
  const type = activityTypes.value.find(t => t.value === activityKindId)
  return type ? type.label : '未知类别'
}

// 创建新活动的方法
const submitCreateActivity = async () => {
  if (!newActivityFormRef.value) return

  try {
    await newActivityFormRef.value.validate()
    submittingNewActivity.value = true

    // 导入活动创建API
    const { createActivity } = await import('@/api/activities')

    // 准备活动数据
    const activityData = {
      activityName: newActivityForm.value.activityName.trim(),
      activityKindId: addForm.value.activityKindId,
      userId: currentUserId.value
    }

    // 调用API创建活动
    const newActivity = await createActivity(activityData)

    ElMessage.success('活动创建成功！')

    // 重新从API加载活动列表，确保获取最新数据
    await loadUserActivities()

    // 关闭对话框并重置表单
    showCreateActivityDialog.value = false
    newActivityForm.value = {
      activityName: ''
    }

    // 自动选择新创建的活动（通过名称匹配）
    const newlyCreatedActivity = userActivities.value.find(
      a => a.activityName === activityData.activityName && a.activityKindId === activityData.activityKindId
    )
    if (newlyCreatedActivity) {
      addForm.value.activityId = newlyCreatedActivity.activityId
    }

  } catch (error) {
    console.error('创建活动失败:', error)
    ElMessage.error('创建活动失败: ' + error.message)
  } finally {
    submittingNewActivity.value = false
  }
}

const getActivityTypeClass = (activityId) => {
  // 首先在用户活动列表中找到对应的活动，获取activityKindId
  const activity = userActivities.value.find(a => a.activityId === activityId)
  if (!activity) return 'default'

  // 然后根据activityKindId返回对应的CSS类
  const classes = {
    1: 'diet',      // 喂养
    2: 'exercise',  // 互动
    3: 'hygiene',   // 清洁
    4: 'play',      // 外出
    5: 'training',  // 运动
    6: 'medical',   // 医疗
    7: 'breeding',  // 生育
    8: 'alert',     // 异常
    9: 'other'      // 其他
  }
  return classes[activity.activityKindId] || 'default'
}

const loadUserPets = async () => {
  if (!currentUserId.value) return

  try {
    loading.value = true
    // 使用正确的pets API
    const { getUserPets } = await import('@/api/pets')
    const pets = await getUserPets(currentUserId.value)

    // 处理API响应格式
    if (pets && pets.data) {
      userPets.value = Array.isArray(pets.data) ? pets.data : []
    } else {
      userPets.value = Array.isArray(pets) ? pets : []
    }

    // 默认选中第一个宠物
    if (!selectedPetId.value && userPets.value.length > 0) {
      const firstPetId = userPets.value[0].petId || userPets.value[0].id
      selectedPetId.value = firstPetId
      selectedPetIds.value = [firstPetId] // 同步初始化多选列表
    }
  } catch (error) {
    console.error('加载用户宠物失败:', error)
    userPets.value = []
  } finally {
    loading.value = false
  }
}

const loadUserActivities = async () => {
  if (!currentUserId.value) return

  try {
    const { getActivitiesByUserId } = await import('@/api/activities')
    const response = await getActivitiesByUserId(currentUserId.value)

    // 处理API响应格式
    if (response && response.data) {
      userActivities.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userActivities.value = response
    } else {
      userActivities.value = []
    }
  } catch (error) {
    console.error('加载用户活动失败:', error)
    userActivities.value = []
  }
}

const loadActivityRecords = async () => {
  try {
    loading.value = true



    // 获取选中宠物的活动记录 (支持多选)
    const queryIds = selectedPetIds.value.length > 0 ? selectedPetIds.value : (selectedPetId.value ? [selectedPetId.value] : [])

    if (queryIds.length === 0) {
      activityRecords.value = []
      return
    }

    const { getActivityRecordsByPetIds } = await import('@/api/activities')
    const { getRelatedMedia } = await import('@/api/media')

    // 构建查询参数
    const params = {
      page: 0,
      size: 100
    }
    
    // 只有当有日期选择时才添加日期参数
    if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
      const startDate = new Date(dateRange.value[0])
      params.startDate = startDate.toISOString()
      const endDate = new Date(dateRange.value[1])
      // 设置为当天的最后一刻
      endDate.setHours(23, 59, 59, 999)
      params.endDate = endDate.toISOString()
    }
    
    // 合并逻辑：使用 queryIds 支持多选，结合 page/size/date 的精确 params
    const recordsResponse = await getActivityRecordsByPetIds(queryIds, params)

    // 处理API响应格式 - 支持数组和Page对象两种格式
    let records = []
    if (recordsResponse) {
      // 方式1: Spring Page对象格式 (有content属性)
      if (recordsResponse.content && Array.isArray(recordsResponse.content)) {
        records = recordsResponse.content
      }
      // 方式2: 直接数组格式
      else if (Array.isArray(recordsResponse)) {
        records = recordsResponse
      }
      // 方式3: 有data属性的响应
      else if (recordsResponse.data) {
        if (recordsResponse.data.content && Array.isArray(recordsResponse.data.content)) {
          records = recordsResponse.data.content
        } else if (Array.isArray(recordsResponse.data)) {
          records = recordsResponse.data
        }
      }
    }

    // 为每个活动记录获取关联的媒体文件
    const recordsWithMedia = await Promise.all(
      records.map(async (record) => {
        try {
          const mediaResponse = await getRelatedMedia('activity', record.activityRecordId || record.id)
          const mediaFiles = (mediaResponse && mediaResponse.data) ? mediaResponse.data : []
          return {
            ...record,
            mediaFiles: mediaFiles,
            mediaCount: mediaFiles.length,
            firstMediaUrl: mediaFiles.length > 0 ? mediaFiles[0].fileUrl : null
          }
        } catch (mediaError) {
          return {
            ...record,
            mediaFiles: [],
            mediaCount: 0,
            firstMediaUrl: null
          }
        }
      })
    )

    activityRecords.value = recordsWithMedia
  } catch (error) {
    console.error('加载活动记录失败:', error)
    activityRecords.value = []
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  // 1. 首先加载宠物数据，这样才能设置selectedPetId
  await loadUserPets()
  // 2. 加载用户活动数据
  await loadUserActivities()
  // 3. 然后加载活动记录（依赖selectedPetId）
  await loadActivityRecords()
  // 4. 最后加载宠物活动统计数据
  await loadPetActivityStats()
}

// 打开添加记录对话框，自动设置当前时间
const openAddDialog = () => {
  // 设置默认时间为当前系统时间
  const now = new Date()
  const formattedNow = formatDateToLocal(now)
  addForm.value.activityDate = formattedNow

  // 设置默认宠物为当前选中的宠物
  addForm.value.petId = selectedPetId.value

  showAddDialog.value = true
}

const handleDateRangeChange = () => {
  loadActivityRecords()
}

const submitAddForm = async () => {
  if (!addFormRef.value) return

  try {
    await addFormRef.value.validate()
    submitting.value = true

    // 导入API
    const { createActivityRecord, createActivityRecordByKind } = await import('@/api/activities')
    const { uploadMedia } = await import('@/api/media')

    // 只为当前选中的宠物创建活动记录
    const pet = userPets.value.find(pet => (pet.petId || pet.id) === selectedPetId.value)
    if (!pet) {
      ElMessage.error('请先选择宠物')
      return
    }

    // 判断使用哪种创建方式
    const availableActivities = getActivitiesByKind(addForm.value.activityKindId)
    const useDirectKindMode = availableActivities.length === 0 || !addForm.value.activityId

    const petId = pet.petId || pet.id

    // 格式化日期为API要求的格式 yyyy-MM-dd HH:mm:ss
    const formattedDate = formatDateToLocal(addForm.value.activityDate)
    
    let finalDescription = addForm.value.description
    if (addForm.value.activityKindId === 6 && addForm.value.dosage) {
      finalDescription = `${finalDescription} 【用药量：${addForm.value.dosage}】`
    }

    let recordData
    let result

    if (useDirectKindMode) {
      // 直接使用活动种类ID创建记录（新的API方式）
      recordData = {
        activityKindId: addForm.value.activityKindId,
        description: finalDescription,
        date: formattedDate,
        userId: Number(currentUserId.value)
      }
      result = await createActivityRecordByKind(petId, recordData)
    } else {
      // 使用具体活动ID创建记录（原有方式）
      recordData = {
        activityId: addForm.value.activityId,
        description: finalDescription,
        date: formattedDate,
        userId: Number(currentUserId.value)
      }
      result = await createActivityRecord(petId, recordData)
    }

    // 如果有媒体文件，上传媒体
    if (activityFileList.value.length > 0 && result && result.activityRecordId) {
      for (const fileItem of activityFileList.value) {
        if (fileItem.raw) {
          try {
            await uploadMedia(fileItem.raw, currentUserId.value, 'activity', result.activityRecordId)
          } catch (uploadError) {
            console.error('媒体上传失败:', uploadError)
            ElMessage.warning(`文件 ${fileItem.name} 上传失败，但活动记录已创建`)
          }
        }
      }
    }

    ElMessage.success(`成功为 ${pet.name} 添加活动记录！`)
    showAddDialog.value = false

    // 重置表单
    addForm.value = {
      petId: null,
      activityKindId: null,
      activityId: null,
      activityDate: '',
      description: '',
      dosage: ''
    }
    activityFileList.value = []

    // 刷新活动记录列表
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('添加活动记录失败:', error)
    ElMessage.error('添加活动记录失败: ' + error.message)
  } finally {
    submitting.value = false
  }
}

const editRecord = (record) => {
  try {
    // 活动记录中已经包含了activityKindId，直接使用
    const activityKindId = record.activityKindId

    // 找到宠物名称
    const pet = getPetInfo(record.petId)
    
    let desc = record.activityDescription || record.description || ''
    let dosage = ''
    const dosageMatch = desc.match(/【用药量：(.*?)】/)
    if (dosageMatch) {
      dosage = dosageMatch[1]
      desc = desc.replace(/【用药量：.*?】/, '').trim()
    }

    // 填充编辑表单
    editForm.value = {
      activityRecordId: record.activityRecordId || record.id,
      petId: record.petId,
      petName: pet.name || '未知宠物',
      activityId: activityKindId, // 直接使用活动记录中的activityKindId
      activityDate: record.activityDate || '',
      description: desc,
      mediaFiles: record.mediaFiles || [], // 初始化媒体文件数组
      dosage: dosage
    }

    editActivityFileList.value = []
    showEditDialog.value = true
  } catch (error) {
    console.error('编辑记录失败:', error)
    ElMessage.error('无法编辑该记录')
  }
}

const submitEditForm = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()
    submitting.value = true

    const selectedActivityKindId = editForm.value.activityId

    // 根据活动种类ID找到对应的实际活动ID
    const matchingActivity = userActivities.value.find(activity =>
      activity.activityKindId === selectedActivityKindId
    )

    // 格式化日期为API要求的格式 yyyy-MM-dd HH:mm:ss
    const formattedDate = formatDateToLocal(editForm.value.activityDate)

    // 导入API并更新记录
    const { updateActivityRecord } = await import('@/api/activities')
    const { uploadMedia } = await import('@/api/media')

    // 确保description字段不为空（API要求必需字段）
    let description = editForm.value.description || '无'
    if (editForm.value.activityId === 6 && editForm.value.dosage) {
      description = `${description} 【用药量：${editForm.value.dosage}】`
    }

    if (matchingActivity) {
      // 找到了具体的活动，使用现有的API
      await updateActivityRecord(editForm.value.activityRecordId, {
        newActivityId: matchingActivity.activityId,
        description: description,
        date: formattedDate
      })
    } else {
      // 没有找到具体活动，需要先创建一个对应的活动
      console.log('submitEditForm: 没有找到具体活动，先创建新活动')

      const { createActivity } = await import('@/api/activities')
      const activityTypeName = getActivityKindName(selectedActivityKindId)

      // 创建一个新的活动
      const newActivity = await createActivity({
        activityName: activityTypeName, // 使用活动种类名称作为活动名称
        activityKindId: selectedActivityKindId,
        userId: currentUserId.value
      })

      console.log('submitEditForm: 创建的新活动:', newActivity)

      // 使用新创建的活动ID更新记录
      if (newActivity && newActivity.activityId) {
        await updateActivityRecord(editForm.value.activityRecordId, {
          newActivityId: newActivity.activityId,
          description: description,
          date: formattedDate
        })
      } else {
        throw new Error('创建新活动失败，无法获取活动ID')
      }
    }

    // 如果有新的媒体文件，上传媒体
    if (editActivityFileList.value.length > 0) {
      for (const fileItem of editActivityFileList.value) {
        if (fileItem.raw) {
          try {
            await uploadMedia(fileItem.raw, currentUserId.value, 'activity', editForm.value.activityRecordId)
            console.log('媒体上传成功:', fileItem.name)
          } catch (uploadError) {
            console.error('媒体上传失败:', uploadError)
            ElMessage.warning(`文件 ${fileItem.name} 上传失败，但活动记录已更新`)
          }
        }
      }
    }

    ElMessage.success('活动记录更新成功！')
    showEditDialog.value = false

    // 刷新活动记录列表
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('更新活动记录失败:', error)
    ElMessage.error('更新活动记录失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const deleteRecord = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这条活动记录吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 导入API
    const { deleteActivityRecord } = await import('@/api/activities')

    // 删除活动记录
    const recordId = record.activityRecordId || record.id
    await deleteActivityRecord(recordId)

    ElMessage.success('删除成功！')
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除记录失败:', error)
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 宠物相关方法
const selectPet = (petId) => {
  // 单机卡片：切换主宠物，且重置多选视界仅为该宠物
  selectedPetId.value = petId
  selectedPetIds.value = [petId]
  loadActivityRecords()
}

const togglePetSelection = (petId) => {
  // 多选 Checkbox 点击逻辑
  const index = selectedPetIds.value.indexOf(petId)
  if (index === -1) {
    selectedPetIds.value.push(petId)
  } else {
    selectedPetIds.value.splice(index, 1)
  }

  // 移除了保底机制，允许完全不选择宠物
  loadActivityRecords()
}

// 加载所有宠物的活动统计数据
const loadPetActivityStats = async () => {
  if (!currentUserId.value || userPets.value.length === 0) {
    petActivityStats.value = {}
    return
  }

  try {
    const { getActivityRecordsByPetIds } = await import('@/api/activities')

    // 获取用户所有宠物的ID
    const allPetIds = userPets.value.map(pet => pet.petId || pet.id)

    // 获取所有宠物的所有活动记录（不限时间范围）
    const allRecordsResponse = await getActivityRecordsByPetIds(allPetIds, {
      startDate: null, // 不限制开始时间
      endDate: null    // 不限制结束时间
    })

    // 处理响应数据
    let allRecords = []
    if (allRecordsResponse && allRecordsResponse.data) {
      allRecords = Array.isArray(allRecordsResponse.data) ? allRecordsResponse.data : []
    } else if (Array.isArray(allRecordsResponse)) {
      allRecords = allRecordsResponse
    }

    // 统计每个宠物的活动记录数
    const stats = {}
    allPetIds.forEach(petId => {
      stats[petId] = 0
    })

    allRecords.forEach(record => {
      const petId = record.petId
      if (stats.hasOwnProperty(petId)) {
        stats[petId]++
      }
    })

    petActivityStats.value = stats
    console.log('宠物活动统计:', petActivityStats.value)

  } catch (error) {
    console.error('加载宠物活动统计失败:', error)
    // 发生错误时初始化为0
    const stats = {}
    userPets.value.forEach(pet => {
      stats[pet.petId || pet.id] = 0
    })
    petActivityStats.value = stats
  }
}

const getPetActivityCount = (petId) => {
  return petActivityStats.value[petId] || 0
}

const submitPetForm = async () => {
  if (!petFormRef.value) return

  try {
    await petFormRef.value.validate()
    submittingPet.value = true

    // 计算年龄（处理未来生日的情况）
    const today = new Date()
    const birthday = new Date(petForm.value.birthday)
    let age = today.getFullYear() - birthday.getFullYear()
    const monthDiff = today.getMonth() - birthday.getMonth()

    // 如果生日还没到，年龄减1
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthday.getDate())) {
      age -= 1
    }

    // 确保年龄不为负数，最小为0
    age = Math.max(0, age)

    // 确保age是数字类型
    if (isNaN(age) || age === null || age === undefined) {
      age = 0
    }

    console.log('宠物信息:', {
      birthday: petForm.value.birthday,
      today: today.toISOString(),
      calculatedAge: age,
      ageType: typeof age,
      userId: currentUserId.value,
      userIdType: typeof currentUserId.value
    })

    // 验证必需字段
    if (!currentUserId.value) {
      throw new Error('用户未登录，无法创建宠物')
    }

    // 构建符合API要求的宠物数据
    const petData = {
      name: petForm.value.name.trim(),
      species: petForm.value.type, // 前端type映射到后端species
      breed: petForm.value.breed.trim(),
      gender: petForm.value.gender === 'male' ? true : petForm.value.gender === 'female' ? false : null, // 转换为Boolean类型
      userId: Number(currentUserId.value), // 确保userId是数字类型
      birthday: petForm.value.birthday
    }

    // 最后的数据完整性验证
    const requiredFields = ['name', 'species', 'breed', 'userId', 'birthday']
    const missingFields = requiredFields.filter(field => {
      const value = petData[field]
      // 字段不能为null、undefined或空字符串
      return value === null || value === undefined || value === ''
    })

    if (missingFields.length > 0) {
      console.error('缺少必需字段:', missingFields)
      console.error('完整的petData:', petData)
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 重新验证字段存在性（修复后的逻辑）
    console.log('验证字段存在性:', {
      name: !!petData.name,
      species: !!petData.species,
      breed: !!petData.breed,
      userId: !!petData.userId,
      birthday: !!petData.birthday
    })

    console.log('发送到API的宠物数据:', petData)
    console.log('各字段类型和值:', {
      name: { value: petData.name, type: typeof petData.name },
      species: { value: petData.species, type: typeof petData.species },
      breed: { value: petData.breed, type: typeof petData.breed },
      userId: { value: petData.userId, type: typeof petData.userId },
      birthday: { value: petData.birthday, type: typeof petData.birthday }
    })

    // 调用真实的创建宠物API
    const { createPet } = await import('@/api/pets')
    const newPet = await createPet(petData)

    ElMessage.success('宠物添加成功！')
    showAddPetDialog.value = false

    // 重置表单
    petForm.value = {
      name: '',
      type: '',
      breed: '',
      gender: '',
      birthday: ''
    }

    // 重新加载宠物数据
    await loadUserPets()

    // 选中新添加的宠物
    if (newPet && newPet.petId) {
      selectedPetId.value = newPet.petId
    }

    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('添加宠物失败:', error)
    ElMessage.error('添加宠物失败: ' + error.message)
  } finally {
    submittingPet.value = false
  }
}

// 状态记录相关方法
const switchToActivity = () => {
  currentRecordType.value = 'activity'
  // 切换回活动记录时刷新数据，确保数据同步
  refreshData()
}

const switchToStatus = () => {
  currentRecordType.value = 'status'
  loadStatusData()
}

const handleStatusPetSelectionChange = () => {
  // 状态记录的筛选逻辑已在computed中实现
}

const handleStatusDateRangeChange = () => {
  // 状态记录的筛选逻辑已在computed中实现
}

const loadStatusData = async () => {
  try {
    await Promise.all([
      loadUserStatuses(),
      loadStatusRecords()
    ])
  } catch (error) {
    console.error('加载状态数据失败:', error)
  }
}

const loadUserStatuses = async () => {
  // 根据选中的宠物获取状态列表
  if (!selectedPetId.value) {
    userStatuses.value = []
    return
  }

  try {
    // 为选中的宠物获取状态
    const response = await statusApi.getUserStatuses(selectedPetId.value)

    // 处理返回的状态
    if (response && response.data) {
      userStatuses.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userStatuses.value = response
    } else {
      userStatuses.value = []
    }

    console.log('加载到的宠物状态:', userStatuses.value)
  } catch (error) {
    console.error('加载宠物状态失败:', error)
    userStatuses.value = []
  }
}

const loadStatusRecords = async () => {
  if (!currentUserId.value || !selectedPetId.value) {
    statusRecords.value = []
    return
  }

  try {
    statusLoading.value = true
    const response = await statusApi.getStatusRecords(selectedPetId.value, {
      startDate: statusDateRange.value[0],
      endDate: statusDateRange.value[1]
    })

    if (response && response.data) {
      statusRecords.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      statusRecords.value = response
    } else {
      statusRecords.value = []
    }
    console.log('加载到的状态记录:', statusRecords.value)
  } catch (error) {
    console.error('加载状态记录失败:', error)
    statusRecords.value = []
  } finally {
    statusLoading.value = false
  }
}

const refreshStatusData = async () => {
  console.log('refreshStatusData: 开始刷新状态数据')
  await loadStatusData()
}

const handleStatusFileChange = (file, fileList) => {
  statusFileList.value = fileList
  if (file.raw) {
    addStatusForm.value.file = file.raw
  }
}

const handleEditStatusFileChange = (file, fileList) => {
  editStatusFileList.value = fileList
  if (file.raw) {
    editStatusForm.value.file = file.raw
  }
}

const submitAddStatusForm = async () => {
  if (!addStatusFormRef.value) return

  try {
    await addStatusFormRef.value.validate()
    submittingStatus.value = true

    const recordData = {
      statusId: addStatusForm.value.statusId,
      petId: addStatusForm.value.petId,
      startDate: addStatusForm.value.startDate,
      description: addStatusForm.value.description,
      userId: currentUserId.value
    }

    if (addStatusForm.value.file) {
      recordData.file = addStatusForm.value.file
    }

    console.log('创建状态记录:', recordData)
    const result = await statusApi.createStatusRecord(recordData)

    ElMessage.success('状态记录创建成功！')
    showAddStatusDialog.value = false

    // 重置表单
    addStatusForm.value = {
      petId: null,
      statusId: null,
      startDate: '',
      description: '',
      file: null
    }
    statusFileList.value = []

    await loadStatusRecords()
  } catch (error) {
    console.error('创建状态记录失败:', error)
    ElMessage.error('创建状态记录失败: ' + error.message)
  } finally {
    submittingStatus.value = false
  }
}

const editStatusRecord = (record) => {
  try {
    console.log('editStatusRecord: 编辑状态记录:', record)

    const pet = getPetInfo(record.petId)

    editStatusForm.value = {
      statusRecordId: record.statusRecordId,
      petId: record.petId,
      petName: pet.name || '未知宠物',
      statusId: record.statusId,
      startDate: record.startDate,
      endDate: record.endDate,
      description: record.statusDescription || '',
      file: null
    }

    editStatusFileList.value = []
    showEditStatusDialog.value = true
  } catch (error) {
    console.error('编辑状态记录失败:', error)
    ElMessage.error('无法编辑该状态记录')
  }
}

const submitEditStatusForm = async () => {
  if (!editStatusFormRef.value) return

  try {
    await editStatusFormRef.value.validate()
    submittingStatus.value = true

    const updateData = {
      description: editStatusForm.value.description,
      startDate: editStatusForm.value.startDate,
      endDate: editStatusForm.value.endDate
    }

    if (editStatusForm.value.file) {
      updateData.file = editStatusForm.value.file
      updateData.userId = currentUserId.value
    }

    console.log('更新状态记录:', editStatusForm.value.statusRecordId, updateData)
    await statusApi.updateStatusRecord(editStatusForm.value.statusRecordId, updateData)

    ElMessage.success('状态记录更新成功！')
    showEditStatusDialog.value = false

    await loadStatusRecords()
  } catch (error) {
    console.error('更新状态记录失败:', error)
    ElMessage.error('更新状态记录失败: ' + error.message)
  } finally {
    submittingStatus.value = false
  }
}

const stopStatusRecord = async (record) => {
  try {
    await ElMessageBox.confirm('确定要停止这条状态记录吗？', '确认停止', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const endDate = new Date().toISOString().split('T')[0]
    await statusApi.stopStatusRecord(record.statusRecordId, endDate)

    ElMessage.success('状态记录已停止')
    await loadStatusRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停止状态记录失败:', error)
      ElMessage.error('停止状态记录失败: ' + error.message)
    }
  }
}

const deleteStatusRecord = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这条状态记录吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await statusApi.deleteStatusRecord(record.statusRecordId)

    ElMessage.success('状态记录删除成功')
    await loadStatusRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除状态记录失败:', error)
      ElMessage.error('删除状态记录失败: ' + error.message)
    }
  }
}

const previewMedia = (media) => {
  if (media.fileType.startsWith('image/')) {
    // 对于图片文件，添加预览参数避免直接下载
    const previewUrl = media.fileUrl + (media.fileUrl.includes('?') ? '&' : '?') + 'preview=true'
    window.open(previewUrl, '_blank')
  } else if (media.fileType.startsWith('video/')) {
    // 对于视频文件，在浏览器中播放
    window.open(media.fileUrl, '_blank')
  } else {
    ElMessage.info('非图片/视频文件暂不支持预览，将直接下载')
    window.open(media.fileUrl, '_blank')
  }
}

// 处理编辑对话框中的媒体文件移除
const removeMediaFromEdit = (media) => {
  const targetId = typeof media === 'object' ? media.mediaId : media
  if (!editForm.value.mediaFiles) return
  const index = editForm.value.mediaFiles.findIndex(m => m.mediaId === targetId)
  if (index > -1) {
    editForm.value.mediaFiles.splice(index, 1)
  }
}

// 处理活动记录文件选择变化
const handleActivityFileChange = (file, fileList) => {
  activityFileList.value = fileList
  if (file.raw) {
    addForm.value.file = file.raw
  }
}

// 处理编辑活动记录文件选择变化
const handleEditActivityFileChange = (file, fileList) => {
  editActivityFileList.value = fileList
  if (file.raw) {
    editForm.value.file = file.raw
  }
}

// 活动记录上传前的处理
const beforeActivityUpload = (file) => {
  const isValid = file.size / 1024 / 1024 < 50 // 50MB限制
  if (!isValid) {
    ElMessage.error('文件大小不能超过 50MB!')
    return false
  }
  return true
}

// 处理活动记录上传成功
const handleActivityUploadSuccess = (response, file) => {
  if (response.code === 200) {
    ElMessage.success('媒体文件上传成功!')
    // 上传成功后可以预览或处理文件
  } else {
    ElMessage.error('上传失败: ' + response.message)
  }
}

// 处理活动记录上传错误
const handleActivityUploadError = (error) => {
  ElMessage.error('上传过程中发生错误')
  console.error('Upload error:', error)
}

// 生命周期
onMounted(async () => {
  await refreshData()
})

// 活动类型筛选方法
const handleActivityTypeFilter = () => {
  // 筛选逻辑已在computed属性中实现
  console.log('选择的活动类型:', selectedActivityTypes.value)
}

const clearActivityTypeFilter = () => {
  selectedActivityTypes.value = []
  console.log('清除活动类型筛选')
}

// AI状态总结相关方法
const formattedAISummary = computed(() => {
  if (!aiSummaryContent.value) return ''
  // 将换行符转换为HTML换行，移除markdown星号，格式化列表
  return aiSummaryContent.value
    .replace(/\n/g, '<br>')
    .replace(/\*\*/g, '')
    .replace(/- /g, '• ')
    .replace(/\*(.+?)\*/g, '<strong>$1</strong>')
})

const getAIStatusSummary = async (petId, petName) => {
  console.log(`打开宠物 ${petName} (ID: ${petId}) 的AI状态总结对话框`)

  // 初始化状态
  currentAnalyzingPetId.value = petId
  currentPetName.value = petName
  showAISummaryDialog.value = true
  aiSummaryContent.value = ''
  userPrompt.value = ''
  useCustomPrompt.value = false // 默认使用通用分析
}

// 开始通用分析（不使用提示词）
const startGeneralAnalysis = async () => {
  // 防止并发请求
  if (aiSummaryLoading.value) {
    console.log('已有AI分析请求进行中，跳过新请求')
    return
  }

  console.log(`开始通用AI分析，宠物: ${currentPetName.value} (ID: ${currentAnalyzingPetId.value})`)

  aiSummaryLoading.value = true
  aiSummaryContent.value = ''

  try {
    console.log('调用getPetStatusSummary，参数: petId=', currentAnalyzingPetId.value, 'userRequirement=null')
    const result = await getPetStatusSummary(currentAnalyzingPetId.value) // 不传提示词
    console.log('通用AI分析返回结果:', result)
    console.log('结果类型:', typeof result)
    console.log('结果键:', result ? Object.keys(result) : 'null')

    // GraphQL 返回的数据结构: { petId, name, breed, species, healthAdvice, statusRecords }
    if (result && result.healthAdvice) {
      console.log('找到healthAdvice字段:', result.healthAdvice)
      aiSummaryContent.value = result.healthAdvice
      ElMessage.success('通用AI分析完成！')
    } else if (result && result.summary) {
      console.log('找到summary字段:', result.summary)
      aiSummaryContent.value = result.summary
      ElMessage.success('通用AI分析完成！')
    } else if (typeof result === 'string') {
      console.log('结果是字符串:', result)
      aiSummaryContent.value = result
      ElMessage.success('通用AI分析完成！')
    } else if (result && result.data && result.data.summary) {
      console.log('找到result.data.summary字段:', result.data.summary)
      aiSummaryContent.value = result.data.summary
      ElMessage.success('通用AI分析完成！')
    } else {
      console.log('未找到有效的分析结果，结果对象:', result)
      aiSummaryContent.value = '暂无分析数据，请确保该宠物有足够的活动记录'
      ElMessage.warning('AI分析数据不足')
    }
  } catch (error) {
    console.error('通用AI分析失败:', error)
    handleAnalysisError(error)
  } finally {
    aiSummaryLoading.value = false
  }
}

// 开始个性化分析（使用用户输入的提示词）
const startCustomAnalysis = async () => {
  // 防止并发请求
  if (aiSummaryLoading.value) {
    console.log('已有AI分析请求进行中，跳过新请求')
    return
  }

  if (!userPrompt.value || !userPrompt.value.trim()) {
    ElMessage.warning('请输入您对宠物健康的关注点')
    return
  }

  console.log(`开始个性化AI分析，宠物: ${currentPetName.value} (ID: ${currentAnalyzingPetId.value})，提示词: ${userPrompt.value}`)

  aiSummaryLoading.value = true
  aiSummaryContent.value = ''

  try {
    console.log('调用getPetStatusSummary，参数: petId=', currentAnalyzingPetId.value, 'userRequirement=', userPrompt.value.trim())
    const result = await getPetStatusSummary(currentAnalyzingPetId.value, userPrompt.value.trim())
    console.log('个性化AI分析返回结果:', result)
    console.log('结果类型:', typeof result)
    console.log('结果键:', result ? Object.keys(result) : 'null')

    // GraphQL 返回的数据结构: { petId, name, breed, species, healthAdvice, statusRecords }
    if (result && result.healthAdvice) {
      console.log('找到healthAdvice字段:', result.healthAdvice)
      aiSummaryContent.value = result.healthAdvice
      ElMessage.success('个性化AI分析完成！')
    } else if (result && result.summary) {
      console.log('找到summary字段:', result.summary)
      aiSummaryContent.value = result.summary
      ElMessage.success('个性化AI分析完成！')
    } else if (typeof result === 'string') {
      console.log('结果是字符串:', result)
      aiSummaryContent.value = result
      ElMessage.success('个性化AI分析完成！')
    } else if (result && result.data && result.data.summary) {
      console.log('找到result.data.summary字段:', result.data.summary)
      aiSummaryContent.value = result.data.summary
      ElMessage.success('个性化AI分析完成！')
    } else {
      console.log('未找到有效的分析结果，结果对象:', result)
      aiSummaryContent.value = '暂无分析数据，请确保该宠物有足够的活动记录'
      ElMessage.warning('AI分析数据不足')
    }
  } catch (error) {
    console.error('个性化AI分析失败:', error)
    handleAnalysisError(error)
  } finally {
    aiSummaryLoading.value = false
    currentAnalyzingPetId.value = null
  }
}

// 处理分析错误的公共函数
const handleAnalysisError = (error) => {
  let errorMsg = '分析失败，请稍后重试'
  if (error.response) {
    errorMsg = `服务错误: ${error.response.status}`
  } else if (error.message) {
    errorMsg = error.message
  }

  aiSummaryContent.value = `❌ ${errorMsg}\n\n请检查：\n1. 网关是否正常运行（端口9000）\n2. LLM服务是否启动并注册到Nacos\n3. 网络连接是否正常`
  ElMessage.error('获取AI分析失败')
}

// 重新分析（清空结果，让用户重新输入提示词）
const reanalyzeWithNewPrompt = () => {
  aiSummaryContent.value = ''
  userPrompt.value = ''
}

// 设置示例提示词
const setExamplePrompt = (exampleText) => {
  userPrompt.value = exampleText
}

const copyAISummary = () => {
  let content = aiSummaryContent.value
  if (userPrompt.value && userPrompt.value.trim()) {
    content = `主人关注点：${userPrompt.value}\n\n${content}`
  }

  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('报告已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 获取宠物活动统计
const getPetStats = async (petId, petName) => {
  console.log(`开始获取宠物 ${petName} (ID: ${petId}) 的活动统计`)

  statsLoading.value = true
  currentStatsPetId.value = petId
  currentStatsPetName.value = petName
  showStatsDialog.value = true
  statsData.value = null

  try {
    const { getActivityStats } = await import('@/api/statistics')
    const result = await getActivityStats(petId, statsPeriod.value)
    console.log('活动统计返回:', result)

    // 处理不同的响应格式
    if (result && result.data) {
      statsData.value = result.data
    } else if (result) {
      statsData.value = result
    } else {
      statsData.value = null
      ElMessage.warning('暂无统计数据')
    }
  } catch (error) {
    console.error('获取活动统计失败:', error)
    ElMessage.error('获取统计数据失败: ' + (error.message || '未知错误'))
    statsData.value = null
  } finally {
    statsLoading.value = false
    currentStatsPetId.value = null
  }
}

// 切换统计周期
const handleStatsPeriodChange = async () => {
  if (currentStatsPetName.value) {
    // 重新获取统计数据
    const petId = currentStatsPetId.value || statsData.value?.petId
    if (petId) {
      await getPetStats(petId, currentStatsPetName.value)
    }
  }
}

// 监听器
watch([currentUserId], () => {
  if (currentUserId.value) {
    refreshData()
  }
})
</script>

<style scoped>
.activities-page {
  min-height: 100vh;
  background: #FEF7FF; /* MD3 Surface */
  font-family: "Microsoft YaHei", Roboto, system-ui, sans-serif;
  color: #1C1B1F;
}

/* Hero Section Retained */
.banner-section {
  position: relative;
  min-height: 48vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #000;
  text-align: center;
  padding: 40px 20px;
}

.banner-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  opacity: 0.85; z-index: 0;
  background-image: url('https://images.unsplash.com/photo-1450778869180-41d0601e046e?auto=format&fit=crop&q=80');
  background-size: cover;
  background-position: center;
}
.banner-bg::after {
  content: ''; position: absolute; bottom: 0; left: 0; width: 100%; height: 60%;
  background: linear-gradient(to top, rgba(0,0,0,0.85), rgba(0,0,0,0));
}
.banner-content {
  position: relative; z-index: 10; display: flex; flex-direction: column; align-items: center;
}
.banner-title {
  font-size: 56px; font-weight: 800; color: #fff; margin-bottom: 24px;
}
.banner-subtitle {
  font-size: 20px; color: rgba(255,255,255,0.9); max-width: 600px;
}

/* ===== MD3 Page Body ===== */
.md3-page-body {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 40px 80px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ===== Stats Row ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.md3-stat-card {
  background: white; border-radius: 16px; padding: 24px 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.07), 0 4px 12px rgba(103,80,164,0.04);
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid rgba(103, 80, 164, 0.06);
}
.md3-stat-card:hover {
  box-shadow: 0 4px 16px rgba(103,80,164,0.10); transform: translateY(-2px);
}

.stat-icon-wrap {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; flex-shrink: 0;
}

.stat-info { flex: 1; }
.stat-number { font-size: 28px; font-weight: 700; color: #1C1B1F; line-height: 1.1; }
.stat-label { font-size: 13px; color: #625B71; margin-top: 4px; }

/* ===== Dashboard Two-Column Panel ===== */
.dashboard-panel {
  display: grid; grid-template-columns: 340px 1fr; gap: 24px; align-items: start;
}

/* ===== MD3 Generic Card ===== */
.md3-card {
  background: white; border-radius: 16px; padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.07), 0 2px 8px rgba(103,80,164,0.04);
  border: 1px solid rgba(103, 80, 164, 0.06);
}

.panel-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.panel-title { font-size: 16px; font-weight: 600; color: #1C1B1F; }
.pets-count-badge { background: #6750A4; color: white; font-size: 12px; font-weight: 700; padding: 2px 9px; border-radius: 100px; }

/* ===== Pet List Panel ===== */
.pet-list-panel { min-height: 400px; }
.pets-list { display: flex; flex-direction: column; gap: 8px; }

.md3-pet-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  border-radius: 12px; cursor: pointer; transition: background 0.15s, box-shadow 0.15s; position: relative;
}
.md3-pet-item:hover { background: rgba(103, 80, 164, 0.05); }
.md3-pet-item.active { background: rgba(103, 80, 164, 0.10); box-shadow: inset 3px 0 0 #6750A4; }

.pet-item-left { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.pet-item-info { flex: 1; min-width: 0; }
.pet-item-name { font-size: 15px; font-weight: 600; color: #1C1B1F; }
.pet-item-sub { font-size: 12px; color: #625B71; margin-top: 2px; }
.pet-activity-count { font-size: 11px; color: #9E99A3; margin-top: 3px; }
.pet-item-actions { flex-shrink: 0; }

.more-options-btn {
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border-radius: 50%; cursor: pointer; transition: background 0.15s; color: #625B71;
}
.more-options-btn:hover { background: rgba(0,0,0,0.06); }

.md3-add-pet {
  display: flex; align-items: center; gap: 10px; padding: 12px 14px;
  border-radius: 12px; cursor: pointer; border: 2px dashed rgba(103, 80, 164, 0.2);
  color: #6750A4; font-size: 14px; font-weight: 500; transition: all 0.15s; margin-top: 4px;
}
.md3-add-pet:hover { border-color: #6750A4; background: rgba(103, 80, 164, 0.05); }

/* ===== Pet Detail Panel ===== */
.pet-detail-panel { min-height: 400px; }

.pet-detail-header {
  display: flex; align-items: center; gap: 16px; padding-bottom: 20px; border-bottom: 1px solid #F3EDF7; flex-wrap: wrap;
}

.detail-avatar { flex-shrink: 0; }
.pet-detail-meta { flex: 1; min-width: 200px; }
.pet-detail-name { font-size: 22px; font-weight: 700; color: #1C1B1F; }
.pet-detail-sub { font-size: 14px; color: #625B71; margin-top: 4px; display: flex; align-items: center; }
.pet-detail-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.pet-detail-status { margin: 20px 0; }
.pet-detail-recent { margin-top: 4px; }
.recent-header { font-size: 14px; font-weight: 600; color: #49454F; margin-bottom: 12px; }
.recent-empty { font-size: 14px; color: #9E99A3; padding: 16px 0; }

.recent-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid #F3EDF7;
}

.recent-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; background: #6750A4; }
.recent-dot.diet { background: #f59e0b; }
.recent-dot.exercise { background: #10b981; }
.recent-dot.hygiene { background: #3b82f6; }
.recent-dot.medical { background: #ef4444; }
.recent-dot.play { background: #8b5cf6; }
.recent-dot.training { background: #06b6d4; }
.recent-dot.breeding { background: #ec4899; }
.recent-dot.alert { background: #f97316; }

.recent-content { flex: 1; display: flex; justify-content: space-between; align-items: center; }
.recent-type { font-size: 14px; font-weight: 500; color: #1C1B1F; }
.recent-time { font-size: 12px; color: #9E99A3; }

.no-pet-selected {
  display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 20px; text-align: center;
}
.no-pet-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.4; }
.no-pet-text { font-size: 18px; font-weight: 600; color: #49454F; }
.no-pet-sub { font-size: 14px; color: #9E99A3; margin-top: 8px; }

/* ===== Timeline Area ===== */
.timeline-area { display: flex; flex-direction: column; gap: 20px; }

.scrubber-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.scrubber-actions { display: flex; align-items: center; }
.filter-card .filter-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.filter-card .filter-content { display: flex; flex-wrap: wrap; gap: 8px; }

.scrubber-track {
  display: flex;
  overflow-x: auto;
  gap: 12px;
  padding-bottom: 12px;
  -webkit-overflow-scrolling: touch;
}
.scrubber-track::-webkit-scrollbar { height: 6px; }
.scrubber-track::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 4px; }
.scrubber-track::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }

.scrubber-day {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-width: 60px; height: 70px; border-radius: 12px;
  background-color: #f8fafc; border: 1px solid #e2e8f0;
  cursor: pointer; transition: all 0.2s ease; flex-shrink: 0;
}
.scrubber-day:hover { background-color: #f1f5f9; transform: translateY(-2px); }
.scrubber-day.active { background-color: #6750A4; border-color: #6750A4; color: white; box-shadow: 0 4px 12px rgba(103,80,164,0.3); }
.scrubber-day.active .day-name, .scrubber-day.active .day-num { color: white; }
.day-name { font-size: 11px; color: #64748b; margin-bottom: 4px; font-weight: 500; }
.day-num { font-size: 18px; font-weight: 700; color: #0f172a; line-height: 1; }
.activity-indicator { width: 4px; height: 4px; border-radius: 50%; background-color: transparent; margin-top: 4px; }
.activity-indicator.has-activity { background-color: #38bdf8; }
.scrubber-day.active .activity-indicator.has-activity { background-color: white; }

/* Timeline Items */
.activity-timeline { display: flex; flex-direction: column; gap: 24px; margin-top: 20px;}
.timeline-group { display: flex; gap: 16px; }
.timeline-date { width: 80px; flex-shrink: 0; text-align: right; }
.date-badge { display: inline-block; padding: 4px 8px; background: rgba(103,80,164,0.1); color: #6750A4; border-radius: 12px; font-size: 12px; font-weight: 600; }
.timeline-items { flex: 1; display: flex; flex-direction: column; gap: 16px; }
.timeline-item { display: flex; gap: 16px; cursor: pointer; }
.timeline-item:hover .record-card { transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.timeline-marker { display: flex; flex-direction: column; align-items: center; width: 12px; }
.marker-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; border: 2px solid white; box-shadow: 0 0 0 1px #e2e8f0; background: #cbd5e1; }
.marker-line { width: 2px; flex: 1; background: #e2e8f0; margin-top: 4px; }

.timeline-content { flex: 1; padding-bottom: 24px; }
.record-card {
  background: white; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px;
  transition: all 0.2s;
}
.record-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.pet-info { display: flex; gap: 8px; align-items: center; }
.pet-details { display: flex; flex-direction: column; }
.pet-name { font-size: 14px; font-weight: 600; color: #1e293b; }
.activity-type { font-size: 12px; color: #64748b; }
.record-time { font-size: 12px; color: #94a3b8; font-variant-numeric: tabular-nums; }
.record-description { font-size: 14px; color: #334155; line-height: 1.5; white-space: pre-wrap; }

.record-media { margin-top: 12px; }
.media-preview { display: flex; gap: 8px; flex-wrap: wrap; }
.media-item { width: 60px; height: 60px; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; background: #f8fafc; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.media-item img { width: 100%; height: 100%; object-fit: cover; }
.file-icon { font-size: 24px; color: #94a3b8; }
.more-media { width: 60px; height: 60px; border-radius: 8px; background: rgba(103,80,164,0.1); color: #6750A4; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 14px; cursor: pointer; }
.record-actions { margin-top: 12px; display: flex; gap: 8px; justify-content: flex-end; opacity: 0.5; transition: opacity 0.2s; }
.record-card:hover .record-actions { opacity: 1; }

.marker-dot.diet { background-color: #f59e0b; }
.marker-dot.exercise { background-color: #10b981; }
.marker-dot.hygiene { background-color: #3b82f6; }
.marker-dot.medical { background-color: #ef4444; }
.marker-dot.play { background-color: #8b5cf6; }
.marker-dot.training { background-color: #06b6d4; }
.marker-dot.breeding { background-color: #ec4899; }
.marker-dot.alert { background-color: #f97316; }


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
.report-status.red { color: #ef4444; }
.report-status.red { color: #ef4444; }
.report-text { font-size: 13px; line-height: 1.6; color: #4b5563; margin: 0 0 16px 0; background: rgba(0,0,0,0.02); padding: 12px; border-radius: 12px; }
.report-metrics { display: flex; gap: 16px; margin-top: auto; }
.metric { flex: 1; background: #f8fafc; padding: 12px; border-radius: 12px; text-align: center; }
.metric-val { font-size: 18px; font-weight: 700; color: #1d1d1f; margin-bottom: 2px; }
.metric-label { font-size: 11px; color: #64748b; }

</style>
