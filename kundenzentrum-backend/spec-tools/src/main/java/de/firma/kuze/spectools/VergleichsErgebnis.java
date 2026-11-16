package de.firma.kuze.spectools;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public record VergleichsErgebnis(List<SpecOperation> gemeinsam,
                                 List<SpecOperation> nurNetze,
                                 List<SpecOperation> nurOdr,
                                 List<Abweichung> abweichend) {

    public record Abweichung(SpecOperation netze, SpecOperation odr, List<String> unterschiede) {
        public String operationId() {
            return netze.operationId();
        }
    }

    /** Inhalt von {@code common.yaml}. */
    public Set<String> idsGemeinsam() {
        return ids(gemeinsam.stream().map(SpecOperation::operationId));
    }

    /** Inhalt von {@code netze-rest.yaml}: nur in Netze plus abweichend. */
    public Set<String> idsNetzeRest() {
        return ids(Stream.concat(nurNetze.stream().map(SpecOperation::operationId),
                abweichend.stream().map(Abweichung::operationId)));
    }

    /** Inhalt von {@code odr-rest.yaml}: nur in ODR plus abweichend. */
    public Set<String> idsOdrRest() {
        return ids(Stream.concat(nurOdr.stream().map(SpecOperation::operationId),
                abweichend.stream().map(Abweichung::operationId)));
    }

    private static Set<String> ids(Stream<String> ids) {
        return ids.collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
    }
}
