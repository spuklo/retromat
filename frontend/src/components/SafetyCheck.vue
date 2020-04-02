<template>
  <el-dialog title="Safety check" 
    :visible.sync="dialogVisible"
    :show-close=false
    :close-on-click-modal=false
    :close-on-press-escape=false
    center>
    <span class="longer-text">"Regardless of what we discover, we understand and truly believe that everyone did the best job they could, given what they knew at the time, their skills and abilities, the resources available, and the situation at hand."</span>

    <el-form v-model="form">
        <el-form-item label="Safety check">
        <el-select v-model="form.safetyLevel" placeholder="Please specify your safety level">
          <el-option label="No problem, I'll talk about anything" value="5"></el-option>
          <el-option label="I'll talk about almost anything; a few things may be difficult" value="4"></el-option>
          <el-option label="I'll talk about some things, but others will be hard to say" value="3"></el-option>
          <el-option label="I'm not going to say much, I'll let others bring up issues" value="2"></el-option>
          <el-option label="I'll smile, claim everything is great and agree with managers" value="1"></el-option>
        </el-select>
      </el-form-item>
      </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button type="primary" @click="sendSafetyCheck">Confirm</el-button>
        </span>
  </el-dialog>
</template>

<script>
export default {
    name: "SafetyCheck",
    data() {
        return {
            dialogVisible: true,
            formLabelWidth:"120px",
            form: {
              safetyLevel: ''
            }
        }
    },
    methods: {
      sendSafetyCheck() {
        if (this.form.safetyLevel == '') {
          return;
        }
        const msg = {
          'type': 'SAFETY_LEVEL',
          'body': {
            'level': this.form.safetyLevel
          }
        }
        this.$socket.send(JSON.stringify(msg))
        this.dialogVisible = false;
      }
    }
}
</script>

<style scoped>
.longer-text {
    white-space: pre-wrap;
    word-break: keep-all;
}
</style>