package net.njsdomain.alertviewer.data.jira;

import java.util.ArrayList;
import java.util.List;

/**
 * The jira labels an alert's ticket might carry.
 *
 * A ticket is found by a label spelling the alert's fingerprint, and the label is built
 * in two halves. This tool asks wraith to label a ticket "<jira.label.prefix>:<fingerprint>",
 * and wraith's generic endpoint puts its own "wraith:" in front of whatever it is asked
 * for, so a ticket raised here lands in jira as "wraith:<jira.label.prefix>:<fingerprint>".
 * Tickets raised by anything else carry a prefix of their own -- wraith's alertmanager
 * webhook stamps "alertmanager:<fingerprint>", other tools stamp theirs -- and jira cannot
 * be searched for a label by its ending, JQL matches labels whole, so the prefixes to look
 * under are configured too, in jira.label.search.prefixes.
 */
public final class JiraLabels {

    /** What this tool asks wraith to label a ticket with, when nothing is configured. */
    public static final String DEFAULT_PREFIX = "cortana";

    /** What tickets raised by wraith's alertmanager webhook carry. */
    public static final String DEFAULT_SEARCH_PREFIX = "alertmanager";

    //wraith's generic endpoint hardcodes this in front of the label it is asked for, so
    //a ticket raised here can only ever be found under it
    private static final String WRAITH_PREFIX = "wraith";

    private JiraLabels() {}

    /** The prefix asked of wraith for a ticket raised here, sans wraith's own. */
    public static String prefix(String configured) {
        String prefix = clean(configured);
        return prefix.isEmpty() ? DEFAULT_PREFIX : prefix;
    }

    /**
     * The prefix a ticket raised here actually carries in jira: what was asked for with
     * wraith's own in front of it. This is the label to show for an alert, and the one to
     * paste into a jira search.
     */
    public static String createdPrefix(String configured) {
        return WRAITH_PREFIX + ":" + prefix(configured);
    }

    /**
     * Every prefix to look for an alert's ticket under, in the order configured, without
     * blanks or repeats. What tickets raised here carry is always searched, appended if
     * the list leaves it out: a rebuild that cannot find them would clear its own links.
     */
    public static List<String> searchPrefixes(String configured, String createPrefix) {
        List<String> prefixes = new ArrayList<>();
        for (String part : (configured == null ? "" : configured).split(",")) {
            String prefix = clean(part);
            if (!prefix.isEmpty() && !prefixes.contains(prefix)) { prefixes.add(prefix); }
        }
        String own = createdPrefix(createPrefix);
        if (!prefixes.contains(own)) { prefixes.add(own); }
        return prefixes;
    }

    /** Every label the alert of this fingerprint could be carrying, one per prefix. */
    public static List<String> labelsFor(List<String> prefixes, String fingerprint) {
        List<String> labels = new ArrayList<>();
        for (String prefix : prefixes) {
            labels.add(prefix + ":" + fingerprint);
        }
        return labels;
    }

    /**
     * The fingerprint out of a label or a label's tail, whatever prefix it was given.
     * Fingerprints carry no colon, so the last segment is the whole of it.
     */
    public static String fingerprintOf(String label) {
        if (label == null) { return null; }
        return label.substring(label.lastIndexOf(':') + 1);
    }

    //a prefix written with the separator already on it, "cortana:", would otherwise build
    //"cortana::<fingerprint>" and match nothing
    private static String clean(String prefix) {
        return (prefix == null) ? "" : prefix.trim().replaceAll(":+$", "").trim();
    }
}
