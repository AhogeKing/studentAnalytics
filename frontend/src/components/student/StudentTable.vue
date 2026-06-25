<script setup lang="ts">
import { Delete, EditPen, View } from "@element-plus/icons-vue";
import type { Sort } from "element-plus";
import type { StudentOverviewItem } from "../../types";

defineProps<{
  students: StudentOverviewItem[];
  loading?: boolean;
  canEdit?: boolean;
}>();

const emit = defineEmits<{
  view: [student: StudentOverviewItem];
  edit: [student: StudentOverviewItem];
  delete: [student: StudentOverviewItem];
  sort: [value: Sort];
}>();

function gradeTagType(value: number | null | undefined) {
  if (value === 0) {
    return "success";
  }
  if (value === 4) {
    return "danger";
  }
  if (value === 3) {
    return "warning";
  }
  return "primary";
}
</script>

<template>
  <el-table
    v-loading="loading"
    class="student-table"
    :data="students"
    row-key="student_no"
    empty-text="暂无匹配学生"
    @sort-change="emit('sort', $event)"
    @row-dblclick="emit('view', $event)"
  >
    <el-table-column prop="studentNo" label="学号" width="112" sortable="custom">
      <template #default="{ row }">{{ row.student_no }}</template>
    </el-table-column>
    <el-table-column prop="name" label="姓名" min-width="116" sortable="custom" />
    <el-table-column prop="age" label="年龄" width="96" sortable="custom" />
    <el-table-column label="性别" width="96">
      <template #default="{ row }">{{ row.gender?.label || "-" }}</template>
    </el-table-column>
    <el-table-column label="班级" min-width="132">
      <template #default="{ row }">{{ row.class_info?.class_name || "-" }}</template>
    </el-table-column>
    <el-table-column prop="gpa" label="GPA" width="104" sortable="custom">
      <template #default="{ row }">{{ row.gpa == null ? "-" : Number(row.gpa).toFixed(2) }}</template>
    </el-table-column>
    <el-table-column prop="gradeClass" label="成绩等级" width="124" sortable="custom">
      <template #default="{ row }">
        <el-tag :type="gradeTagType(row.grade_class?.value)" effect="light">
          {{ row.grade_class?.label || "-" }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="更新时间" min-width="168">
      <template #default="{ row }">{{ row.update_time?.replace("T", " ") || "-" }}</template>
    </el-table-column>
    <el-table-column label="操作" width="184" fixed="right" align="right">
      <template #default="{ row }">
        <div class="table-actions">
          <el-tooltip content="查看详情" placement="top">
            <el-button circle :icon="View" @click.stop="emit('view', row)" />
          </el-tooltip>
          <el-tooltip v-if="canEdit" content="编辑" placement="top">
            <el-button circle type="primary" :icon="EditPen" @click.stop="emit('edit', row)" />
          </el-tooltip>
          <el-tooltip v-if="canEdit" content="删除" placement="top">
            <el-button circle type="danger" :icon="Delete" @click.stop="emit('delete', row)" />
          </el-tooltip>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>
