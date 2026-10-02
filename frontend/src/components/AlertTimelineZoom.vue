<template>
  <div class="tl-zoom">
    <v-btn icon size="x-small" variant="text" :disabled="atMin" title="Zoom in" @click="step(-1)">
      <v-icon size="small">mdi-magnify-plus-outline</v-icon>
    </v-btn>

    <v-menu location="top">
      <template v-slot:activator="{ props }">
        <button class="tl-zoom__label" v-bind="props" title="Timeline span — click to pick">
          {{ formatSpan(modelValue) }}
        </button>
      </template>
      <v-list density="compact" class="tl-zoom__list">
        <v-list-item
          v-for="stop in stops"
          :key="stop"
          :active="stop == modelValue"
          @click="$emit('update:modelValue', stop)"
        >
          <v-list-item-title class="tl-zoom__item">
            {{ formatSpan(stop) }}<span v-if="stop == retentionMinutes" class="tl-zoom__note">all history</span>
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>

    <v-btn icon size="x-small" variant="text" :disabled="atMax" title="Zoom out" @click="step(1)">
      <v-icon size="small">mdi-magnify-minus-outline</v-icon>
    </v-btn>
  </div>
</template>

<script>
  import { zoomStops, formatSpan, DEFAULT_RETENTION_MINUTES } from './timelineWindow'

  export default {
    name: 'AlertTimelineZoom',
    props: {
      // Current graph span, in minutes.
      modelValue: { type: Number, required: true },
      // Widest span worth offering: the backend's resolved.remove.minutes.
      retentionMinutes: { type: Number, default: DEFAULT_RETENTION_MINUTES },
    },
    emits: ['update:modelValue'],
    computed: {
      stops() {
        return zoomStops(this.retentionMinutes)
      },
      index() {
        // The current span may sit between two stops if retention has just
        // changed, so fall back to the closest rung rather than -1.
        const exact = this.stops.indexOf(this.modelValue)
        if (exact >= 0) { return exact }
        let best = 0
        this.stops.forEach((s, i) => {
          if (Math.abs(s - this.modelValue) < Math.abs(this.stops[best] - this.modelValue)) { best = i }
        })
        return best
      },
      atMin() { return this.index <= 0 },
      atMax() { return this.index >= this.stops.length - 1 },
    },
    methods: {
      formatSpan,
      step(direction) {
        const next = this.index + direction
        if (next < 0 || next > this.stops.length - 1) { return }
        this.$emit('update:modelValue', this.stops[next])
      },
    },
  }
</script>

<style scoped>
.tl-zoom {
  display: flex;
  align-items: center;
  gap: 1px;
  padding-right: 4px;
  white-space: nowrap;
}
/* Fixed width so stepping through the stops does not shuffle the graph
   sideways as the label changes length. */
.tl-zoom__label {
  min-width: 46px;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  color: #334155;
  background: #e2e8f0;
  cursor: pointer;
}
.tl-zoom__label:hover {
  background: #cbd5e1;
}
.tl-zoom__item {
  font-size: 12px;
}
.tl-zoom__note {
  margin-left: 8px;
  font-size: 10px;
  color: #64748b;
}
</style>
