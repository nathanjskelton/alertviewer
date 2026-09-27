<template>
  <router-view />
</template>

<script setup>
  //
</script>

<style>


.customize-table {

  --easy-table-border: none;
  --easy-table-row-border: 1px solid #eef2f7;

  --easy-table-header-background-color: #ffffff;
  --easy-table-header-font-color: #64748b;
  --easy-table-header-font-size: 11px;
  --easy-table-header-height: 34px;

  --easy-table-header-item-padding: 2px 8px;

  --easy-table-body-row-height: 35px;
  --easy-table-body-row-font-size: 12px;

  --easy-table-body-item-padding: 2px 5px;

}

/* Clean, modern table header: no heavy grey block, just an uppercase
   label row with a subtle underline and no vertical separators. */
.customize-table thead th {
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 700;
  border-right: none !important;
  border-bottom: 2px solid #e2e8f0 !important;
}
.customize-table tbody td {
  border-right: none !important;
}

/* Fixed column layout so the single shared header (.master-header) and every
   grouped body table line up column-for-column regardless of content. */
.customize-table table {
  table-layout: fixed;
  width: 100%;
}
.customize-table th:nth-child(1), .customize-table td:nth-child(1) { width: 40px; }
/* Indicator column: must fit the status indicator plus the annotations, notes
   and jira icons side by side, or the icons get squashed out of sight. Worst
   case is a flapping row (two status icons) carrying all three: ~141px of
   content. */
.customize-table th:nth-child(2), .customize-table td:nth-child(2) { width: 164px; }
.customize-table th:nth-child(3), .customize-table td:nth-child(3) { width: 90px; }
.customize-table th:nth-child(4), .customize-table td:nth-child(4) { width: 96px; }
.customize-table th:nth-child(5), .customize-table td:nth-child(5) { width: 84px; }
.customize-table th:nth-child(6), .customize-table td:nth-child(6) { width: 150px; }
.customize-table th:nth-child(7), .customize-table td:nth-child(7) { width: 150px; }
.customize-table th:nth-child(8), .customize-table td:nth-child(8) { width: 110px; }
/* column 9 (Summary) intentionally left auto to take the remaining width */

/* The column widths above are descendant selectors, so they also hit the cells
   of any table nested inside a body cell -- the indicator cell draws a small
   one -- stretching it to the width of columns 1 and 2 and pushing whatever
   follows out of sight. Keep nested tables at their natural size. */
.customize-table td table { table-layout: auto; width: auto; border-spacing: 0; }
.customize-table td table td { width: auto !important; padding: 0; }

/* The shared header table carries no rows of its own — hide its empty body
   and the "No Available Data" placeholder so only the header row shows. */
.master-header tbody { display: none; }
.master-header-root .vue3-easy-data-table__message { display: none; }

/* Rows whose alertmanager is offline: data may be stale — grey the row. */
.customize-table tbody tr.stale-row td {
  background-color: #eceff1 !important;
}
.customize-table tbody tr.stale-row td:first-child {
  box-shadow: inset 4px 0 0 #90a4ae;
}

/* Freshly firing alerts: left bar only (no row highlight — keeps text readable).
   Red for <=1 min, orange for <=10 min. */
.customize-table tbody tr.firing-new-row td:first-child {
  box-shadow: inset 4px 0 0 #dc2626;
}
.customize-table tbody tr.firing-recent-row td:first-child {
  box-shadow: inset 4px 0 0 #f97316;
}

/* Keep each table's pager on screen. The app footer (status bar + timeline
   graph) is fixed over the bottom of the page, so a long table used to leave
   its pager below the fold or under the footer. Sticking it just above the
   footer (--v-layout-bottom is the footer's measured height, set on v-main)
   keeps it reachable while any part of its table is visible. */
.customize-table .vue3-easy-data-table__footer {
  position: sticky;
  bottom: var(--v-layout-bottom, 0px);
  z-index: 3;
  background-color: #ffffff;
}

.v-expansion-panel-text__wrapper { padding: 0 !important; }
</style>