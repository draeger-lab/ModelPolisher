package de.uni_halle.informatik.biodata.mp.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DeleteOnCloseFileInputStream extends FileInputStream {

    File file;

    public DeleteOnCloseFileInputStream(String s) throws FileNotFoundException {
        this(new File(s));
    }

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (file != null) {
                if (!file.delete()) {
                    throw new IOException("Could not delete file on close.");
                }
                file = null;
            }
        }
    }
}


