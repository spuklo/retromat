<template>
  <div id="app">
    <SafetyCheck/>
    <el-container>
      <el-header>
        <el-row type="flex" class="row-bg" justify="space-around">
          <el-col :span="12" class='el-col-center'><div class="grid-content bg-purple" ><Stats/></div></el-col>
          <el-col :span="12"><div class="grid-content bg-purple-light"><AddCard/></div></el-col>
        </el-row>
      </el-header>
      <el-main>
        <el-row type="flex" class="row-bg" justify="space-around">
          <el-col :span="11"><CardList cardType="POSITIVE"/></el-col>
          <el-col :span="11"><CardList cardType="NEGATIVE"/></el-col>
        </el-row>
        <el-row type="flex" class="row-bg" justify="space-around">
          <el-col :span="11"><CardList cardType="OTHER"/></el-col>
          <el-col :span="11"><CardList cardType="ACTION"/></el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import Stats from './components/Stats.vue'
import SafetyCheck from './components/SafetyCheck.vue'
import CardList from './components/CardList.vue'
import AddCard from './components/AddCard.vue'

export default {
  name: 'App',
  components: {
    Stats,
    SafetyCheck,
    CardList,
    AddCard
  }
}
</script>

<style>
body {
  font-family: "Helvetica Neue",Helvetica
}
.el-col-center {
  text-align: center;
}
</style>
