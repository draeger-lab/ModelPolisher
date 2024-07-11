package edu.ucsd.sbrg.reporting;

import org.sbml.jsbml.AbstractSBase;

public record ProgressUpdate(String text, AbstractSBase obj) {
}
