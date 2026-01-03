<template>
  <div class="activities-page">
    <!-- 使用统一的布局头部 -->
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-emoji">🐾</div>
          <h2 class="hero-title">宠物活动中心</h2>
          <p class="hero-subtitle">记录和管理爱宠的每一个精彩瞬间</p>
        </div>
      </div>
    </section>

    <!-- 记录类型切换卡片 -->
    <section class="record-switch-section">
      <div class="container">
        <div class="record-type-cards">
          <div
            class="record-type-card"
            :class="{ active: currentRecordType === 'activity' }"
            @click="switchToActivity"
          >
            <div class="card-icon">
              <i class="fas fa-calendar-check"></i>
            </div>
            <div class="card-content">
              <h3>活动记录</h3>
              <p>记录宠物的日常活动和特殊时刻</p>
              <div class="card-count">{{ activityCount }}</div>
            </div>
            <div class="card-indicator"></div>
          </div>

          <div
            class="record-type-card"
            :class="{ active: currentRecordType === 'status' }"
            @click="switchToStatus"
          >
            <div class="card-icon">
              <i class="fas fa-heartbeat"></i>
            </div>
            <div class="card-content">
              <h3>状态记录</h3>
              <p>追踪宠物的健康和状态变化</p>
              <div class="card-count">{{ statusCount }}</div>
            </div>
            <div class="card-indicator"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 主要内容区域 -->
    <div class="content-layout">
    <!-- 左侧宠物边栏 -->
    <div class="pets-sidebar">
        <div class="sidebar-header">
          <div class="sidebar-title">
            <span class="title-icon">🐾</span>
            <span class="title-text">我的宠物</span>
            <span class="pets-count-badge">{{ userPets.length }}</span>
          </div>
        </div>

        <div class="sidebar-content">
          <div class="pets-list">
            <!-- 宠物卡片 -->
            <div
              v-for="pet in userPets"
              :key="pet.id || pet.petId"
              class="pet-card"
              :class="{ active: selectedPetIds.includes(pet.id || pet.petId) }"
              @click="togglePetSelection(pet.id || pet.petId)"
            >
              <div class="pet-main">
                <div class="pet-avatar">
                  <el-avatar :size="36" :src="pet.avatar_url">
                    {{ pet.name.charAt(0) }}
                  </el-avatar>
                  <div class="pet-status-dot" v-if="selectedPetIds.includes(pet.id || pet.petId)"></div>
                </div>
                <div class="pet-content">
                  <div class="pet-info-row">
                    <div class="pet-name">{{ pet.name }}</div>
                    <div class="pet-details">{{ pet.species || pet.type }} · {{ pet.breed }}</div>
                  </div>
                  <div class="pet-activities">
                    <div class="activity-count">{{ getPetActivityCount(pet.id || pet.petId) }}</div>
                    <div class="activity-label">活动</div>
                  </div>
                </div>
                <div class="pet-actions">
                  <!-- AI状态总结按钮 -->
                  <div class="pet-ai-action" @click.stop>
                  <el-button
                    type="primary"
                    size="small"
                    @click="getAIStatusSummary(pet.id || pet.petId, pet.name)"
                    :loading="aiSummaryLoading && currentAnalyzingPetId === (pet.id || pet.petId)"
                    :icon="aiSummaryLoading && currentAnalyzingPetId === (pet.id || pet.petId) ? 'Loading' : 'MagicStick'"
                  >
                    AI总结
                  </el-button>
                  </div>
                  <!-- 活动统计按钮 -->
                  <div class="pet-stats-action" @click.stop>
                  <el-button
                    type="info"
                    size="small"
                    @click="getPetStats(pet.id || pet.petId, pet.name)"
                    :loading="statsLoading && currentStatsPetId === (pet.id || pet.petId)"
                  >
                    统计
                  </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 添加宠物卡片 -->
            <div class="pet-card add-pet-card" @click="showAddPetDialog = true">
              <div class="add-pet-icon">
                <el-icon size="20"><Plus /></el-icon>
              </div>
              <div class="add-pet-text">添加宠物</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧主内容区域 -->
      <div class="main-content-area">

        <!-- 活动记录内容 -->
        <div v-if="currentRecordType === 'activity'" class="activity-content">
          <div class="content-container">

        <!-- 操作栏 -->
        <div class="action-bar">
          <div class="action-left">
            <el-select
              v-model="selectedPetIds"
              multiple
              placeholder="选择宠物"
              style="width: 200px"
              @change="handlePetSelectionChange"
            >
              <el-option
                v-for="pet in userPets"
                :key="pet.id || pet.petId"
                :label="pet.name"
                :value="pet.id || pet.petId"
              />
            </el-select>

            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
              size="default"
              style="width: 240px; margin-left: 12px"
            />
          </div>

          <div class="action-right">
            <el-button type="success" @click="showAddDialog = true">
              <el-icon><Plus /></el-icon>
              添加记录
            </el-button>
            <el-button @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>

        <!-- 活动类型筛选 -->
        <div class="activity-filters">
          <div class="filter-header">
            <span class="filter-title">活动类型筛选：</span>
          </div>
          <div class="filter-options">
            <el-checkbox-group v-model="selectedActivityTypes" @change="handleActivityTypeFilter">
              <el-checkbox
                v-for="type in activityTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              >
                <span class="filter-label" :class="`filter-${getActivityTypeClass(type.value)}`">
                  {{ type.label }}
                </span>
              </el-checkbox>
            </el-checkbox-group>
            <el-button
              size="small"
              @click="clearActivityTypeFilter"
              style="margin-left: 16px;"
            >
              清除筛选
            </el-button>
          </div>
        </div>

        <!-- 活动记录时间线 -->
        <div class="timeline-section">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="filteredRecords.length === 0" class="empty-state">
            <el-empty description="暂无活动记录">
              <el-button type="primary" @click="showAddDialog = true">
                创建第一条记录
              </el-button>
            </el-empty>
          </div>

          <div v-else class="activity-timeline">
            <div
              v-for="(group, date) in groupedRecords"
              :key="date"
              class="timeline-group"
            >
              <div class="timeline-date">
                <div class="date-badge">{{ formatDate(date) }}</div>
              </div>

              <div class="timeline-items">
                <div
                  v-for="record in group"
                  :key="record.activityRecordId"
                  class="timeline-item"
                  @click="editRecord(record)"
                >
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
                        <div class="record-time">
                          {{ formatTime(record.activityDate) }}
                        </div>
                      </div>

                      <div class="record-description">
                        {{ record.activityDescription }}
                      </div>

                      <!-- 活动记录媒体文件显示 -->
                      <div class="record-media" v-if="record.mediaFiles && record.mediaFiles.length > 0">
                        <div class="media-preview">
                          <div
                            v-for="media in record.mediaFiles.slice(0, 4)"
                            :key="media.mediaId"
                            class="media-item"
                            @click.stop="previewMedia(media)"
                          >
                            <img
                              v-if="media.fileType.startsWith('image/')"
                              :src="media.fileUrl"
                              :alt="media.fileName"
                            />
                            <div v-else class="file-icon">
                              <i class="fas fa-file"></i>
                            </div>
                          </div>
                          <div v-if="record.mediaFiles.length > 4" class="more-media">
                            +{{ record.mediaFiles.length - 4 }}
                          </div>
                        </div>
                      </div>

                      <div class="record-actions">
                        <el-button size="small" @click.stop="editRecord(record)">
                          编辑
                        </el-button>
                        <el-button size="small" type="danger" @click.stop="deleteRecord(record)">
                          删除
                        </el-button>
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

        <!-- 状态记录内容 -->
        <div v-if="currentRecordType === 'status'" class="status-content">
          <div class="content-container">
          <!-- 状态记录操作栏 -->
          <div class="status-action-bar">
            <div class="action-left">
              <el-select
                v-model="selectedPetIds"
                multiple
                placeholder="选择宠物"
                style="width: 200px"
                @change="handleStatusPetSelectionChange"
              >
                <el-option
                  v-for="pet in userPets"
                  :key="pet.id || pet.petId"
                  :label="pet.name"
                  :value="pet.id || pet.petId"
                />
              </el-select>

              <el-date-picker
                v-model="statusDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="handleStatusDateRangeChange"
                size="default"
                style="width: 240px; margin-left: 12px"
              />
            </div>

            <div class="action-right">
              <el-button type="success" @click="showAddStatusDialog = true">
                <el-icon><Plus /></el-icon>
                添加状态记录
              </el-button>
              <el-button @click="refreshStatusData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>

          <!-- 状态记录时间线 -->
          <div class="status-timeline-section">
            <div v-if="statusLoading" class="loading-container">
              <el-skeleton :rows="5" animated />
            </div>

            <div v-else-if="filteredStatusRecords.length === 0" class="empty-state">
              <el-empty description="暂无状态记录">
                <el-button type="primary" @click="showAddStatusDialog = true">
                  创建第一条状态记录
                </el-button>
              </el-empty>
            </div>

            <div v-else class="status-timeline">
              <div
                v-for="(group, date) in groupedStatusRecords"
                :key="date"
                class="timeline-group"
              >
                <div class="timeline-date">
                  <div class="date-badge">{{ formatDate(date) }}</div>
                </div>

                <div class="timeline-items">
                  <div
                    v-for="record in group"
                    :key="record.statusRecordId"
                    class="timeline-item status-item"
                    @click="editStatusRecord(record)"
                  >
                    <div class="timeline-marker">
                      <div class="marker-dot status-dot"></div>
                      <div class="marker-line"></div>
                    </div>

                    <div class="timeline-content">
                      <div class="record-card status-record-card">
                        <div class="record-header">
                          <div class="pet-info">
                            <el-avatar :size="32" :src="getPetInfo(record.petId).avatar_url">
                              {{ getPetInfo(record.petId).name.charAt(0) }}
                            </el-avatar>
                            <div class="pet-details">
                              <div class="pet-name">{{ getPetInfo(record.petId).name }}</div>
                              <div class="status-type">{{ record.statusName }}</div>
                            </div>
                          </div>
                          <div class="record-status-info">
                            <div class="record-time">{{ formatTime(record.startDate) }}</div>
                            <div class="status-duration" v-if="record.endDate">
                              至 {{ formatTime(record.endDate) }}
                            </div>
                            <div class="status-active" v-else>
                              <span class="active-indicator"></span>
                              进行中
                            </div>
                          </div>
                        </div>

                        <div class="record-description" v-if="record.statusDescription">
                          {{ record.statusDescription }}
                        </div>

                        <div class="record-media" v-if="record.mediaFiles && record.mediaFiles.length > 0">
                          <div class="media-preview">
                            <div
                              v-for="media in record.mediaFiles.slice(0, 3)"
                              :key="media.mediaId"
                              class="media-item"
                              @click.stop="previewMedia(media)"
                            >
                              <img
                                v-if="media.fileType.startsWith('image/')"
                                :src="media.fileUrl"
                                :alt="media.fileName"
                              />
                              <div v-else class="file-icon">
                                <i class="fas fa-file"></i>
                              </div>
                            </div>
                            <div v-if="record.mediaFiles.length > 3" class="more-media">
                              +{{ record.mediaFiles.length - 3 }}
                            </div>
                          </div>
                        </div>

                        <div class="record-actions">
                          <el-button size="small" @click.stop="editStatusRecord(record)">
                            编辑
                          </el-button>
                          <el-button
                            v-if="!record.endDate"
                            size="small"
                            type="warning"
                            @click.stop="stopStatusRecord(record)"
                          >
                            停止
                          </el-button>
                          <el-button size="small" type="danger" @click.stop="deleteStatusRecord(record)">
                            删除
                          </el-button>
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
      </div>
    </div>

  
    <!-- 添加活动记录对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加活动记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="addFormRules"
        label-width="100px"
      >
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="addForm.petId" placeholder="请选择宠物" style="width: 100%">
            <el-option
              v-for="pet in userPets"
              :key="pet.id || pet.petId"
              :label="pet.name"
              :value="pet.id || pet.petId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动类别" prop="activityKindId">
          <el-select
            v-model="addForm.activityKindId"
            placeholder="选择活动类别"
            style="width: 100%"
            @change="handleActivityKindChange"
          >
            <el-option
              v-for="type in activityTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="addForm.activityKindId"
          label="具体活动"
          prop="activityId"
        >
          <div style="display: flex; gap: 8px;">
            <el-select
              v-model="addForm.activityId"
              placeholder="选择具体活动（可选，也可直接使用类别）"
              style="flex: 1"
              filterable
              no-data-text="该类别下暂无活动，可直接使用类别创建记录"
            >
              <el-option
                v-for="activity in getActivitiesByKind(addForm.activityKindId)"
                :key="activity.activityId"
                :label="activity.activityName"
                :value="activity.activityId"
              />
            </el-select>
            <el-button
              type="primary"
              plain
              @click="showCreateActivityDialog = true"
              :disabled="!addForm.activityKindId"
            >
              新建活动
            </el-button>
          </div>
          <div v-if="addForm.activityKindId"
               style="margin-top: 8px; padding: 8px; background: #f0f9ff; border: 1px solid #bfdbfe; border-radius: 4px; font-size: 12px; color: #1e40af;">
            <div v-if="getActivitiesByKind(addForm.activityKindId).length === 0">
              该类别下还没有具体的活动。您可以选择：
              <ul style="margin: 4px 0; padding-left: 16px;">
                <li><strong>直接提交</strong>：系统将使用活动类别直接创建记录</li>
                <li><strong>新建活动</strong>：点击右侧"新建活动"按钮创建具体活动</li>
              </ul>
              例如，{{ getActivityKindName(addForm.activityKindId) }}类别可以包括：
              <span v-if="addForm.activityKindId === 1">吃零食、吃饭、喝水等</span>
              <span v-else-if="addForm.activityKindId === 2">玩耍、拥抱、训练等</span>
              <span v-else-if="addForm.activityKindId === 3">洗澡、刷牙、剪指甲等</span>
              <span v-else-if="addForm.activityKindId === 4">散步、公园游玩、旅行等</span>
              <span v-else-if="addForm.activityKindId === 5">跑步、爬楼梯、玩玩具等</span>
              <span v-else-if="addForm.activityKindId === 6">体检、打疫苗、吃药等</span>
              <span v-else-if="addForm.activityKindId === 7">发情、怀孕、生产等</span>
              <span v-else-if="addForm.activityKindId === 8">呕吐、腹泻、跛行等</span>
              <span v-else>其他具体活动</span>
            </div>
            <div v-else>
              既有具体活动可选，也可以直接使用活动类别创建记录
            </div>
          </div>
        </el-form-item>

        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker
            v-model="addForm.activityDate"
            type="datetime"
            placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="addForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述..."
          />
        </el-form-item>

        <el-form-item label="上传媒体文件">
          <el-upload
            ref="activityUploadRef"
            :auto-upload="false"
            :on-change="handleActivityFileChange"
            :limit="5"
            :file-list="activityFileList"
            action="#"
            :accept="'image/*,video/*,.pdf,.doc,.docx'"
            multiple
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、视频、PDF、Word文档，最多5个文件，每个文件不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑活动记录对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑活动记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        label-width="100px"
      >
        <el-form-item label="宠物名称" prop="petId">
          <el-input v-model="editForm.petName" disabled placeholder="宠物名称" />
        </el-form-item>

        <el-form-item label="活动类型" prop="activityId">
          <el-select v-model="editForm.activityId" placeholder="选择活动类型" style="width: 100%">
            <el-option
              v-for="type in activityTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker
            v-model="editForm.activityDate"
            type="datetime"
            placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述..."
          />
        </el-form-item>

        <el-form-item label="上传媒体文件">
          <el-upload
            ref="editActivityUploadRef"
            :auto-upload="false"
            :on-change="handleEditActivityFileChange"
            :limit="5"
            :file-list="editActivityFileList"
            action="#"
            :accept="'image/*,video/*,.pdf,.doc,.docx'"
            multiple
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                上传新文件将替换现有文件，支持图片、视频、PDF、Word文档
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 已关联的媒体文件显示 -->
        <el-form-item v-if="editForm.mediaFiles && editForm.mediaFiles.length > 0" label="已上传文件">
          <div class="existing-media">
            <div
              v-for="media in editForm.mediaFiles"
              :key="media.mediaId"
              class="media-item-small"
            >
              <img
                v-if="media.fileType.startsWith('image/')"
                :src="media.fileUrl"
                :alt="media.fileName"
                @click="previewMedia(media)"
              />
              <div v-else class="file-icon-small" @click="previewMedia(media)">
                <i class="fas fa-file"></i>
              </div>
              <el-button
                size="small"
                type="danger"
                @click="removeMediaFromEdit(media)"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm" :loading="submitting">
          更新
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加宠物对话框 -->
    <el-dialog
      v-model="showAddPetDialog"
      title="添加宠物"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="petFormRef"
        :model="petForm"
        :rules="petFormRules"
        label-width="100px"
      >
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
          <el-date-picker
            v-model="petForm.birthday"
            type="date"
            placeholder="选择宠物生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddPetDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPetForm" :loading="submittingPet">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 创建新活动对话框 -->
    <el-dialog
      v-model="showCreateActivityDialog"
      title="创建新活动"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="newActivityFormRef"
        :model="newActivityForm"
        :rules="newActivityFormRules"
        label-width="100px"
      >
        <el-form-item label="活动名称" prop="activityName">
          <el-input
            v-model="newActivityForm.activityName"
            placeholder="请输入活动名称，如：吃零食、散步、洗澡等"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="活动类别">
          <el-input
            :value="getActivityKindName(addForm.activityKindId)"
            disabled
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateActivityDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitCreateActivity"
          :loading="submittingNewActivity"
        >
          创建活动
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加状态记录对话框 -->
    <el-dialog
      v-model="showAddStatusDialog"
      title="添加状态记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="addStatusFormRef"
        :model="addStatusForm"
        :rules="addStatusFormRules"
        label-width="100px"
      >
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="addStatusForm.petId" placeholder="请选择宠物" style="width: 100%">
            <el-option
              v-for="pet in userPets"
              :key="pet.id || pet.petId"
              :label="pet.name"
              :value="pet.id || pet.petId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态类型" prop="statusId">
          <el-select v-model="addStatusForm.statusId" placeholder="选择状态类型" style="width: 100%">
            <el-option
              v-for="status in userStatuses"
              :key="status.statusId"
              :label="status.statusName"
              :value="status.statusId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="addStatusForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="状态描述" prop="description">
          <el-input
            v-model="addStatusForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入状态描述..."
          />
        </el-form-item>

        <el-form-item label="上传文件">
          <el-upload
            ref="statusUploadRef"
            :auto-upload="false"
            :on-change="handleStatusFileChange"
            :limit="1"
            :file-list="statusFileList"
            action="#"
            :accept="'image/*,.pdf,.doc,.docx'"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、PDF、Word文档，文件大小不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddStatusDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddStatusForm" :loading="submittingStatus">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑状态记录对话框 -->
    <el-dialog
      v-model="showEditStatusDialog"
      title="编辑状态记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editStatusFormRef"
        :model="editStatusForm"
        :rules="editStatusFormRules"
        label-width="100px"
      >
        <el-form-item label="宠物名称" prop="petId">
          <el-input v-model="editStatusForm.petName" disabled placeholder="宠物名称" />
        </el-form-item>

        <el-form-item label="状态类型" prop="statusId">
          <el-select v-model="editStatusForm.statusId" placeholder="选择状态类型" style="width: 100%">
            <el-option
              v-for="status in userStatuses"
              :key="status.statusId"
              :label="status.statusName"
              :value="status.statusId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="editStatusForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="editStatusForm.endDate"
            type="date"
            placeholder="选择结束日期（可选）"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="状态描述" prop="description">
          <el-input
            v-model="editStatusForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入状态描述..."
          />
        </el-form-item>

        <el-form-item label="更新文件">
          <el-upload
            ref="editStatusUploadRef"
            :auto-upload="false"
            :on-change="handleEditStatusFileChange"
            :limit="1"
            :file-list="editStatusFileList"
            action="#"
            :accept="'image/*,.pdf,.doc,.docx'"
          >
            <el-button>选择新文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                上传新文件将替换现有文件，支持图片、PDF、Word文档
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditStatusDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditStatusForm" :loading="submittingStatus">
          更新
        </el-button>
      </template>
    </el-dialog>

    <!-- AI状态总结对话框 -->
    <el-dialog
      v-model="showAISummaryDialog"
      :title="`✨ ${currentPetName}的AI状态分析`"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-loading="aiSummaryLoading" element-loading-text="AI正在分析中...">
        <div v-if="aiSummaryContent" class="ai-summary-content">
          <div class="ai-summary-header">
            <el-icon class="ai-icon"><MagicStick /></el-icon>
            <span>AI智能分析报告</span>
          </div>
          <div class="ai-summary-text" v-html="formattedAISummary"></div>
        </div>
        <div v-else-if="!aiSummaryLoading" class="ai-empty">
          <el-empty description="暂无分析数据" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showAISummaryDialog = false">关闭</el-button>
        <el-button type="primary" @click="copyAISummary" v-if="aiSummaryContent">
          复制报告
        </el-button>
      </template>
    </el-dialog>

    <!-- 活动统计对话框 -->
    <el-dialog
      v-model="showStatsDialog"
      :title="`📊 ${currentStatsPetName}的活动统计`"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-loading="statsLoading" element-loading-text="加载统计数据中...">
        <div v-if="statsData" class="stats-content">
          <!-- 统计周期切换 -->
          <div class="stats-period-selector">
            <el-radio-group v-model="statsPeriod" @change="handleStatsPeriodChange">
              <el-radio-button label="MONTHLY">月度统计</el-radio-button>
              <el-radio-button label="WEEKLY">周度统计</el-radio-button>
            </el-radio-group>
          </div>

          <!-- 总览统计 -->
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

          <!-- 最频繁活动 -->
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

          <!-- 月度/周度详细统计 -->
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
        <div v-else-if="!statsLoading" class="stats-empty">
          <el-empty description="暂无统计数据" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showStatsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 使用统一的布局底部 -->
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
import {
  Plus,
  Refresh,
  MagicStick
} from '@element-plus/icons-vue'
import * as statusApi from '@/api/status'
import { getPetStatusSummary } from '@/api/llm'

const authStore = useAuthStore()

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
const selectedPetIds = ref([])
const selectedActivityTypes = ref([1, 2, 3, 4, 5, 6, 7, 8, 9]) // 默认选择所有活动类型
const dateRange = ref([])

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
const addForm = ref({
  petId: null,
  activityKindId: null,
  activityId: null,
  activityDate: '',
  description: ''
})

const editForm = ref({
  activityRecordId: null,
  petId: null,
  petName: '',
  activityId: null,
  activityDate: '',
  description: '',
  mediaFiles: []
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

const selectedPetsCount = computed(() => selectedPetIds.value.length)

const filteredRecords = computed(() => {
  let filtered = activityRecords.value

  // 按选择的宠物过滤
  if (selectedPetIds.value.length > 0) {
    filtered = filtered.filter(record => selectedPetIds.value.includes(record.petId))
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
    console.log('筛选前记录数量:', filtered.length)
    console.log('选择的活动类型:', selectedActivityTypes.value)

    filtered = filtered.filter(record => {
      // 直接使用活动记录中的activityKindId
      const shouldInclude = selectedActivityTypes.value.includes(record.activityKindId)

      console.log('记录筛选结果:', {
        recordId: record.activityRecordId,
        recordActivityKindId: record.activityKindId,
        recordActivityName: record.activityName,
        shouldInclude,
        selectedTypes: selectedActivityTypes.value
      })

      return shouldInclude
    })

    console.log('筛选后记录数量:', filtered.length)
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
  if (selectedPetIds.value.length > 0) {
    filtered = filtered.filter(record => selectedPetIds.value.includes(record.petId))
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

    console.log('创建新活动:', activityData)

    // 调用API创建活动
    const newActivity = await createActivity(activityData)

    ElMessage.success('活动创建成功！')

    // 将新活动添加到用户活动列表
    if (newActivity) {
      userActivities.value.push(newActivity)
    }

    // 关闭对话框并重置表单
    showCreateActivityDialog.value = false
    newActivityForm.value = {
      activityName: ''
    }

    // 自动选择新创建的活动
    if (newActivity && newActivity.activityId) {
      addForm.value.activityId = newActivity.activityId
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

    console.log('加载到的宠物数据:', userPets.value)

    // 默认选中所有宠物
    console.log('loadUserPets: 检查是否需要设置默认选中的宠物')
    console.log('loadUserPets: selectedPetIds.value.length:', selectedPetIds.value.length)
    console.log('loadUserPets: userPets.value.length:', userPets.value.length)

    if (selectedPetIds.value.length === 0 && userPets.value.length > 0) {
      const petIds = userPets.value.map(pet => pet.petId || pet.id)
      console.log('loadUserPets: 设置默认选中的宠物ID:', petIds)
      selectedPetIds.value = petIds
      console.log('loadUserPets: 设置后的selectedPetIds.value:', selectedPetIds.value)
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

    console.log('加载到的用户活动:', userActivities.value)
  } catch (error) {
    console.error('加载用户活动失败:', error)
    userActivities.value = []
  }
}

const loadActivityRecords = async () => {
  try {
    loading.value = true

    console.log('loadActivityRecords: 开始加载活动记录')
    console.log('loadActivityRecords: 选择的宠物ID:', selectedPetIds.value)
    console.log('loadActivityRecords: 日期范围:', dateRange.value)

    // 获取所有选中宠物的活动记录
    if (selectedPetIds.value.length === 0) {
      console.log('loadActivityRecords: 没有选择宠物，清空活动记录')
      activityRecords.value = []
      return
    }

    const { getActivityRecordsByPetIds } = await import('@/api/activities')
    const { getRelatedMedia } = await import('@/api/media')

    // 批量获取活动记录
    console.log('loadActivityRecords: 开始调用API获取活动记录')
    const recordsResponse = await getActivityRecordsByPetIds(selectedPetIds.value, {
      startDate: dateRange.value[0] ? new Date(dateRange.value[0]).toISOString() : null,
      endDate: dateRange.value[1] ? new Date(dateRange.value[1]).toISOString() : null
    })

    console.log('loadActivityRecords: API原始响应:', recordsResponse)

    // 处理API响应格式
    let records = []
    if (recordsResponse && recordsResponse.data) {
      records = Array.isArray(recordsResponse.data) ? recordsResponse.data : []
    } else if (Array.isArray(recordsResponse)) {
      records = recordsResponse
    }

    // 为每个活动记录获取关联的媒体文件
    console.log('loadActivityRecords: 开始获取媒体文件信息')
    const recordsWithMedia = await Promise.all(
      records.map(async (record) => {
        try {
          const mediaResponse = await getRelatedMedia('activity', record.activityRecordId || record.id)
          const mediaFiles = (mediaResponse && mediaResponse.data) ? mediaResponse.data : []
          console.log(`活动记录 ${record.activityRecordId || record.id} 的媒体文件:`, mediaFiles)
          return {
            ...record,
            mediaFiles: mediaFiles,
            mediaCount: mediaFiles.length,
            firstMediaUrl: mediaFiles.length > 0 ? mediaFiles[0].fileUrl : null
          }
        } catch (mediaError) {
          console.error(`获取活动记录 ${record.activityRecordId || record.id} 的媒体文件失败:`, mediaError)
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
    console.log('loadActivityRecords: 处理后的活动记录（含媒体）:', activityRecords.value)
    console.log('loadActivityRecords: 活动记录数量:', activityRecords.value.length)
  } catch (error) {
    console.error('加载活动记录失败:', error)
    activityRecords.value = []
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  console.log('refreshData: 开始刷新数据')

  // 1. 首先加载宠物数据，这样才能设置selectedPetIds
  await loadUserPets()
  console.log('refreshData: 宠物数据加载完成，selectedPetIds:', selectedPetIds.value)

  // 2. 加载用户活动数据
  await loadUserActivities()
  console.log('refreshData: 用户活动数据加载完成')

  // 3. 然后加载活动记录（依赖selectedPetIds）
  await loadActivityRecords()
  console.log('refreshData: 活动记录加载完成')

  // 4. 最后加载宠物活动统计数据
  await loadPetActivityStats()
  console.log('refreshData: 宠物活动统计数据加载完成')
}

const handlePetSelectionChange = () => {
  loadActivityRecords()
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

    // 为每个选中的宠物创建活动记录
    const createPromises = []
    const selectedPets = userPets.value.filter(pet =>
      selectedPetIds.value.includes(pet.petId || pet.id)
    )

    // 判断使用哪种创建方式
    const availableActivities = getActivitiesByKind(addForm.value.activityKindId)
    const useDirectKindMode = availableActivities.length === 0 || !addForm.value.activityId

    for (const pet of selectedPets) {
      const petId = pet.petId || pet.id

      // 格式化日期为API要求的格式 yyyy-MM-dd'T'HH:mm:ss
      const activityDate = new Date(addForm.value.activityDate)
      const formattedDate = activityDate.toISOString().slice(0, 19) // 保留 'T'

      let recordData

      if (useDirectKindMode) {
        // 直接使用活动种类ID创建记录（新的API方式）
        recordData = {
          activityKindId: addForm.value.activityKindId,
          description: addForm.value.description,
          date: formattedDate,
          userId: Number(currentUserId.value) // 新增：添加userId
        }
        console.log(`为宠物 ${pet.name} (ID: ${petId}) 使用活动种类创建记录:`, recordData)
        const result = await createActivityRecordByKind(petId, recordData)

        // 如果有媒体文件，上传媒体
        if (activityFileList.value.length > 0 && result && result.activityRecordId) {
          for (const fileItem of activityFileList.value) {
            if (fileItem.raw) {
              try {
                await uploadMedia(fileItem.raw, currentUserId.value, 'activity', result.activityRecordId)
                console.log('媒体上传成功:', fileItem.name)
              } catch (uploadError) {
                console.error('媒体上传失败:', uploadError)
                ElMessage.warning(`文件 ${fileItem.name} 上传失败，但活动记录已创建`)
              }
            }
          }
        }
      } else {
        // 使用具体活动ID创建记录（原有方式）
        recordData = {
          activityId: addForm.value.activityId,
          description: addForm.value.description,
          date: formattedDate,
          userId: Number(currentUserId.value) // 新增：添加userId
        }
        console.log(`为宠物 ${pet.name} (ID: ${petId}) 使用具体活动创建记录:`, recordData)
        const result = await createActivityRecord(petId, recordData)

        // 如果有媒体文件，上传媒体
        if (activityFileList.value.length > 0 && result && result.activityRecordId) {
          for (const fileItem of activityFileList.value) {
            if (fileItem.raw) {
              try {
                await uploadMedia(fileItem.raw, currentUserId.value, 'activity', result.activityRecordId)
                console.log('媒体上传成功:', fileItem.name)
              } catch (uploadError) {
                console.error('媒体上传失败:', uploadError)
                ElMessage.warning(`文件 ${fileItem.name} 上传失败，但活动记录已创建`)
              }
            }
          }
        }
      }
    }

    ElMessage.success(`成功为 ${selectedPets.length} 只宠物添加活动记录！`)
    showAddDialog.value = false

    // 重置表单
    addForm.value = {
      petId: null,
      activityKindId: null,
      activityId: null,
      activityDate: '',
      description: ''
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
    console.log('editRecord: 编辑活动记录:', record)

    // 活动记录中已经包含了activityKindId，直接使用
    const activityKindId = record.activityKindId

    // 找到宠物名称
    const pet = getPetInfo(record.petId)

    // 填充编辑表单
    editForm.value = {
      activityRecordId: record.activityRecordId || record.id,
      petId: record.petId,
      petName: pet.name || '未知宠物',
      activityId: activityKindId, // 直接使用活动记录中的activityKindId
      activityDate: record.activityDate ? new Date(record.activityDate).toISOString().slice(0, 19).replace('T', ' ') : '',
      description: record.activityDescription || record.description || '',
      mediaFiles: record.mediaFiles || [] // 初始化媒体文件数组
    }

    editActivityFileList.value = []
    console.log('editRecord: 填充的编辑表单数据:', editForm.value)
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
    console.log('submitEditForm: 选择的activityKindId:', selectedActivityKindId)

    // 根据活动种类ID找到对应的实际活动ID
    const matchingActivity = userActivities.value.find(activity =>
      activity.activityKindId === selectedActivityKindId
    )

    console.log('submitEditForm: 找到的匹配活动:', matchingActivity)

    // 格式化日期为API要求的格式 yyyy-MM-dd'T'HH:mm:ss
    const activityDate = new Date(editForm.value.activityDate)
    const formattedDate = activityDate.toISOString().slice(0, 19) // 保留 'T'

    // 导入API并更新记录
    const { updateActivityRecord } = await import('@/api/activities')
    const { uploadMedia } = await import('@/api/media')

    // 确保description字段不为空（API要求必需字段）
    const description = editForm.value.description || '无描述'

    console.log('submitEditForm: 使用的description:', description)

    if (matchingActivity) {
      // 找到了具体的活动，使用现有的API
      console.log('submitEditForm: 使用具体活动ID更新:', matchingActivity.activityId)
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
const togglePetSelection = (petId) => {
  const index = selectedPetIds.value.indexOf(petId)
  if (index > -1) {
    selectedPetIds.value.splice(index, 1)
  } else {
    selectedPetIds.value.push(petId)
  }
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
      selectedPetIds.value.push(newPet.petId)
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
  if (!currentUserId.value) return

  try {
    const response = await statusApi.getUserStatuses(currentUserId.value)
    if (response && response.data) {
      userStatuses.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userStatuses.value = response
    } else {
      userStatuses.value = []
    }
    console.log('加载到的用户状态:', userStatuses.value)
  } catch (error) {
    console.error('加载用户状态失败:', error)
    userStatuses.value = []
  }
}

const loadStatusRecords = async () => {
  if (!currentUserId.value || selectedPetIds.value.length === 0) {
    statusRecords.value = []
    return
  }

  try {
    statusLoading.value = true
    const promises = selectedPetIds.value.map(petId =>
      statusApi.getStatusRecords(petId, {
        startDate: statusDateRange.value[0],
        endDate: statusDateRange.value[1]
      })
    )

    const responses = await Promise.all(promises)
    const allRecords = responses.flatMap(response => {
      if (response && response.data) {
        return Array.isArray(response.data) ? response.data : []
      }
      return Array.isArray(response) ? response : []
    })

    statusRecords.value = allRecords
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
const removeMediaFromEdit = (mediaId) => {
  const index = editForm.mediaFiles.findIndex(m => m.mediaId === mediaId)
  if (index > -1) {
    editForm.mediaFiles.splice(index, 1)
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
  console.log(`开始获取宠物 ${petName} (ID: ${petId}) 的AI状态总结`)

  aiSummaryLoading.value = true
  currentAnalyzingPetId.value = petId
  currentPetName.value = petName
  showAISummaryDialog.value = true
  aiSummaryContent.value = ''

  try {
    const result = await getPetStatusSummary(petId)
    console.log('AI状态总结返回:', result)

    // GraphQL 返回的数据结构: { petId, name, breed, species, healthAdvice, statusRecords }
    if (result && result.healthAdvice) {
      // 使用 GraphQL 返回的 healthAdvice 作为主要内容
      aiSummaryContent.value = result.healthAdvice
      ElMessage.success('AI分析完成！')
    } else if (result && result.summary) {
      aiSummaryContent.value = result.summary
      ElMessage.success('AI分析完成！')
    } else if (typeof result === 'string') {
      aiSummaryContent.value = result
      ElMessage.success('AI分析完成！')
    } else if (result && result.data && result.data.summary) {
      aiSummaryContent.value = result.data.summary
      ElMessage.success('AI分析完成！')
    } else {
      aiSummaryContent.value = '暂无分析数据，请确保该宠物有足够的活动记录'
      ElMessage.warning('AI分析数据不足')
    }
  } catch (error) {
    console.error('获取AI状态总结失败:', error)

    // 确保对话框在错误时也能正常显示
    let errorMsg = '分析失败，请稍后重试'
    if (error.response) {
      errorMsg = `服务错误: ${error.response.status}`
    } else if (error.message) {
      errorMsg = `网络错误: ${error.message}`
    }

    aiSummaryContent.value = `❌ ${errorMsg}\n\n请检查：\n1. 网关是否正常运行（端口9000）\n2. LLM服务是否启动并注册到Nacos\n3. 网络连接是否正常`
    ElMessage.error('获取AI分析失败')
  } finally {
    // 确保无论成功或失败都关闭loading
    aiSummaryLoading.value = false
    currentAnalyzingPetId.value = null
  }
}

const copyAISummary = () => {
  navigator.clipboard.writeText(aiSummaryContent.value).then(() => {
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
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* ===== Hero Section ===== */
.hero-section {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.1) 0%, rgba(250, 204, 21, 0.1) 100%),
              url('https://images.unsplash.com/photo-1450778869188-b1d976e7e5fa?q=80&w=1332&auto=format&fit=crop') center/cover no-repeat;
  padding: 2rem 0;
  position: relative;
  display: flex;
  align-items: center;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.2) 0%, rgba(250, 204, 21, 0.2) 100%);
  z-index: 1;
}

.hero-container {
  width: 100%;
  margin: 0;
  padding: 0 2rem;
  text-align: center;
  position: relative;
  z-index: 2;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-emoji {
  font-size: 3rem;
  margin-bottom: 1rem;
  line-height: 1;
}

.hero-title {
  font-size: 2.25rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.75rem;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: 1.125rem;
  color: #64748b;
  margin-bottom: 0;
  line-height: 1.6;
}

.page-header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  padding: 2rem 0;
}

.header-content {
  text-align: center;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.title-icon {
  font-size: 2.5rem;
}

.page-subtitle {
  color: #64748b;
  font-size: 1.125rem;
}

.main-content {
  padding: 2rem 0;
}

.container {
  width: 100%;
  margin: 0;
  padding: 0 0.5rem;
}

/* 内容区域专用容器 */
.content-container {
  width: 100%;
  margin: 0;
  padding: 0;
}

/* 主内容布局 */
.content-layout {
  display: flex;
  align-items: flex-start;
  width: 100%;
  margin: 0;
  padding: 0;
}

/* 左侧宠物边栏 */
.pets-sidebar {
  position: fixed;
  top: 120px;
  left: calc(2rem + 150px);
  width: 220px;
  max-height: calc(100vh - 140px);
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 0.5rem;
  overflow: hidden;
  z-index: 1000;
}

.sidebar-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.sidebar-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.title-icon {
  font-size: 1.25rem;
}

.title-text {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

.pets-count-badge {
  background: #3b82f6;
  color: white;
  font-size: 0.75rem;
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
  font-weight: 500;
}

.sidebar-content {
  padding: 0.75rem;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.pets-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* 主内容区域 */
.main-content-area {
  flex: 1;
  min-width: 0;
  position: relative;
  margin: 0 auto;
  margin-left: calc(2rem + 400px);
  margin-right: calc(2rem + 140px);
  max-width: 1300px;
  width: 100%;
}

/* 在小屏幕上调整布局 */
@media (max-width: 1400px) {
  .main-content-area {
    margin-left: calc(2rem + 340px);
    margin-right: calc(2rem + 140px);
  }

  .record-type-cards {
    right: calc(2rem + 100px);
  }
}


.pet-card {
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.375rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  position: relative;
}

.pet-card:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
  transform: translateY(-1px);
}

.pet-card.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

/* 宠物卡片主体布局 */
.pet-main {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.pet-content {
  flex: 1;
  min-width: 0;
}

.pet-info-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.pet-avatar {
  position: relative;
  flex-shrink: 0;
}

.pet-status-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: #10b981;
  border: 2px solid white;
  border-radius: 50%;
}

.pet-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pet-details {
  font-size: 0.75rem;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.pet-activities {
  text-align: center;
}

.pet-activities {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.activity-count {
  font-size: 0.875rem;
  font-weight: 600;
  color: #3b82f6;
}

.activity-label {
  font-size: 0.75rem;
  color: #64748b;
}

/* 宠物操作按钮区域 */
.pet-actions {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-left: auto;
  flex-shrink: 0;
}

.pet-ai-action .el-button,
.pet-stats-action .el-button {
  height: 24px;
  font-size: 0.75rem;
  padding: 0 6px;
  min-width: 60px;
}

.add-pet-card {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border: 2px dashed #cbd5e1;
  justify-content: center;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.5rem 0.375rem;
}

.add-pet-card:hover {
  border-color: #cbd5e1;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.add-pet-icon {
  color: #64748b;
  margin: 0 auto;
}

.add-pet-text {
  font-size: 0.75rem;
  font-weight: 500;
  color: #64748b;
  text-align: center;
}

/* 操作栏 */
.action-bar {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.action-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.action-right {
  display: flex;
  gap: 0.5rem;
}

/* 活动类型筛选 */
.activity-filters {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-header {
  margin-bottom: 12px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.filter-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

/* 活动类型筛选标签样式 */
.filter-1 { background-color: #fef3c7; color: #92400e; } /* 喂养 */
.filter-2 { background-color: #dbeafe; color: #1e40af; } /* 互动 */
.filter-3 { background-color: #ede9fe; color: #5b21b6; } /* 清洁 */
.filter-4 { background-color: #fef3c7; color: #b45309; } /* 外出 */
.filter-5 { background-color: #fee2e2; color: #dc2626; } /* 运动 */
.filter-6 { background-color: #e0e7ff; color: #4f46e5; } /* 医疗 */
.filter-7 { background-color: #fce7f3; color: #a21caf; } /* 生育 */
.filter-8 { background-color: #fee2e2; color: #dc2626; } /* 异常 */
.filter-9 { background-color: #f3f4f6; color: #6b7280; } /* 其他 */
.filter-default { background-color: #f3f4f6; color: #6b7280; } /* 默认 */

/* 时间线 */
.timeline-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading-container {
  padding: 2rem;
}

.empty-state {
  padding: 3rem;
}

.activity-timeline {
  padding: 2rem;
}

.timeline-group {
  margin-bottom: 2rem;
}

.timeline-group:last-child {
  margin-bottom: 0;
}

.timeline-date {
  margin-bottom: 1rem;
}

.date-badge {
  display: inline-block;
  background: #f1f5f9;
  color: #475569;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.875rem;
}

.timeline-item {
  display: flex;
  margin-bottom: 1.5rem;
  position: relative;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 1rem;
}

.marker-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
}

.marker-dot.diet {
  background: #10b981;
}

.marker-dot.exercise {
  background: #3b82f6;
}

.marker-dot.hygiene {
  background: #8b5cf6;
}

.marker-dot.play {
  background: #f59e0b;
}

.marker-dot.training {
  background: #ef4444;
}

.marker-dot.medical {
  background: #6366f1;
}

.marker-dot.breeding {
  background: #ec4899;
}

.marker-dot.alert {
  background: #dc2626;
}

.marker-dot.other {
  background: #6b7280;
}

.marker-line {
  width: 2px;
  height: 100%;
  background: #e2e8f0;
  margin-top: 0.5rem;
}

.timeline-content {
  flex: 1;
}

.record-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.record-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pet-name {
  font-weight: 600;
  color: #1e293b;
}

.activity-type {
  font-size: 0.875rem;
  color: #64748b;
}

.record-time {
  font-size: 0.875rem;
  color: #64748b;
}

.record-description {
  color: #475569;
  margin-bottom: 1rem;
  line-height: 1.5;
}

.record-actions {
  display: flex;
  gap: 0.5rem;
}

/* 响应式设计 - 宠物边栏 */
@media (max-width: 768px) {
  .pets-sidebar {
    position: static;
    top: auto;
    left: auto;
    width: 100%;
    max-height: none;
    margin-bottom: 1.5rem;
  }

  .sidebar-content {
    max-height: none;
    overflow-y: visible;
  }

  .main-content-area {
    margin-left: 0;
    margin-right: 0;
    width: 100%;
    max-width: none;
  }

  .record-type-cards {
    right: 1rem;
  }

  .container {
    padding: 0 1rem;
  }
  .hero-emoji {
    font-size: 2.5rem;
  }

  .hero-title {
    font-size: 2rem;
  }

  .page-title {
    font-size: 2rem;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-left {
    justify-content: center;
  }

  .action-right {
    justify-content: center;
  }

  .record-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .timeline-item {
    flex-direction: column;
  }

  .timeline-marker {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }

  .marker-line {
    display: none;
  }
}

/* ===== 记录类型切换卡片 ===== */
.record-switch-section {
  padding: 0.25rem 0;
  min-height: 100px; /* 为固定定位的卡片留出空间 */
  width: 100%;
}

.record-type-cards {
  position: fixed;
  top: 120px;
  right: calc(2rem + 100px);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  z-index: 1000;
}

  .record-type-card {
    background: white;
    border: 2px solid #e2e8f0;
    border-radius: 12px;
    padding: 1rem;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    width: 200px;
  }

.record-type-card:hover {
  border-color: #cbd5e1;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.record-type-card.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  transform: translateY(-2px);
}

.record-type-card.active .card-indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #3b82f6 0%, #1d4ed8 100%);
}

  .card-icon {
    font-size: 1.25rem;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    background: #f8fafc;
    color: #64748b;
    flex-shrink: 0;
  }

.record-type-card.active .card-icon {
  background: #3b82f6;
  color: white;
}

.card-content {
  flex: 1;
}

  .card-content h3 {
    margin: 0 0 0.25rem 0;
    font-size: 1rem;
    font-weight: 600;
    color: #1e293b;
  }

  .card-content p {
    margin: 0 0 0.5rem 0;
    color: #64748b;
    font-size: 0.75rem;
    line-height: 1.3;
  }

  .card-count {
    font-size: 1.125rem;
    font-weight: 600;
    color: #3b82f6;
  }

/* ===== 状态记录样式 ===== */
.status-content {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.status-action-bar {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.status-timeline-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.status-item {
  border-left: 3px solid #10b981;
}

.status-dot {
  background: #10b981;
  width: 14px;
  height: 14px;
}

.status-record-card {
  border-left: 3px solid #10b981;
}

.record-status-info {
  text-align: right;
}

.record-status-info .record-time {
  font-size: 0.875rem;
  color: #64748b;
  margin-bottom: 0.25rem;
}

.status-duration {
  font-size: 0.75rem;
  color: #6b7280;
  font-style: italic;
}

.status-active {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #10b981;
  font-weight: 600;
}

.active-indicator {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.record-media {
  margin-bottom: 1rem;
}

.media-preview {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.media-item {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.media-item:hover {
  border-color: #3b82f6;
  transform: scale(1.05);
}

.media-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  color: #64748b;
  font-size: 1.2rem;
}

.more-media {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

/* ===== 响应式设计 - 记录类型切换卡片 ===== */
@media (max-width: 768px) {
  .record-type-cards {
    position: static;
    top: auto;
    right: auto;
    flex-direction: row;
    justify-content: center;
    gap: 0.5rem;
    margin-bottom: 1rem;
    padding: 0 0.5rem;
  }

  .record-type-card {
    width: 160px;
    padding: 0.75rem;
  }

  .card-icon {
    font-size: 1.125rem;
    width: 28px;
    height: 28px;
  }

  .card-content h3 {
    font-size: 0.875rem;
  }

  .card-count {
    font-size: 0.95rem;
  }
}

/* 活动记录媒体文件样式 */
.media-item-small {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
  position: relative;
  display: inline-block;
  margin-right: 8px;
  margin-bottom: 8px;
}

.media-item-small:hover {
  border-color: #3b82f6;
  transform: scale(1.05);
}

.media-item-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon-small {
  width: 100%;
  height: 100%;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 1rem;
}

.file-icon-small:hover {
  background: #e2e8f0;
  color: #3b82f6;
}

.existing-media {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.existing-media .media-item-small {
  position: relative;
}

.existing-media .el-button {
  position: absolute;
  top: -6px;
  right: -6px;
  padding: 2px 6px;
  font-size: 12px;
  border-radius: 10px;
  z-index: 10;
}

/* ===== AI状态总结样式 ===== */
.pet-ai-action,
.pet-stats-action {
  margin-top: 0.5rem;
}

.pet-ai-action,
.pet-stats-action {
  width: 100%;
}

.pet-ai-action .el-button,
.pet-stats-action .el-button {
  width: 100%;
}

.ai-summary-content {
  padding: 1rem;
}

.ai-summary-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #e4e7ed;
}

.ai-icon {
  font-size: 1.5rem;
}

.ai-summary-text {
  line-height: 1.8;
  color: #333;
  font-size: 0.95rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.ai-summary-text :deep(strong) {
  color: #409eff;
  font-weight: 600;
}

.ai-empty {
  padding: 2rem;
  text-align: center;
}

/* ===== 活动统计弹窗样式 ===== */
.stats-content {
  padding: 0.5rem 0;
}

.stats-period-selector {
  margin-bottom: 1.5rem;
  text-align: center;
}

.stats-overview {
  display: flex;
  justify-content: space-around;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: 8px;
}

.stat-card {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #f97316;
  line-height: 1.2;
}

.stat-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.stats-frequent {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
}

.frequent-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0;
}

.frequent-label {
  color: #64748b;
  font-size: 0.9rem;
  min-width: 80px;
}

.frequent-value {
  color: #f97316;
  font-weight: 600;
  flex: 1;
}

.frequent-count {
  color: #94a3b8;
  font-size: 0.85rem;
  background: #e2e8f0;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

.stats-detail {
  margin-top: 1rem;
}

.stats-detail h4 {
  color: #334155;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.stats-list {
  max-height: 400px;
  overflow-y: auto;
}

.stat-month-item,
.stat-week-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 0.75rem;
}

.month-header,
.week-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.month-name,
.week-name {
  font-weight: 600;
  color: #334155;
}

.month-total,
.week-total {
  color: #f97316;
  font-size: 0.9rem;
  font-weight: 600;
}

.month-activities,
.week-activities {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.activity-tag {
  background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
  color: #9a3412;
  padding: 0.3rem 0.6rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 500;
}

.stats-empty {
  padding: 3rem;
  text-align: center;
}
</style>