<template>
  <div id="app">
    <ConnectionCheck/>
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
          <el-col :span="8"><CardList cardType="APPRECIATION" title="Appreciations"/></el-col>
          <el-col :span="8"><CardList cardType="POSITIVE" title="Positives"/></el-col>
          <el-col :span="8"><CardList cardType="NEGATIVE" title="Negatives"/></el-col>
        </el-row>
        <el-row type="flex" class="row-bg" justify="space-around">
          <el-col :span="8"><CardList cardType="IDEA" title="Ideas"/></el-col>
          <el-col :span="8"><CardList cardType="OTHER" title="Other business"/></el-col>
          <el-col :span="8"><CardList cardType="ACTION" title="Actions"/></el-col>
        </el-row>
      </el-main>
    </el-container>
    <footer class="footer ">
      Retromat v. {{getVersion}} <el-divider direction="vertical"></el-divider> <el-link :underline="false" href="/retro/pdf"><el-button size="small" type="info" icon="el-icon-download" circle></el-button></el-link>
    </footer>
  </div>
</template>

<script>
import Stats from './components/Stats.vue'
import SafetyCheck from './components/SafetyCheck.vue'
import CardList from './components/CardList.vue'
import AddCard from './components/AddCard.vue'
import ConnectionCheck from "./components/ConnectionCheck";
import {mapGetters} from 'vuex'

export default {
  name: 'App',
  components: {
    Stats,
    SafetyCheck,
    CardList,
    AddCard,
    ConnectionCheck
  },
  computed: {
    ...mapGetters(["getVersion"])
  }
}
</script>

<style>
html, body {
    height: 100%;
    font-family: "Helvetica Neue",Helvetica,serif
}
.el-col-center {
  text-align: center;
}
#app {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.footer {
  flex-shrink: 0;
  padding: 25px;
  text-align: center;
  font-size: small;
}
</style>
