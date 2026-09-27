package net.njsdomain.alertviewer.data.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A ticket raised here is labelled in two halves -- what this tool asks for, behind the
 * "wraith:" wraith's generic endpoint adds itself -- while a ticket raised by anything
 * else carries a prefix of its own. So one prefix out, a list of them in.
 */
class JiraLabelsTest {

    private static final String FP = "1a30ba71cca2921f";

    @Test
    void theCreatePrefixIsASingleValueFallingBackToWhatThisToolHasAlwaysAskedFor() {
        assertEquals("cortana", JiraLabels.prefix(" cortana "));
        //written with the separator already on it, it would ask for "abc::<fingerprint>"
        assertEquals("abc", JiraLabels.prefix("abc:"));
        assertEquals(JiraLabels.DEFAULT_PREFIX, JiraLabels.prefix(null));
        assertEquals(JiraLabels.DEFAULT_PREFIX, JiraLabels.prefix("  "));
    }

    @Test
    void aTicketRaisedHereCarriesWraithsPrefixInFrontOfTheConfiguredOne() {
        //wraith's generic endpoint hardcodes the "wraith:", so asking for "abc" yields
        //"wraith:abc:<fingerprint>" in jira, not "abc:<fingerprint>"
        assertEquals("wraith:abc", JiraLabels.createdPrefix("abc"));
        assertEquals("wraith:cortana", JiraLabels.createdPrefix(null));
    }

    @Test
    void searchesTheListInOrder() {
        assertEquals(List.of("alertmanager", "otherTool", "wraith:cortana"),
                JiraLabels.searchPrefixes("alertmanager, otherTool", "cortana"));
    }

    @Test
    void ignoresBlanksRepeatsAndASeparatorLeftOnAPrefix() {
        assertEquals(List.of("alertmanager", "otherTool", "wraith:cortana"),
                JiraLabels.searchPrefixes(" alertmanager , ,otherTool:,alertmanager,", "cortana"));
    }

    @Test
    void alwaysSearchesWhatTicketsRaisedHereCarry() {
        //a rebuild that cannot find what this tool raised would clear its own links
        assertEquals(List.of("alertmanager", "wraith:abc"), JiraLabels.searchPrefixes("alertmanager", "abc"));
        assertEquals(List.of("wraith:cortana"), JiraLabels.searchPrefixes("", null));
        //listed already, it is not searched twice or moved to the end
        assertEquals(List.of("wraith:abc", "alertmanager"),
                JiraLabels.searchPrefixes("wraith:abc,alertmanager", "abc"));
        //the prefix asked of wraith is not itself a label jira holds, so listing it does
        //not stand in for the full one
        assertEquals(List.of("abc", "wraith:abc"), JiraLabels.searchPrefixes("abc", "abc"));
    }

    @Test
    void buildsOneLabelPerPrefix() {
        assertEquals(List.of("alertmanager:" + FP, "wraith:cortana:" + FP),
                JiraLabels.labelsFor(List.of("alertmanager", "wraith:cortana"), FP));
    }

    @Test
    void readsTheFingerprintOffALabelHoweverItWasPrefixed() {
        assertEquals(FP, JiraLabels.fingerprintOf(FP));
        assertEquals(FP, JiraLabels.fingerprintOf("cortana:" + FP));
        assertEquals(FP, JiraLabels.fingerprintOf("wraith:cortana:" + FP));
        assertNull(JiraLabels.fingerprintOf(null));
    }

    @Test
    void findsTheTicketUnderWhicheverPrefixCarriesIt() throws Exception {
        //tickets raised here land under "wraith:cortana", the webhook's under "alertmanager"
        String json = "{\"alertmanager:" + FP + "\":[],\"wraith:cortana:" + FP + "\":[{\"key\":\"GMDEV-52\",\"status\":\"open\"}]}";
        assertEquals("GMDEV-52", WraithTickets.key(preferred(json)));
    }

    @Test
    void anOpenTicketWinsOverAClosedOneUnderAnEarlierPrefix() throws Exception {
        String json = "{\"alertmanager:" + FP + "\":[{\"key\":\"GMDEV-50\",\"status\":\"closed\"}]," +
                "\"wraith:cortana:" + FP + "\":[{\"key\":\"GMDEV-52\",\"status\":\"open\"}]}";
        assertEquals("GMDEV-52", WraithTickets.key(preferred(json)));
    }

    @Test
    void fallsBackToTheFirstPrefixsClosedTicketWhenNothingIsOpen() throws Exception {
        String json = "{\"alertmanager:" + FP + "\":[{\"key\":\"GMDEV-50\",\"status\":\"closed\"}]," +
                "\"wraith:cortana:" + FP + "\":[{\"key\":\"GMDEV-52\",\"status\":\"closed\"}]}";
        assertEquals("GMDEV-50", WraithTickets.key(preferred(json)));
    }

    @Test
    void choosesNothingWhenNoPrefixCarriesATicket() throws Exception {
        assertNull(preferred("{\"alertmanager:" + FP + "\":[],\"wraith:cortana:" + FP + "\":[]}"));
        //the label as the tool asked for it, without wraith's prefix, is not on any ticket
        assertNull(preferred("{\"cortana:" + FP + "\":[{\"key\":\"GMDEV-52\",\"status\":\"open\"}]}"));
    }

    private JsonNode preferred(String json) throws Exception {
        return WraithTickets.preferred(new ObjectMapper().readTree(json),
                JiraLabels.labelsFor(JiraLabels.searchPrefixes("alertmanager", "cortana"), FP));
    }
}
