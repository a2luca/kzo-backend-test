package de.firma.kuze.mandant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Die Bean existiert nur in der Instanz für den Mandanten ODR. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = Mandant.PROPERTY, havingValue = "odr")
public @interface NurOdr {
}
