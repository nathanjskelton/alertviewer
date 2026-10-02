# Alert Viewer — User Guide

Alert Viewer is a web console for the alerts your Alertmanagers are firing. It keeps a
history of what happened, so an alert that resolved an hour ago is still there to look at,
and it gives you somewhere to work: acknowledge an alert, leave a note on it, silence it,
raise a ticket for it.

It reads from one or more Alertmanagers at once. Every alert says which one it came from.

---

## Getting started

Open the address your team publishes for Alert Viewer. There is no sign-in form — the page
knows who you are, and the top right of the purple bar shows it as `you:role`, with the
deployment's mode underneath. The coloured strip across the very top is the classification
banner for the environment you are looking at.

The menu button at the top left moves between the four pages:

| Page | What it is for |
|---|---|
| **Alerts** | Everything firing, resolved and silenced. Where you will spend your time. |
| **Silences** | The silences in force, coming up, and recently gone. |
| **Routes** | What each Alertmanager does with an alert: which route matches, which receiver gets it. |
| **Users** | Who can use Alert Viewer and at what role. Admins only. |

### Roles

Everyone can look at everything and can add notes and jira tickets. The rest needs the
**admin** role:

| Action | Role |
|---|---|
| Viewing alerts, silences, routes | any user |
| Adding a note, creating or linking a jira ticket | any user |
| Ack / unack an alert | admin |
| Creating or ending a silence, declaring an outage | admin |
| Deleting a resolved alert | admin |
| Rebuilding jira links, deleting orphaned alerts | admin |
| Managing users | admin |

The buttons are on screen either way. If your role does not allow an action, it fails and
the reason appears in the message strip at the bottom of the window.

---

## The Alerts page

### Reading a row

| Column | What it holds |
|---|---|
| ⋮ | The actions menu for that alert. |
| *(indicator)* | Status and everything attached to the alert — see the legend below. Click it for full details. |
| **Duration** | How long the alert has been firing, coloured by how recently it was last seen: red within the hour, orange earlier today, yellow yesterday, grey older. |
| **Severity** | As the alert is labelled. |
| **Environment** | An orange bar to the left means the environment came from the Alertmanager rather than the alert; a red bar and the word `legacy` mean the alert carries none. |
| **Alertname** | The service if the alert names one, otherwise the alert name. |
| **Instance** | The host or target. |
| **Team** | A red bar and `TEAM MISSING` means the alert carries no team, so nobody owns it. |
| **Summary** | The first line of the alert's summary. Turn on **extra labels** in the toolbar to show the rest of its labels as chips underneath. |

Rows firing in the last minute are highlighted red, fading out over the next ten minutes,
so anything new stands out. A row greyed out came from an Alertmanager that is currently
offline: what it says may be out of date.

### The indicator column

| Icon | Meaning |
|---|---|
| Red triangle in a ring | Firing. The ring reads how much of the alert's validity window is left — it thins and fades as the alert goes unconfirmed. |
| Red arrows | Flapping: it has been going on and off. |
| Grey `zzz` | Silenced. |
| Green tick | Resolved. |
| Orange person | Acked — somebody has taken it on. |
| Blue-grey clock | The Alertmanager it came from is offline; treat this row as stale. |
| Yellow note | The alert carries extra annotations. Click to read them. |
| Blue speech bubble | Somebody has left notes. Click to read them. |
| Green phone | A call-in alert. |
| Jira mark | A linked ticket. Click to open it in jira. Blue means the ticket is open, grey means it is closed or jira no longer has it. |

Clicking anywhere else in that cell opens the full details for the alert.

### Deciding what you see

The **FILTERS** drawer on the left is opened and closed with the arrow at the top left.

**Status** — `FIRING`, `RESOLVED`, `SILENCED`. The page opens on `FIRING` alone.

**Attributes**

- `ACKED` — an acked alert is out of the view whatever else it is doing. Tick this to bring
  acked alerts back.
- `JIRA TICKET` — likewise for alerts that already have a ticket.
- `FLAPPING` — include alerts that are flapping.
- `NOT CALLIN` — ticked (the default) shows everything. Untick it to see only call-in alerts.

**Group by Field** groups the table under a heading per value — by team, environment, or
any other label the alerts carry. Each group heading carries its own counts, the severities
inside it and how long its worst alert has been firing. The broom next to the selector
clears the grouping; **Expand None / First / All** in the toolbar say how much of it opens.

**Clear** puts the page back to firing and flapping alerts, ungrouped, with no filters.

### Filtering a column

Every column heading has a small funnel. Click it to filter on that column.

Severity and environment offer a list to tick. Alert name, instance, team and summary take
text, matched anywhere in the value:

| You type | You get |
|---|---|
| `disk` | anything whose value contains `disk` |
| `disk,cpu` | anything containing `disk` **or** `cpu` |
| `!test` | everything **except** values containing `test` |
| `disk,!test` | `disk`, but not where the value also contains `test` |

The funnel then tells you the filter's state at a glance: grey is off, green is a plain
match, blue means several terms, red means something is being excluded.

### Keeping the view current

The page refreshes its alerts every 15 seconds and checks the server's status every 5. The
**auto-refresh** switch turns that off when you want the screen to hold still; the
**Refresh** button turns orange to tell you a filter has changed and the table is waiting
for you to fetch it. **Rows per page** sets the page size of the table, or of each group.

The strip along the bottom says what the page last did, how old the data is, and whether
each Alertmanager answered — a green tick each, a red circle for one that did not. The
graph and the status line only run on the Alerts page; the other pages say
`Polling paused`, because nothing there is keeping them current.

### Acting on an alert

The ⋮ menu at the left of a row:

- **DETAILS** — the full picture, same as clicking the indicator.
- **ACK** / **UNACK** — mark that you have it in hand. Acked alerts drop out of the default
  view, and the ack is recorded as a note with your name on it.
- **SILENCE** — opens a silence already filled in from that alert: one matcher per label it
  carries, for 24 hours, on the Alertmanager it came from. Trim the matchers down to what
  you actually want covered before saving, since as it stands it matches only that alert.
- **JIRA** — link a ticket you already have by its key, or raise a new one. If the alert is
  already pointing at a ticket, the dialog says so, and creating a new one replaces it.
- **NOTE** — leave a note on the alert. Notes are kept with it and shown in the details.
- **DELETE** — only offered for a resolved alert, and it goes for good.

### What the details tell you

Alongside the alert's labels, annotations and notes, the details panel shows **what
Alertmanager did with it**: which receivers it was routed to, and what each of those
receivers actually does — email, webhook, and so on. A receiver with nothing configured is
called out, because anything sent there is silently dropped. It also shows how the alert
was grouped and the wait, interval and repeat times in force.

If jira is configured, the panel shows the label the ticket carries in jira, ready to copy
into a jira search.

### History, at a glance

The graph across the bottom of the Alerts page is the firing history of the alerts in your
current view, coloured by the worst severity in each slice. Time an alert spent silenced,
acked or ticketed is left out — it is a graph of what was actually notifying.

- The control to its left sets how far back it reaches, from minutes up to everything the
  server still keeps.
- **Drag across the graph** to pick a window. The table is replaced by a gantt chart of
  every alert that was firing in it, one bar each, oldest first. Hatched stretches on a bar
  are the times it was off the board, with an icon saying why — silenced, acked or
  ticketed. Alerts that were suppressed for the whole window are hidden behind a
  **Show suppressed** switch.
- **Close** on the gantt, or a single click on the graph, gives you the table back.

### Sharing what you are looking at

The address bar carries your filters, grouping, page size and switches. Copy the URL and
whoever opens it sees the same view.

---

## The Silences page

The list shows every silence the Alertmanagers know about, in the order you are most
likely to care about them: the ones in force, then the ones still to start, then the ones
recently gone. **Refresh** re-reads them.

| Column | What it holds |
|---|---|
| **State** | `active` in force now, `pending` not started yet, `expired` over. |
| **Comment** | What the silence was for, with its matchers as chips underneath. |
| **Creator** | Who made it. |
| **Alertmanager** | Which Alertmanager holds it. |
| **Timing** | `6h left` for an active silence, `starts in 3h 20m` for a pending one, `expired 2d 4h ago` for one that has gone. Hover for the exact moment in UTC. |

The matcher chips read the way Alertmanager writes them: `severity=critical`,
`team=~^(dba|network)$`, `alertname!~^(Watchdog)$`.

Expired silences are kept for as long as the Alertmanager keeps them, so a busy instance
shows a tail of recently expired ones. They are there to answer "was this silenced at the
time?" — and they have no delete button, since there is nothing left to stop.

### Making a silence

**New Silence** builds one from scratch: add matchers with the **+**, type a label name and
value into each, and use the icon between them to switch a matcher between *equals* and
*not equals*. Give it a comment, a length in hours, and the Alertmanager to put it on.

It is usually easier to start from the alert itself — **SILENCE** on the alert's ⋮ menu
fills all of this in for you.

### Declaring an outage

**Outage** is for planned work that will set off a lot of alerts at once. Name it, choose
the Alertmanager, set the start and end, and pick the environments and teams it covers.
The lists start as everything that Alertmanager has ever reported and everything is
selected, so it is quicker to remove what the outage does not cover than to pick out what
it does. One silence is created from the result.

**The start and end are UTC**, as the hint on the fields says. The dialog will not let you
save an outage that has already ended, or one with nothing left selected.

### Changing or ending a silence

The pencil reopens a silence so you can change it. The bin ends one early: a silence has no
"delete" as such, so this expires it now.

---

## The Routes page

This page answers "why did this alert go there, and where is *there*?" It shows, for each
Alertmanager, the routing tree as an indented list and every receiver underneath it.

Worth remembering while you read it, and stated at the top of the page: child routes are
tried in order and **the first match wins**. Evaluation only carries on to later routes
when a matched one sets `continue`. A route with no criteria matches everything that
reaches it.

Each row shows what the route matches, the receiver it sends to, and its timings. A
`continue` chip or a mute-interval chip appears where one is set. Ticking **show inherited
settings** adds the timings a route takes from its parent, in grey italics.

Under **Receivers**, each one opens to show what it actually does, and any receiver no
route sends to is marked `unused`. A receiver with no integrations configured is flagged:
Alertmanager accepts it, and quietly drops everything routed there.

The search box narrows the page to a matcher or receiver name. The two arrow buttons expand
and collapse the whole tree, and the refresh button re-reads the configuration.

An Alertmanager that could not be reached is marked `unavailable`, and `stale` means you
are being shown the last configuration that was read successfully.

---

## The Users page

Admins only. Each entry is a person's SID, their distinguished name, and their role —
`user` or `admin`. **New User** adds one, the pencil edits, the bin removes.

---

## Admin housekeeping

Two buttons live at the bottom of the Alerts page filter drawer, for admins.

**Rebuild Jira Links** re-reads every alert's ticket from its label in jira. Alerts whose
label turns up no ticket keep the link they have, unless you tick the box to clear them —
which also drops links added by hand, and cannot be undone.

**Delete Orphaned Alerts** only appears when there are some, with a count. These are alerts
from an Alertmanager that is no longer configured: nothing can resolve them, so they will
read as firing forever and the retention sweep never reaches them. Deleting them is
permanent. If that Alertmanager *should* still be configured, fixing the configuration is
the better answer — the alerts start resolving again by themselves.

---

## Times

**Every time shown is UTC**, including the fields you fill in when declaring an outage.

Durations are relative and rounded: `40m`, `2h 15m`, `3d 4h`. Where a rounded gap is not
enough — the Timing column on the Silences page — hovering shows the exact moment.

---

## When something looks wrong

**"Data is N seconds old"** is how long ago the server last read from the Alertmanagers. A
few tens of seconds is normal.

**"Data is MANY seconds old"** means the page could not reach the server at all. It keeps
trying; if it persists, the server is down or your session has gone.

**A red circle by an Alertmanager's name** in the bottom strip means it did not answer the
last check. Alerts from it are greyed and carry a clock icon, and what they say is only as
recent as the last successful read — an alert may have resolved without Alert Viewer
hearing about it.

**An alert reading as firing that you know is over** usually means the Alertmanager it came
from has gone away. Only the Alertmanager that raised an alert can resolve it.

**Nothing in the table** — check the status filter first. The page opens on firing alerts
only, and acked and ticketed alerts are hidden unless you ask for them. **Clear** in the
filter drawer puts everything back.

**An action that seems not to work** — look at the message strip along the bottom of the
window. Anything the server refused says so there, and the usual reason is that the action
needs the admin role.
