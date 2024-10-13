package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.AnnotationException;

public class BiGGAnnotationException extends AnnotationException {

    private final Object data;

    public BiGGAnnotationException(String msg, Exception e, Object o) {
        super(msg, e);
        this.data = o;
    }

    public Object data() {
        return data;
    }
}
