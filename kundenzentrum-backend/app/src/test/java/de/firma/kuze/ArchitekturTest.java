package de.firma.kuze;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import de.firma.kuze.mandant.Mandant;
import de.firma.kuze.mandant.NurNetze;
import de.firma.kuze.mandant.NurOdr;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Die Architekturregeln aus Abschnitt 3. Welche Klasse zu welchem Modul gehört, wird über den
 * Ablageort der Klassendatei bestimmt (".../core/build/..."), nicht über den Paketnamen.
 */
@AnalyzeClasses(packages = "de.firma.kuze", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitekturTest {

    private static DescribedPredicate<JavaClass> ausModul(String modul) {
        return DescribedPredicate.describe("aus Modul " + modul, klasse -> klasse.getSource()
                .map(quelle -> quelle.getUri().toString().contains("/" + modul + "/build/"))
                .orElse(false));
    }

    @ArchTest
    static final ArchRule coreKenntKeinSap = noClasses().that(ausModul("core"))
            .should().dependOnClassesThat().resideInAPackage("de.firma.kuze.sap..")
            .because("der Core nur über Port-Interfaces mit SAP spricht (Regel 1)");

    @ArchTest
    static final ArchRule coreMachtKeinHttpRichtungSap = noClasses().that(ausModul("core"))
            .should().dependOnClassesThat().resideInAPackage("org.springframework.web.client..")
            .because("HTTP-Code Richtung SAP in die Adapter gehört (Regel 1)");

    @ArchTest
    static final ArchRule netzeGreiftNichtAufOdrZu = noClasses().that(ausModul("adapter-netze"))
            .should().dependOnClassesThat().resideInAPackage("de.firma.kuze.sap.odr..")
            .because("adapter-netze und adapter-odr nicht voneinander abhängen (Regel 7)");

    @ArchTest
    static final ArchRule odrGreiftNichtAufNetzeZu = noClasses().that(ausModul("adapter-odr"))
            .should().dependOnClassesThat().resideInAPackage("de.firma.kuze.sap.netze..")
            .because("adapter-netze und adapter-odr nicht voneinander abhängen (Regel 7)");

    @ArchTest
    static final ArchRule commonKenntKeineMandantenAdapter = noClasses().that(ausModul("adapter-common"))
            .should().dependOnClassesThat().resideInAnyPackage("de.firma.kuze.sap.netze..", "de.firma.kuze.sap.odr..")
            .because("adapter-common nur von core abhängt (Regel 7)");

    @ArchTest
    static final ArchRule commonHatKeineMandantenAnnotationen = noClasses().that(ausModul("adapter-common"))
            .should().beAnnotatedWith(NurNetze.class).orShould().beAnnotatedWith(NurOdr.class)
            .because("derselbe Code in beiden Instanzen läuft (Abschnitt 10)");

    @ArchTest
    static final ArchRule keineMandantenWeicheImCode = noClasses().that().resideOutsideOfPackage("de.firma.kuze.mandant..")
            .should().dependOnClassesThat().areAssignableTo(Mandant.class)
            .because("Unterschiede über Beans gelöst werden, nicht über if (mandant == ...) (Regel 3)");

    @ArchTest
    static final ArchRule generierteKlassenNurImAdapter = noClasses().that().resideOutsideOfPackage("de.firma.kuze.sap..")
            .should().dependOnClassesThat().resideInAPackage("de.firma.kuze.sap.*.client..")
            .because("generierte Klassen im Adapter bleiben (Regel 2)");
}
