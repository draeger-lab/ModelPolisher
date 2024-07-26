package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.annotation.AnnotationException;

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
