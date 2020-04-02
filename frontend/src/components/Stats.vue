<template>
    <div>
        <el-tag type="info">Users: {{getStats.sessions}}</el-tag>
        <el-tag type="danger">Min safety level: {{getStats.min_safety}}</el-tag>
        <el-tag type="warning">Avg safety level: {{getStats.avg_safety}}</el-tag>
        <el-tag type="info">Max safety level: {{getStats.max_safety}}</el-tag>
        <el-tag type="info">Votes remaining: {{votesRemaining}}</el-tag>
    </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
    name: 'Stats',
    computed: {
        ...mapGetters(["getStats", "votesRemaining"])
    }
}
</script>

<style scoped>
.el-tag {
    margin-left: 5px;
    margin-right: 5px;
}
</style>