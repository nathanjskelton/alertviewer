<template>
  <v-main >
    <router-view @alertManagerStatus="setAlertManagers" @alerts="setAlerts" @alert="setAlert"  @status="setStatus" @version="setVersion" 
        @token="setToken" @user="setUser" @role="setRole"  @lastIngest="setLastIngest" @banner="setBanner" @retention="setRetention"
        @alertIntervals="setAlertIntervals"
        :cortana_token=this.cortana_token :cortana_user=this.cortana_user :cortana_role=this.cortana_role
        :timeline_selection="timeline_selection" @closeTimeline="$emit('closeTimeline')" />
  </v-main>
</template>

<script>
  export default {
    props: {
      timeline_selection: Object,
    },
    data() {
      return {
        cortana_token: '',
        cortana_user: '',
        cortana_role: '',
      }
    },
    methods: {
      setAlert(alert, type) {
        this.$emit('alert', alert, type);
      },
      setAlerts(alerts) {
        this.$emit('alerts', alerts);
      },
      setStatus(status) {
        this.$emit('status', status);
      },
      setVersion(version) {
        this.$emit('version', version);
      },
      setBanner(banner) {
        this.$emit('banner', banner);
      },
      setRetention(retention) {
        this.$emit('retention', retention);
      },
      setToken(cortana_token) {
        this.cortana_token = cortana_token;
      },
      setUser(cortana_user) {
        this.cortana_user = cortana_user;
        this.$emit('user', cortana_user);
      },
      setLastIngest(lastIngest) {
        this.lastIngest = lastIngest;
        this.$emit('lastIngest', lastIngest);
      },
      setRole(cortana_role) {
        this.cortana_role = cortana_role;
        this.$emit('role', cortana_role);
      },
      setAlertManagers(ams) {
        this.$emit('alertManagerStatus', ams);
      },
      setAlertIntervals(intervals) {
        this.$emit('alertIntervals', intervals);
      },
    },
    emits: ['alerts','alert','status','version','token','user','role','banner','retention','alertManagerStatus','lastIngest','alertIntervals','closeTimeline'],
  }

</script>
