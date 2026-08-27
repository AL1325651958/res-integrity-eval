<script setup lang="ts">
import { computed } from 'vue'
import type { MenuItem } from '@/types'
import { collectLeaves, dirOf, menuFullPath, normPath } from '@/utils/menu'

const props = defineProps<{
  item: MenuItem
  /** 父级目录（M）的路径，用于计算子菜单项完整路径 */
  parentPath?: string
}>()

/** 目录（M）自身的路由路径：作为子菜单 index 与子项的 basePath */
const subMenuPath = computed(() => {
  const leaves = collectLeaves(props.item.children || [])
  if (!leaves.length) return ''
  return normPath(props.item.path) || normPath(dirOf(leaves[0]!.component))
})

/** 菜单项完整路由路径（后端 path 即为完整路径，与 router 注册一致） */
const itemIndex = computed(() => {
  if (props.item.menuType === 'C') {
    return menuFullPath(props.item)
  }
  return subMenuPath.value
})

/** 可渲染的子项（菜单，或含子级的目录） */
const visibleChildren = computed(() =>
  (props.item.children || []).filter((c) => c.menuType === 'C' || (c.children && c.children.length > 0))
)
</script>

<template>
  <!-- 叶子菜单 -->
  <el-menu-item v-if="item.menuType === 'C'" :index="itemIndex">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>

  <!-- 目录：递归渲染子菜单 -->
  <el-sub-menu v-else-if="visibleChildren.length" :index="subMenuPath">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <SidebarItem
      v-for="child in visibleChildren"
      :key="child.menuId"
      :item="child"
      :parent-path="subMenuPath"
    />
  </el-sub-menu>
</template>
