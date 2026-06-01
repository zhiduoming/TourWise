<template>
  <div class="graph-editor-page">
    <AppHeader />

    <section class="workspace">
      <aside class="side-panel">
        <header class="panel-header">
          <h2>路网标定</h2>
          <p>在平面图上拖点、加路口、连道路，保存后路线规划会直接使用这些数据。</p>
        </header>

        <el-form label-width="76px">
          <el-form-item label="景点">
            <el-select
              v-model="placeGroupId"
              placeholder="选择内部景点"
              filterable
              style="width: 100%"
              @change="handlePlaceGroupChange"
            >
              <el-option
                v-for="scope in routeScopes"
                :key="scope.placeGroupId"
                :label="scope.name"
                :value="scope.placeGroupId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="工具">
            <el-segmented v-model="tool" :options="toolOptions" />
          </el-form-item>

          <el-form-item v-if="tool === 'add-route'" label="节点名">
            <el-input v-model="newRouteNodeName" placeholder="例如 鸿雁路口" />
          </el-form-item>

          <template v-if="tool === 'add-poi'">
            <el-form-item label="POI名">
              <el-input v-model="newPoiForm.name" placeholder="例如 学生服务中心" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="newPoiForm.categoryCode" style="width: 100%" @change="handlePoiCategoryChange">
                <el-option
                  v-for="option in poiCategoryOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="区域">
              <el-input v-model="newPoiForm.areaName" placeholder="例如 生活服务" />
            </el-form-item>
          </template>

          <el-form-item v-if="tool === 'connect'" label="路段名">
            <el-input v-model="edgeDescription" placeholder="例如 沿鸿雁路向东" />
          </el-form-item>
        </el-form>

        <div class="hint-list">
          <div><strong>拖动</strong>：移动已有 POI 或路线节点。</div>
          <div><strong>加路口</strong>：点击图上道路，新增隐藏路线节点。</div>
          <div><strong>加 POI</strong>：点击图上建筑或场所，实时补充用户可选 POI。</div>
          <div><strong>连线</strong>：依次点击两个点，创建双向道路。</div>
          <div><strong>删除</strong>：点击路线边、隐藏路口或误加 POI 删除。</div>
        </div>

        <div class="stats">
          <el-tag>POI {{ poiNodeCount }}</el-tag>
          <el-tag type="info">路口 {{ routeNodeCount }}</el-tag>
          <el-tag type="success">边 {{ normalizedEdges.length }}</el-tag>
          <el-tag v-if="hasUnsavedChanges" type="warning">有未保存改动</el-tag>
        </div>

        <section class="route-debugger">
          <div class="route-debugger-header">
            <div>
              <div class="route-debugger-title">路线试算</div>
              <div class="route-debugger-desc">用当前画布路网预览最短路径，不写入数据库。</div>
            </div>
            <el-button text type="primary" :disabled="!routeDebugResult" @click="clearRouteDebug">
              清除
            </el-button>
          </div>

          <el-form label-width="52px" class="route-debugger-form">
            <el-form-item label="起点">
              <el-select
                v-model="routeDebugForm.startClientId"
                placeholder="选择起点 POI"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="node in visiblePoiNodes"
                  :key="node.clientId"
                  :label="node.name"
                  :value="node.clientId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="终点">
              <el-select
                v-model="routeDebugForm.endClientId"
                placeholder="选择终点 POI"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="node in visiblePoiNodes"
                  :key="node.clientId"
                  :label="node.name"
                  :value="node.clientId"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <div class="route-debugger-actions">
            <el-button
              type="primary"
              :disabled="!canRunRouteDebug"
              @click="runRouteDebug"
            >
              试算路线
            </el-button>
            <el-button
              :disabled="!routeDebugForm.startClientId || !routeDebugForm.endClientId"
              @click="swapRouteDebugEndpoints"
            >
              交换
            </el-button>
          </div>

          <el-alert
            v-if="routeDebugResult?.status === 'unreachable'"
            class="route-debugger-alert"
            type="error"
            :closable="false"
            :title="routeDebugResult.message"
          />

          <div v-if="routeDebugResult?.status === 'ok'" class="route-debugger-result">
            <div class="route-debugger-metrics">
              <span>距离 {{ routeDebugResult.totalDistance }}m</span>
              <span>约 {{ routeDebugResult.totalDuration }} 分钟</span>
              <span>{{ routeDebugResult.nodeClientIds.length }} 个节点</span>
            </div>
            <ol class="route-debugger-path">
              <li v-for="node in routeDebugPathNodes" :key="node.clientId">
                {{ node.name }}
              </li>
            </ol>
          </div>
        </section>

        <section class="connectivity-panel">
          <div class="connectivity-header">
            <div>
              <div class="connectivity-title">连通性检查</div>
              <div class="connectivity-desc">根据当前画布判断哪些 POI 没接入主路网。</div>
            </div>
            <el-switch
              v-model="showConnectivityOverlay"
              active-text="显示"
              inactive-text="隐藏"
            />
          </div>

          <div class="connectivity-metrics">
            <el-tag type="success">主路网 POI {{ routeConnectivity.mainPoiCount }}/{{ visiblePoiNodes.length }}</el-tag>
            <el-tag :type="routeConnectivity.disconnectedPoiNodes.length ? 'danger' : 'info'">
              未接入 {{ routeConnectivity.disconnectedPoiNodes.length }}
            </el-tag>
            <el-tag :type="routeConnectivity.orphanRouteNodes.length ? 'warning' : 'info'">
              孤立路口 {{ routeConnectivity.orphanRouteNodes.length }}
            </el-tag>
            <el-tag type="info">连通块 {{ routeConnectivity.componentCount }}</el-tag>
          </div>

          <el-alert
            v-if="!routeConnectivity.problemNodes.length"
            type="success"
            :closable="false"
            title="当前可见 POI 已全部接入同一个主路网。"
          />
          <div v-else class="connectivity-list">
            <button
              v-for="item in routeConnectivity.problemNodes.slice(0, 8)"
              :key="item.clientId"
              class="connectivity-item"
              type="button"
              @click="locateConnectivityNode(item)"
            >
              <span class="connectivity-dot" :class="item.connectivityIssueType" />
              <span class="connectivity-name">{{ item.name }}</span>
              <span class="connectivity-reason">{{ connectivityIssueText(item.connectivityIssueType) }}</span>
            </button>
            <div v-if="routeConnectivity.problemNodes.length > 8" class="connectivity-more">
              还有 {{ routeConnectivity.problemNodes.length - 8 }} 个问题节点，建议先处理列表中的高风险节点。
            </div>
          </div>
        </section>

        <div class="actions">
          <el-button type="primary" :loading="saving" @click="openSaveDialog">
            保存路网
          </el-button>
          <el-button type="success" plain :loading="versionCreating" @click="createVersionSnapshot">
            创建快照
          </el-button>
          <el-button type="warning" plain :loading="qualityLoading" @click="openQualityDrawer">
            路网体检
          </el-button>
          <el-button plain @click="openVersionDrawer">
            版本管理
          </el-button>
          <el-button plain @click="exportGraphJson">
            导出 JSON
          </el-button>
          <el-button plain @click="triggerImport">
            导入 JSON
          </el-button>
          <el-button type="danger" plain @click="clearRoutes">
            清空路线
          </el-button>
          <el-button :disabled="!canUndo" @click="undoLastAction">
            撤销上一步
          </el-button>
          <el-button @click="reloadGraph">重新加载</el-button>
          <el-button type="success" plain @click="goRoutePlan">
            去测试路线
          </el-button>
        </div>

        <el-alert
          v-if="hasUnsavedChanges"
          class="dirty-alert"
          type="warning"
          :closable="false"
          title="当前画布有未保存改动，切换景点、恢复版本、导入或离开页面前请先保存。"
        />

        <input
          ref="importFileRef"
          class="hidden-file-input"
          type="file"
          accept="application/json,.json"
          @change="handleImportFile"
        />

        <el-alert
          v-if="connectStart"
          type="info"
          :closable="false"
          :title="`已选择起点：${connectStart.name}，再点一个终点完成连线`"
        />

        <div v-if="importPreviewActive" class="import-preview-banner">
          <div class="preview-title">正在预览导入路网</div>
          <div class="preview-text">
            紫色节点和橙色虚线来自导入文件，当前还没有写入数据库。
          </div>
          <div class="preview-actions">
            <el-button type="primary" size="small" :loading="importing" @click="confirmImportGraph">
              确认导入
            </el-button>
            <el-button size="small" @click="importPreviewVisible = true">
              查看预检
            </el-button>
            <el-button size="small" @click="clearImportGraphPreview">
              退出预览
            </el-button>
          </div>
        </div>

        <div v-if="versionPreviewActive" class="import-preview-banner version-preview-banner">
          <div class="preview-title">正在预览历史版本</div>
          <div class="preview-text">
            紫色节点和橙色虚线来自历史版本，当前还没有恢复到数据库。
          </div>
          <div class="preview-actions">
            <el-button
              type="primary"
              size="small"
              :loading="versionRestoring"
              @click="restoreVersion(versionPreviewSource)"
            >
              恢复此版本
            </el-button>
            <el-button size="small" @click="clearVersionGraphPreview">
              退出预览
            </el-button>
          </div>
        </div>
      </aside>

      <main ref="mapPanelRef" class="map-panel">
        <div v-if="!graphMap?.imageUrl" class="empty-map">
          请先在路线规划页上传该景点平面图
        </div>
        <svg
          v-else
          ref="svgRef"
          class="editor-svg"
          :viewBox="`0 0 ${mapWidth} ${mapHeight}`"
          @click="handleMapClick"
          @pointermove="handlePointerMove"
          @pointerup="stopDragging"
          @pointerleave="stopDragging"
        >
          <image
            :href="graphMap.imageUrl"
            x="0"
            y="0"
            :width="mapWidth"
            :height="mapHeight"
            preserveAspectRatio="none"
          />

          <g v-if="importPreviewActive" class="preview-edges">
            <line
              v-for="edge in normalizedImportPreviewEdges"
              :key="edge.key"
              :x1="edge.from.mapX"
              :y1="edge.from.mapY"
              :x2="edge.to.mapX"
              :y2="edge.to.mapY"
            />
          </g>

          <g v-if="importPreviewActive" class="preview-nodes">
            <g
              v-for="node in importPreviewNodes"
              :key="node.clientId"
              class="preview-node"
              :class="{ route: node.nodeType === 'route' }"
            >
              <circle
                :cx="node.mapX"
                :cy="node.mapY"
                :r="node.nodeType === 'route' ? 6 : 9"
              />
              <text :x="node.mapX + 10" :y="node.mapY + 18">
                {{ node.name }}
              </text>
            </g>
          </g>

          <g v-if="versionPreviewActive" class="preview-edges version-preview-edges">
            <line
              v-for="edge in normalizedVersionPreviewEdges"
              :key="edge.key"
              :x1="edge.from.mapX"
              :y1="edge.from.mapY"
              :x2="edge.to.mapX"
              :y2="edge.to.mapY"
            />
          </g>

          <g v-if="versionPreviewActive" class="preview-nodes">
            <g
              v-for="node in versionPreviewNodes"
              :key="node.clientId"
              class="preview-node version-preview-node"
              :class="{ route: node.nodeType === 'route' }"
            >
              <circle
                :cx="node.mapX"
                :cy="node.mapY"
                :r="node.nodeType === 'route' ? 6 : 9"
              />
              <text :x="node.mapX + 10" :y="node.mapY + 18">
                {{ node.name }}
              </text>
            </g>
          </g>

          <g v-if="diffFocusEdges.length" class="diff-focus-edges">
            <line
              v-for="edge in diffFocusEdges"
              :key="edge.key"
              :x1="edge.fromMapX"
              :y1="edge.fromMapY"
              :x2="edge.toMapX"
              :y2="edge.toMapY"
            />
          </g>

          <g v-if="diffFocusMarkers.length" class="diff-focus-markers">
            <g
              v-for="marker in diffFocusMarkers"
              :key="marker.key"
              class="diff-focus-marker"
              :class="marker.kind"
            >
              <circle :cx="marker.mapX" :cy="marker.mapY" r="13" />
              <text :x="marker.mapX + 15" :y="marker.mapY - 12">
                {{ marker.label }}
              </text>
            </g>
          </g>

          <g class="edges">
            <line
              v-for="edge in normalizedEdges"
              :key="edge.key"
              :x1="edge.from.mapX"
              :y1="edge.from.mapY"
              :x2="edge.to.mapX"
              :y2="edge.to.mapY"
              :class="{ selected: selectedEdgeKey === edge.key }"
              @click.stop="handleEdgeClick(edge)"
            />
          </g>

          <g v-if="routeDebugEdges.length" class="route-debug-edges">
            <line
              v-for="edge in routeDebugEdges"
              :key="edge.key"
              :x1="edge.from.mapX"
              :y1="edge.from.mapY"
              :x2="edge.to.mapX"
              :y2="edge.to.mapY"
            />
          </g>

          <g v-if="routeDebugPathNodes.length" class="route-debug-markers">
            <g
              v-for="(node, index) in routeDebugPathNodes"
              :key="node.clientId"
              class="route-debug-marker"
              :class="{ endpoint: index === 0 || index === routeDebugPathNodes.length - 1 }"
            >
              <circle :cx="node.mapX" :cy="node.mapY" :r="index === 0 || index === routeDebugPathNodes.length - 1 ? 12 : 8" />
              <text :x="node.mapX + 14" :y="node.mapY - 13">
                {{ index + 1 }}
              </text>
            </g>
          </g>

          <g v-if="showConnectivityOverlay && connectivityMarkers.length" class="connectivity-markers">
            <g
              v-for="marker in connectivityMarkers"
              :key="marker.clientId"
              class="connectivity-marker"
              :class="marker.connectivityIssueType"
            >
              <circle :cx="marker.mapX" :cy="marker.mapY" r="14" />
              <text :x="marker.mapX + 17" :y="marker.mapY + 6">
                {{ marker.name }}
              </text>
            </g>
          </g>

          <g class="nodes">
            <g
              v-for="node in nodes"
              :key="node.clientId"
              class="node"
              :class="{ route: node.nodeType === 'route', selected: selectedNode?.clientId === node.clientId }"
              @pointerdown.stop="startDragging(node, $event)"
              @click.stop="handleNodeClick(node)"
            >
              <circle
                :cx="node.mapX"
                :cy="node.mapY"
                :r="node.nodeType === 'route' ? 7 : 10"
              />
              <text
                v-if="node.nodeType !== 'route' || showRouteLabels"
                :x="node.mapX + 12"
                :y="node.mapY - 10"
              >
                {{ node.name }}
              </text>
            </g>
          </g>
        </svg>
      </main>
    </section>

    <el-drawer v-model="versionDrawerVisible" title="路网版本管理" size="760px">
      <div class="version-toolbar">
        <el-button type="primary" :loading="versionCreating" @click="createVersionSnapshot">
          创建当前快照
        </el-button>
        <el-button :loading="versionLoading" @click="loadVersions">刷新</el-button>
      </div>

      <div class="version-filter">
        <el-input
          v-model="versionFilter.keyword"
          clearable
          placeholder="搜索版本名称、备注、创建人"
        />
        <el-select
          v-model="versionFilter.creator"
          clearable
          filterable
          placeholder="创建人"
        >
          <el-option
            v-for="creator in versionCreators"
            :key="creator"
            :label="creator"
            :value="creator"
          />
        </el-select>
        <el-date-picker
          v-model="versionFilter.dateRange"
          type="daterange"
          unlink-panels
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button @click="resetVersionFilter">重置</el-button>
      </div>

      <div class="version-filter-summary">
        共 {{ versions.length }} 个版本，当前显示 {{ filteredVersions.length }} 个
      </div>

      <el-table v-loading="versionLoading" :data="filteredVersions" border empty-text="暂无匹配版本">
        <el-table-column type="expand" width="46">
          <template #default="{ row }">
            <div class="version-expanded">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="版本名称">
                  {{ row.name || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建人">
                  {{ row.createdByName || `用户 ${row.createdBy || '-'}` }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ formatDateTime(row.createdAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="路网规模">
                  {{ row.nodeCount }} 个节点 / {{ row.edgeCount }} 条路线边
                </el-descriptions-item>
                <el-descriptions-item label="修改说明" :span="2">
                  <span class="version-remark">
                    {{ row.remark || '暂无修改说明。建议后续保存路网时写清楚这次调整了哪些路口、路线边或 POI 坐标。' }}
                  </span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="versionNo" label="版本" width="80">
          <template #default="{ row }">v{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="名称 / 说明" min-width="220">
          <template #default="{ row }">
            <div class="version-name">{{ row.name || `路网版本 v${row.versionNo}` }}</div>
            <div class="version-brief">{{ row.remark || '暂无修改说明' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="规模" width="110">
          <template #default="{ row }">
            {{ row.nodeCount }} 点 / {{ row.edgeCount }} 边
          </template>
        </el-table-column>
        <el-table-column label="创建信息" width="170">
          <template #default="{ row }">
            <div class="version-meta">{{ row.createdByName || `用户 ${row.createdBy || '-'}` }}</div>
            <div class="version-time">{{ formatDateTime(row.createdAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openVersionDiff(row)">对比</el-button>
            <el-button type="success" link @click="previewVersion(row)">预览</el-button>
            <el-button type="primary" link @click="restoreVersion(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        class="version-tip"
        type="info"
        :closable="false"
        title="恢复版本前会自动创建一份当前路网备份，所以误恢复后还能回滚。"
      />
    </el-drawer>

    <el-drawer v-model="qualityDrawerVisible" title="路网数据体检" size="620px">
      <div class="quality-summary" v-if="graphQuality">
        <el-statistic title="业务 POI" :value="graphQuality.poiCount || 0" />
        <el-statistic title="隐藏路口" :value="graphQuality.routeNodeCount || 0" />
        <el-statistic title="路线边" :value="graphQuality.edgeCount || 0" />
        <el-statistic title="阻断问题" :value="graphQuality.blockingIssueCount || 0" />
      </div>

      <div class="quality-toolbar">
        <el-button type="primary" :loading="qualityLoading" @click="loadGraphQuality">
          重新体检
        </el-button>
        <el-button
          type="danger"
          plain
          :loading="qualityCleaning"
          :disabled="!hasAutoCleanableIssues"
          @click="cleanupQualityIssues"
        >
          一键清理异常边
        </el-button>
        <el-alert
          type="info"
          :closable="false"
          title="一键清理只处理无效边、跨景点边、自环边和重复边；长边、孤立 POI 仍需要你在图上人工确认。"
        />
      </div>

      <el-empty
        v-if="!qualityLoading && graphQuality && !graphQuality.issueCount"
        description="当前路网没有发现明显数据问题"
      />

      <el-table
        v-else
        v-loading="qualityLoading"
        :data="graphQuality?.issues || []"
        :row-class-name="qualityRowClassName"
        border
        empty-text="暂无体检结果"
        @row-click="locateQualityIssue"
      >
        <el-table-column label="级别" width="84">
          <template #default="{ row }">
            <el-tag :type="issueTagType(row.severity)" effect="dark">
              {{ row.severity === 'error' ? '阻断' : '提醒' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="问题" min-width="180">
          <template #default="{ row }">
            <div class="issue-title">{{ row.title }}</div>
            <div class="issue-desc">{{ row.description }}</div>
          </template>
        </el-table-column>
        <el-table-column label="对象" min-width="180">
          <template #default="{ row }">
            <span v-if="row.nodeName">{{ row.nodeName }}</span>
            <span v-else>{{ row.fromNodeName || row.fromNodeId || '未知节点' }} -> {{ row.toNodeName || row.toNodeId || '未知节点' }}</span>
            <span v-if="row.distanceM" class="issue-distance">（{{ row.distanceM }}m）</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="locateQualityIssue(row)">定位</el-button>
            <el-button
              v-if="canRepairQualityIssue(row)"
              type="warning"
              link
              @click.stop="repairQualityIssue(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="importPreviewVisible" title="导入路网预检" width="680px">
      <div v-if="importPreview" class="import-preview">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="当前景点">
            {{ currentScopeName }}
          </el-descriptions-item>
          <el-descriptions-item label="导入景点">
            {{ importPreview.placeGroupName || '旧版文件未记录' }}
          </el-descriptions-item>
          <el-descriptions-item label="格式版本">
            {{ importPreview.schemaVersion || '旧版 nodes/edges 格式' }}
          </el-descriptions-item>
          <el-descriptions-item label="导出时间">
            {{ importPreview.exportedAt || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="节点数量">
            {{ importPreview.poiCount }} 个 POI / {{ importPreview.routeNodeCount }} 个路口
          </el-descriptions-item>
          <el-descriptions-item label="路线边数量">
            {{ importPreview.edgeCount }}
          </el-descriptions-item>
          <el-descriptions-item label="导入底图">
            {{ importPreview.importMapText }}
          </el-descriptions-item>
          <el-descriptions-item label="当前底图">
            {{ importPreview.currentMapText }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          class="import-warning"
          type="warning"
          :closable="false"
          title="确认导入后会覆盖当前景点的隐藏路口和路线边，并更新业务 POI 的平面图坐标；后端会先自动创建当前路网快照。"
        />

        <div v-if="importPreview.warnings.length" class="import-risk-list">
          <div class="risk-title">预检风险</div>
          <el-alert
            v-for="item in importPreview.warnings"
            :key="item"
            type="error"
            :closable="false"
            :title="item"
          />
        </div>
        <el-alert
          v-else
          class="import-warning"
          type="success"
          :closable="false"
          title="未发现明显格式风险，可以确认导入。"
        />
      </div>

      <template #footer>
        <el-button @click="cancelImportPreview">取消</el-button>
        <el-button type="warning" plain @click="previewImportGraph">
          预览到画布
        </el-button>
        <el-button type="primary" :loading="importing" @click="confirmImportGraph">
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="saveDialogVisible" title="保存路网" width="560px">
      <el-form label-width="96px">
        <el-form-item label="保存内容">
          <div class="save-summary">
            {{ poiNodeCount }} 个 POI，{{ routeNodeCount }} 个隐藏路口，{{ normalizedEdges.length }} 条路线边
          </div>
        </el-form-item>
        <div v-if="saveRiskItems.length" class="save-risk">
          <el-alert
            type="warning"
            :closable="false"
            title="当前路网还有风险，保存后用户路线规划可能不可达或绕路。"
          />
          <ul>
            <li v-for="item in saveRiskItems" :key="item">{{ item }}</li>
          </ul>
        </div>
        <el-form-item label="创建快照">
          <el-checkbox v-model="saveVersionForm.createVersion">
            保存后创建版本快照
          </el-checkbox>
        </el-form-item>
        <template v-if="saveVersionForm.createVersion">
          <el-form-item label="快照名称">
            <el-input
              v-model="saveVersionForm.versionName"
              maxlength="80"
              show-word-limit
              placeholder="例如：沙河校区西门到雁南园路线修正"
            />
          </el-form-item>
          <el-form-item label="修改说明">
            <el-input
              v-model="saveVersionForm.versionRemark"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              placeholder="写清楚这次改了哪些路口、路线边或 POI 坐标，后面回滚时会非常有用。"
            />
          </el-form-item>
        </template>
        <el-alert
          type="info"
          :closable="false"
          title="建议每次完成一段明确修改后都创建快照；如果只是临时试点，可以取消创建快照。"
        />
      </el-form>

      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGraph">
          确认保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionDiffVisible" title="版本差异对比" width="960px">
      <div v-loading="versionDiffLoading" class="version-diff">
        <template v-if="versionDiff">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="对比版本">
              v{{ versionDiff.version?.versionNo }} {{ versionDiff.version?.name || '' }}
            </el-descriptions-item>
            <el-descriptions-item label="当前规模">
              {{ versionDiff.currentStats?.poiCount || 0 }} POI /
              {{ versionDiff.currentStats?.routeNodeCount || 0 }} 路口 /
              {{ versionDiff.currentStats?.edgeCount || 0 }} 边
            </el-descriptions-item>
            <el-descriptions-item label="版本规模">
              {{ versionDiff.versionStats?.poiCount || 0 }} POI /
              {{ versionDiff.versionStats?.routeNodeCount || 0 }} 路口 /
              {{ versionDiff.versionStats?.edgeCount || 0 }} 边
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="versionDiff.warnings?.length" class="import-risk-list">
            <el-alert
              v-for="item in versionDiff.warnings"
              :key="item"
              type="warning"
              :closable="false"
              :title="item"
            />
          </div>

          <div class="diff-grid">
            <section class="diff-card">
              <h3>恢复后会新增的节点</h3>
              <el-table :data="versionDiff.addedNodes || []" height="180" empty-text="无新增节点">
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="nodeType" label="类型" width="80" />
                <el-table-column label="版本坐标" width="120">
                  <template #default="{ row }">{{ row.versionMapX }}, {{ row.versionMapY }}</template>
                </el-table-column>
                <el-table-column label="操作" width="76" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="locateDiffNode(row, 'version')">定位</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section class="diff-card">
              <h3>恢复后会删除的节点</h3>
              <el-table :data="versionDiff.removedNodes || []" height="180" empty-text="无删除节点">
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="nodeType" label="类型" width="80" />
                <el-table-column label="当前坐标" width="120">
                  <template #default="{ row }">{{ row.currentMapX }}, {{ row.currentMapY }}</template>
                </el-table-column>
                <el-table-column label="操作" width="76" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="locateDiffNode(row, 'current')">定位</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section class="diff-card">
              <h3>恢复后会移动的 POI</h3>
              <el-table :data="versionDiff.movedNodes || []" height="180" empty-text="无移动 POI">
                <el-table-column prop="name" label="名称" />
                <el-table-column label="当前" width="100">
                  <template #default="{ row }">{{ row.currentMapX }}, {{ row.currentMapY }}</template>
                </el-table-column>
                <el-table-column label="版本" width="100">
                  <template #default="{ row }">{{ row.versionMapX }}, {{ row.versionMapY }}</template>
                </el-table-column>
                <el-table-column label="操作" width="132" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="locateMovedDiffNode(row)">对照</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section class="diff-card">
              <h3>恢复后会新增的路线边</h3>
              <el-table :data="versionDiff.addedEdges || []" height="180" empty-text="无新增路线边">
                <el-table-column label="路线" min-width="180">
                  <template #default="{ row }">{{ row.fromName }} -> {{ row.toName }}</template>
                </el-table-column>
                <el-table-column prop="description" label="说明" />
                <el-table-column label="操作" width="76" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="locateDiffEdge(row, 'version')">定位</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section class="diff-card full">
              <h3>恢复后会删除的路线边</h3>
              <el-table :data="versionDiff.removedEdges || []" height="180" empty-text="无删除路线边">
                <el-table-column label="路线" min-width="180">
                  <template #default="{ row }">{{ row.fromName }} -> {{ row.toName }}</template>
                </el-table-column>
                <el-table-column prop="description" label="说明" />
                <el-table-column label="操作" width="76" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="locateDiffEdge(row, 'current')">定位</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="versionDiffVisible = false">关闭</el-button>
        <el-button type="success" plain :disabled="!selectedVersionForDiff" @click="previewVersion(selectedVersionForDiff)">
          预览此版本
        </el-button>
        <el-button
          type="primary"
          :loading="versionRestoring"
          :disabled="!selectedVersionForDiff"
          @click="restoreVersion(selectedVersionForDiff)"
        >
          恢复此版本
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import {
  getRouteScopes,
  getAdminRouteGraph,
  saveAdminRouteGraphWithVersion,
  exportAdminRouteGraph,
  importAdminRouteGraph,
  getAdminRouteGraphVersions,
  getAdminRouteGraphQuality,
  cleanupAdminRouteGraphQuality,
  createAdminRouteGraphPoi,
  deleteAdminRouteGraphPoi,
  createAdminRouteGraphVersion,
  getAdminRouteGraphVersionSnapshot,
  diffAdminRouteGraphVersion,
  restoreAdminRouteGraphVersion
} from '@/api/route'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const routeScopes = ref([])
const placeGroupId = ref(null)
const lastLoadedPlaceGroupId = ref(null)
const lastSavedGraphFingerprint = ref('')
const skipNextRouteLeaveGuard = ref(false)
const graphMap = ref(null)
const nodes = ref([])
const edges = ref([])
const tool = ref('move')
const svgRef = ref(null)
const mapPanelRef = ref(null)
const draggingNode = ref(null)
const connectStart = ref(null)
const selectedNode = ref(null)
const selectedEdgeKey = ref('')
const saving = ref(false)
const saveDialogVisible = ref(false)
const saveVersionForm = ref({
  createVersion: true,
  versionName: '',
  versionRemark: ''
})
const newRouteNodeName = ref('')
const edgeDescription = ref('')
const routeNodeSeq = ref(1)
const showRouteLabels = ref(false)
const newPoiForm = ref({
  name: '',
  categoryCode: 'service',
  areaCode: 'service',
  areaName: '生活服务'
})
const undoStack = ref([])
const maxUndoSteps = 30
const importFileRef = ref(null)
const versionDrawerVisible = ref(false)
const versionLoading = ref(false)
const versionCreating = ref(false)
const versionRestoring = ref(false)
const versions = ref([])
const versionFilter = ref({
  keyword: '',
  creator: '',
  dateRange: []
})
const versionDiffVisible = ref(false)
const versionDiffLoading = ref(false)
const versionDiff = ref(null)
const selectedVersionForDiff = ref(null)
const versionPreviewActive = ref(false)
const versionPreviewSource = ref(null)
const versionPreviewNodes = ref([])
const versionPreviewEdges = ref([])
const diffFocusMarkers = ref([])
const diffFocusEdges = ref([])
const qualityDrawerVisible = ref(false)
const qualityLoading = ref(false)
const qualityCleaning = ref(false)
const graphQuality = ref(null)
const selectedQualityIssueKey = ref('')
const importPreviewVisible = ref(false)
const pendingImportPayload = ref(null)
const importPreview = ref(null)
const importing = ref(false)
const importPreviewActive = ref(false)
const importPreviewNodes = ref([])
const importPreviewEdges = ref([])
const routeDebugForm = ref({
  startClientId: '',
  endClientId: ''
})
const routeDebugResult = ref(null)
const showConnectivityOverlay = ref(true)
const mapPixelToMeter = 0.35

const toolOptions = [
  { label: '拖动', value: 'move' },
  { label: '加 POI', value: 'add-poi' },
  { label: '加路口', value: 'add-route' },
  { label: '连线', value: 'connect' },
  { label: '删除', value: 'delete' }
]

const poiCategoryOptions = [
  { label: '校门', value: 'gate', areaCode: 'gate', areaName: '校门' },
  { label: '教学楼', value: 'teaching', areaCode: 'teaching', areaName: '教学区' },
  { label: '图书馆', value: 'library', areaCode: 'library', areaName: '教学科研区' },
  { label: '宿舍', value: 'dormitory', areaCode: 'dormitory', areaName: '宿舍区' },
  { label: '餐饮', value: 'dining', areaCode: 'dining', areaName: '餐饮区' },
  { label: '运动', value: 'sports', areaCode: 'sports', areaName: '运动区' },
  { label: '医疗', value: 'medical', areaCode: 'medical', areaName: '医疗服务' },
  { label: '办公', value: 'office', areaCode: 'office', areaName: '办公服务' },
  { label: '购物', value: 'shop', areaCode: 'shopping', areaName: '购物服务' },
  { label: '景观', value: 'scenic', areaCode: 'landscape', areaName: '景观区' },
  { label: '交通', value: 'transport', areaCode: 'transport', areaName: '校外交通' },
  { label: '生活服务', value: 'service', areaCode: 'service', areaName: '生活服务' }
]

const mapWidth = computed(() => graphMap.value?.mapWidth || 1000)
const mapHeight = computed(() => graphMap.value?.mapHeight || 680)
const poiNodeCount = computed(() => nodes.value.filter(node => node.nodeType !== 'route').length)
const routeNodeCount = computed(() => nodes.value.filter(node => node.nodeType === 'route').length)
const canUndo = computed(() => undoStack.value.length > 0)
const hasUnsavedChanges = computed(() => {
  if (!lastSavedGraphFingerprint.value || !placeGroupId.value) {
    return false
  }
  return graphFingerprint(buildGraphSavePayload()) !== lastSavedGraphFingerprint.value
})
const currentScopeName = computed(() => {
  const scope = routeScopes.value.find(item => Number(item.placeGroupId) === Number(placeGroupId.value))
  return scope?.name || `景点分组 ${placeGroupId.value || '-'}`
})
const autoCleanableIssueTypes = ['invalidEdge', 'crossGroupEdge', 'selfLoopEdge', 'duplicateEdge']
const hasAutoCleanableIssues = computed(() => {
  return Boolean(graphQuality.value?.issues?.some(issue => autoCleanableIssueTypes.includes(issue.type)))
})
const visiblePoiNodes = computed(() => {
  return nodes.value
    .filter(node => node.nodeType !== 'route')
    .filter(node => Number.isFinite(Number(node.mapX)) && Number.isFinite(Number(node.mapY)))
    .slice()
    .sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN'))
})
const canRunRouteDebug = computed(() => {
  return Boolean(
    routeDebugForm.value.startClientId &&
    routeDebugForm.value.endClientId &&
    routeDebugForm.value.startClientId !== routeDebugForm.value.endClientId &&
    normalizedEdges.value.length
  )
})

const versionCreators = computed(() => {
  const creators = versions.value
    .map(row => versionCreatorText(row))
    .filter(Boolean)
  return Array.from(new Set(creators)).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const filteredVersions = computed(() => {
  const keyword = versionFilter.value.keyword.trim().toLowerCase()
  const creator = versionFilter.value.creator
  const [startDate, endDate] = versionFilter.value.dateRange || []
  return versions.value.filter(row => {
    if (keyword) {
      const searchText = [
        row.versionNo ? `v${row.versionNo}` : '',
        row.name,
        row.remark,
        versionCreatorText(row),
        formatDateTime(row.createdAt)
      ].filter(Boolean).join(' ').toLowerCase()
      if (!searchText.includes(keyword)) {
        return false
      }
    }
    if (creator && versionCreatorText(row) !== creator) {
      return false
    }
    if (startDate || endDate) {
      const createdDate = dateOnly(row.createdAt)
      if (!createdDate) {
        return false
      }
      if (startDate && createdDate < startDate) {
        return false
      }
      if (endDate && createdDate > endDate) {
        return false
      }
    }
    return true
  })
})

const nodeMap = computed(() => {
  const result = new Map()
  nodes.value.forEach(node => result.set(node.clientId, node))
  return result
})

const normalizedEdges = computed(() => {
  return edges.value
    .map(edge => {
      const from = nodeMap.value.get(edge.fromClientId)
      const to = nodeMap.value.get(edge.toClientId)
      if (!from || !to) return null
      return {
        ...edge,
        key: edgeKey(edge.fromClientId, edge.toClientId),
        from,
        to
      }
    })
    .filter(Boolean)
})

const routeDebugPathNodes = computed(() => {
  if (routeDebugResult.value?.status !== 'ok') return []
  return (routeDebugResult.value.nodeClientIds || [])
    .map(clientId => nodeMap.value.get(clientId))
    .filter(Boolean)
})

const routeDebugEdges = computed(() => {
  if (routeDebugResult.value?.status !== 'ok') return []
  const edgeKeys = new Set(routeDebugResult.value.edgeKeys || [])
  return normalizedEdges.value.filter(edge => edgeKeys.has(edge.key))
})

const routeConnectivity = computed(() => {
  const adjacency = buildRouteDebugAdjacency()
  const visited = new Set()
  const components = []

  adjacency.forEach((_, clientId) => {
    if (visited.has(clientId)) return
    const queue = [clientId]
    const nodeClientIds = []
    visited.add(clientId)
    while (queue.length) {
      const current = queue.shift()
      nodeClientIds.push(current)
      for (const edge of adjacency.get(current) || []) {
        if (!visited.has(edge.toClientId)) {
          visited.add(edge.toClientId)
          queue.push(edge.toClientId)
        }
      }
    }
    const poiClientIds = nodeClientIds.filter(clientId => {
      const node = nodeMap.value.get(clientId)
      return node && node.nodeType !== 'route'
    })
    const hasEdges = nodeClientIds.some(clientId => (adjacency.get(clientId) || []).length > 0)
    components.push({
      id: components.length + 1,
      nodeClientIds,
      poiClientIds,
      nodeCount: nodeClientIds.length,
      poiCount: poiClientIds.length,
      hasEdges
    })
  })

  const componentIdByNode = new Map()
  components.forEach(component => {
    component.nodeClientIds.forEach(clientId => {
      componentIdByNode.set(clientId, component.id)
    })
  })

  const mainComponent = components
    .slice()
    .filter(component => component.hasEdges)
    .sort((a, b) => {
      if (b.poiCount !== a.poiCount) return b.poiCount - a.poiCount
      return b.nodeCount - a.nodeCount
    })[0] || null
  const mainComponentId = mainComponent?.id || null
  const isolatedPoiNodes = visiblePoiNodes.value
    .filter(node => (adjacency.get(node.clientId) || []).length === 0)
    .map(node => ({ ...node, connectivityIssueType: 'isolated-poi' }))
  const isolatedPoiIds = new Set(isolatedPoiNodes.map(node => node.clientId))
  const disconnectedPoiNodes = visiblePoiNodes.value
    .filter(node => mainComponentId && componentIdByNode.get(node.clientId) !== mainComponentId)
    .filter(node => !isolatedPoiIds.has(node.clientId))
    .map(node => ({ ...node, connectivityIssueType: 'disconnected-poi' }))
  const orphanRouteNodes = nodes.value
    .filter(node => node.nodeType === 'route')
    .filter(node => (adjacency.get(node.clientId) || []).length === 0)
    .map(node => ({ ...node, connectivityIssueType: 'orphan-route' }))
  const problemNodes = [
    ...isolatedPoiNodes,
    ...disconnectedPoiNodes,
    ...orphanRouteNodes
  ].sort((a, b) => issueWeight(a.connectivityIssueType) - issueWeight(b.connectivityIssueType))

  return {
    componentCount: components.length,
    components,
    mainComponentId,
    mainPoiCount: mainComponent?.poiCount || 0,
    isolatedPoiNodes,
    disconnectedPoiNodes,
    orphanRouteNodes,
    problemNodes
  }
})

const connectivityMarkers = computed(() => {
  return routeConnectivity.value.problemNodes
    .filter(node => Number.isFinite(Number(node.mapX)) && Number.isFinite(Number(node.mapY)))
})
const saveRiskItems = computed(() => {
  const risks = []
  if (visiblePoiNodes.value.length && normalizedEdges.value.length === 0) {
    risks.push('当前没有任何路线边，所有 POI 都无法进行内部路线规划。')
  }
  if (routeConnectivity.value.isolatedPoiNodes.length) {
    risks.push(`有 ${routeConnectivity.value.isolatedPoiNodes.length} 个 POI 完全孤立，没有连接任何路线。`)
  }
  if (routeConnectivity.value.disconnectedPoiNodes.length) {
    risks.push(`有 ${routeConnectivity.value.disconnectedPoiNodes.length} 个 POI 不在主路网中，和主要路线不连通。`)
  }
  if (routeConnectivity.value.orphanRouteNodes.length) {
    risks.push(`有 ${routeConnectivity.value.orphanRouteNodes.length} 个隐藏路口没有连接任何路线。`)
  }
  const effectiveComponentCount = routeConnectivity.value.components
    .filter(component => component.hasEdges && component.poiCount > 0)
    .length
  if (effectiveComponentCount > 1) {
    risks.push(`当前存在 ${effectiveComponentCount} 个带 POI 的独立路网，跨连通块规划会失败。`)
  }
  return risks
})

const importPreviewNodeMap = computed(() => {
  const result = new Map()
  importPreviewNodes.value.forEach(node => result.set(node.clientId, node))
  return result
})

const normalizedImportPreviewEdges = computed(() => {
  return importPreviewEdges.value
    .map(edge => {
      const from = importPreviewNodeMap.value.get(edge.fromClientId)
      const to = importPreviewNodeMap.value.get(edge.toClientId)
      if (!from || !to) return null
      return {
        ...edge,
        key: edgeKey(edge.fromClientId, edge.toClientId),
        from,
        to
      }
    })
    .filter(Boolean)
})

const versionPreviewNodeMap = computed(() => {
  const result = new Map()
  versionPreviewNodes.value.forEach(node => result.set(node.clientId, node))
  return result
})

const normalizedVersionPreviewEdges = computed(() => {
  return versionPreviewEdges.value
    .map(edge => {
      const from = versionPreviewNodeMap.value.get(edge.fromClientId)
      const to = versionPreviewNodeMap.value.get(edge.toClientId)
      if (!from || !to) return null
      return {
        ...edge,
        key: edgeKey(edge.fromClientId, edge.toClientId),
        from,
        to
      }
    })
    .filter(Boolean)
})

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入路网标定页面')
    return
  }
  const res = await getRouteScopes()
  routeScopes.value = res.data || []
  if (routeScopes.value.length) {
    const queryPlaceGroupId = Number(route.query.placeGroupId)
    const matchedScope = routeScopes.value.find(scope => Number(scope.placeGroupId) === queryPlaceGroupId)
    placeGroupId.value = matchedScope?.placeGroupId || routeScopes.value[0].placeGroupId
    await loadGraph()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(async () => {
  if (skipNextRouteLeaveGuard.value) {
    skipNextRouteLeaveGuard.value = false
    return true
  }
  return await confirmDiscardChanges('当前路网有未保存改动，离开页面后这些改动会丢失。确认离开吗？')
})

const loadGraph = async () => {
  if (!placeGroupId.value) return
  const res = await getAdminRouteGraph(placeGroupId.value)
  graphMap.value = res.data?.map || null
  nodes.value = (res.data?.nodes || []).map(node => ({
    ...node,
    clientId: node.clientId || `id:${node.id}`,
    mapX: Number(node.mapX),
    mapY: Number(node.mapY)
  }))
  edges.value = collapseBidirectionalEdges(res.data?.edges || [])
  selectedNode.value = null
  selectedEdgeKey.value = ''
  selectedQualityIssueKey.value = ''
  connectStart.value = null
  graphQuality.value = null
  clearImportGraphPreview()
  clearVersionGraphPreview()
  clearDiffFocus()
  clearRouteDebug()
  undoStack.value = []
  lastLoadedPlaceGroupId.value = placeGroupId.value
  lastSavedGraphFingerprint.value = graphFingerprint(buildGraphSavePayload())
}

const reloadGraph = async () => {
  const confirmed = await confirmDiscardChanges('当前画布有未保存改动，重新加载会用数据库内容覆盖当前画布。确认重新加载吗？')
  if (!confirmed) return
  await loadGraph()
  ElMessage.success('已重新加载数据库路网')
}

const handlePlaceGroupChange = async (nextPlaceGroupId) => {
  const previousPlaceGroupId = lastLoadedPlaceGroupId.value
  if (hasUnsavedChanges.value) {
    const confirmed = await confirmDiscardChanges('当前画布有未保存改动，切换景点会丢失这些改动。确认切换吗？')
    if (!confirmed) {
      placeGroupId.value = previousPlaceGroupId
      return
    }
  }
  placeGroupId.value = nextPlaceGroupId
  await loadGraph()
}

const goRoutePlan = async () => {
  const confirmed = await confirmDiscardChanges('当前路网有未保存改动，去测试路线前建议先保存。确认离开编辑器吗？')
  if (!confirmed) return
  skipNextRouteLeaveGuard.value = true
  router.push('/route-plan')
}

const openVersionDrawer = async () => {
  versionDrawerVisible.value = true
  await loadVersions()
}

const loadVersions = async () => {
  if (!placeGroupId.value) return
  versionLoading.value = true
  try {
    const res = await getAdminRouteGraphVersions(placeGroupId.value)
    versions.value = res.data || []
  } finally {
    versionLoading.value = false
  }
}

const resetVersionFilter = () => {
  versionFilter.value = {
    keyword: '',
    creator: '',
    dateRange: []
  }
}

const createVersionSnapshot = async () => {
  if (!placeGroupId.value) return
  try {
    if (hasUnsavedChanges.value) {
      await ElMessageBox.confirm(
        '当前画布有未保存改动。直接创建快照只会保存数据库中已保存的路网，不会包含当前画布改动。建议先点击“保存路网”。仍然继续创建快照吗？',
        '存在未保存改动',
        {
          type: 'warning',
          confirmButtonText: '继续创建',
          cancelButtonText: '先不创建'
        }
      )
    }
    const { value } = await ElMessageBox.prompt('给这次路网快照起个名字，方便之后回滚。', '创建路网快照', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：沙河校区手工标定稳定版'
    })
    versionCreating.value = true
    await createAdminRouteGraphVersion(placeGroupId.value, {
      name: value || undefined
    })
    ElMessage.success('路网快照已创建')
    if (versionDrawerVisible.value) {
      await loadVersions()
    }
  } catch (error) {
    // user cancelled
  } finally {
    versionCreating.value = false
  }
}

const restoreVersion = async (version) => {
  if (!placeGroupId.value || !version?.id) return
  try {
    const discardConfirmed = await confirmDiscardChanges('当前画布有未保存改动，恢复版本前这些改动不会被自动备份。确认继续恢复吗？')
    if (!discardConfirmed) return
    await ElMessageBox.confirm(
      `确定恢复到「${version.name || `v${version.versionNo}`}」吗？恢复前系统会自动保存当前路网快照。`,
      '恢复路网版本',
      {
        type: 'warning',
        confirmButtonText: '恢复',
        cancelButtonText: '取消'
      }
    )
    versionRestoring.value = true
    await restoreAdminRouteGraphVersion(placeGroupId.value, version.id)
    await loadGraph()
    await loadVersions()
    versionDiffVisible.value = false
    ElMessage.success('已恢复路网版本')
  } catch (error) {
    // user cancelled
  } finally {
    versionRestoring.value = false
  }
}

const openVersionDiff = async (version) => {
  if (!placeGroupId.value || !version?.id) return
  selectedVersionForDiff.value = version
  versionDiffVisible.value = true
  versionDiffLoading.value = true
  versionDiff.value = null
  clearDiffFocus()
  try {
    const res = await diffAdminRouteGraphVersion(placeGroupId.value, version.id)
    versionDiff.value = res.data || null
  } finally {
    versionDiffLoading.value = false
  }
}

const previewVersion = async (version) => {
  if (!placeGroupId.value || !version?.id) return
  const res = await getAdminRouteGraphVersionSnapshot(placeGroupId.value, version.id)
  const payload = res.data || { nodes: [], edges: [] }
  const previewNodes = graphPayloadPreviewNodes(payload)
  if (!previewNodes.length) {
    ElMessage.warning('该版本里没有可在平面图上预览的节点坐标')
    return
  }
  clearImportGraphPreview()
  clearDiffFocus()
  versionPreviewSource.value = version
  versionPreviewNodes.value = previewNodes
  versionPreviewEdges.value = (payload.edges || []).map(edge => ({ ...edge }))
  versionPreviewActive.value = true
  versionDiffVisible.value = false
  const firstNode = previewNodes[0]
  centerMapOnPoint(firstNode.mapX, firstNode.mapY)
  ElMessage.success('已将历史版本临时叠加到画布，恢复前不会写入数据库')
}

const clearVersionGraphPreview = () => {
  versionPreviewActive.value = false
  versionPreviewSource.value = null
  versionPreviewNodes.value = []
  versionPreviewEdges.value = []
}

const locateDiffNode = (row, source) => {
  const mapX = Number(source === 'current' ? row.currentMapX : row.versionMapX)
  const mapY = Number(source === 'current' ? row.currentMapY : row.versionMapY)
  if (!Number.isFinite(mapX) || !Number.isFinite(mapY)) {
    ElMessage.warning('这个节点没有可定位的平面图坐标')
    return
  }
  diffFocusEdges.value = []
  diffFocusMarkers.value = [
    {
      key: `${source}:${row.name}:${mapX}:${mapY}`,
      label: `${row.name}（${source === 'current' ? '当前' : '版本'}）`,
      kind: source,
      mapX,
      mapY
    }
  ]
  versionDiffVisible.value = false
  centerMapOnPoint(mapX, mapY)
}

const locateMovedDiffNode = (row) => {
  const currentX = Number(row.currentMapX)
  const currentY = Number(row.currentMapY)
  const versionX = Number(row.versionMapX)
  const versionY = Number(row.versionMapY)
  if (![currentX, currentY, versionX, versionY].every(Number.isFinite)) {
    ElMessage.warning('这个 POI 缺少完整坐标，无法对照定位')
    return
  }
  diffFocusMarkers.value = [
    {
      key: `current:${row.name}:${currentX}:${currentY}`,
      label: `${row.name}（当前）`,
      kind: 'current',
      mapX: currentX,
      mapY: currentY
    },
    {
      key: `version:${row.name}:${versionX}:${versionY}`,
      label: `${row.name}（版本）`,
      kind: 'version',
      mapX: versionX,
      mapY: versionY
    }
  ]
  diffFocusEdges.value = [
    {
      key: `moved:${row.name}`,
      fromMapX: currentX,
      fromMapY: currentY,
      toMapX: versionX,
      toMapY: versionY
    }
  ]
  versionDiffVisible.value = false
  centerMapOnPoint((currentX + versionX) / 2, (currentY + versionY) / 2)
}

const locateDiffEdge = (row, source) => {
  const fromMapX = Number(row.fromMapX)
  const fromMapY = Number(row.fromMapY)
  const toMapX = Number(row.toMapX)
  const toMapY = Number(row.toMapY)
  if (![fromMapX, fromMapY, toMapX, toMapY].every(Number.isFinite)) {
    ElMessage.warning('这条路线边缺少端点坐标，无法在图上定位')
    return
  }
  diffFocusMarkers.value = [
    {
      key: `${source}:from:${row.fromName}`,
      label: row.fromName,
      kind: source,
      mapX: fromMapX,
      mapY: fromMapY
    },
    {
      key: `${source}:to:${row.toName}`,
      label: row.toName,
      kind: source,
      mapX: toMapX,
      mapY: toMapY
    }
  ]
  diffFocusEdges.value = [
    {
      key: `${source}:edge:${row.fromName}:${row.toName}`,
      fromMapX,
      fromMapY,
      toMapX,
      toMapY
    }
  ]
  versionDiffVisible.value = false
  centerMapOnPoint((fromMapX + toMapX) / 2, (fromMapY + toMapY) / 2)
}

const clearDiffFocus = () => {
  diffFocusMarkers.value = []
  diffFocusEdges.value = []
}

const openQualityDrawer = async () => {
  qualityDrawerVisible.value = true
  await loadGraphQuality()
}

const loadGraphQuality = async () => {
  if (!placeGroupId.value) return
  qualityLoading.value = true
  try {
    const res = await getAdminRouteGraphQuality(placeGroupId.value)
    graphQuality.value = res.data || null
  } finally {
    qualityLoading.value = false
  }
}

const cleanupQualityIssues = async () => {
  if (!placeGroupId.value || !hasAutoCleanableIssues.value) return
  try {
    const discardConfirmed = await confirmDiscardChanges('当前画布有未保存改动，一键清理会基于数据库路网执行并重新加载画布。确认继续吗？')
    if (!discardConfirmed) return
    await ElMessageBox.confirm(
      '系统会先创建当前路网快照，然后清理无效边、跨景点边、自环边和重复边。长边和孤立 POI 不会自动删除。确认继续吗？',
      '一键清理异常边',
      {
        type: 'warning',
        confirmButtonText: '清理',
        cancelButtonText: '取消'
      }
    )
    qualityCleaning.value = true
    const res = await cleanupAdminRouteGraphQuality(placeGroupId.value)
    await loadGraph()
    graphQuality.value = res.data?.quality || null
    selectedQualityIssueKey.value = ''
    ElMessage.success(res.data?.message || '异常路线边已清理')
  } catch (error) {
    // user cancelled
  } finally {
    qualityCleaning.value = false
  }
}

const locateQualityIssue = (issue) => {
  selectedQualityIssueKey.value = qualityIssueKey(issue)
  if (issue.nodeId) {
    locateNodeById(issue.nodeId)
    return
  }
  if (issue.fromNodeId && issue.toNodeId) {
    locateEdgeByNodeIds(issue.fromNodeId, issue.toNodeId)
    return
  }
  ElMessage.warning('这个问题没有可定位的图上对象')
}

const canRepairQualityIssue = (issue) => {
  return [
    'isolatedPoi',
    'isolatedRouteNode',
    'longEdge',
    'selfLoopEdge',
    'duplicateEdge'
  ].includes(issue.type)
}

const repairQualityIssue = (issue) => {
  selectedQualityIssueKey.value = qualityIssueKey(issue)
  if (issue.type === 'isolatedPoi') {
    prepareConnectFromIssueNode(issue)
    return
  }
  if (issue.type === 'isolatedRouteNode') {
    prepareDeleteIssueNode(issue)
    return
  }
  if (issue.type === 'longEdge') {
    prepareDeleteIssueEdge(issue)
    return
  }
  if (issue.type === 'selfLoopEdge') {
    removeIssueEdgeFromCanvas(issue)
    return
  }
  if (issue.type === 'duplicateEdge') {
    prepareCleanDuplicateEdges()
  }
}

const prepareConnectFromIssueNode = (issue) => {
  const node = findNodeById(issue.nodeId)
  if (!node) {
    ElMessage.warning('这个 POI 缺少平面图坐标，先在图上校准位置后才能连线')
    return
  }
  tool.value = 'connect'
  selectedNode.value = node
  selectedEdgeKey.value = ''
  connectStart.value = node
  centerMapOnPoint(node.mapX, node.mapY)
  ElMessage.success(`已切到连线工具，并把「${node.name}」设为连线起点。接下来点击相邻路口或 POI 完成连接。`)
}

const prepareDeleteIssueNode = (issue) => {
  const node = findNodeById(issue.nodeId)
  if (!node) {
    ElMessage.warning('这个路线节点当前无法在图上定位')
    return
  }
  tool.value = 'delete'
  selectedNode.value = node
  selectedEdgeKey.value = ''
  connectStart.value = null
  showRouteLabels.value = true
  centerMapOnPoint(node.mapX, node.mapY)
  ElMessage.success(`已切到删除工具。确认「${node.name}」是误加路口后，点击图上高亮节点即可删除。`)
}

const prepareDeleteIssueEdge = (issue) => {
  const edge = findNormalizedEdgeByNodeIds(issue.fromNodeId, issue.toNodeId)
  if (!edge) {
    ElMessage.warning('这条路线边当前无法在图上定位，可能需要重新加载路网后处理')
    return
  }
  tool.value = 'delete'
  selectedNode.value = null
  selectedEdgeKey.value = edge.key
  connectStart.value = null
  centerMapOnPoint((edge.from.mapX + edge.to.mapX) / 2, (edge.from.mapY + edge.to.mapY) / 2)
  ElMessage.success('已切到删除工具。如果这条高亮路线确实不合理，点击高亮路线边即可删除。')
}

const removeIssueEdgeFromCanvas = (issue) => {
  const edge = findNormalizedEdgeByNodeIds(issue.fromNodeId, issue.toNodeId)
  if (!edge) {
    ElMessage.warning('这条自环边当前不在画布中，重新保存当前路网通常可以清理它')
    return
  }
  pushUndoSnapshot()
  edges.value = edges.value.filter(item => edgeKey(item.fromClientId, item.toClientId) !== edge.key)
  selectedNode.value = null
  selectedEdgeKey.value = ''
  connectStart.value = null
  clearRouteDebugPreview()
  ElMessage.success('已从当前画布移除这条自环边，点击“保存路网”后写入数据库；误操作可点“撤销上一步”。')
}

const prepareCleanDuplicateEdges = () => {
  selectedNode.value = null
  selectedEdgeKey.value = ''
  connectStart.value = null
  ElMessage.success('当前画布已按无重复边展示。点击“保存路网”会按当前画布重建路线边，从而清理数据库中的重复边。')
}

const locateNodeById = (nodeId) => {
  const node = findNodeById(nodeId)
  if (!node) {
    ElMessage.warning('这个 POI 当前没有平面图坐标，暂时无法在图上定位')
    return
  }
  selectedNode.value = node
  selectedEdgeKey.value = ''
  connectStart.value = null
  if (node.nodeType === 'route') {
    showRouteLabels.value = true
  }
  centerMapOnPoint(node.mapX, node.mapY)
}

const locateEdgeByNodeIds = (fromNodeId, toNodeId) => {
  const edge = findNormalizedEdgeByNodeIds(fromNodeId, toNodeId)
  if (!edge) {
    ElMessage.warning('这条路线边当前无法在图上定位，可能连接了无效节点')
    return
  }
  selectedNode.value = null
  selectedEdgeKey.value = edge.key
  connectStart.value = null
  centerMapOnPoint((edge.from.mapX + edge.to.mapX) / 2, (edge.from.mapY + edge.to.mapY) / 2)
}

const findNodeById = (nodeId) => {
  return nodes.value.find(item => Number(item.id) === Number(nodeId))
}

const findNormalizedEdgeByNodeIds = (fromNodeId, toNodeId) => {
  const fromClientId = `id:${fromNodeId}`
  const toClientId = `id:${toNodeId}`
  return normalizedEdges.value.find(item => edgeKey(item.fromClientId, item.toClientId) === edgeKey(fromClientId, toClientId))
}

const centerMapOnPoint = (mapX, mapY) => {
  const panel = mapPanelRef.value
  const svg = svgRef.value
  if (!panel || !svg) return
  const rect = svg.getBoundingClientRect()
  const scaleX = rect.width / mapWidth.value
  const scaleY = rect.height / mapHeight.value
  panel.scrollTo({
    left: Math.max(0, mapX * scaleX - panel.clientWidth / 2),
    top: Math.max(0, mapY * scaleY - panel.clientHeight / 2),
    behavior: 'smooth'
  })
}

const confirmDiscardChanges = async (message) => {
  if (!hasUnsavedChanges.value) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      message,
      '未保存改动',
      {
        type: 'warning',
        confirmButtonText: '继续',
        cancelButtonText: '取消'
      }
    )
    return true
  } catch (error) {
    return false
  }
}

const runRouteDebug = () => {
  if (!canRunRouteDebug.value) {
    ElMessage.warning('请先选择不同的起点和终点，并至少画出一条路线边')
    return
  }
  const result = findShortestPathOnCanvas(
    routeDebugForm.value.startClientId,
    routeDebugForm.value.endClientId
  )
  routeDebugResult.value = result
  if (result.status === 'ok') {
    focusRouteDebugPath()
    ElMessage.success('路线试算完成，蓝色为当前普通路网，红色为试算最短路径')
  } else {
    ElMessage.error(result.message)
  }
}

const swapRouteDebugEndpoints = () => {
  const start = routeDebugForm.value.startClientId
  routeDebugForm.value.startClientId = routeDebugForm.value.endClientId
  routeDebugForm.value.endClientId = start
  routeDebugResult.value = null
}

const clearRouteDebug = () => {
  routeDebugForm.value = {
    startClientId: '',
    endClientId: ''
  }
  routeDebugResult.value = null
}

const clearRouteDebugPreview = () => {
  routeDebugResult.value = null
}

const findShortestPathOnCanvas = (startClientId, endClientId) => {
  const adjacency = buildRouteDebugAdjacency()
  if (!adjacency.has(startClientId) || !adjacency.has(endClientId)) {
    return {
      status: 'unreachable',
      message: '起点或终点不在当前画布节点中，请重新选择。'
    }
  }

  const distances = new Map()
  const previousNode = new Map()
  const previousEdge = new Map()
  const unvisited = new Set(adjacency.keys())
  adjacency.forEach((_, clientId) => {
    distances.set(clientId, Number.POSITIVE_INFINITY)
  })
  distances.set(startClientId, 0)

  while (unvisited.size) {
    let current = null
    let currentDistance = Number.POSITIVE_INFINITY
    unvisited.forEach(clientId => {
      const distance = distances.get(clientId)
      if (distance < currentDistance) {
        current = clientId
        currentDistance = distance
      }
    })

    if (!current || currentDistance === Number.POSITIVE_INFINITY) break
    if (current === endClientId) break
    unvisited.delete(current)

    for (const edge of adjacency.get(current) || []) {
      if (!unvisited.has(edge.toClientId)) continue
      const nextDistance = currentDistance + edge.distance
      if (nextDistance < distances.get(edge.toClientId)) {
        distances.set(edge.toClientId, nextDistance)
        previousNode.set(edge.toClientId, current)
        previousEdge.set(edge.toClientId, edge.key)
      }
    }
  }

  const totalDistance = distances.get(endClientId)
  if (!Number.isFinite(totalDistance)) {
    const startName = nodeMap.value.get(startClientId)?.name || '起点'
    const endName = nodeMap.value.get(endClientId)?.name || '终点'
    return {
      status: 'unreachable',
      message: `当前路网中「${startName}」到「${endName}」不可达，需要补路线边或中间路口。`
    }
  }

  const nodeClientIds = []
  const edgeKeys = []
  let cursor = endClientId
  while (cursor) {
    nodeClientIds.unshift(cursor)
    const edgeKeyValue = previousEdge.get(cursor)
    if (edgeKeyValue) {
      edgeKeys.unshift(edgeKeyValue)
    }
    if (cursor === startClientId) break
    cursor = previousNode.get(cursor)
  }

  return {
    status: 'ok',
    totalDistance: Math.max(1, Math.round(totalDistance)),
    totalDuration: Math.max(1, Math.ceil(totalDistance / 80)),
    nodeClientIds,
    edgeKeys
  }
}

const buildRouteDebugAdjacency = () => {
  const adjacency = new Map()
  nodes.value.forEach(node => {
    adjacency.set(node.clientId, [])
  })
  normalizedEdges.value.forEach(edge => {
    const distance = edgeDistanceMeters(edge)
    adjacency.get(edge.fromClientId)?.push({
      toClientId: edge.toClientId,
      distance,
      key: edge.key
    })
    if (edge.bidirectional !== false) {
      adjacency.get(edge.toClientId)?.push({
        toClientId: edge.fromClientId,
        distance,
        key: edge.key
      })
    }
  })
  return adjacency
}

const edgeDistanceMeters = (edge) => {
  const dx = Number(edge.to.mapX) - Number(edge.from.mapX)
  const dy = Number(edge.to.mapY) - Number(edge.from.mapY)
  return Math.max(1, Math.round(Math.sqrt(dx * dx + dy * dy) * mapPixelToMeter))
}

const focusRouteDebugPath = () => {
  const pathNodes = routeDebugPathNodes.value
  if (!pathNodes.length) return
  const center = pathNodes.reduce((acc, node) => {
    acc.x += Number(node.mapX)
    acc.y += Number(node.mapY)
    return acc
  }, { x: 0, y: 0 })
  centerMapOnPoint(center.x / pathNodes.length, center.y / pathNodes.length)
}

const locateConnectivityNode = (node) => {
  if (!node) return
  selectedNode.value = nodeMap.value.get(node.clientId) || node
  selectedEdgeKey.value = ''
  connectStart.value = null
  showConnectivityOverlay.value = true
  if (node.nodeType === 'route') {
    showRouteLabels.value = true
  }
  centerMapOnPoint(Number(node.mapX), Number(node.mapY))
}

const connectivityIssueText = (type) => {
  if (type === 'isolated-poi') return '完全孤立'
  if (type === 'disconnected-poi') return '不在主路网'
  if (type === 'orphan-route') return '孤立路口'
  return '待检查'
}

const issueWeight = (type) => {
  if (type === 'isolated-poi') return 1
  if (type === 'disconnected-poi') return 2
  if (type === 'orphan-route') return 3
  return 9
}

const handleBeforeUnload = (event) => {
  if (!hasUnsavedChanges.value) return
  event.preventDefault()
  event.returnValue = ''
}

const exportGraphJson = async () => {
  if (!placeGroupId.value) return
  const res = await exportAdminRouteGraph(placeGroupId.value)
  const data = res.data || { nodes: [], edges: [] }
  const payload = JSON.stringify(data, null, 2)
  const blob = new Blob([payload], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = exportFileName(data)
  link.click()
  URL.revokeObjectURL(url)
}

const triggerImport = () => {
  importFileRef.value?.click()
}

const handleImportFile = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || !placeGroupId.value) return
  try {
    const text = await file.text()
    const parsed = JSON.parse(text)
    const payload = normalizeImportPayload(parsed)
    if (!Array.isArray(payload.nodes) || !Array.isArray(payload.edges)) {
      ElMessage.error('JSON 格式不正确，需要包含 nodes 和 edges')
      return
    }
    pendingImportPayload.value = payload
    importPreview.value = buildImportPreview(payload)
    importPreviewVisible.value = true
  } catch (error) {
    if (error instanceof SyntaxError) {
      ElMessage.error('JSON 文件解析失败')
    }
  }
}

const normalizeImportPayload = (parsed) => {
  if (parsed?.data?.nodes) return parsed.data
  return parsed
}

const buildImportPreview = (payload) => {
  const nodes = payload.nodes || []
  const edges = payload.edges || []
  const routeNodeCountValue = nodes.filter(node => isImportRouteNode(node)).length
  const poiCountValue = nodes.length - routeNodeCountValue
  const warnings = []

  if (payload.placeGroupId && Number(payload.placeGroupId) !== Number(placeGroupId.value)) {
    warnings.push(`导入文件属于 placeGroupId=${payload.placeGroupId}，当前页面是 placeGroupId=${placeGroupId.value}，请确认没有选错景点。`)
  }
  if (payload.map?.mapWidth && payload.map?.mapHeight && graphMap.value?.mapWidth && graphMap.value?.mapHeight) {
    if (Number(payload.map.mapWidth) !== Number(graphMap.value.mapWidth) || Number(payload.map.mapHeight) !== Number(graphMap.value.mapHeight)) {
      warnings.push(`导入底图尺寸 ${payload.map.mapWidth}x${payload.map.mapHeight} 与当前底图 ${graphMap.value.mapWidth}x${graphMap.value.mapHeight} 不一致，POI 坐标可能会偏移。`)
    }
  }
  const nodeIds = new Set(nodes.map(node => node.clientId).filter(Boolean))
  const brokenEdgeCount = edges.filter(edge => !nodeIds.has(edge.fromClientId) || !nodeIds.has(edge.toClientId)).length
  if (brokenEdgeCount > 0) {
    warnings.push(`有 ${brokenEdgeCount} 条路线边引用了导入文件中不存在的节点，导入后这些边会被后端忽略。`)
  }
  const duplicateEdgeCount = countDuplicateImportEdges(edges)
  if (duplicateEdgeCount > 0) {
    warnings.push(`导入文件中存在 ${duplicateEdgeCount} 条重复路线边，保存时后端会自动去重。`)
  }
  if (payload.stats) {
    if (Number(payload.stats.edgeCount || 0) !== edges.length) {
      warnings.push(`文件统计的路线边数量是 ${payload.stats.edgeCount}，实际读取到 ${edges.length} 条，文件可能被手动改过。`)
    }
    const expectedNodeCount = Number(payload.stats.poiCount || 0) + Number(payload.stats.routeNodeCount || 0)
    if (expectedNodeCount && expectedNodeCount !== nodes.length) {
      warnings.push(`文件统计的节点数量是 ${expectedNodeCount}，实际读取到 ${nodes.length} 个，文件可能被手动改过。`)
    }
  }
  if (!payload.schemaVersion) {
    warnings.push('这是旧版路网 JSON，没有景点、底图和导出时间元信息，导入前请重点确认文件来源。')
  }

  return {
    schemaVersion: payload.schemaVersion,
    exportedAt: payload.exportedAt,
    placeGroupName: payload.placeGroupName || payload.placeGroupShortName,
    poiCount: poiCountValue,
    routeNodeCount: routeNodeCountValue,
    edgeCount: edges.length,
    importMapText: mapText(payload.map),
    currentMapText: mapText(graphMap.value),
    warnings
  }
}

const isImportRouteNode = (node) => {
  return node.nodeType === 'route' || node.areaCode === 'route'
}

const countDuplicateImportEdges = (edges) => {
  const seen = new Set()
  let duplicateCount = 0
  edges.forEach(edge => {
    const key = edgeKey(edge.fromClientId, edge.toClientId)
    if (seen.has(key)) {
      duplicateCount += 1
    } else {
      seen.add(key)
    }
  })
  return duplicateCount
}

const mapText = (map) => {
  if (!map) return '未记录'
  const size = map.mapWidth && map.mapHeight ? `${map.mapWidth}x${map.mapHeight}` : '未记录尺寸'
  return `${size}${map.originalName ? ` / ${map.originalName}` : ''}`
}

const confirmImportGraph = async () => {
  if (!placeGroupId.value || !pendingImportPayload.value) return
  importing.value = true
  try {
    const discardConfirmed = await confirmDiscardChanges('当前画布有未保存改动，导入 JSON 会覆盖当前景点路网。确认继续导入吗？')
    if (!discardConfirmed) return
    await importAdminRouteGraph(placeGroupId.value, pendingImportPayload.value)
    await loadGraph()
    ElMessage.success('路网 JSON 已导入')
  } finally {
    importing.value = false
  }
}

const cancelImportPreview = () => {
  importPreviewVisible.value = false
  clearImportGraphPreview()
}

const previewImportGraph = () => {
  if (!pendingImportPayload.value) return
  const payload = pendingImportPayload.value
  const previewNodes = graphPayloadPreviewNodes(payload)

  if (!previewNodes.length) {
    ElMessage.warning('导入文件里没有可在平面图上预览的节点坐标')
    return
  }

  clearVersionGraphPreview()
  importPreviewNodes.value = previewNodes
  importPreviewEdges.value = (payload.edges || []).map(edge => ({ ...edge }))
  importPreviewActive.value = true
  importPreviewVisible.value = false
  const firstNode = previewNodes[0]
  centerMapOnPoint(firstNode.mapX, firstNode.mapY)
  ElMessage.success('已将导入文件临时预览到画布，确认前不会写入数据库')
}

const graphPayloadPreviewNodes = (payload) => {
  return (payload.nodes || [])
    .map((node, index) => ({
      ...node,
      clientId: node.clientId || `preview:${node.id || node.name || index}`,
      nodeType: isImportRouteNode(node) ? 'route' : 'poi',
      mapX: Number(node.mapX),
      mapY: Number(node.mapY)
    }))
    .filter(node => Number.isFinite(node.mapX) && Number.isFinite(node.mapY))
}

const clearImportGraphPreview = () => {
  importPreviewActive.value = false
  importPreviewNodes.value = []
  importPreviewEdges.value = []
  pendingImportPayload.value = null
  importPreview.value = null
  importPreviewVisible.value = false
}

const clearRoutes = async () => {
  if (!nodes.value.length && !edges.value.length) return
  try {
    await ElMessageBox.confirm(
      '这会清空当前图上的隐藏路口和所有路线边，但会保留已有 POI。确认后还需要点击“保存路网”才会写入数据库。',
      '清空当前路线',
      {
        type: 'warning',
        confirmButtonText: '清空',
        cancelButtonText: '取消'
      }
    )
    pushUndoSnapshot()
    nodes.value = nodes.value.filter(node => node.nodeType !== 'route')
    edges.value = []
    connectStart.value = null
    selectedNode.value = null
    selectedEdgeKey.value = ''
    clearRouteDebugPreview()
    ElMessage.success('已清空画布路线，保存后会同步到数据库')
  } catch (error) {
    // user cancelled
  }
}

const collapseBidirectionalEdges = (rawEdges) => {
  const seen = new Set()
  const result = []
  rawEdges.forEach(edge => {
    const from = `id:${edge.fromPoiId}`
    const to = `id:${edge.toPoiId}`
    const key = edgeKey(from, to)
    if (seen.has(key)) return
    seen.add(key)
    result.push({
      fromClientId: from,
      toClientId: to,
      description: cleanupDescription(edge.description),
      transportType: edge.transportType || 'walk',
      indoor: Boolean(edge.indoor),
      bidirectional: true
    })
  })
  return result
}

const cleanupDescription = (text) => {
  if (!text) return ''
  return String(text).replace(/^返回：/, '')
}

const exportFileName = (data) => {
  const name = sanitizeFileName(data.placeGroupName || data.placeGroupShortName || `place-group-${placeGroupId.value}`)
  const exportedAt = String(data.exportedAt || new Date().toISOString()).replace(/[:.]/g, '-').slice(0, 19)
  return `route-graph-${name}-${exportedAt}.json`
}

const sanitizeFileName = (text) => {
  return String(text)
    .trim()
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .slice(0, 48) || 'route-graph'
}

const handleMapClick = async (event) => {
  if (tool.value === 'add-poi') {
    await createPoiAt(event)
    return
  }
  if (tool.value !== 'add-route') return
  pushUndoSnapshot()
  clearRouteDebugPreview()
  const point = svgPoint(event)
  const clientId = `local:${Date.now()}-${routeNodeSeq.value}`
  const name = newRouteNodeName.value.trim() || `路口 ${routeNodeSeq.value}`
  routeNodeSeq.value += 1
  nodes.value.push({
    id: null,
    clientId,
    name,
    nodeType: 'route',
    areaCode: 'route',
    areaName: '路线节点',
    categoryCode: 'scenic',
    mapX: Math.round(point.x),
    mapY: Math.round(point.y),
    visible: false
  })
}

const createPoiAt = async (event) => {
  if (!placeGroupId.value) {
    ElMessage.warning('请先选择景点')
    return
  }
  const name = newPoiForm.value.name.trim()
  if (!name) {
    ElMessage.warning('先填写 POI 名称，再点击地图位置')
    return
  }
  const point = svgPoint(event)
  const category = poiCategoryOptions.find(item => item.value === newPoiForm.value.categoryCode)
  const areaCode = category?.areaCode || newPoiForm.value.areaCode || 'service'
  const areaName = newPoiForm.value.areaName.trim() || category?.areaName || '补充点位'
  const hadUnsavedChanges = hasUnsavedChanges.value
  try {
    const res = await createAdminRouteGraphPoi(placeGroupId.value, {
      name,
      categoryCode: newPoiForm.value.categoryCode,
      areaCode,
      areaName,
      mapX: clamp(Math.round(point.x), 0, mapWidth.value),
      mapY: clamp(Math.round(point.y), 0, mapHeight.value)
    })
    const node = res.data
    if (!node?.id) {
      ElMessage.error('POI 已提交，但接口没有返回节点信息，请刷新页面确认')
      return
    }
    nodes.value.push({
      ...node,
      clientId: node.clientId || `id:${node.id}`,
      nodeType: 'poi',
      mapX: Number(node.mapX),
      mapY: Number(node.mapY)
    })
    if (!hadUnsavedChanges) {
      lastSavedGraphFingerprint.value = graphFingerprint(buildGraphSavePayload())
    }
    newPoiForm.value.name = ''
    clearRouteDebugPreview()
    ElMessage.success(`已添加 POI：${node.name}`)
  } catch (error) {
    console.error('添加 POI 失败:', error)
  }
}

const handlePoiCategoryChange = (categoryCode) => {
  const category = poiCategoryOptions.find(item => item.value === categoryCode)
  if (!category) return
  newPoiForm.value.areaCode = category.areaCode
  newPoiForm.value.areaName = category.areaName
}

const handleNodeClick = (node) => {
  selectedNode.value = node
  selectedEdgeKey.value = ''
  if (tool.value === 'connect') {
    if (!connectStart.value) {
      connectStart.value = node
      return
    }
    if (connectStart.value.clientId === node.clientId) {
      connectStart.value = null
      return
    }
    addEdge(connectStart.value, node)
    connectStart.value = node
  } else if (tool.value === 'delete') {
    deleteNode(node)
  }
}

const handleEdgeClick = async (edge) => {
  selectedNode.value = null
  selectedEdgeKey.value = edge.key
  if (tool.value !== 'delete') return
  pushUndoSnapshot()
  edges.value = edges.value.filter(item => edgeKey(item.fromClientId, item.toClientId) !== edge.key)
  clearRouteDebugPreview()
}

const addEdge = (from, to) => {
  const key = edgeKey(from.clientId, to.clientId)
  if (edges.value.some(edge => edgeKey(edge.fromClientId, edge.toClientId) === key)) {
    ElMessage.info('这两个点之间已经有路段')
    return
  }
  pushUndoSnapshot()
  clearRouteDebugPreview()
  edges.value.push({
    fromClientId: from.clientId,
    toClientId: to.clientId,
    description: edgeDescription.value.trim() || `${from.name} 到 ${to.name}`,
    transportType: 'walk',
    indoor: false,
    bidirectional: true
  })
}

const deleteNode = async (node) => {
  if (node.nodeType !== 'route') {
    await deletePoiNode(node)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除路线节点「${node.name}」及相关边吗？`, '删除路线节点', {
      type: 'warning'
    })
    pushUndoSnapshot()
    clearRouteDebugPreview()
    nodes.value = nodes.value.filter(item => item.clientId !== node.clientId)
    edges.value = edges.value.filter(edge => edge.fromClientId !== node.clientId && edge.toClientId !== node.clientId)
  } catch (error) {
    // user cancelled
  }
}

const deletePoiNode = async (node) => {
  if (!node?.id || !placeGroupId.value) {
    ElMessage.warning('这个 POI 还没有数据库 ID，刷新页面后再确认')
    return
  }
  try {
    const hadUnsavedChanges = hasUnsavedChanges.value
    await ElMessageBox.confirm(
      `确定删除业务 POI「${node.name}」吗？相关路线边也会一起删除。这个操作会立即写入数据库。`,
      '删除 POI',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await deleteAdminRouteGraphPoi(placeGroupId.value, node.id)
    clearRouteDebugPreview()
    nodes.value = nodes.value.filter(item => item.clientId !== node.clientId)
    edges.value = edges.value.filter(edge => edge.fromClientId !== node.clientId && edge.toClientId !== node.clientId)
    if (!hadUnsavedChanges) {
      lastSavedGraphFingerprint.value = graphFingerprint(buildGraphSavePayload())
    }
    selectedNode.value = null
    selectedEdgeKey.value = ''
    ElMessage.success(`已删除 POI：${node.name}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除 POI 失败:', error)
    }
  }
}

const startDragging = (node, event) => {
  if (tool.value !== 'move') return
  pushUndoSnapshot()
  draggingNode.value = node
  selectedNode.value = node
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

const handlePointerMove = (event) => {
  if (!draggingNode.value) return
  const point = svgPoint(event)
  draggingNode.value.mapX = clamp(Math.round(point.x), 0, mapWidth.value)
  draggingNode.value.mapY = clamp(Math.round(point.y), 0, mapHeight.value)
}

const stopDragging = () => {
  draggingNode.value = null
}

const pushUndoSnapshot = () => {
  undoStack.value.push({
    nodes: clone(nodes.value),
    edges: clone(edges.value),
    routeNodeSeq: routeNodeSeq.value
  })
  if (undoStack.value.length > maxUndoSteps) {
    undoStack.value.shift()
  }
}

const undoLastAction = () => {
  const snapshot = undoStack.value.pop()
  if (!snapshot) return
  nodes.value = snapshot.nodes
  edges.value = snapshot.edges
  routeNodeSeq.value = snapshot.routeNodeSeq
  draggingNode.value = null
  connectStart.value = null
  selectedNode.value = null
  selectedEdgeKey.value = ''
  clearRouteDebugPreview()
  ElMessage.success('已撤销上一步操作')
}

const clone = (value) => JSON.parse(JSON.stringify(value))

const svgPoint = (event) => {
  const svg = svgRef.value
  const point = svg.createSVGPoint()
  point.x = event.clientX
  point.y = event.clientY
  return point.matrixTransform(svg.getScreenCTM().inverse())
}

const openSaveDialog = () => {
  if (!placeGroupId.value) return
  saveVersionForm.value = {
    createVersion: true,
    versionName: `${currentScopeName.value} 路网 ${formatDateTime(new Date().toISOString())}`,
    versionRemark: ''
  }
  saveDialogVisible.value = true
}

const buildGraphSavePayload = () => ({
  nodes: nodes.value.map(node => ({
    id: node.id,
    clientId: node.clientId,
    name: node.name,
    nodeType: node.nodeType,
    categoryCode: node.categoryCode || 'scenic',
    areaCode: node.nodeType === 'route' ? 'route' : node.areaCode,
    areaName: node.nodeType === 'route' ? '路线节点' : node.areaName,
    mapX: Math.round(node.mapX),
    mapY: Math.round(node.mapY),
    visible: node.nodeType !== 'route'
  })),
  edges: edges.value
})

const graphFingerprint = (payload) => {
  const fingerprintNodes = (payload.nodes || [])
    .map(node => ({
      id: node.id || null,
      clientId: node.clientId || '',
      name: node.name || '',
      nodeType: node.nodeType || '',
      categoryCode: node.categoryCode || '',
      areaCode: node.areaCode || '',
      areaName: node.areaName || '',
      mapX: Number(node.mapX),
      mapY: Number(node.mapY),
      visible: Boolean(node.visible)
    }))
    .sort((a, b) => String(a.clientId || a.id).localeCompare(String(b.clientId || b.id)))

  const fingerprintEdges = (payload.edges || [])
    .map(edge => {
      const [fromClientId, toClientId] = [edge.fromClientId || '', edge.toClientId || ''].sort()
      return {
        key: edgeKey(fromClientId, toClientId),
        fromClientId,
        toClientId,
        description: edge.description || '',
        transportType: edge.transportType || 'walk',
        indoor: Boolean(edge.indoor),
        bidirectional: edge.bidirectional !== false
      }
    })
    .sort((a, b) => a.key.localeCompare(b.key))

  return JSON.stringify({
    nodes: fingerprintNodes,
    edges: fingerprintEdges
  })
}

const saveGraph = async () => {
  if (!placeGroupId.value) return
  try {
    const confirmed = await confirmRouteGraphSaveRisk()
    if (!confirmed) return
    saving.value = true
    await saveAdminRouteGraphWithVersion(placeGroupId.value, {
      graph: buildGraphSavePayload(),
      createVersion: saveVersionForm.value.createVersion,
      versionName: saveVersionForm.value.versionName,
      versionRemark: saveVersionForm.value.versionRemark
    })
    await loadGraph()
    saveDialogVisible.value = false
    ElMessage.success(saveVersionForm.value.createVersion ? '路网已保存，并已创建版本快照' : '路网已保存')
  } catch (error) {
    console.error('保存路网失败:', error)
  } finally {
    saving.value = false
  }
}

const confirmRouteGraphSaveRisk = async () => {
  if (!saveRiskItems.value.length) {
    return true
  }
  const riskHtml = `
    <div class="route-save-risk-confirm">
      <p>当前画布还存在路网风险，直接保存后用户可能遇到不可达或明显绕路。</p>
      <ul>${saveRiskItems.value.map(item => `<li>${escapeHtml(item)}</li>`).join('')}</ul>
      <p>如果这是阶段性草稿，可以继续保存；如果准备给用户使用，建议先补齐路线边。</p>
    </div>
  `
  try {
    await ElMessageBox.confirm(
      riskHtml,
      '保存前路网风险确认',
      {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '仍然保存',
        cancelButtonText: '返回修改'
      }
    )
    return true
  } catch (error) {
    showConnectivityOverlay.value = true
    return false
  }
}

const escapeHtml = (text) => {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

const edgeKey = (a, b) => [a, b].sort().join('|')
const clamp = (value, min, max) => Math.max(min, Math.min(max, value))
const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
const dateOnly = (value) => {
  if (!value) return ''
  return String(value).slice(0, 10)
}
const versionCreatorText = (row) => {
  return row?.createdByName || (row?.createdBy ? `用户 ${row.createdBy}` : '未知')
}
const issueTagType = (severity) => severity === 'error' ? 'danger' : 'warning'
const qualityIssueKey = (issue) => [
  issue.type || 'issue',
  issue.nodeId || '',
  issue.fromNodeId || '',
  issue.toNodeId || '',
  issue.edgeId || ''
].join(':')
const qualityRowClassName = ({ row }) => {
  return qualityIssueKey(row) === selectedQualityIssueKey.value ? 'selected-quality-row' : ''
}
</script>

<style lang="scss" scoped>
.graph-editor-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.workspace {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  padding: 16px;
}

.side-panel,
.map-panel {
  background: #fff;
  border: 1px solid #dfe5ef;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.side-panel {
  padding: 18px;
  align-self: start;
}

.panel-header {
  h2 {
    margin: 0 0 8px;
    font-size: 22px;
    color: #1f2937;
  }

  p {
    margin: 0 0 18px;
    color: #6b7280;
    line-height: 1.6;
  }
}

.hint-list {
  display: grid;
  gap: 8px;
  margin: 12px 0;
  color: #4b5563;
  line-height: 1.5;
}

.save-summary {
  color: #374151;
  line-height: 1.6;
}

.save-risk {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;

  ul {
    margin: 0;
    padding: 10px 12px 10px 28px;
    border: 1px solid #fde68a;
    border-radius: 6px;
    background: #fffbeb;
    color: #92400e;
    line-height: 1.7;
  }
}

.import-preview-banner {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  background: #fffbeb;
}

.version-preview-banner {
  border-color: #8b5cf6;
  background: #f5f3ff;

  .preview-title {
    color: #5b21b6;
  }

  .preview-text {
    color: #4c1d95;
  }
}

.preview-title {
  font-weight: 700;
  color: #92400e;
}

.preview-text {
  color: #78350f;
  line-height: 1.5;
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.route-debugger {
  display: grid;
  gap: 10px;
  margin: 14px 0 16px;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #f8fbff;
}

.route-debugger-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.route-debugger-title {
  font-weight: 700;
  color: #1f2937;
}

.route-debugger-desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.route-debugger-form {
  display: grid;
  gap: 8px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.route-debugger-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.route-debugger-alert {
  margin-top: 2px;
}

.route-debugger-result {
  display: grid;
  gap: 8px;
  padding-top: 2px;
}

.route-debugger-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    padding: 3px 8px;
    border-radius: 4px;
    color: #1d4ed8;
    background: #dbeafe;
    font-size: 12px;
    font-weight: 700;
  }
}

.route-debugger-path {
  max-height: 132px;
  overflow: auto;
  margin: 0;
  padding-left: 22px;
  color: #374151;
  line-height: 1.65;
  font-size: 13px;
}

.connectivity-panel {
  display: grid;
  gap: 10px;
  margin: 14px 0 16px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.connectivity-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.connectivity-title {
  font-weight: 700;
  color: #1f2937;
}

.connectivity-desc {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.connectivity-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.connectivity-list {
  display: grid;
  gap: 7px;
}

.connectivity-item {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  padding: 8px 9px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
  color: #374151;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;

  &:hover {
    border-color: #93c5fd;
    background: #eff6ff;
  }
}

.connectivity-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #f59e0b;

  &.isolated-poi {
    background: #ef4444;
  }

  &.disconnected-poi {
    background: #f97316;
  }

  &.orphan-route {
    background: #64748b;
  }
}

.connectivity-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.connectivity-reason {
  color: #6b7280;
  font-size: 12px;
}

.connectivity-more {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.actions {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
}

.dirty-alert {
  margin-bottom: 16px;
}

.hidden-file-input {
  display: none;
}

.version-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.version-filter {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 260px auto;
  gap: 10px;
  margin-bottom: 10px;
  align-items: center;
}

.version-filter-summary {
  margin-bottom: 10px;
  color: #6b7280;
  font-size: 13px;
}

.version-expanded {
  padding: 12px 18px;
  background: #f8fafc;
}

.version-name {
  font-weight: 700;
  color: #1f2937;
  line-height: 1.4;
}

.version-brief {
  margin-top: 4px;
  max-width: 260px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-meta {
  color: #374151;
  line-height: 1.4;
}

.version-time {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.version-remark {
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
}

.version-tip {
  margin-top: 14px;
}

.quality-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  :deep(.el-statistic) {
    padding: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    background: #f8fafc;
  }
}

.quality-toolbar {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.issue-title {
  font-weight: 700;
  color: #1f2937;
}

.issue-desc {
  margin-top: 4px;
  color: #6b7280;
  line-height: 1.5;
}

.issue-distance {
  color: #909399;
}

.import-preview {
  display: grid;
  gap: 14px;
}

.import-warning {
  margin-top: 2px;
}

.import-risk-list {
  display: grid;
  gap: 10px;
}

.risk-title {
  font-weight: 700;
  color: #1f2937;
}

.version-diff {
  min-height: 220px;
  display: grid;
  gap: 16px;
}

.diff-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.diff-card {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fbfdff;

  h3 {
    margin: 0 0 10px;
    font-size: 15px;
    color: #1f2937;
  }

  &.full {
    grid-column: 1 / -1;
  }
}

.map-panel {
  min-height: calc(100vh - 120px);
  padding: 16px;
  overflow: auto;
}

.empty-map {
  height: 520px;
  display: grid;
  place-items: center;
  color: #909399;
  background: #f7f9fc;
  border-radius: 6px;
}

.editor-svg {
  display: block;
  width: 100%;
  min-width: 960px;
  height: auto;
  background: #f7f9fc;
  user-select: none;
}

.edges line {
  stroke: #409eff;
  stroke-width: 8;
  stroke-linecap: round;
  opacity: 0.78;
  cursor: pointer;
  transition: stroke 0.2s ease, stroke-width 0.2s ease, opacity 0.2s ease;

  &.selected {
    stroke: #f56c6c;
    stroke-width: 12;
    opacity: 1;
  }
}

.route-debug-edges {
  pointer-events: none;

  line {
    stroke: #ef4444;
    stroke-width: 11;
    stroke-linecap: round;
    opacity: 0.96;
    filter: drop-shadow(0 2px 4px rgba(239, 68, 68, 0.35));
  }
}

.route-debug-markers {
  pointer-events: none;
}

.route-debug-marker {
  circle {
    fill: #ef4444;
    stroke: #fff;
    stroke-width: 4;
  }

  text {
    font-size: 20px;
    font-weight: 800;
    fill: #991b1b;
    paint-order: stroke;
    stroke: #fff;
    stroke-width: 5px;
  }

  &.endpoint circle {
    fill: #f97316;
  }
}

.connectivity-markers {
  pointer-events: none;
}

.connectivity-marker {
  circle {
    fill: rgba(249, 115, 22, 0.9);
    stroke: #fff;
    stroke-width: 5;
    filter: drop-shadow(0 3px 6px rgba(15, 23, 42, 0.3));
  }

  text {
    font-size: 22px;
    font-weight: 800;
    fill: #9a3412;
    paint-order: stroke;
    stroke: #fff;
    stroke-width: 6px;
  }

  &.isolated-poi circle {
    fill: rgba(239, 68, 68, 0.92);
  }

  &.isolated-poi text {
    fill: #991b1b;
  }

  &.disconnected-poi circle {
    fill: rgba(249, 115, 22, 0.92);
  }

  &.orphan-route circle {
    fill: rgba(100, 116, 139, 0.92);
  }

  &.orphan-route text {
    fill: #334155;
  }
}

.preview-edges {
  pointer-events: none;

  line {
    stroke: #f97316;
    stroke-width: 7;
    stroke-linecap: round;
    stroke-dasharray: 18 12;
    opacity: 0.9;
  }
}

.version-preview-edges line {
  stroke: #8b5cf6;
}

.preview-nodes {
  pointer-events: none;
}

.preview-node {
  circle {
    fill: #8b5cf6;
    stroke: #fff7ed;
    stroke-width: 4;
    opacity: 0.95;
  }

  text {
    font-size: 22px;
    font-weight: 700;
    fill: #581c87;
    paint-order: stroke;
    stroke: #fff7ed;
    stroke-width: 5px;
  }

  &.route circle {
    fill: #f97316;
  }
}

.version-preview-node {
  circle {
    fill: #7c3aed;
  }

  &.route circle {
    fill: #f59e0b;
  }
}

.diff-focus-edges {
  pointer-events: none;

  line {
    stroke: #ef4444;
    stroke-width: 10;
    stroke-linecap: round;
    stroke-dasharray: 14 8;
    opacity: 0.95;
  }
}

.diff-focus-markers {
  pointer-events: none;
}

.diff-focus-marker {
  circle {
    fill: #ef4444;
    stroke: #fff;
    stroke-width: 5;
    opacity: 0.96;
  }

  text {
    font-size: 22px;
    font-weight: 800;
    fill: #991b1b;
    paint-order: stroke;
    stroke: #fff;
    stroke-width: 6px;
  }

  &.version circle {
    fill: #7c3aed;
  }

  &.version text {
    fill: #4c1d95;
  }

  &.current circle {
    fill: #ef4444;
  }
}

.node {
  cursor: grab;

  circle {
    fill: #67c23a;
    stroke: #fff;
    stroke-width: 4;
    transition: fill 0.2s ease, stroke 0.2s ease, stroke-width 0.2s ease;
  }

  text {
    font-size: 24px;
    font-weight: 700;
    fill: #1f2937;
    paint-order: stroke;
    stroke: #fff;
    stroke-width: 5px;
    pointer-events: none;
  }

  &.route circle {
    fill: #64748b;
    r: 6;
  }

  &.selected circle {
    fill: #f59e0b;
    stroke: #f97316;
    stroke-width: 6;
  }
}

:deep(.selected-quality-row) {
  --el-table-tr-bg-color: #fff7ed;

  td {
    background: #fff7ed !important;
  }
}

@media (max-width: 980px) {
  .workspace {
    grid-template-columns: 1fr;
  }
}
</style>
