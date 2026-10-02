/* eslint-disable prettier/prettier */
<template>
  <v-app-bar class="mt-5"
    color="purple-lighten-5"
    density="compact"
    dark
    flat
  >    
    <v-icon size="x-large" v-if="showDrawer==false" @click="toggleDrawer();">mdi-menu-right</v-icon>
    <v-icon size="x-large" v-if="showDrawer==true" @click="toggleDrawer();">mdi-menu-left</v-icon>
    <div style="font-synthesis-small-caps: auto; font-size: x-small; color: purple">FILTERS</div>

    <div style="width: 65px;margin-top: 15px;margin-left: 200px">
      <v-select density="compact" variant="underlined" :single-line=true v-model="this.rowsPerPage" label="Rows per Page"
          :items="[1,5,15,25,50,100]" @update:modelValue="this.setQueryString(); this.updateDataTables();">
      </v-select>
    </div>
    <div style="width: 50px;margin-top: 2px;margin-left: 6px;">rows</div>
    <v-spacer></v-spacer>
    
    

    <div v-if="Object.keys(this.info)[0] != 'ALL'">
      <v-slide-group mandatory="force" v-model="expandMode">
        <v-slide-group-item value="none"
          v-slot="{ isSelected, toggle }"
        >
          <v-btn
            rounded="0"
            density=compact
            :color="isSelected ? 'purple' : '#999'"
            :style="isSelected ? 'border-left: 4px solid purple; padding-left: 1px;' : 'padding-left: 5px'"
            class="ma-1 mt-2"
            @click="toggle"
          >
            Expand None
          </v-btn>
        </v-slide-group-item>
        <v-slide-group-item value="first"
          v-slot="{ isSelected, toggle }"
        >
          <v-btn
            rounded="0"
            density=compact
            :color="isSelected ? 'purple' : '#999'"
            :style="isSelected ? 'border-left: 4px solid purple; padding-left: 1px;' : 'padding-left: 5px'"
            class="ma-1 mt-2"
            @click="toggle"
          >
            Expand First
          </v-btn>
        </v-slide-group-item>
        <v-slide-group-item value="all"
          v-slot="{ isSelected, toggle }"
        >
          <v-btn
            rounded="0"
            density=compact
            :color="isSelected ? 'purple' : '#999'"
            :style="isSelected ? 'border-left: 4px solid purple; padding-left: 1px;' : 'padding-left: 5px'"
            class="ma-1 mt-2"
            @click="toggle"
          >
            Expand All
          </v-btn>
        </v-slide-group-item>
      </v-slide-group>
    </div>

    <div class="mt-6 mr-6 ml-15" >
      <v-switch color='amber-darken-2' true-icon="mdi-tag-multiple" false-icon="mdi-tag-off" v-model="showExtraLabels" :label="showExtraLabels ? 'extra labels shown' : 'extra labels hidden'" density="compact" @update:modelValue="this.setQueryString();"></v-switch>
    </div>

    <div class="mt-6 mr-9" >
      <v-switch color='green' true-icon="mdi-refresh" false-icon="mdi-close" :style="autoRefresh ? 'color: green;' : 'color: red;'" v-model="autoRefresh" :label="autoRefresh ? 'auto-refresh enabled' : 'auto-refresh disabled'"  density="compact" @update:modelValue="this.setQueryString();"></v-switch>
    </div>

    <v-btn class="mt-0" tile @click="fetchData()" target="_blank" text :color="refreshStyle">
      Refresh
      <v-icon>mdi-database-refresh</v-icon>
    </v-btn>

    <!--
    <v-btn class="mt-0" tile @click.stop="fetchData(true)" target="_blank" text>
      <div >
        Export
        <v-icon>mdi-application-export</v-icon>
      </div>
    </v-btn>
    -->
  </v-app-bar>

    
  <!-- DRAWER -->

  <v-navigation-drawer v-model="showDrawer" app color="purple-lighten-5" class="mt-5">
    <div class="px-2 mt-0">
      <v-btn width=250 height=50 @click="clearFilters()" target="_blank" text style="background-color:rgba(0, 0, 0, 0.04);">
        <span class="mr-2">Clear</span>
        <v-icon>mdi-notification-clear-all</v-icon>
      </v-btn>
    </div>
    
    <div class="pa-2 mt-0 mb-0"><table style="width: 100%"><tr>
      <td v-if="groupField != null" style="width: 30px"><v-icon @click="groupField=null;panel=[];fetchData();">mdi-broom</v-icon></td>
        <v-select style="max-height: 50px" :items="allFields" v-model="groupField" label="Group by Field"></v-select>
      <td></td></tr></table>
    </div>
    
    


    <div class="pa-2" >
      <v-card class="px-2" style="background-color:rgba(0, 0, 0, 0.04);" >
        <v-card-title style="max-height: 45px" class="caption">Status</v-card-title>
          <v-checkbox style="max-height: 45px" hide-details dense v-model="statuses" label="FIRING" value="NEW" append-icon="mdi-alert-outline"></v-checkbox>
          
          <v-checkbox style="max-height: 45px" hide-details dense v-model="statuses" label="RESOLVED" value="RESOLVED" append-icon="mdi-checkbox-marked-circle-outline"></v-checkbox>
          <v-checkbox style="margin-bottom: 10px; max-height: 45px" hide-details dense v-model="statuses" label="SILENCED" value="SILENCED" append-icon="mdi-sleep"></v-checkbox>
          
      </v-card>

      <v-card class="mt-2 px-2" style="background-color:rgba(0, 0, 0, 0.04);" >
        <v-card-title style="max-height: 45px" class="caption">Attributes</v-card-title>
        <v-checkbox style="max-height: 45px" hide-details dense v-model="statuses" label="ACKED" value="ACKED" append-icon="mdi-account-check"
            title="An acked alert is out of the view whatever it is doing. Tick to bring them back."></v-checkbox>
        <v-checkbox v-if="jiraEnabled" style="max-height: 45px" hide-details dense v-model="statuses" label="JIRA TICKET" value="JIRA" append-icon="mdi-jira"
            title="An alert with a jira ticket is out of the view the same way an acked one is. Tick to bring them back."></v-checkbox>
        <v-checkbox style="max-height: 45px" hide-details dense v-model="statuses" label="FLAPPING" value="FLAPPING" append-icon="mdi-sync-alert"></v-checkbox>
        <v-checkbox style="margin-bottom: 10px; max-height: 45px" hide-details dense v-model="notCallin" label="NOT CALLIN" append-icon="mdi-phone"
            title="Ticked: show every alert. Unticked: only alerts whose callin label is true or 1."></v-checkbox>
      </v-card>

      <!-- Admin only, and confirmed first: it re-reads every alert's ticket from
           the labels in jira, and can optionally drop the links no ticket claims. -->
      <v-btn v-if="cortana_role == 'admin' && jiraEnabled" block class="mt-4" height=50
          :loading="jiraRebuild.running" @click="jiraRebuild.clear = false; jiraRebuild.dialog = true"
          style="background-color:rgba(0, 0, 0, 0.04);"
          title="Re-read every alert's jira ticket from its label.">
        <span class="mr-2">Rebuild Jira Links</span>
        <v-icon>mdi-link-variant</v-icon>
      </v-btn>

      <!-- Admin only, and only when the poll has found some: alerts whose alertmanager
           is no longer configured can never be resolved, so deleting them is the only
           way they leave the view. Confirmed first, since they go for good. -->
      <v-btn v-if="cortana_role == 'admin' && orphans.count > 0" block class="mt-4 drawer-btn-wrap" height=50
          :loading="orphans.running" @click="orphans.dialog = true"
          style="background-color:rgba(0, 0, 0, 0.04);"
          :title="orphans.count + ' alerts came from an alertmanager that is no longer configured, so nothing can resolve them.'">
        <span class="mr-2">Delete Orphaned Alerts</span>
        <v-chip size="small" color="red" class="mr-2">{{ orphans.count }}</v-chip>
        <v-icon>mdi-delete-sweep</v-icon>
      </v-btn>

    </div>
  </v-navigation-drawer>


  <!-- DIALOGS -->

  <v-dialog max-width="90%" v-model="alertDetails.dialog" persistent >
    <v-card class="pa-3" v-if="alertDetails.item != null">
      <v-card-title class="pa-4" style="background-color: purple; color: white; font-size: large; font-weight: bold;">
        <table style="width: 100%;"><tr><td>Alert Details</td><td align="right"><v-icon @click="alertDetails.dialog=false;">mdi-window-close</v-icon></td></tr></table>
      </v-card-title>
      <!-- scroll the body rather than the whole card, so the title bar and its
           close button stay put however much detail the alert carries -->
      <v-card-text style="max-height: 70vh; overflow-y: auto;">
        <v-container fluid>
          <v-row><v-col>

            <div>
              <span class="ps-5">
                <v-chip color="green lighten-1" size="small">STARTED</v-chip>
                {{ alertDetails.item.friendlyStartTime }} UTC
              </span>
              <span class="ps-5">
                <v-chip color="red lighten-1" size="small">ENDS</v-chip>
                {{ alertDetails.item.friendlyEndTime }} UTC
              </span>
            </div>
          </v-col></v-row> 
          <v-row >
            <v-col class="pt-0 mt-0" cols="4" offset="0">
              <div style="margin-left: 20px; font-size: 16px; font-weight: bold;">Details</div>
            </v-col>
            <v-col class="pt-0 mt-0">
              <div style="margin-left: 20px; font-size: 16px; font-weight: bold;">Routing</div>
            </v-col>
          </v-row>
          <v-row>
            <v-col class="pt-0 mt-0" cols="4" >
                <div style="font-size: 14px;margin-left: 20px; font-weight: bold; color: #944">{{alertDetails.item.alert.labels.alertname}}</div>
                <div style="font-size: 14px;margin-left: 20px; font-weight: bold; color: #779">{{alertDetails.item.alertmanager}}</div>

                <div style="font-size: 14px;margin-left: 20px; margin-top: 15px; font-weight: bold;">Summary</div>
                <div style="font-size: 12px;margin-left: 20px; vertical-align: top; white-space: pre-wrap;" v-html="alertDetails.item.alert.annotations.summary"></div>

                <div style="font-size: 12px;margin-left: 20px;margin-top: 15px;"><span style="font-weight: bold"> Acked: </span> {{alertDetails.item.acked}}</div>
                <div style="font-size: 12px;margin-left: 20px;"><span style="font-weight: bold"> Flapping: </span> {{alertDetails.item.flapping}}</div>


                <div style="font-size: 12px;margin-left: 20px;margin-top: 15px;"><span style="font-weight: bold"> Severity: </span> {{alertDetails.item.alert.labels.severity}}</div>
                <div style="font-size: 12px;margin-left: 20px;"><span style="font-weight: bold"> Instance: </span> {{alertDetails.item.alert.labels.instance}}</div>
                <div style="font-size: 12px;margin-left: 20px;"><span style="font-weight: bold"> Environment: </span> {{alertDetails.item.alert.labels.environment}}</div>
                <div style="font-size: 12px;margin-left: 20px;"><span style="font-weight: bold"> Team: </span> {{alertDetails.item.alert.labels.team}}</div>

                <!-- the label this alert's ticket carries in jira, spelled exactly as it
                     appears there, so it can be pasted straight into a jira search -->
                <div v-if="jiraEnabled" style="font-size: 12px;margin-left: 20px;margin-top: 15px;"><span style="font-weight: bold"> Jira Label: </span>
                  <span style="font-family: monospace; user-select: all;">{{jiraLabel(alertDetails.item)}}</span></div>


                <div style="font-size: 14px;margin-left: 20px; margin-top: 15px;"><span style="font-weight: bold">Additional Labels</span></div>
                <div v-for="value, label in alertDetails.item.alert.labels" class="mx-0 px-0">
                  <div v-if="label != 'instance'
                        && label != 'environment' && label != 'severity'
                        && label != 'alertname'" style="font-size: 12px;margin-left: 20px;" class="px-0"> <span style="color: #777; font-weight: bold">{{ label }}: </span> {{ value }} </div>
                </div>

                <div style="font-size: 14px;margin-left: 20px; margin-top: 15px;"><span style="font-weight: bold">Additional Annotations</span></div>
                <div v-for="value, label in alertDetails.item.alert.annotations" class="mx-0 px-0">
                  <div v-if="label != 'service'
                        && label != 'summary'"  style="font-size: 12px;margin-left: 20px;" class="px-0"> <span style="color: #777; font-weight: bold">{{ label }}: </span> {{ value }} </div>
                </div>

            </v-col>
            <v-col class="pt-0 mt-0" cols=8>
              <!-- What alertmanager did with this alert. The receivers are fact, read
                   off the alert itself; the actions come from the live config. -->
              <div style="font-size: 12px; margin-left: 20px; color: #777;">
                Receivers assigned by {{ alertDetails.item.alertmanager }}
              </div>

              <div v-if="detailsReceivers(alertDetails.item).length == 0"
                  style="font-size: 12px; margin-left: 20px; margin-top: 8px; color: #777;">
                No receivers recorded for this alert.
              </div>

              <div v-for="r in detailsReceivers(alertDetails.item)" :key="r.name"
                  style="margin-left: 20px; margin-top: 10px;">
                <v-chip size="small" label variant="tonal" color="#2563EB">
                  <v-icon start>mdi-call-split</v-icon>{{ r.name }}
                </v-chip>

                <!-- these three states mean very different things and must not look alike -->
                <span v-if="r.state == 'unavailable'" style="font-size: 12px; margin-left: 10px; color: #b45309;">
                  {{ alertDetails.item.alertmanager }} configuration unavailable &mdash; cannot show what this receiver does
                </span>
                <span v-else-if="r.state == 'unknown'" style="font-size: 12px; margin-left: 10px; color: #b45309;">
                  not in the current configuration (it may have changed since this alert fired)
                </span>
                <span v-else-if="r.actions.length == 0" style="font-size: 12px; margin-left: 10px; color: #b45309;">
                  <v-icon size="16" color="#b45309">mdi-bell-off-outline</v-icon>
                  no action configured &mdash; notifications sent here are discarded
                </span>

                <div v-for="(a, i) in r.actions" :key="i"
                    style="font-size: 12px; margin-left: 30px; margin-top: 3px;">
                  <v-icon size="16" color="#777">{{ actionIcon(a.type) }}</v-icon>
                  <span style="color: #777; font-weight: bold;"> {{ a.type }}</span>
                  <span v-if="a.target"> &rarr; {{ a.target }}</span>
                  <span v-if="a.sendResolved != null" style="color: #777;">
                    &nbsp;&middot; send_resolved: {{ a.sendResolved }}</span>
                </div>
              </div>

              <div v-if="detailsRouteDefaults(alertDetails.item) != null"
                  style="font-size: 12px; margin-left: 20px; margin-top: 12px; color: #777;">
                Grouped by {{ groupByLabel(detailsRouteDefaults(alertDetails.item).groupBy) }} &middot;
                wait {{ detailsRouteDefaults(alertDetails.item).groupWait }} &middot;
                interval {{ detailsRouteDefaults(alertDetails.item).groupInterval }} &middot;
                repeat {{ detailsRouteDefaults(alertDetails.item).repeatInterval }}
              </div>

              <div style="margin-left: 20px; font-weight: bold; font-size: 16px; margin-top: 20px;">Notes</div>
              <v-list style="margin-left: 20px; padding-left: 0;margin-top: 0; padding-top: 0;background-color: inherit" density="compact">
                <v-list-item v-for="note in alertDetails.item.notes" class="mx-0 px-0">
                  <div class="px-0 mx-0" style="font-size: 12px;"><v-chip size="small">{{ note.timestamp }}</v-chip><v-chip size="small">{{ note.user }}</v-chip> {{ note.message }} </div>
                </v-list-item>
              </v-list>

            </v-col>
          </v-row>
        </v-container>
      </v-card-text>

    </v-card>
  </v-dialog>

  <v-dialog max-width="480px" v-model="annotationsDialog.dialog">
    <v-card v-if="annotationsDialog.item != null">
      <v-card-title class="pa-3" style="background-color: purple; color: white; font-size: medium; font-weight: bold;">
        <table style="width: 100%;"><tr>
          <td>Annotations</td>
          <td align="right"><v-icon color="white" @click="annotationsDialog.dialog=false;">mdi-window-close</v-icon></td>
        </tr></table>
      </v-card-title>
      <v-card-text class="pa-3">
        <div v-for="tag in getExtraAnnotations(annotationsDialog.item)" :key="tag.key" class="mb-2">
          <div style="font-size: 12px; font-weight: bold; color: #6b21a8;">{{ tag.key }}</div>
          <div style="font-size: 12px; white-space: pre-wrap;">{{ tag.value }}</div>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog max-width="600px" v-model="notesDialog.dialog">
    <v-card v-if="notesDialog.item != null">
      <v-card-title class="pa-3" style="background-color: #0369A1; color: white; font-size: medium; font-weight: bold;">
        <table style="width: 100%;"><tr>
          <td>Notes</td>
          <td align="right"><v-icon color="white" @click="notesDialog.dialog=false;">mdi-window-close</v-icon></td>
        </tr></table>
      </v-card-title>
      <v-card-text class="pa-3">
        <div v-for="note in getUserNotes(notesDialog.item)" :key="note.timestamp" class="mb-2">
          <div style="font-size: 12px;"><v-chip size="small">{{ note.timestamp }}</v-chip><v-chip size="small">{{ note.user }}</v-chip></div>
          <div style="font-size: 12px; white-space: pre-wrap;">{{ note.message }}</div>
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>

  <v-dialog max-width="500px" v-model="copyDialog.dialog">
    <v-card>
      <v-card-title>{{copyDialog.title}}</v-card-title>
      <v-card-text>
        <v-textarea readonly ref="copydialog" :value="copyDialog.text"></v-textarea>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="copyDialogText()">Copy/Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog max-width="600px" v-model="jiraRebuild.dialog" persistent>
    <v-card>
      <v-card-title class="pa-4" style="background-color: purple; color: white; font-size: large; font-weight: bold;">
        Rebuild Jira Links
      </v-card-title>
      <v-card-text class="pt-4">
        Every alert's jira link is re-read from the ticket labelled with its
        fingerprint. Alerts whose label turns up no ticket keep the link they
        have now, unless you tick the box below.
        <v-checkbox class="mt-2" hide-details density="compact" v-model="jiraRebuild.clear"
            label="Clear jira links that don't match a ticket label"
            title="Alerts whose label turns up no ticket lose the link they have now, including links added by hand. This cannot be undone."></v-checkbox>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="rebuildJiraLinks();">Rebuild</v-btn>
        <v-btn color="blue-darken-1" text @click="jiraRebuild.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog max-width="600px" v-model="orphans.dialog" persistent>
    <v-card>
      <v-card-title class="pa-4" style="background-color: purple; color: white; font-size: large; font-weight: bold;">
        Delete Orphaned Alerts
      </v-card-title>
      <v-card-text class="pt-4">
        {{ orphans.count }} alert{{ orphans.count == 1 ? '' : 's' }} in the database came from
        <span style="font-weight: bold;">{{ orphans.alertmanagers.join(', ') }}</span>,
        which {{ orphans.alertmanagers.length == 1 ? 'is' : 'are' }} not in this deployment's
        configuration. Only the alertmanager an alert came from can resolve it, so these will
        read as firing for as long as they are kept, and the retention sweep never reaches them.
        <div class="mt-2" style="font-weight: bold;">
          They are deleted outright. This cannot be undone.
        </div>
        <div class="mt-2" style="font-size: 12px; color: #666;">
          If one of these alertmanagers should still be configured, fix the configuration
          instead: the alerts will start resolving again on the next ingest.
        </div>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="deleteOrphanedAlerts();">Delete</v-btn>
        <v-btn color="blue-darken-1" text @click="orphans.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog max-width="600px" v-model="note.dialog" persistent>
    <v-card>
      <v-card-text>
        <v-textarea v-model="note.message" :label="note.caption"></v-textarea>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="note.dialog=false;saveNote();">Apply</v-btn>
        <v-btn v-if="note.prefix == 'Note'" color="blue-darken-1" text @click="note.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog max-width="600px" v-model="jira.dialog" persistent>
    <v-card>
      <v-card-text>

        <!-- what this alert is pointing at right now -->
        <v-row v-if="jira.currentKey" class="pa-0 ma-0">
          <v-col cols="3" class="pb-0">Linked ticket</v-col>
          <v-col class="pb-0">
            <v-chip size="small" color="#2684FF" variant="tonal" label
                style="cursor: pointer;" title="Open this ticket in jira"
                @click="openJiraKey(jira.currentKey);">
              <v-icon start>mdi-jira</v-icon>{{ jira.currentKey }}
            </v-chip>
          </v-col>
        </v-row>

        <v-row class="pa-0 ma-0">
          <v-col cols="3" class="pb-0">Action</v-col>
          <v-col class="pb-0">
            <v-radio-group v-model="jira.mode" hide-details density="compact" inline>
              <v-radio label="Link existing" value="link"></v-radio>
              <v-radio :label="jira.currentKey ? 'Create new (replaces)' : 'Create new'" value="create"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>

        <!-- link: just record a key the user already has -->
        <template v-if="jira.mode == 'link'">
          <v-row>
            <v-col cols="3">Ticket key</v-col>
            <v-col>
              <v-text-field v-model="jira.linkKey" placeholder="JIRA Key" hide-details
                  @keyup.enter="jira.dialog=false;submitJira();">
                <template v-slot:append-inner>
                  <v-icon v-if="jiraUrlForKey(jira.linkKey)" color="#2684FF"
                      style="cursor: pointer;" title="Open this ticket in jira"
                      @click="openJiraKey(jira.linkKey);">mdi-open-in-new</v-icon>
                </template>
              </v-text-field>
              <div style="font-size: 11px; color: #64748b; margin-top: 4px;">
                Points this alert at a ticket that already exists. Nothing is created.
              </div>
            </v-col>
          </v-row>
        </template>

        <!-- create: the original new-ticket form -->
        <template v-else>
          <v-row>
            <v-col cols="3">Summary</v-col>
            <v-col><v-text-field v-model="currentJira.summary" hide-details></v-text-field></v-col>
          </v-row>
          <v-row>
            <v-col cols="3">Description</v-col>
            <v-col><v-textarea v-model="currentJira.description" hide-details></v-textarea></v-col>
          </v-row>
          <v-row v-if="jira.currentKey">
            <v-col cols="3"></v-col>
            <v-col>
              <div style="font-size: 11px; color: #b45309;">
                A new ticket will be raised and {{ jira.currentKey }} will be replaced by it.
              </div>
            </v-col>
          </v-row>
        </template>

      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text
            :disabled="jira.mode == 'link' && !(jira.linkKey || '').trim()"
            @click="jira.dialog=false;submitJira();">
          {{ jira.mode == 'link' ? 'Link' : 'Create' }}
        </v-btn>
        <v-btn color="blue-darken-1" text @click="jira.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <v-dialog max-width="700px" v-model="silence.dialog" persistent>
    <v-card class="pa-3">
      <v-card-title class="pa-4" style="background-color: purple; color: white; font-size: large; font-weight: bold;">Silence</v-card-title>
      <v-card-text>
        <v-form dense>
          
          <v-row class="pa-0 ma-0">
            <v-col cols=12 class="pt-0 pb-0 px-0 ma-0">

              <v-card
                class="mx-auto"
                max-width="620"
              >
                <v-card-item class="bg-purple-lighten-4">
                  <v-card-title>
                    Matchers
                  </v-card-title>

                  <template v-slot:append>
                    <v-btn
                      color="white"
                      icon="mdi-plus"
                      size="small"
                      @click="newMatcher();"
                    ></v-btn>
                  </template>

                </v-card-item>

                <v-divider></v-divider>

                <v-virtual-scroll
                  :items="currentSilence.matchers"
                  height=160
                  item-height="50"
                >
                  <template v-slot:default="{ item }">
                    <v-list-item>
                      <v-container class="pa-0 ma-0"><v-row dense no-gutters >
                      <v-col cols="1" class="pt-2 pl-2">
                        <v-icon @click="deleteMatcher(item.name, item.value);" size="x-large" color="purple-lighten-2">mdi-delete-circle</v-icon>
                      </v-col>
                      
                      <v-col cols="5">
                        <v-list-item><v-text-field v-model="item.name" density="compact"></v-text-field></v-list-item>
                      </v-col>
                      <v-col cols="1" class="pt-2 pl-2"> 
                        <v-icon @click="toggleMatcher(item.name, item.value)" v-if="item.isEqual == true" size="x-large" color="grey-lighten-2">mdi-equal-box</v-icon>
                        <v-icon @click="toggleMatcher(item.name, item.value)" v-if="item.isEqual == false" size="x-large" color="grey-lighten-2">mdi-code-not-equal</v-icon>
                      </v-col>
                      <v-col cols="5">
                        <v-list-item><v-text-field v-model="item.value" density="compact"></v-text-field></v-list-item>
                      </v-col>
                    </v-row></v-container>
                    </v-list-item>
                  </template>
                </v-virtual-scroll>
              </v-card>

            </v-col>
          </v-row >
          
          <v-row class="px-0 mx-0">
            <v-col cols=2 class="pt-4 pb-0 px-0 ma-0"><div>Comment</div></v-col>
            <v-col cols=10 class="pa-0 ma-0"><v-text-field v-model="currentSilence.comment"></v-text-field></v-col>
          </v-row>
          <v-row class="pa-0 ma-0">
            <v-col cols=2 class="pt-4 pb-0 px-0 ma-0"><div>Hours</div></v-col>
            <v-col cols=2 class="pa-0 ma-0"><v-text-field v-model="currentSilence.hours"></v-text-field></v-col>
          </v-row>
          <v-row class="px-0 mx-0">
            <v-col cols=12 class="pa-0 ma-0">
              
              <v-select
                    :items="alertmanagers"
                    density="compact"
                    label="Alertmanager"
                    v-model="currentSilence.alertmanager"
              ></v-select>
              
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-btn color="blue-darken-1" text @click="silence.dialog=false;saveSilence();">Save</v-btn>
        <v-btn color="blue-darken-1" text @click="silence.dialog=false;">Cancel</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
  
  <!--
  <v-toolbar dense color="purple-lighten-5" height="50" elevation=3>      
    <v-text-field
      clearable
      clear-icon="mdi-broom"
      dense
      v-model="searchValue"
      label="Search"
      single-line
      hide-details
    ></v-text-field>    
  </v-toolbar> 
  -->

  <!-- Scrubbing the footer timeline swaps the table out for the gantt view. -->
  <alert-gantt
    v-if="timeline_selection != null"
    :items="selectionRows"
    :start="timeline_selection.start"
    :end="timeline_selection.end"
    @close="$emit('closeTimeline')"
  />

  <template v-else>

  <!-- Single shared column header (with filters) for all groups -->
  <EasyDataTable
    class="master-header-root"
    :headers="headers"
    :items="[]"
    :sort-by="sortBy"
    :sort-type="sortType"
    :filter-options="filterOptions"
    :header-item-class-name="headerItemClassName"
    :table-min-height="0"
    hide-footer
    table-class-name="customize-table master-header"
    @update-sort="sortBy = $event.sortBy; sortType = $event.sortType"
  >

          <template #header-alert.labels.severity="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
        
              <template v-slot:default="{ isActive }">
            
                  <v-select autofocus=true @keyup.enter="isActive.value=false" menu-icon="mdi-arrow-left-bold-circle" 
                      clearable=true @update:menu="isActive.value=false"  bg-color="white" 
                      label="Severity" menu density=compact multiple :items="logTypes" v-model="searchSeverity" @update:modelValue="this.setQueryString();">
                  </v-select>
          
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchSeverity != null && searchSeverity.length > 0" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>                
                <v-icon color="#999" v-if="searchSeverity == null || searchSeverity.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>
            
            <div>Severity</div>
          </template>

          <template #header-alert.labels.environment="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
        
              <template v-slot:default="{ isActive }">
            
                  <v-select autofocus=true @keyup.enter="isActive.value=false" menu-icon="mdi-arrow-left-bold-circle" 
                      clearable=true @update:menu="isActive.value=false"  bg-color="white" 
                      label="Environment" menu density=compact multiple :items="environments" v-model="searchEnvironment" @update:modelValue="this.setQueryString();">
                  </v-select>
          
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchEnvironment != null && searchEnvironment.length > 0" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>              
                <v-icon color="#999" v-if="searchEnvironment == null || searchEnvironment.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>
            
            <div>Environment</div>
          </template>

          <template #header-alert.labels.alertname="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
              <template v-slot:default="{ isActive }">
                <v-text-field label="AlertName" @keyup.enter="isActive.value=false" clearable=true 
                    autofocus=true density=compact bg-color="white"  v-model="searchAlertName" @update:modelValue="this.setQueryString();">
                  <template v-slot:append-inner>
                    <v-icon @click="isActive.value=false">mdi-arrow-right-bold-circle</v-icon>
                  </template>
                </v-text-field>
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchAlertName != null && !searchAlertName.includes(',') && searchAlertName.length > 0 && !searchAlertName.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>
                <v-icon color="red" v-if="searchAlertName != null && !searchAlertName.includes(',') && searchAlertName.length > 0 && searchAlertName.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-remove</v-icon>
                <v-icon color="blue" v-if="searchAlertName != null && searchAlertName.includes(',')" 
                    v-bind="activatorProps">mdi-filter-plus</v-icon>                    
                <v-icon color="#999" v-if="searchAlertName == null || searchAlertName.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>
            
            <div>AlertName</div>
          </template>

          <template #header-alert.labels.instance="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
              <template v-slot:default="{ isActive }">
                <v-text-field label="Instances" @keyup.enter="isActive.value=false" clearable=true 
                    autofocus=true density=compact bg-color="white"  v-model="searchInstance" @update:modelValue="this.setQueryString();">
                  <template v-slot:append-inner>
                     <v-icon @click="isActive.value=false">mdi-arrow-right-bold-circle</v-icon>
                  </template>
                </v-text-field>
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchInstance != null && !searchInstance.includes(',') && searchInstance.length > 0 && !searchInstance.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>
                <v-icon color="red" v-if="searchInstance != null && !searchInstance.includes(',') && searchInstance.length > 0 && searchInstance.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-remove</v-icon>
                <v-icon color="blue" v-if="searchInstance != null && searchInstance.includes(',')" 
                    v-bind="activatorProps">mdi-filter-plus</v-icon>                    
                <v-icon color="#999" v-if="searchInstance == null || searchInstance.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>

            <div>Instances</div>
          </template>

          <template #header-alert.labels.team="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
              <template v-slot:default="{ isActive }">
                <v-text-field label="Team" @keyup.enter="isActive.value=false" clearable=true 
                    autofocus=true density=compact bg-color="white"  v-model="searchTeam" @update:modelValue="this.setQueryString();">
                  <template v-slot:append-inner>
                    <v-icon @click="isActive.value=false">mdi-arrow-right-bold-circle</v-icon>
                  </template>
                </v-text-field>
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchTeam != null && !searchTeam.includes(',') && searchTeam.length > 0 && !searchTeam.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>
                <v-icon color="red" v-if="searchTeam != null && !searchTeam.includes(',') && searchTeam.length > 0 && searchTeam.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-remove</v-icon>
                <v-icon color="blue" v-if="searchTeam != null && searchTeam.includes(',')" 
                    v-bind="activatorProps">mdi-filter-plus</v-icon>                    
                <v-icon color="#999" v-if="searchTeam == null || searchTeam.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>
            
            <div>Team</div>
          </template>

          <template #header-alert.annotations.summary="header">
            <v-dialog offset=-40 max-width="300" location-strategy="connected">
              <template v-slot:default="{ isActive }">
                <v-text-field label="Summary" @keyup.enter="isActive.value=false" clearable=true 
                    autofocus=true density=compact bg-color="white"  v-model="searchSummary" @update:modelValue="this.setQueryString();">
                  <template v-slot:append-inner>
                    <v-icon @click="isActive.value=false">mdi-arrow-right-bold-circle</v-icon>
                  </template>
                </v-text-field>
              </template>
              <template v-slot:activator="{ props: activatorProps }">
                <v-icon color="green" v-if="searchSummary != null && !searchSummary.includes(',') && searchSummary.length > 0 && !searchSummary.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-check</v-icon>
                <v-icon color="red" v-if="searchSummary != null && !searchSummary.includes(',') && searchSummary.length > 0 && searchSummary.startsWith('!')" 
                    v-bind="activatorProps">mdi-filter-remove</v-icon>
                <v-icon color="blue" v-if="searchSummary != null && searchSummary.includes(',')" 
                    v-bind="activatorProps">mdi-filter-plus</v-icon>                    
                <v-icon color="#999" v-if="searchSummary == null || searchSummary.length == 0" v-bind="activatorProps">mdi-filter-off</v-icon>
              </template>
            </v-dialog>
            
            <div>Summary</div>
          </template>

  </EasyDataTable>

  <v-expansion-panels v-model="panel" multiple flat class="group-panels">
    <template v-for="group, key in info" :key="key">
    <v-expansion-panel v-if="groupStats[key] && groupStats[key].total > 0" :value="key" bg-color="transparent" elevation="0">
      <v-expansion-panel-title class="group-header" hide-actions>
        <template v-slot:default="{ expanded }">
            <v-icon size="small" class="group-chevron" :class="{ 'group-chevron--open': expanded }">mdi-chevron-right</v-icon>
            <div class="group-left">
              <v-chip label size="small" variant="outlined" :color="getGroupSeverityColor(groupStats[key].severities)" class="font-weight-bold">
                <span style="color: #000;">
                  <template v-if="key == 'ALL'">Ungrouped</template>
                  <template v-else>{{ groupTitle(key) }}</template>
                </span>
              </v-chip>
              <v-chip v-for="sev in groupStats[key].severities" :key="'sev-' + sev" label size="x-small" :color="getSeverityColorByValue(sev)">
                {{ sev }}
              </v-chip>
              <template v-if="showExtraLabels">
                <v-chip v-for="team in groupStats[key].teams" :key="'team-' + team" label size="x-small" variant="tonal" :color="getLabelColor(team)" prepend-icon="mdi-account-group">
                  {{ team }}
                </v-chip>
                <v-chip v-for="gm in groupStats[key].environments" :key="'gm-' + gm" label size="x-small" variant="tonal" :color="getLabelColor(gm)" prepend-icon="mdi-server">
                  {{ gm }}
                </v-chip>
              </template>
            </div>
            <v-spacer></v-spacer>
            <v-chip v-if="groupStats[key].newCount > 0" size="x-small" label variant="flat" color="red" class="mr-2" prepend-icon="mdi-alert-decagram">
              {{ groupStats[key].newCount }} new
            </v-chip>
            <v-chip v-if="groupStats[key].staleCount > 0" size="x-small" label variant="flat" color="blue-grey" class="mr-2" prepend-icon="mdi-clock-alert">
              {{ groupStats[key].staleCount }} stale
            </v-chip>
            <v-chip v-if="groupStats[key].firingFor != null" size="x-small" label variant="tonal" color="orange" class="mr-2" prepend-icon="mdi-clock-outline">
              firing for {{ groupStats[key].firingFor }}
            </v-chip>
            <v-chip size="x-small" label variant="tonal" :color="groupStats[key].firing > 0 ? 'red' : 'green'">
              {{ groupStats[key].firing }}/{{ groupStats[key].total }} firing
            </v-chip>
        </template>
      </v-expansion-panel-title>
      <v-expansion-panel-text >
        <EasyDataTable
          ref="dataTable"
          :key="'tbl-' + key + '-' + sortBy + '-' + sortType + '-' + tableEpoch"
          :headers="headers"
          :items="group.list"
          :loading="loading"
          :sort-by="sortBy"
          :sort-type="sortType"
          hide-header
          :rows-items=[1,5,15,25,50,100]
          :table-min-height=10
          :hide-footer="group.list.length <= this.rowsPerPage"
          :rows-per-page="this.rowsPerPage"
          :filter-options="filterOptions"
          :body-row-class-name="bodyRowClassName"
          table-class-name="customize-table"
        >


          <!-- ITEMS -->

          <template #item-alert.annotations.summary="item">
            <div style="max-height: 65px;" v-html="getSummaryHeader(item.alert.labels.alertname, item.alert.annotations.summary)">
            </div>
            <div v-if="showExtraLabels && getExtraLabels(item).length > 0" style="display: flex; flex-wrap: wrap; gap: 4px; margin: 3px 0 5px 0;">
              <v-chip v-for="tag in getExtraLabels(item)" :key="tag.key" size="x-small" label variant="tonal" :color="getLabelColor(tag.key)">
                <span style="font-weight: 600;">{{ tag.key }}</span>:&nbsp;{{ tag.value }}
              </v-chip>
            </div>
          </template>

          <template #item-alert.startsAt="item">
            <div style="text-align: center;"><v-chip size="small" :color="getLastOccColor(item)">{{ item.duration }}</v-chip></div>
          </template>

          <template #item-icon="item">
            <div class="icon-cell"
                title="View details"
                @click="alertDetails.item=item; alertDetails.dialog=true;">
              <v-icon v-if="isStale(item)" size="24" color="blue-grey-darken-1" class="mr-1"
                  title="Alertmanager offline — this alert may be stale">mdi-clock-alert</v-icon>
              <v-progress-circular v-if="item.status == 'NEW' && !isStale(item)" :rotate="0" :size="26" :width="2" bg-color="#ddd"
                  :color="getColorByPercent(Math.round((((new Date(item.alert.endsAt) - new Date()) / 1000)) / 300 * 100))"
                  :model-value="Math.round((((new Date(item.alert.endsAt) - new Date()) / 1000)) / 300 * 100)" >
                <template v-slot:default>
                  <v-icon  style="padding-bottom: 2px;" color=red
                      v-if="item.status == 'NEW' && item.flapping != true">mdi-alert-outline</v-icon>
                  <v-icon style="padding-bottom: 0px;" color=red
                      v-if="item.flapping == true">mdi-sync-alert</v-icon>
                </template>
              </v-progress-circular>

              <table v-if="item.status != 'NEW' && !isStale(item)"><tr><td style="padding-left: 3px;">
                  <v-icon color=grey class="pb-0" v-if="item.status == 'SILENCED'">mdi-sleep</v-icon>
                  <v-icon color=green class="pb-0" v-if="item.status == 'RESOLVED'">mdi-checkbox-marked-circle-outline</v-icon>
                  </td><td>
                  <v-icon color=red class="pb-0" v-if="item.flapping == true">mdi-sync-alert</v-icon>
                  </td></tr>
              </table>

              <!-- acked is independent of the status, so it shows alongside whatever
                   the alert is doing rather than in place of it -->
              <v-icon color=orange v-if="item.acked" class="ml-1"
                  title="Acked">mdi-account-check</v-icon>

              <v-icon v-if="getExtraAnnotations(item).length > 0" color="#EAB308"
                  class="ml-1" style="cursor: pointer;" title="View annotations"
                  @click.stop="annotationsDialog.item=item; annotationsDialog.dialog=true;">mdi-note-text</v-icon>

              <v-icon v-if="getUserNotes(item).length > 0" color="#38BDF8"
                  class="ml-1" style="cursor: pointer;" title="View notes"
                  @click.stop="notesDialog.item=item; notesDialog.dialog=true;">mdi-comment-text-outline</v-icon>

              <v-icon v-if="isCallin(item)" color="#16A34A"
                  class="ml-1" title="Call-in alert">mdi-phone</v-icon>

              <v-icon v-if="item.jiraKey" :color="jiraIconColor(item)"
                  class="ml-1" style="cursor: pointer;" :title="jiraIconTitle(item)"
                  @click.stop="openJira(item);">mdi-jira</v-icon>
            </div>
          </template>

          <template #item-alert.labels.instance="item">
            <div style="text-align: center;">{{ item.alert.labels.instance }}</div>
          </template>

          <template #item-alert.labels.severity="item">
            <div style="text-align: center;"><v-chip size="small" :color="getSeverityColor(item)">{{ item.alert.labels.severity }}</v-chip></div>
          </template>

          <template #item-alert.labels.alertname="item">
            <div v-if="item.alert.labels.service != null" style="padding-left: 4px; border-left: 3px solid orange; text-align: center;">
              {{ item.alert.labels.service }}
            </div>
            <div v-if="item.alert.labels.service == null" style="text-align: center;">
              {{ item.alert.labels.alertname }}
            </div>
          </template>

          <template #item-alert.labels.team="item">
            <div v-if="item.alert.labels.team != null" style="text-align: center;">
              {{ item.alert.labels.team }}
            </div>
            <div size="small" color="red" v-if="(item.alert.labels.team == null)" style="padding-left: 4px; border-left: 3px solid red; text-align: center;">
              TEAM MISSING
            </div>
          </template>



          <template #item-alert.labels.environment="item">
            <div v-if="item.alert.labels.environment != null && item.alert.annotations.environment_from_am == null" style="text-align: center;">
              {{item.alert.labels.environment}}
            </div>
            <div v-if="item.alert.labels.environment != null && item.alert.annotations.environment_from_am == 'true'"
                style="padding-left: 4px; border-left: 3px solid orange; text-align: center;">
              {{item.alert.labels.environment}}
            </div>
            <div v-if="item.alert.labels.environment == null"
                style="padding-left: 4px; border-left: 3px solid red; text-align: center;">
              legacy
            </div>
          </template>


          <template #item-actions="item">
            <v-menu offset-y>
              <template v-slot:activator="{ props }">
                <v-icon small v-bind="props">mdi-dots-vertical</v-icon>
              </template>
              <v-card><v-list>
                    <v-list-item>
                      <v-btn
                        value="DETAILS"
                        small
                        style="width: 75px;"
                        color="purple"
                        elevation=0
                        @click="alertDetails.dialog=true;alertDetails.item=item;"
                      >DETAILS</v-btn>
                    </v-list-item>
                <v-list-item v-if="!item.acked" >
                      <v-btn 
                        value="ACK"
                        small
                        style="width: 75px;"
                        color="green" 
                        elevation=0
                        @click="mark(item, 'ACKED')"
                      >ACK</v-btn>
                    </v-list-item>
                    <v-list-item v-if="item.acked">
                      <v-btn  
                        value="UNACK"
                        small
                        style="width: 75px;color:white !important"
                        color="blue-lighten-2"
                        elevation=0
                        @click="mark(item, 'UNACKED')"
                      >UNACK</v-btn>
                    </v-list-item>
                    <v-list-item v-if="item.status == 'RESOLVED'"> 
                      <v-btn  
                        value="DELETE"
                          small
                          style="width: 75px;"
                          color="red"
                          elevation=0
                          @click="deleteRecord(item.id);"
                        >DELETE</v-btn>
                      </v-list-item>
                      <v-list-item v-if="item.status != 'RESOLVED'" >
                        <v-btn 
                          value="SILENCE"
                          small
                          style="width: 75px;"
                          color="red"
                          elevation=0
                          @click="newSilence(item);silence.dialog = true;"
                        >SILENCE</v-btn>
                      </v-list-item>
                      <v-list-item v-if="jiraEnabled">
                        <v-btn 
                          value="JIRA"
                          small
                          style="width: 75px;"
                          color="blue"
                          elevation=0
                          @click="newJira(item);jira.dialog = true;"
                        >JIRA</v-btn>
                      </v-list-item>
                      <v-list-item>
                        <v-btn
                          value="NOTE"
                          small
                          style="width: 75px;"
                          color="yellow"
                          elevation=0
                          @click="note.dialog = true;note.id = item.id;note.message='';note.prefix='Note';note.caption='Add a note to the record';"
                        >NOTE</v-btn>
                      </v-list-item>
                      
                  </v-list>
                </v-card>
            </v-menu>
          </template>

        </EasyDataTable>
      </v-expansion-panel-text>
    </v-expansion-panel>
    </template>

  </v-expansion-panels>

  </template>

</template>


<script src="./ResultListModel.js"/>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
/* The drawer is only as wide as the filter card, and a v-btn label is uppercased and
   letter-spaced, so a label of more than a couple of words overflows its button rather
   than wrapping: v-btn__content is nowrap by default. Let it use the second line the
   50px height already allows. */
.drawer-btn-wrap :deep(.v-btn__content) {
  white-space: normal;
  line-height: 1.15;
  letter-spacing: 0.04em;
}

/* Status + annotations + notes + jira indicators share one fixed-width column, so they
   must never be squashed: a shrunk v-icon collapses to nothing and the
   indicator silently disappears instead of the column overflowing visibly.
   The column width itself is set in App.vue. */
.icon-cell {
  border: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
}
.icon-cell > * {
  flex-shrink: 0;
}

h3 {
  margin: 40px 0 0;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  display: inline-block;
  margin: 0 10px;
}
a {
  color: #42b983;
}
/* Center the header label for columns whose body cells are centered.
   EasyDataTable's header-text-direction is table-wide, so we target
   just these columns via a per-column class instead. */
:deep(th.center-header .header) {
  justify-content: center !important;
}
:deep(th.center-header .header > div) {
  text-align: center;
}

/* Group headers styled as clean, modern expandable rows instead of
   stacked grey blocks. */
.group-panels {
  gap: 4px;
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
  transition: background 0.15s ease, border-color 0.15s ease;
}
:deep(.group-header:hover) {
  background: #eef2f7;
}
:deep(.v-expansion-panel-title--active.group-header) {
  background: #eef2ff;
  border-color: #c7d2fe;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}
.group-chevron {
  color: #64748b;
  margin-right: 8px;
  transition: transform 0.2s ease;
}
.group-chevron--open {
  transform: rotate(90deg);
}
.group-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
/* Left side of the group header: name chip + distinct label chips, wrapping. */
.group-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
  flex: 1 1 auto;
}
</style>
