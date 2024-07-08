package edu.ucsd.sbrg.util;

import org.sbml.jsbml.AbstractSBase;

public record ProgressUpdate(String text, AbstractSBase obj) {
}
