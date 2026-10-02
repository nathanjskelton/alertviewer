<template>
  <v-app>
    <!-- The classification banner has to stay on screen. It used to sit in normal
         flow, so on any page long enough to scroll it slid away and page content
         showed through the strip above the toolbars. Fixed at the top instead,
         under the app bars (z 1008/1010) so they still overlap it exactly as
         before, with a spacer below keeping the rest of the layout where it was. -->
    <v-container fluid class="ma-0 pa-0 banner-fixed">

    <v-row dense >
      <v-col class="ma-0 pa-0">
      <v-card class="pa-0 d-flex align-center justify-center" height="30px" flat color="red lighten-2">
        <span>{{ banner }}</span></v-card>
      </v-col>
    </v-row>
    
    </v-container>
    <div style="height: 30px;"></div>
    <app-bar :cortana_user=getUser() :cortana_role=getRole() />
    <default-view @alertManagerStatus="setAlertManagers" @alertIntervals="setAlertIntervals"
      :timeline_selection="timelineSelection" @closeTimeline="timelineSelection = null"
      @alerts="setAlerts" @alert="setAlert" @banner="setBanner" @retention="setRetention" @status="setStatus" @version="setVersion" @lastIngest="setLastIngest" @user="setUser" @role="setRole" />
    
    <v-footer app class="ma-0 pa-0">
      <v-container fluid class="ma-0 pa-0">
      <v-row dense class="ms-2 me-2 mb-0 mt-0 pa-0">
        <v-col class="ma-0 pa-0">
        <v-alert v-bind:key="item.message" v-for="item in alerts" v-model="alerts" transition="fade-transition" :type="item.type" rounded>{{ item.message }}</v-alert>
        <v-alert v-model="showAlert" transition="fade-transition" :type="alertType" rounded>{{ alert }}</v-alert>
        </v-col>
      </v-row>
      <!-- Firing history over the zoom control's span, coloured by worst
           severity. The zoom only resizes this graph; scrubbing inside it still
           drives the gantt.

           Alerts page only: it is a graph of the alert list, it is fed by that page's
           polling, and the gantt it scrubs is drawn there too. On the other pages
           there is nothing behind it and nothing keeping it current. -->
      <v-row v-if="onAlertsPage" dense class="flex-nowrap align-center">
        <v-col cols="auto" class="ma-0 pa-0">
        <alert-timeline-zoom v-model="timelineZoom" :retention-minutes="retentionMinutes" />
        </v-col>
        <v-col class="ma-0 pa-0">
        <alert-timeline :intervals="alertIntervals" :window-minutes="timelineZoom" v-model:selection="timelineSelection" />
        </v-col>
      </v-row>
      <v-row dense >
        <v-col class="ma-0 pa-0">
        <!-- Every part of this -- the status message, the age of the data, the
             alertmanager health -- comes from the poll the alerts page runs, so off that
             page it would only ever show how things stood when the page was left. Say
             that instead. -->
        <v-card class="pa-0" height="30px" flat color="grey lighten-2"><v-card-text class="pt-1 ps-5">
          <template v-if="onAlertsPage">
          {{ status }} &nbsp;&nbsp; Data is {{ lastIngest }} seconds old. <span style="padding-left: 20px;" v-for="item, key in alertManagerStatus">
            <v-icon color=red v-if="lastIngest != 'MANY' && item==false" class="mb-1">mdi-alert-circle</v-icon>
            <v-icon color=#339933 v-if="lastIngest != 'MANY' && item==true" class="mb-1">mdi-check-circle</v-icon>
            <v-icon color=red v-if="lastIngest == 'MANY'" class="mb-1">mdi-help-circle</v-icon>  {{key}}</span>
          </template>
          <template v-else>
          Polling paused<span v-if="version"> - version {{ version }}</span>
          </template></v-card-text></v-card>
        </v-col>
      </v-row>
      <!--      
      <v-row dense >
        <v-col class="ma-0 pa-0">
        <v-card class="pa-0 d-flex align-center justify-center" height="30px" flat color="red lighten-2">
          <span>test</span></v-card>
        </v-col>
      </v-row>
    -->
      </v-container>
    </v-footer>
    
  </v-app>
</template>



<script setup>
  
  import AppBar from './AppBar.vue'
  import DefaultView from './View.vue'
  import AlertTimeline from '@/components/AlertTimeline.vue'
  import AlertTimelineZoom from '@/components/AlertTimelineZoom.vue'
  import { DEFAULT_RETENTION_MINUTES, nearestStop } from '@/components/timelineWindow'
  import axios from "axios";
  import { computed, ref } from 'vue'
  import { useRoute } from 'vue-router'

  import { getCurrentInstance } from 'vue'

  const { proxy } = getCurrentInstance();
  const bannerText = proxy.$bannerText;

  const alert = ref('');
  const user = ref('');
  const role = ref('');
  const status = ref('Initializing...');
  const lastIngest = ref(0);
  const alerts = ref([]);
  const alertType = ref('success');
  const showAlert = ref(false);
  const banner = ref('');
  
  const alertManagerStatus = ref('');

  //the build version, kept from the last poll so the footer can still name it once the
  //alerts page and its polling have gone
  const version = ref('');

  //Every route here shares one path and differs only by name, so the name is what says
  //which page is on screen. The alerts page is the only one that polls.
  const route = useRoute();
  const onAlertsPage = computed(() => route.name == 'Results');

  // The graph opens showing everything the backend still holds; the zoom
  // control narrows it from there. Both are minutes.
  const retentionMinutes = ref(DEFAULT_RETENTION_MINUTES);
  const timelineZoom = ref(DEFAULT_RETENTION_MINUTES);
  const alertIntervals = ref([]);
  const timelineSelection = ref(null);

  function getUser() {
    return user;
  }

  function getRole() {
    return role;
  }

  function getBanner() {
    return banner;
  }

  function setAlert(x, t) {
    alert.value = x;
    alertType.value = t; 
    showAlert.value = false;
    setTimeout(() => {showAlert.value = true}, 250);
    setTimeout(() => {showAlert.value = false}, 4000);
  }

  function setAlerts(x) {
    alerts.value = x;
  }
  
  function setStatus(x) {
    status.value = x;
  }

  function setVersion(x) {
    if (x) { version.value = x; }
  }

  function setBanner(x) {
    banner.value = x;
    console.log("Default setBanner: "+x);
  }

  function setRetention(x) {
    const minutes = Number(x);
    if (!isFinite(minutes) || minutes <= 0) { return; }
    const wasAtMax = timelineZoom.value == retentionMinutes.value;
    retentionMinutes.value = minutes;
    // Keep "showing everything" meaning everything once the real retention
    // lands, and otherwise snap to a stop the new ladder actually offers.
    timelineZoom.value = wasAtMax ? minutes : nearestStop(timelineZoom.value, minutes);
  }

  function setLastIngest(x) {
    lastIngest.value = x;
  }

  function setUser(x) {
    user.value = x;
    console.log("Default setUser: "+x);
  }

  function setRole(x) {
    role.value = x;
  }

  function setAlertIntervals(x) {
    alertIntervals.value = x;
  }

  function setAlertManagers(ams) {
    let txt = "";

    alertManagerStatus.value = ams;
  }
</script>

<style>
.banner-fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1005;
}
</style>
