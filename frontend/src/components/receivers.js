// Shared routing helpers, used by both the Alert Details dialog and the Routes
// screen. Pure functions only -- no Vue, no axios -- so both callers can import
// them and they stay testable. Same precedent as timelineWindow.js.

// Receivers as plain names. The backend now sends alert.receiverNames already
// normalised, but the raw alert.receivers field is kept as a fallback: the two
// alertmanager api versions store it differently (v2 as {name}, v1 as a string)
// and both shapes are sitting in the database.
export function receiverNamesOf(item) {
  const alert = item && item.alert ? item.alert : null
  if (!alert) { return [] }
  if (Array.isArray(alert.receiverNames)) { return alert.receiverNames }
  if (!Array.isArray(alert.receivers)) { return [] }
  const names = []
  alert.receivers.forEach(r => {
    const name = (r && typeof r === 'object') ? r.name : r
    if (name && !names.includes(name)) { names.push(String(name)) }
  })
  return names.sort()
}

// Index every receiver by alertmanager AND name. Never by name alone: the
// alertmanagers have separate configs and could each define a different "bugle".
export function buildReceiverIndex(alertmanagers) {
  const index = {}
  ;(alertmanagers || []).forEach(am => {
    ;(am.receivers || []).forEach(r => {
      index[am.name + '::' + r.name] = r
    })
    index['__am::' + am.name] = am
  })
  return index
}

// Three outcomes that must stay distinguishable, because they mean very
// different things operationally:
//   ok          -- found it; actions may still be empty, which means alertmanager
//                  throws these notifications away
//   unknown     -- the alert names a receiver the current config does not define,
//                  normal for an old alert after a config change
//   unavailable -- we could not read that alertmanager's config at all
export function lookupReceiver(index, alertmanagerName, receiverName) {
  const am = index['__am::' + alertmanagerName]
  if (!am || !am.available) {
    return { name: receiverName, state: 'unavailable', actions: [] }
  }
  const receiver = index[alertmanagerName + '::' + receiverName]
  if (!receiver) {
    return { name: receiverName, state: 'unknown', actions: [] }
  }
  return { name: receiverName, state: 'ok', actions: receiver.actions || [] }
}

const ACTION_ICONS = {
  webhook: 'mdi-webhook',
  email: 'mdi-email-outline',
  slack: 'mdi-slack',
  pagerduty: 'mdi-phone-alert',
  opsgenie: 'mdi-shield-alert-outline',
  victorops: 'mdi-bullhorn-outline',
  pushover: 'mdi-cellphone-message',
  telegram: 'mdi-send-outline',
  discord: 'mdi-message-text-outline',
  msteams: 'mdi-microsoft-teams',
  webex: 'mdi-video-outline',
  wechat: 'mdi-wechat',
  sns: 'mdi-cloud-outline'
}

export function actionIcon(type) {
  return ACTION_ICONS[type] || 'mdi-bell-outline'
}

// Icon for a matcher operator, matching the silence dialog's existing idiom.
export function matcherIcon(op) {
  if (op == '=') { return 'mdi-equal-box' }
  if (op == '!=') { return 'mdi-code-not-equal' }
  return 'mdi-regex'
}

// Flatten the route tree into rows carrying their depth. A tree component is not
// an option (v-treeview is not in Vuetify 3.5 stable) and nested expansion panels
// get unreadable quickly, so the screen renders one indented list instead.
// Collapsed nodes simply stop the walk, so their subtree is never emitted.
export function flattenRoutes(route, collapsed) {
  const rows = []
  const walk = (node, depth) => {
    if (!node) { return }
    const children = node.routes || []
    const isCollapsed = collapsed && collapsed.has(node.id)
    rows.push({ node: node, depth: depth, hasChildren: children.length > 0, collapsed: isCollapsed })
    if (isCollapsed) { return }
    children.forEach(c => walk(c, depth + 1))
  }
  walk(route, 0)
  return rows
}

// Every route id in a tree, so the screen can expand or collapse the lot.
export function allRouteIds(route) {
  const ids = []
  const walk = n => {
    if (!n) { return }
    ids.push(n.id)
    ;(n.routes || []).forEach(walk)
  }
  walk(route)
  return ids
}
