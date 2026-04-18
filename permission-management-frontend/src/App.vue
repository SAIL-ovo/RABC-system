<template>
  <div class="app-container">
    <router-view v-if="$route.meta.requiresAuth" />
    <router-view v-else />
  </div>
</template>

<script setup>
// App.vue 主组件
</script>

<style scoped>
.app-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}
</style>