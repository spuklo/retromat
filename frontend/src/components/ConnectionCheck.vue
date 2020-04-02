<template>
    <el-dialog
            title=""
            :visible.sync="this.isDisconnected"
            :show-close=false
            :close-on-click-modal=false
            :close-on-press-escape=false
            width="75%">
        <h1>Hang on, looks like server is practicing social distancing...</h1>
    </el-dialog>
</template>

<script>
    import {mapGetters} from 'vuex';

    export default {
        name: "ConnectionCheck",
        computed: {
            ...mapGetters(['isDisconnected'])
        }
    }
</script>

<style scoped>
    h1 {
        text-align: center;
        white-space: pre-wrap;
        word-break: keep-all;
    }
</style>