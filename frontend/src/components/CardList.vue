<template>
<div>
    <h4>{{title}}</h4>
        <div v-for="card in sortedCards" :key="card.id" class="spaced">
        <CardDetails v-bind:card="card"/>
    </div>
</div>
</template>

<script>
import { mapGetters } from 'vuex'
import CardDetails from './CardDetails.vue'

export default {
    name: "CardList",
    props: {
        cardType: String,
        title: String
    },
    methods: {
        ...mapGetters(["getCards"]),
    },
    computed: {
        sortedCards() {
            const filtered= this.getCards().filter(it => it.type === this.cardType)
            const byId = filtered.sort((c1,c2) => c1.id - c2.id)
            return byId.sort((c1,c2) => c2.votes - c1.votes)
        }

    },
    components: {
        CardDetails
    }
}
</script>

<style scoped>
h4 {
    text-align: center;
}
.spaced {
    margin-bottom: 5%;
}
</style>