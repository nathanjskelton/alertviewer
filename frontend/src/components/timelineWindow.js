// Zoom maths for the footer timeline graph.
//
// The graph always shows the last N minutes ending at "now". N is picked by the
// user from a ladder of stops running from five minutes up to whatever the
// backend still keeps, and everything else here -- bucket size, gridline
// spacing, label precision -- is derived from it so the graph stays readable at
// both ends of that range.

export const MIN_WINDOW_MINUTES = 5

// Fallback span used until the backend's retention arrives on the login
// response, and the cap we apply if it never does.
export const DEFAULT_RETENTION_MINUTES = 10080

// Candidate zoom stops, in minutes: 5m through 4 weeks. Clipped to the
// retention at runtime, since a span wider than the history mongo still holds
// would just be dead space on the right-hand side of the graph.
const STOPS = [5, 15, 30, 60, 180, 360, 720, 1440, 2880, 4320, 10080, 20160, 40320]

// Never draw more buckets than this. The graph is rarely wider than ~1200px, so
// past this point extra buckets are work that lands on a pixel a neighbour has
// already claimed.
const MAX_BUCKETS = 1200

// Bucket sizes, in seconds, from a second up to a day.
const BUCKET_STEPS_SEC = [1, 2, 5, 10, 15, 30, 60, 120, 300, 600, 900, 1800, 3600, 7200, 14400, 21600, 43200, 86400]

// Gridline spacings, in seconds, from a minute up to a week.
const GRID_STEPS_SEC = [60, 120, 300, 600, 900, 1800, 3600, 7200, 10800, 21600, 43200, 86400, 172800, 604800]

// Aim for no more than this many gridlines; fewer is fine.
const MAX_GRIDLINES = 10

function clampRetention(retentionMinutes) {
  const n = Number(retentionMinutes)
  if (!isFinite(n) || n <= 0) { return DEFAULT_RETENTION_MINUTES }
  return Math.max(n, MIN_WINDOW_MINUTES)
}

// The zoom stops actually offered for a given retention: the ladder clipped to
// it, with the retention itself as the final stop so "all of it" is always
// reachable even when it falls between two rungs.
export function zoomStops(retentionMinutes) {
  const cap = clampRetention(retentionMinutes)
  const out = STOPS.filter(m => m < cap)
  out.push(cap)
  return out
}

// Snap an arbitrary span to the nearest available stop, so a retention change
// cannot strand the graph on a width that is no longer offered.
export function nearestStop(minutes, retentionMinutes) {
  const stops = zoomStops(retentionMinutes)
  return stops.reduce((best, s) => Math.abs(s - minutes) < Math.abs(best - minutes) ? s : best, stops[0])
}

function smallestStep(steps, totalSec, maxCount) {
  for (let i = 0; i < steps.length; i++) {
    if (totalSec / steps[i] <= maxCount) { return steps[i] }
  }
  return steps[steps.length - 1]
}

export function bucketMs(windowMinutes) {
  return smallestStep(BUCKET_STEPS_SEC, windowMinutes * 60, MAX_BUCKETS) * 1000
}

export function gridMs(windowMinutes) {
  return smallestStep(GRID_STEPS_SEC, windowMinutes * 60, MAX_GRIDLINES) * 1000
}

// "5m", "3h", "2d 6h" -- used on the zoom control and in tooltips.
export function formatSpan(minutes) {
  const m = Math.round(minutes)
  if (m < 60) { return m + 'm' }
  if (m < 1440) {
    const h = Math.floor(m / 60)
    const rest = m % 60
    return h + 'h' + (rest > 0 ? ' ' + rest + 'm' : '')
  }
  const d = Math.floor(m / 1440)
  const restH = Math.round((m % 1440) / 60)
  return d + 'd' + (restH > 0 ? ' ' + restH + 'h' : '')
}

// How precise a timestamp has to be to distinguish two points in this window.
// Under an hour the seconds matter; over a day the clock time alone repeats.
// In UTC, like every time this tool shows: the backend and alertmanager both
// work in UTC, and a viewer's local clock would disagree with them.
export function formatStamp(ms, windowMinutes) {
  const at = new Date(ms)
  const hhmm = ('0' + at.getUTCHours()).slice(-2) + ':' + ('0' + at.getUTCMinutes()).slice(-2)
  if (windowMinutes <= 60) {
    return hhmm + ':' + ('0' + at.getUTCSeconds()).slice(-2)
  }
  if (windowMinutes <= 1440) { return hhmm }
  return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][at.getUTCDay()] + ' ' + hhmm
}
