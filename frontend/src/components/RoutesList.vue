<template>
  <v-app-bar height="40" class="mt-5" color="purple-lighten-5" dense dark flat>
    <div style="font-size: 14px; font-weight: bold; color: #333; margin-left: 15px;">
      ALERTMANAGER ROUTES
    </div>
    <v-spacer></v-spacer>
    <v-text-field v-model="search" density="compact" hide-details variant="solo" flat
        placeholder="filter by matcher or receiver" style="max-width: 300px;"
        prepend-inner-icon="mdi-magnify"></v-text-field>
    <v-btn size="small" variant="text" color="#333" @click="expandAll()" title="Expand every route">
      <v-icon>mdi-unfold-more-horizontal</v-icon>
    </v-btn>
    <v-btn size="small" variant="text" color="#333" @click="collapseAll()" title="Collapse every route">
      <v-icon>mdi-unfold-less-horizontal</v-icon>
    </v-btn>
    <v-btn size="small" variant="text" color="#333" @click="fetchData()" title="Reload from alertmanager">
      <v-icon>mdi-database-refresh</v-icon>
    </v-btn>
  </v-app-bar>

  <v-container fluid class="pt-2">

    <!-- The semantics people misremember, stated once rather than implied by layout. -->
    <v-alert density="compact" variant="tonal" color="purple" class="mb-3" style="font-size: 12px;">
      Child routes are tried in order and the <strong>first match wins</strong>. Evaluation only
      carries on to later siblings when a matched route sets <strong>continue</strong>. A route
      with no criteria matches everything that reaches it.
    </v-alert>

    <div v-if="loading" style="font-size: 12px; color: #777;">Loading…</div>
    <div v-if="!loading && alertmanagers.length == 0" style="font-size: 12px; color: #777;">
      No alertmanagers configured.
    </div>

    <v-card v-for="am in alertmanagers" :key="am.name" class="mb-4" variant="outlined">
      <v-card-item class="bg-purple-lighten-4 py-2">
        <v-card-title style="font-size: 14px;">
          <v-icon size="18" class="mb-1">mdi-server</v-icon> {{ am.name }}
          <v-chip v-if="am.stale" size="x-small" label color="orange" class="ml-2">stale</v-chip>
          <v-chip v-if="!am.available" size="x-small" label color="red" class="ml-2">unavailable</v-chip>
        </v-card-title>
      </v-card-item>

      <div v-if="am.error" class="pa-3" style="font-size: 12px; color: #b45309;">
        <v-icon size="16" color="#b45309">mdi-alert-outline</v-icon>
        {{ am.error }}
        <span v-if="am.route"> Showing the last configuration read successfully.</span>
      </div>

      <div v-if="!am.route && !am.error" class="pa-3" style="font-size: 12px; color: #777;">
        No routing configuration has been read from this alertmanager yet.
      </div>

      <!-- Routes. Flattened to an indented list rather than nested panels: the tree
           can be deep, and Vuetify 3.5 has no stable tree component. -->
      <div v-if="am.route" class="pa-3">
        <div style="font-size: 13px; font-weight: bold; color: #334155; margin-bottom: 6px;">Routes</div>

        <div v-for="row in visibleRows(am)" :key="row.node.id"
            class="route-row" :style="{ paddingLeft: (row.depth * 22 + 4) + 'px' }">

          <v-icon v-if="row.hasChildren" size="18" class="route-chevron"
              :class="{ 'route-chevron--open': !row.collapsed }"
              @click="toggle(am.name, row.node.id)">mdi-chevron-right</v-icon>
          <v-icon v-else size="18" color="transparent">mdi-chevron-right</v-icon>

          <!-- criteria -->
          <span v-if="row.node.matchers.length == 0">
            <v-chip size="x-small" label variant="tonal" color="grey">catch-all</v-chip>
          </span>
          <span v-else>
            <v-chip v-for="(m, i) in row.node.matchers" :key="i" size="x-small" label variant="tonal"
                class="mr-1" :color="matcherColor(m)">
              <v-icon start size="14">{{ matcherIcon(m.op) }}</v-icon>
              <span v-if="m.name"><strong>{{ m.name }}</strong>{{ m.op }}{{ m.value }}</span>
              <span v-else>{{ m.raw }}</span>
            </v-chip>
          </span>

          <v-icon size="16" color="#94a3b8" class="mx-1">mdi-arrow-right-thin</v-icon>
          <v-chip size="x-small" label variant="tonal" color="#2563EB" style="cursor: pointer;"
              title="Jump to this receiver" @click="openReceiver(am.name, row.node.receiver)">
            {{ row.node.receiver }}
          </v-chip>

          <v-chip v-if="row.node.continueOnMatch" size="x-small" label variant="tonal"
              color="orange" class="ml-1" title="Evaluation carries on to later sibling routes">
            <v-icon start size="14">mdi-arrow-down-bold-outline</v-icon>continue
          </v-chip>

          <v-chip v-for="iv in row.node.muteTimeIntervals" :key="'m'+iv" size="x-small" label
              variant="tonal" color="blue-grey" class="ml-1" title="Muted during this interval">
            <v-icon start size="14">mdi-volume-off</v-icon>{{ iv }}
          </v-chip>

          <!-- timings: always show what is set here, inherited only on request -->
          <span class="route-timings">
            <span v-for="t in timings(row.node)" :key="t.label"
                v-show="!t.inherited || showInherited"
                :style="{ opacity: t.inherited ? 0.55 : 1, fontStyle: t.inherited ? 'italic' : 'normal' }">
              {{ t.label }} {{ t.value }}<span v-if="t.inherited"> (inherited)</span>&nbsp;&middot;&nbsp;
            </span>
          </span>
        </div>

        <v-checkbox v-model="showInherited" hide-details density="compact" label="show inherited settings"
            style="font-size: 12px; max-height: 34px;" class="mt-1"></v-checkbox>
      </div>

      <!-- Receivers and what they actually do -->
      <div v-if="am.route" class="pa-3 pt-0">
        <div style="font-size: 13px; font-weight: bold; color: #334155; margin-bottom: 6px;">Receivers</div>
        <v-expansion-panels v-model="openPanels[am.name]" multiple class="group-panels">
          <v-expansion-panel v-for="r in visibleReceivers(am)" :key="r.name" :value="r.name">
            <v-expansion-panel-title class="group-header">
              <v-chip size="x-small" label variant="tonal" color="#2563EB" class="mr-2">{{ r.name }}</v-chip>
              <v-chip v-if="!r.used" size="x-small" label variant="tonal" color="grey" class="mr-2"
                  title="No route sends anything to this receiver">unused</v-chip>
              <span v-if="r.actions.length == 0" style="font-size: 12px; color: #b45309;">
                <v-icon size="16" color="#b45309">mdi-bell-off-outline</v-icon>
                no action configured &mdash; notifications sent here are discarded
              </span>
              <span v-else style="font-size: 12px; color: #64748b;">
                <v-icon v-for="(a, i) in r.actions" :key="i" size="16" class="mr-1">{{ actionIcon(a.type) }}</v-icon>
                {{ r.actions.length }} action<span v-if="r.actions.length != 1">s</span>
              </span>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <div v-if="r.actions.length == 0" style="font-size: 12px; color: #777;">
                Alertmanager accepts a receiver with no integrations. Anything routed here is
                silently dropped.
              </div>
              <div v-for="(a, i) in r.actions" :key="i" class="mb-2" style="font-size: 12px;">
                <v-icon size="16" color="#777">{{ actionIcon(a.type) }}</v-icon>
                <span style="font-weight: bold; color: #6b21a8;"> {{ a.type }}</span>
                <!-- deliberately not a link: these are internal endpoints, and a
                     redacted &lt;secret&gt; would make a nonsense href -->
                <span v-if="a.target"> &rarr; {{ a.target }}</span>
                <span v-if="a.sendResolved != null" style="color: #777;">
                  &nbsp;&middot; send_resolved: {{ a.sendResolved }}</span>
                <div v-if="Object.keys(a.details).length > 0" style="margin-left: 22px; color: #777;">
                  <span v-for="(v, k) in a.details" :key="k">{{ k }}: {{ v }}&nbsp;&nbsp;</span>
                </div>
              </div>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>

        <div v-if="am.timeIntervals.length > 0" style="font-size: 12px; color: #777; margin-top: 10px;">
          Time intervals defined: {{ am.timeIntervals.join(', ') }}
        </div>
      </div>
    </v-card>
  </v-container>
</template>

<script>
  import axios from 'axios';
  import { flattenRoutes, allRouteIds, actionIcon, matcherIcon } from './receivers';

  export default {
    name: 'RoutesList',
    props: {
      cortana_token: String,
      cortana_user: String,
      cortana_role: String,
    },
    emits: ['alert'],
    data() {
      return {
        alertmanagers: [],
        loading: false,
        search: '',
        showInherited: false,
        // collapsed route ids, per alertmanager
        collapsed: {},
        openPanels: {},
      }
    },
    watch: {
      // Only ResultList logs in, so the token can arrive after this screen mounts.
      // Watching it is exact where the other screens guess with a timeout.
      cortana_token: {
        immediate: true,
        handler(token) {
          if (token) { this.fetchData(); }
        }
      }
    },
    methods: {
      fetchData() {
        if (!this.cortana_token) { return; }
        this.loading = true;
        axios
          .get(this.baseUrl + "routes", {headers: {"CORTANA-TOKEN": this.cortana_token}})
          .then(response => {
            this.alertmanagers = response.data.payload.alertmanagers || [];
            //seed the per-alertmanager maps so v-model never binds to undefined
            this.alertmanagers.forEach(am => {
              if (this.collapsed[am.name] == null) { this.collapsed[am.name] = new Set(); }
              if (this.openPanels[am.name] == null) { this.openPanels[am.name] = []; }
            });
            this.loading = false;
          })
          .catch(error => {
            this.handleError(error);
            this.loading = false;
          });
      },
      handleError(error) {
        this.$emit("alert", error, "error");
      },
      actionIcon(type) { return actionIcon(type); },
      matcherIcon(op) { return matcherIcon(op); },
      matcherColor(m) {
        if (m.op == '!=' || m.op == '!~') { return 'red'; }
        if (m.op == '=~') { return 'purple'; }
        return 'green';
      },
      rowsFor(am) {
        const collapsed = this.collapsed[am.name] || new Set();
        return flattenRoutes(am.route, collapsed);
      },
      // Filtering ignores collapse state so a match can never hide inside a
      // collapsed branch; matching on matcher text or receiver name.
      visibleRows(am) {
        const term = (this.search || '').trim().toLowerCase();
        if (term == '') { return this.rowsFor(am); }
        return flattenRoutes(am.route, new Set()).filter(row => this.rowMatches(row.node, term));
      },
      rowMatches(node, term) {
        if ((node.receiver || '').toLowerCase().includes(term)) { return true; }
        return (node.matchers || []).some(m => (m.raw || '').toLowerCase().includes(term));
      },
      visibleReceivers(am) {
        const term = (this.search || '').trim().toLowerCase();
        if (term == '') { return am.receivers; }
        return am.receivers.filter(r =>
          r.name.toLowerCase().includes(term) ||
          (r.actions || []).some(a => (a.target || '').toLowerCase().includes(term)));
      },
      toggle(amName, id) {
        const set = new Set(this.collapsed[amName] || []);
        if (set.has(id)) { set.delete(id); } else { set.add(id); }
        this.collapsed[amName] = set;
      },
      expandAll() {
        this.alertmanagers.forEach(am => { this.collapsed[am.name] = new Set(); });
      },
      collapseAll() {
        this.alertmanagers.forEach(am => {
          //everything except the root, which would otherwise hide the whole tree
          const ids = allRouteIds(am.route).filter(id => id != (am.route ? am.route.id : null));
          this.collapsed[am.name] = new Set(ids);
        });
      },
      openReceiver(amName, receiverName) {
        const open = (this.openPanels[amName] || []).slice();
        if (!open.includes(receiverName)) { open.push(receiverName); }
        this.openPanels[amName] = open;
      },
      groupByLabel(groupBy) {
        if (groupBy == null || groupBy.length == 0) { return 'all alerts together'; }
        if (groupBy.length == 1 && groupBy[0] == '...') { return 'every label'; }
        return groupBy.join(', ');
      },
      // Only the settings whose inheritance is well established across alertmanager
      // versions. Mute/active intervals are shown where set, never as inherited.
      timings(node) {
        return [
          //an empty group_by is not "no grouping": alertmanager puts every alert
          //into a single group. '...' is its shorthand for grouping by every label.
          { label: 'group by', value: this.groupByLabel(node.groupBy), inherited: !!node.inherited.groupBy },
          { label: 'wait', value: node.groupWait, inherited: !!node.inherited.groupWait },
          { label: 'interval', value: node.groupInterval, inherited: !!node.inherited.groupInterval },
          { label: 'repeat', value: node.repeatInterval, inherited: !!node.inherited.repeatInterval },
        ].filter(t => t.value != null && t.value !== '');
      },
    }
  }
</script>

<style scoped>
.route-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding-top: 3px;
  padding-bottom: 3px;
  border-left: 1px solid #e5e9f0;
  font-size: 12px;
}
.route-row:hover {
  background: #f8fafc;
}
.route-chevron {
  color: #64748b;
  cursor: pointer;
  transition: transform 0.2s ease;
}
.route-chevron--open {
  transform: rotate(90deg);
}
.route-timings {
  color: #94a3b8;
  margin-left: 8px;
  font-size: 11px;
}
:deep(.group-panels .v-expansion-panel) {
  background: transparent;
  margin-top: 4px;
}
:deep(.group-panels .v-expansion-panel::after) {
  border: none;
}
:deep(.group-header) {
  min-height: 36px !important;
  padding: 4px 12px !important;
  background: #f8fafc;
  border: 1px solid #e5e9f0;
  border-radius: 6px;
}
</style>
