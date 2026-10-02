<template>
  <div class="gantt">
    <div class="gantt__head">
      <v-icon size="small" class="gantt__head-icon">mdi-chart-timeline</v-icon>
      <span class="gantt__title">Firing {{ formatStamp(start) }} &ndash; {{ formatStamp(end) }} UTC</span>
      <span class="gantt__sub">{{ spanLabel }} &middot; {{ rows.length }} alert{{ rows.length == 1 ? '' : 's' }}</span>
      <v-spacer></v-spacer>
      <!-- only worth offering when the window actually holds some -->
      <v-switch v-if="suppressedCount > 0" v-model="showSuppressed" hide-details density="compact"
          color="blue-grey" class="gantt__suppressed-toggle"
          :label="(showSuppressed ? 'Hide' : 'Show') + ' ' + suppressedCount + ' suppressed'"></v-switch>
      <v-btn size="small" variant="tonal" color="purple" prepend-icon="mdi-close" @click="$emit('close')">
        Close
      </v-btn>
    </div>

    <div class="gantt__axis">
      <div class="gantt__axis-track">
        <span v-for="tick in ticks" :key="'t-' + tick.ms" class="gantt__axis-label" :style="{ left: tick.pct + '%' }">
          {{ tick.label }}
        </span>
      </div>
    </div>

    <div v-if="rows.length == 0" class="gantt__empty">
      <template v-if="suppressedCount > 0">
        Nothing was firing in the selected window except {{ suppressedCount }} suppressed alert{{ suppressedCount == 1 ? '' : 's' }}.
      </template>
      <template v-else>
        No alerts were firing in the selected window.
      </template>
    </div>

    <div v-else class="gantt__body">
      <div class="gantt__rows">
        <div class="gantt__grid">
          <span v-for="tick in ticks" :key="'g-' + tick.ms" class="gantt__gridline" :style="{ left: tick.pct + '%' }"></span>
        </div>
        <div v-for="row in rows" :key="row.id" class="gantt__row" :class="{ 'gantt__row--suppressed': row.suppressed }">
          <div class="gantt__label">
            <!-- one icon per reason it was off the board, since a row can be silenced,
                 acked and ticketed across the window and "zzz" only says the first -->
            <v-icon v-for="reason in row.reasons" :key="'r-' + row.id + '-' + reason"
                size="14" color="blue-grey" class="gantt__suppressed-icon"
                :title="reasonTitle(reason)">{{ reasonIcon(reason) }}</v-icon>
            <span v-if="!row.suppressed" class="gantt__dot" :style="{ background: severityColor(row.severity) }"></span>
            <span class="gantt__name" :title="row.alertname">{{ row.alertname }}</span>
            <span class="gantt__env" :title="row.instance">{{ row.environment || '—' }}</span>
          </div>
          <div class="gantt__track">
            <div
              class="gantt__bar"
              :class="{ 'gantt__bar--open-left': row.clippedLeft, 'gantt__bar--open-right': row.clippedRight }"
              :style="{ left: row.leftPct + '%', width: row.widthPct + '%', background: severityColor(row.severity) }"
              :title="barTitle(row)"
            ></div>
            <!-- the stretches it was off the board for, laid over the bar so the same
                 row shows what was notifying and what was not -->
            <div
              v-for="(quiet, i) in row.suppressedBars" :key="'q-' + row.id + '-' + i"
              class="gantt__bar gantt__bar--suppressed"
              :style="{ left: quiet.leftPct + '%', width: quiet.widthPct + '%' }"
              :title="reasonLabel(quiet.reasons) + ' ' + formatStamp(quiet.start) + ' – ' + (quiet.end == null ? 'still' : formatStamp(quiet.end))"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  const COLOR_CRITICAL = '#dc2626'
  const COLOR_WARNING = '#f59e0b'
  const COLOR_OTHER = '#94a3b8'

  // Candidate axis steps, in minutes, from a minute up to two days. The
  // timeline can be scrubbed across a whole week, so the top of this range has
  // to reach far enough that a wide selection still snaps to ~8 ticks.
  const STEPS = [1, 2, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880]

  // Why an alert was off the board, each with the icon the alert table uses for it, so
  // the same thing is recognisable in both. All in the same quiet colour as the rest of
  // a suppressed row: which of the three it was matters less than that it was one.
  const REASONS = {
    silenced: { icon: 'mdi-sleep', title: 'Silenced', label: 'Silenced' },
    acked: { icon: 'mdi-account-check', title: 'Acked', label: 'Acked' },
    jira: { icon: 'mdi-jira', title: 'Has a jira ticket', label: 'Ticketed' },
  }
  //the order they read in, rather than whichever happened to come first
  const REASON_ORDER = ['silenced', 'acked', 'jira']

  export default {
    name: 'AlertGantt',
    props: {
      // [{ id, alertname, environment, instance, severity, status, acked, jiraKey,
      // suppressed, suppressedWindows, start, end }]. end == null means the alert is
      // still firing.
      //
      // A silence, an ack and a jira ticket all take an alert off the board, so all
      // three arrive as suppressed stretches and are drawn the same way.
      items: {
        type: Array,
        default: () => []
      },
      start: { type: Number, required: true },
      end: { type: Number, required: true },
    },
    emits: ['close'],
    data() {
      return {
        //off by default: the window is for seeing what was actually firing
        showSuppressed: false,
      }
    },
    computed: {
      // rows off the board for the whole window, which are the ones hidden by default
      suppressedCount() {
        return this.items.filter(item => item.suppressed).length
      },
      span() {
        return Math.max(60000, this.end - this.start)
      },
      spanLabel() {
        const minutes = Math.round(this.span / 60000)
        if (minutes < 60) { return minutes + ' min' }
        const hours = Math.floor(minutes / 60)
        const rest = minutes % 60
        if (hours < 48) { return hours + 'h' + (rest > 0 ? ' ' + rest + 'm' : '') }
        const days = Math.floor(hours / 24)
        const restHours = hours % 24
        return days + 'd' + (restHours > 0 ? ' ' + restHours + 'h' : '')
      },
      // Once the selection covers more than a day the bare clock labels repeat,
      // so the axis switches to weekday-qualified ticks.
      multiDay() {
        return this.span > 24 * 3600000
      },
      // Aim for roughly eight labelled ticks, snapped to a readable step and
      // aligned to the clock rather than to the selection's ragged edge.
      ticks() {
        const minutes = this.span / 60000
        let step = STEPS[STEPS.length - 1]
        for (let i = 0; i < STEPS.length; i++) {
          if (minutes / STEPS[i] <= 8) { step = STEPS[i]; break }
        }
        const stepMs = step * 60000
        const out = []
        for (let ms = Math.ceil(this.start / stepMs) * stepMs; ms <= this.end; ms += stepMs) {
          out.push({
            ms: ms,
            pct: (ms - this.start) / this.span * 100,
            label: this.multiDay ? this.formatDayTime(ms) : this.formatTime(ms),
          })
        }
        return out
      },
      rows() {
        const now = Date.now()
        const visible = this.showSuppressed ? this.items : this.items.filter(item => !item.suppressed)
        return visible.map(item => {
          const from = Math.max(item.start, this.start)
          const to = Math.min(item.end == null ? now : item.end, this.end)
          const leftPct = (from - this.start) / this.span * 100
          const widthPct = Math.max(0.4, (Math.max(to, from) - from) / this.span * 100)
          const bars = this.suppressedBars(item, now)
          return Object.assign({}, item, {
            leftPct: leftPct,
            widthPct: Math.min(widthPct, 100 - leftPct),
            clippedLeft: item.start < this.start,
            clippedRight: (item.end == null ? now : item.end) > this.end,
            suppressedBars: bars,
            reasons: this.reasonsOf(bars),
          })
        })
      },
    },
    methods: {
      // Each suppressed stretch of one alert, clipped to the window on screen.
      suppressedBars(item, now) {
        const out = []
        const windows = item.suppressedWindows || []
        for (let i = 0; i < windows.length; i++) {
          const from = Math.max(windows[i].start, this.start)
          const to = Math.min(windows[i].end == null ? now : windows[i].end, this.end)
          if (to <= from) { continue }
          const leftPct = (from - this.start) / this.span * 100
          out.push({
            reasons: windows[i].reasons || [],
            start: windows[i].start,
            end: windows[i].end,
            leftPct: leftPct,
            widthPct: Math.min(Math.max(0.4, (to - from) / this.span * 100), 100 - leftPct),
          })
        }
        return out
      },
      severityColor(severity) {
        const s = (severity == null ? '' : String(severity)).toLowerCase()
        if (s == 'critical' || s == 'crit' || s == 'fatal' || s == 'emergency' || s == 'page') {
          return COLOR_CRITICAL
        }
        if (s == 'warning' || s == 'warn' || s == 'major' || s == 'minor') {
          return COLOR_WARNING
        }
        return COLOR_OTHER
      },
      // All in UTC, like every time this tool shows.
      formatTime(ms) {
        const at = new Date(ms)
        return ('0' + at.getUTCHours()).slice(-2) + ':' + ('0' + at.getUTCMinutes()).slice(-2)
      },
      formatDayTime(ms) {
        const at = new Date(ms)
        const day = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][at.getUTCDay()]
        // Midnight ticks carry the day on its own; the clock adds nothing.
        return at.getUTCHours() == 0 && at.getUTCMinutes() == 0 ? day : day + ' ' + this.formatTime(ms)
      },
      formatStamp(ms) {
        const at = new Date(ms)
        const sameDay = at.toISOString().slice(0, 10) == new Date().toISOString().slice(0, 10)
        return (sameDay ? '' : (at.getUTCMonth() + 1) + '/' + at.getUTCDate() + ' ') + this.formatTime(ms)
      },
      reasonIcon(reason) {
        return REASONS[reason].icon
      },
      reasonTitle(reason) {
        return REASONS[reason].title
      },
      // Every reason this row was off the board for any part of the window on screen.
      // Read off the stretches themselves rather than the alert's state today, which
      // would mislabel one acked through the window and unacked since.
      reasonsOf(bars) {
        const seen = []
        bars.forEach(bar => {
          (bar.reasons || []).forEach(reason => {
            if (REASONS[reason] != null && seen.indexOf(reason) < 0) { seen.push(reason) }
          })
        })
        return REASON_ORDER.filter(reason => seen.indexOf(reason) >= 0)
      },
      // "Acked", or "Silenced + acked" where a stretch was both at once.
      reasonLabel(reasons) {
        const named = REASON_ORDER.filter(r => (reasons || []).indexOf(r) >= 0)
        if (named.length == 0) { return 'Suppressed' }
        return named.map((r, i) => i == 0 ? REASONS[r].label : REASONS[r].label.toLowerCase()).join(' + ')
      },
      barTitle(row) {
        let text = row.alertname
        if (row.instance) { text += ' @ ' + row.instance }
        text += '\n' + (row.severity || 'unknown') + ' · ' + row.status
        if (row.reasons.length > 0) {
          text += '\n' + this.reasonLabel(row.reasons).toLowerCase() +
              (row.suppressed ? ' for the whole of this window' : ' for part of this window')
        }
        text += '\nfired ' + this.formatStamp(row.start)
        text += '\n' + (row.end == null ? 'still firing' : 'ended ' + this.formatStamp(row.end))
        if (row.summary) { text += '\n\n' + row.summary }
        return text
      },
    },
  }
</script>

<style scoped>
.gantt {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  margin-top: 4px;
  overflow: hidden;
}
.gantt__head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #eef2ff;
  border-bottom: 1px solid #c7d2fe;
}
.gantt__head-icon {
  color: #4f46e5;
}
.gantt__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.gantt__sub {
  font-size: 11px;
  color: #64748b;
}
/* The axis and every row share this label-column width so ticks, gridlines and
   bars all line up. */
.gantt__axis {
  display: flex;
  padding: 2px 10px 2px 0;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}
.gantt__axis-track {
  position: relative;
  flex: 1 1 auto;
  height: 14px;
  margin-left: 280px;
}
.gantt__axis-label {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  font-size: 9px;
  line-height: 14px;
  color: #94a3b8;
  white-space: nowrap;
}
.gantt__body {
  /* Fill what is left under the app bars, but never collapse to nothing on a
     short viewport. */
  max-height: calc(100vh - 300px);
  min-height: 140px;
  overflow-y: auto;
}
.gantt__rows {
  position: relative;
}
.gantt__grid {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 280px;
  right: 10px;
  pointer-events: none;
}
.gantt__gridline {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: #f1f5f9;
}
.gantt__row {
  position: relative;
  display: flex;
  align-items: center;
  height: 22px;
  border-bottom: 1px solid #f8fafc;
}
.gantt__row:hover {
  background: #f8fafc;
}
.gantt__label {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 280px;
  flex: 0 0 280px;
  padding-left: 8px;
  font-size: 11px;
  overflow: hidden;
}
.gantt__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex: 0 0 7px;
}
.gantt__name {
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gantt__env {
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 0 0 auto;
  max-width: 96px;
}
.gantt__track {
  position: relative;
  flex: 1 1 auto;
  height: 100%;
  margin-right: 10px;
}
.gantt__bar {
  position: absolute;
  top: 6px;
  height: 10px;
  border-radius: 3px;
  min-width: 2px;
}
/* Square the edge where the alert runs past the selected window. */
.gantt__bar--open-left {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  box-shadow: inset 3px 0 0 rgba(255, 255, 255, 0.55);
}
.gantt__bar--open-right {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  box-shadow: inset -3px 0 0 rgba(255, 255, 255, 0.55);
}
.gantt__bar--open-left.gantt__bar--open-right {
  box-shadow: inset 3px 0 0 rgba(255, 255, 255, 0.55), inset -3px 0 0 rgba(255, 255, 255, 0.55);
}
/* A suppressed alert was firing, but somebody had deliberately taken it off the
   board -- silenced, acked, or turned into a jira ticket. It has to stay visibly
   apart from live firing: faded, hatched rather than solid, and named in grey,
   so no one reads a silenced bar as something that wanted attention. */
.gantt__bar--suppressed {
  background: repeating-linear-gradient(
    45deg,
    #cbd5e1 0,
    #cbd5e1 3px,
    #f8fafc 3px,
    #f8fafc 6px
  );
  box-shadow: inset 0 0 0 1px rgba(100, 116, 139, 0.45);
}
.gantt__row--suppressed .gantt__name,
.gantt__row--suppressed .gantt__env {
  color: #94a3b8;
  font-style: italic;
}
.gantt__suppressed-icon {
  flex: 0 0 14px;
}
/* The switch sits in a flex header row built for chips and buttons. */
.gantt__suppressed-toggle {
  flex: 0 0 auto;
  margin-right: 12px;
}
.gantt__suppressed-toggle :deep(.v-label) {
  font-size: 12px;
  opacity: 1;
  color: #64748b;
  white-space: nowrap;
}
.gantt__empty {
  padding: 24px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
</style>
