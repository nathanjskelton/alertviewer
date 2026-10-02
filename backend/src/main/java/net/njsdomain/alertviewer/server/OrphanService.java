package net.njsdomain.alertviewer.server;

import com.mongodb.client.result.DeleteResult;
import net.njsdomain.alertviewer.data.AlertManagerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Alerts left behind by an alertmanager that is no longer configured.
 *
 * An alert only ever stops firing through the ingest pass for the alertmanager it came
 * from: that pass resolves the entries its alertmanager has stopped reporting, and it
 * considers none but the entries whose alertmanager matches the config being ingested.
 * Take an alertmanager out of the configuration -- decommissioned, renamed, split onto
 * another instance of this tool -- and everything it left behind is stranded. No pass
 * will ever resolve it, and the retention sweep only deletes alerts that reached
 * RESOLVED, so it reads as firing for good and cannot be cleared from the UI.
 *
 * Deleting them is a button rather than something the ingest does by itself, because the
 * two reasons an alertmanager leaves the configuration look identical from here: it was
 * retired, or somebody fat-fingered a property. The second must not cost the alerts it
 * was holding, so nothing goes without being asked for.
 */
@Component
public class OrphanService {

    private static final Logger log = LoggerFactory.getLogger(OrphanService.class);

    /** How an entry carrying no alertmanager at all is named in the UI. */
    public static final String UNSET = "(unset)";

    @Autowired
    StateBuffer state;

    @Autowired
    MongoTemplate mongo;

    /**
     * The alertmanagers that alerts in the database name and the configuration does not,
     * in the order they read. Empty when the configuration names none at all: a config
     * that failed to load must not make every alert in the database look orphaned.
     */
    public List<String> orphanedAlertmanagers() {
        List<String> configured = state.getAlertmanagerNames();
        if (configured.isEmpty()) {
            log.warn("No alertmanagers are configured, so no alert can be called orphaned");
            return Collections.emptyList();
        }
        List<String> orphaned = new ArrayList<>();
        for (String name : mongo.findDistinct(new Query(), "alertmanager", AlertManagerEntry.class, String.class)) {
            String named = (name == null || name.isBlank()) ? UNSET : name;
            if (!configured.contains(name) && !orphaned.contains(named)) { orphaned.add(named); }
        }
        Collections.sort(orphaned);
        return orphaned;
    }

    /** How many alerts those alertmanagers left behind. */
    public long count() {
        Query query = orphanQuery();
        return (query == null) ? 0 : mongo.count(query, AlertManagerEntry.class);
    }

    /** Delete them, and report how many went. */
    public long delete() {
        Query query = orphanQuery();
        if (query == null) { return 0; }
        DeleteResult result = mongo.remove(query, AlertManagerEntry.class);
        return result.getDeletedCount();
    }

    //Every entry the configuration cannot account for, or null when there is nothing to
    //compare against. "not in the configured names" also covers an entry carrying no
    //alertmanager at all, which is stranded in exactly the same way.
    private Query orphanQuery() {
        List<String> configured = state.getAlertmanagerNames();
        if (configured.isEmpty()) { return null; }
        return new Query(Criteria.where("alertmanager").nin(configured));
    }
}
