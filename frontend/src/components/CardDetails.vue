<template>
    <div @dblclick="toggleVoted($event)"> 
        {{ card.votes}} <i v-if="voted" class="el-icon-star-on"></i>
        <i v-else class="el-icon-star-off"></i> {{ card.text }}
    </div>
</template>

<script>
import { mapActions , mapGetters } from 'vuex';

export default {
    name: "CardDetails",
    props: {
        card: {}
    },
    data() {
        return {
            voted: false
        }
    },
    methods: {
        ...mapActions(['handleVote']),
        ...mapGetters(['votesRemaining']),
        toggleVoted(event) {
            event.preventDefault();
            event.stopPropagation();
            if (this.votesRemaining() > 0 || this.voted) {
                this.voted = !this.voted;
                this.handleVote(this.voted)
                const msg = {
                    'type': 'VOTE',
                    'body': {
                        'id': this.card.id,
                        'vote': this.voted ? 1 : -1
                    }
                }
                this.$socket.send(JSON.stringify(msg))
            }
            return false;
        }
    }
};
</script>

<style scoped>

</style>