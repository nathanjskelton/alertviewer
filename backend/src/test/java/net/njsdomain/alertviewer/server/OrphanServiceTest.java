package net.njsdomain.alertviewer.server;

import com.mongodb.client.result.DeleteResult;
import net.njsdomain.alertviewer.data.AlertManagerEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * An alert is only ever resolved by the ingest pass for the alertmanager it came from, so
 * one whose alertmanager has left the configuration is stuck firing for good. These are
 * the alerts the UI offers to delete, and the one thing that must never happen is
 * offering to delete everything because the configuration failed to load.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrphanServiceTest {

    @Mock
    StateBuffer state;

    @Mock
    MongoTemplate mongo;

    @InjectMocks
    OrphanService orphans;

    private void configured(String... names) {
        given(state.getAlertmanagerNames()).willReturn(Arrays.asList(names));
    }

    private void inDatabase(String... alertmanagers) {
        given(mongo.findDistinct(any(Query.class), eq("alertmanager"),
                eq(AlertManagerEntry.class), eq(String.class))).willReturn(Arrays.asList(alertmanagers));
    }

    @Test
    void namesOnlyTheAlertmanagersTheConfigurationDoesNot() {
        configured("alertmanager1", "alertmanager2");
        inDatabase("alertmanager1", "alertmanager2", "demo");
        assertEquals(List.of("demo"), orphans.orphanedAlertmanagers());
    }

    @Test
    void findsNothingWhenEveryAlertmanagerIsStillConfigured() {
        configured("alertmanager1", "alertmanager2");
        inDatabase("alertmanager1", "alertmanager2");
        assertTrue(orphans.orphanedAlertmanagers().isEmpty());
    }

    @Test
    void anEntryCarryingNoAlertmanagerIsStrandedTheSameWay() {
        //nothing writes one, but it would be resolved by no pass either
        configured("alertmanager1");
        inDatabase("alertmanager1", null, "");
        assertEquals(List.of(OrphanService.UNSET), orphans.orphanedAlertmanagers());
    }

    @Test
    void reportsEachOrphanedAlertmanagerOnce() {
        configured("alertmanager1");
        inDatabase("retired-am", "demo", "demo");
        assertEquals(List.of("demo", "retired-am"), orphans.orphanedAlertmanagers());
    }

    @Test
    void aConfigurationThatNamesNoAlertmanagerOrphansNothing() {
        //the whole database would otherwise look orphaned, and the UI would offer to
        //delete every alert in it because a property failed to load
        configured();
        inDatabase("alertmanager1", "demo");
        assertTrue(orphans.orphanedAlertmanagers().isEmpty());
        assertEquals(0, orphans.count());
        assertEquals(0, orphans.delete());
        verify(mongo, never()).remove(any(Query.class), eq(AlertManagerEntry.class));
        verify(mongo, never()).count(any(Query.class), eq(AlertManagerEntry.class));
    }

    @Test
    void countsAndDeletesEverythingTheConfiguredNamesDoNotAccountFor() {
        configured("alertmanager1", "alertmanager2");
        given(mongo.count(any(Query.class), eq(AlertManagerEntry.class))).willReturn(16L);
        given(mongo.remove(any(Query.class), eq(AlertManagerEntry.class)))
                .willReturn(DeleteResult.acknowledged(16));

        assertEquals(16L, orphans.count());
        assertEquals(16L, orphans.delete());

        //the query has to be "not one of the configured names" rather than a list of the
        //orphans found, so an entry carrying no alertmanager is swept up too
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(AlertManagerEntry.class));
        String json = query.getValue().getQueryObject().toJson();
        assertTrue(json.contains("$nin"), json);
        assertTrue(json.contains("alertmanager1") && json.contains("alertmanager2"), json);
    }

    @Test
    void sortsTheNamesSoTheDialogReadsTheSameEveryTime() {
        configured("alertmanager1");
        inDatabase("zulu", "alpha", "mike");
        assertEquals(List.of("alpha", "mike", "zulu"), orphans.orphanedAlertmanagers());
    }

    @Test
    void anEmptyDatabaseHasNoOrphans() {
        configured("alertmanager1");
        inDatabase();
        assertEquals(Collections.emptyList(), orphans.orphanedAlertmanagers());
    }
}
