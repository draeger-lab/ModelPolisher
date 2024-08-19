package edu.ucsd.sbrg.cli;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;

import java.io.File;

public interface ConfigOptions extends KeyProvider {

    Option<File> CONFIG = new Option<>("CONFIG_FILE", File.class, "Config file.");
}
