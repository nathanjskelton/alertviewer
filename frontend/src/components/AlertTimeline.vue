<template>
  <div ref="wrap" class="alert-timeline" @mousemove="onMove" @mouseleave="onLeave" @mousedown="onDown">
    <canvas ref="canvas" class="alert-timeline__canvas"></canvas>
    <div v-if="tip" class="alert-timeline__tip" :style="{ left: tip.left + 'px' }">{{ tip.label }}</div>
  </div>
</template>

<script>
  import { markRaw } from 'vue'
  import { bucketMs, gridMs, formatSpan, formatStamp } from './timelineWindow'

  const HEIGHT = 26
  // Below this a drag is treated as a click (i.e. clear the selection).
  const DRAG_SLOP = 3

  // Redraw cadence, so the window keeps sliding even when no new data arrives.
  // Tied to the bucket size: at five minutes' zoom a bucket is a second wide and
  // a stale graph is obvious, at a week it is ten minutes and redrawing often
  // would be pointless work.
  const MIN_REFRESH_MS = 2000
  const MAX_REFRESH_MS = 30000

  const COLOR_CRITICAL = '#dc2626'
  const COLOR_WARNING = '#f59e0b'
  const COLOR_OTHER = '#94a3b8'

  export default {
    name: 'AlertTimeline',
    props: {
      // [{ start: <ms>, end: <ms|null for still firing>, severity: <string> }]
      intervals: {
        type: Array,
        default: () => []
      },
      // { start: <ms>, end: <ms> } scrubbed window, or null.
      selection: {
        type: Object,
        default: null
      },
      // How much history the graph covers, in minutes. Driven by the zoom
      // control; everything below is derived from it.
      windowMinutes: {
        type: Number,
        default: 1440
      }
    },
    emits: ['update:selection'],
    data() {
      return {
        tip: null,
        stats: null,
        drag: null,
      }
    },
    computed: {
      bucketMs() {
        return bucketMs(this.windowMinutes)
      },
      buckets() {
        return Math.round(this.windowMinutes * 60000 / this.bucketMs)
      },
      gridMs() {
        return gridMs(this.windowMinutes)
      },
      spanMs() {
        return this.buckets * this.bucketMs
      },
    },
    watch: {
      intervals: {
        handler() {
          this.rebuild()
        }
      },
      selection: {
        handler() {
          this.draw()
        }
      },
      windowMinutes: {
        handler() {
          // A selection made at one zoom level can fall entirely outside the
          // next, which would leave the gantt showing a window the graph no
          // longer draws. Drop it rather than leave the two disagreeing.
          const selection = this.selection
          if (selection != null) {
            const start = Date.now() - this.windowMinutes * 60000
            if (selection.end < start) {
              this.$emit('update:selection', null)
            }
          }
          this.restartTimer()
          this.rebuild()
        }
      }
    },
    mounted() {
      this.rebuild()
      if (typeof ResizeObserver !== 'undefined') {
        this.observer = markRaw(new ResizeObserver(() => this.draw()))
        this.observer.observe(this.$refs.wrap)
      } else {
        window.addEventListener('resize', this.draw)
      }
      this.restartTimer()
    },
    beforeUnmount() {
      if (this.observer) { this.observer.disconnect() } else { window.removeEventListener('resize', this.draw) }
      clearInterval(this.timer)
      this.detachDragListeners()
    },
    methods: {
      restartTimer() {
        clearInterval(this.timer)
        const every = Math.min(MAX_REFRESH_MS, Math.max(MIN_REFRESH_MS, this.bucketMs))
        this.timer = setInterval(() => this.rebuild(), every)
      },
      severityRank(severity) {
        let s = (severity == null ? '' : String(severity)).toLowerCase()
        if (s == 'critical' || s == 'crit' || s == 'fatal' || s == 'emergency' || s == 'page') {
          return 0
        }
        if (s == 'warning' || s == 'warn' || s == 'major' || s == 'minor') {
          return 1
        }
        return 2
      },
      // Bucket the firing intervals into one count per bucket, split by
      // severity class. Built with a difference array so cost is O(alerts +
      // buckets) rather than O(alerts * buckets-firing).
      rebuild() {
        const nowMs = Date.now()
        const size = this.bucketMs
        const count = this.buckets
        const lastBucket = Math.floor(nowMs / size)
        const firstBucket = lastBucket - (count - 1)

        // One extra slot so an interval ending on the last bucket can close.
        const diffs = [
          new Int32Array(count + 1),
          new Int32Array(count + 1),
          new Int32Array(count + 1),
        ]

        this.intervals.forEach(item => {
          if (item == null || item.start == null) { return }
          let from = Math.floor(item.start / size)
          let to = Math.floor((item.end == null ? nowMs : item.end) / size)
          if (to < from) { to = from }
          if (to < firstBucket || from > lastBucket) { return }
          if (from < firstBucket) { from = firstBucket }
          if (to > lastBucket) { to = lastBucket }
          const diff = diffs[this.severityRank(item.severity)]
          diff[from - firstBucket]++
          diff[to - firstBucket + 1]--
        })

        const counts = [new Int32Array(count), new Int32Array(count), new Int32Array(count)]
        let peak = 0
        for (let rank = 0; rank < 3; rank++) {
          let running = 0
          for (let i = 0; i < count; i++) {
            running += diffs[rank][i]
            counts[rank][i] = running
          }
        }
        for (let i = 0; i < count; i++) {
          const total = counts[0][i] + counts[1][i] + counts[2][i]
          if (total > peak) { peak = total }
        }

        this.stats = markRaw({ counts, peak, firstBucket, size, count })
        this.draw()
      },
      // Pixel <-> time helpers. The graph always covers
      // [firstBucket, +windowMinutes).
      windowStartMs() {
        return this.stats == null ? null : this.stats.firstBucket * this.stats.size
      },
      msToX(ms, width) {
        const from = this.windowStartMs()
        let x = (ms - from) / (this.stats.count * this.stats.size) * width
        if (x < 0) { x = 0 }
        if (x > width) { x = width }
        return x
      },
      indexAtX(x, width) {
        const count = this.stats.count
        let index = Math.floor(x / width * count)
        if (index < 0) { index = 0 }
        if (index > count - 1) { index = count - 1 }
        return index
      },
      // Gridlines on round clock times rather than on the window's ragged edge,
      // which is what lets the eye read "that spike was around midnight".
      // Anchored to UTC midnight because every step divides a day evenly, so
      // stepping from there keeps hour and day lines on the hour and the day.
      gridTicks(startMs, endMs) {
        const step = this.gridMs
        const anchor = new Date(startMs)
        anchor.setUTCHours(0, 0, 0, 0)
        // Jump straight to the first multiple inside the window rather than
        // stepping up to it -- at five minutes' zoom the anchor is most of a day
        // behind the window.
        const base = anchor.getTime()
        const first = base + Math.ceil((startMs - base) / step) * step
        const out = []
        for (let ms = first; ms <= endMs; ms += step) {
          const at = new Date(ms)
          let major
          if (step < 3600000) {
            major = at.getUTCMinutes() == 0
          } else if (step < 86400000) {
            major = at.getUTCHours() == 0
          } else {
            major = at.getUTCDay() == 1
          }
          out.push({ ms, major })
          if (out.length > 200) { break }
        }
        return out
      },
      draw() {
        const wrap = this.$refs.wrap
        const canvas = this.$refs.canvas
        if (wrap == null || canvas == null) { return }
        const width = wrap.clientWidth
        if (width <= 0) { return }

        const dpr = window.devicePixelRatio || 1
        canvas.width = Math.round(width * dpr)
        canvas.height = Math.round(HEIGHT * dpr)
        canvas.style.width = width + 'px'
        canvas.style.height = HEIGHT + 'px'

        const ctx = canvas.getContext('2d')
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
        ctx.clearRect(0, 0, width, HEIGHT)

        ctx.fillStyle = '#f1f5f9'
        ctx.fillRect(0, 0, width, HEIGHT)

        const stats = this.stats
        if (stats != null) {
          const startMs = this.windowStartMs()
          this.gridTicks(startMs, startMs + stats.count * stats.size).forEach(tick => {
            ctx.fillStyle = tick.major ? '#cbd5e1' : '#e5eaf0'
            ctx.fillRect(Math.round(this.msToX(tick.ms, width)), 0, 1, HEIGHT)
          })
        }

        if (stats == null || stats.peak == 0) {
          ctx.fillStyle = '#cbd5e1'
          ctx.fillRect(0, HEIGHT - 1, width, 1)
          if (stats != null) { this.drawSelection(ctx, width) }
          return
        }

        const counts = stats.counts
        for (let i = 0; i < stats.count; i++) {
          const total = counts[0][i] + counts[1][i] + counts[2][i]
          if (total == 0) { continue }
          const x0 = Math.floor(i / stats.count * width)
          const x1 = Math.max(x0 + 1, Math.floor((i + 1) / stats.count * width))
          const barHeight = Math.max(1, Math.round(total / stats.peak * (HEIGHT - 1)))
          ctx.fillStyle = counts[0][i] > 0 ? COLOR_CRITICAL : (counts[1][i] > 0 ? COLOR_WARNING : COLOR_OTHER)
          ctx.fillRect(x0, HEIGHT - barHeight, x1 - x0, barHeight)
        }

        this.drawSelection(ctx, width)

        // Scale hint, on a washed-out plate so bars behind it stay readable.
        const label = 'peak ' + stats.peak
        ctx.font = '9px sans-serif'
        const textWidth = ctx.measureText(label).width
        ctx.fillStyle = 'rgba(241, 245, 249, 0.8)'
        ctx.fillRect(2, 1, textWidth + 6, 11)
        ctx.fillStyle = '#64748b'
        ctx.textBaseline = 'top'
        ctx.fillText(label, 5, 2)
      },
      // Dim everything outside the scrubbed window and outline it. The drag in
      // progress wins over the committed selection so the band tracks the mouse.
      drawSelection(ctx, width) {
        if (this.stats == null) { return }
        const range = this.drag != null ? this.dragRange() : this.selection
        if (range == null) { return }

        const x0 = this.msToX(range.start, width)
        const x1 = this.msToX(range.end, width)
        if (x1 <= x0) { return }

        ctx.fillStyle = 'rgba(248, 250, 252, 0.66)'
        ctx.fillRect(0, 0, x0, HEIGHT)
        ctx.fillRect(x1, 0, width - x1, HEIGHT)

        ctx.fillStyle = 'rgba(37, 99, 235, 0.10)'
        ctx.fillRect(x0, 0, x1 - x0, HEIGHT)

        ctx.fillStyle = '#2563eb'
        ctx.fillRect(Math.max(0, x0 - 1), 0, 1, HEIGHT)
        ctx.fillRect(Math.min(width - 1, x1), 0, 1, HEIGHT)
      },
      dragRange() {
        if (this.drag == null || this.stats == null) { return null }
        const from = Math.min(this.drag.fromIndex, this.drag.toIndex)
        const to = Math.max(this.drag.fromIndex, this.drag.toIndex)
        return this.rangeForIndexes(from, to)
      },
      rangeForIndexes(from, to) {
        const windowStart = this.windowStartMs()
        const size = this.stats.size
        return {
          start: windowStart + from * size,
          end: windowStart + (to + 1) * size - 1,
        }
      },
      stamp(ms) {
        return formatStamp(ms, this.windowMinutes)
      },
      duration(ms) {
        const seconds = Math.round(ms / 1000)
        if (seconds < 60) { return seconds + ' sec' }
        return formatSpan(seconds / 60)
      },
      xInWrap(event) {
        return event.clientX - this.$refs.wrap.getBoundingClientRect().left
      },
      onDown(event) {
        if (event.button != 0 || this.stats == null) { return }
        const width = this.$refs.wrap.clientWidth
        if (width <= 0) { return }
        event.preventDefault()
        const x = this.xInWrap(event)
        const index = this.indexAtX(x, width)
        this.drag = { fromIndex: index, toIndex: index, fromX: x, moved: false }
        this.onDragMove = this.onDragMove || this.handleDragMove.bind(this)
        this.onDragUp = this.onDragUp || this.handleDragUp.bind(this)
        window.addEventListener('mousemove', this.onDragMove)
        window.addEventListener('mouseup', this.onDragUp)
      },
      handleDragMove(event) {
        if (this.drag == null) { return }
        const width = this.$refs.wrap.clientWidth
        if (width <= 0) { return }
        const x = this.xInWrap(event)
        if (Math.abs(x - this.drag.fromX) >= DRAG_SLOP) { this.drag.moved = true }
        this.drag.toIndex = this.indexAtX(x, width)
        const range = this.dragRange()
        this.tip = {
          left: Math.min(Math.max(x, 80), width - 80),
          label: this.stamp(range.start) + ' – ' + this.stamp(range.end) + ' UTC · ' +
            this.duration(range.end + 1 - range.start),
        }
        this.draw()
      },
      handleDragUp() {
        const drag = this.drag
        this.detachDragListeners()
        this.drag = null
        if (drag == null) { return }
        if (drag.moved) {
          const from = Math.min(drag.fromIndex, drag.toIndex)
          const to = Math.max(drag.fromIndex, drag.toIndex)
          this.$emit('update:selection', this.rangeForIndexes(from, to))
        } else if (this.selection != null) {
          // A plain click on the graph drops the current selection.
          this.$emit('update:selection', null)
        }
        this.tip = null
        this.draw()
      },
      detachDragListeners() {
        if (this.onDragMove) { window.removeEventListener('mousemove', this.onDragMove) }
        if (this.onDragUp) { window.removeEventListener('mouseup', this.onDragUp) }
      },
      onLeave() {
        if (this.drag == null) { this.tip = null }
      },
      onMove(event) {
        const stats = this.stats
        const wrap = this.$refs.wrap
        if (stats == null || wrap == null || this.drag != null) { return }
        const width = wrap.clientWidth
        if (width <= 0) { return }

        const x = this.xInWrap(event)
        const index = this.indexAtX(x, width)

        const critical = stats.counts[0][index]
        const warning = stats.counts[1][index]
        const other = stats.counts[2][index]
        const total = critical + warning + other

        let breakdown = []
        if (critical > 0) { breakdown.push(critical + ' critical') }
        if (warning > 0) { breakdown.push(warning + ' warning') }
        if (other > 0) { breakdown.push(other + ' other') }

        this.tip = {
          left: Math.min(Math.max(x, 80), width - 80),
          label: this.stamp((stats.firstBucket + index) * stats.size) + ' UTC · ' + total + ' firing' +
            (breakdown.length > 0 ? ' (' + breakdown.join(', ') + ')' : '') + ' — drag to inspect',
        }
      },
    },
  }
</script>

<style scoped>
.alert-timeline {
  position: relative;
  width: 100%;
  line-height: 0;
  user-select: none;
}
.alert-timeline__canvas {
  display: block;
  width: 100%;
  cursor: crosshair;
}
.alert-timeline__tip {
  position: absolute;
  bottom: 100%;
  transform: translateX(-50%);
  margin-bottom: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #334155;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}
</style>
