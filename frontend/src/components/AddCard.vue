<template>
    <div>
        <el-row :gutter="15">
            <el-form v-model="form" :inline="true">
                <el-col :span="5">
                    <el-select v-model="form.type" placeholder="Card type" prop="type" size="small">
                        <el-option label="POSITIVE" value="POSITIVE"></el-option>
                        <el-option label="NEGATIVE" value="NEGATIVE"></el-option>
                        <el-option label="APPRECIATION" value="APPRECIATION"></el-option>
                        <el-option label="IDEA" value="IDEA"></el-option>
                        <el-option label="OTHER" value="OTHER"></el-option>
                        <el-option label="ACTION" value="ACTION"></el-option>
                    </el-select>
                </el-col>
                <el-col :span="15">
                        <el-input placeholder="I'd like to say..." v-model="form.text" @keyup.enter.native="submitNewRetroCard" size="small"></el-input>
                </el-col>
                <el-col :span="4">
                    <el-button type="primary" @click="submitNewRetroCard" size="small">Add</el-button>
                </el-col>
            </el-form>
        </el-row>
    </div>
</template>

<script>
export default {
    name: "AddCard",
    data() {
        return {
            // labelWidth: "120px",
            dialogVisible: false,
            form: {
                text: '',
                type: ''
            }
        }
    },
    methods: {
        submitNewRetroCard() {
            if (this.form.type == '' || this.form.text.length < 3) {
                return;
            }
            const msg = {
                'type': 'CARD',
                'body': {
                    'type': this.form.type,
                    'text': this.form.text
                }
            }
            this.$socket.send(JSON.stringify(msg))
            this.dialogVisible = false;
            this.form.text = '';
            this.form.type = '';
        }
    }
}
</script>

<style scoped>
.wide {
    width: 100%;
}
</style>