package edu.berkeley.nlp.mt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import edu.berkeley.nlp.util.Logger;
import fig.basic.IOUtils;
import fig.basic.LogInfo;
import fig.basic.String2DoubleMap;

/**
 * A lookup table interface for a bilingual dictionary from foreign (fr) to
 * English (en).
 */
public interface WordMap {
    public boolean contains(String fr, String en);

    public Collection<String> ens(String fr);

    public Collection<String> frs(String en);

    public class String2DoubleWordMap extends String2DoubleMap implements WordMap {
        private static final long serialVersionUID = 1L;

        private String2DoubleWordMap() {
        }

        String2DoubleMap en2fr = new String2DoubleMap();


        public static String2DoubleWordMap fromFile(String file, boolean splitDefinitions,
                boolean lowercaseWords) {
            return fromFile(new File(file), splitDefinitions, lowercaseWords);
        }

        private static String2DoubleWordMap fromFile(File file, boolean splitDefinitions,
                boolean lowercaseWords) {
            String2DoubleWordMap dict = new String2DoubleWordMap();
            BufferedReader dictReader = IOUtils.openInEasy(file);
            if (dictReader == null) {
                return null;
            } else {
                try {
                    Logger.startTrack("Loading dictionary from %s", file.getAbsolutePath());
                    while (dictReader.ready()) {
                        String[] words = dictReader.readLine().split("\\t");
                        String[] translations = words[1].split("/");
                        for (int i = 1; i < translations.length; i++) {
                            String translation = translations[i];
                            if (lowercaseWords) {
                                translation = translation.toLowerCase();
                            }
                            if (splitDefinitions) {
                                String[] transwords = translation.split(" ");
                                int len = transwords.length;
                                for (int j = 0; j < len; j++) {
                                    dict.incr(words[0].intern(), transwords[j].intern(),
                                            1.0 / len);
                                }
                            } else {
                                dict.incr(words[0].intern(), translation.intern(), 1);
                            }
                        }
                    }
                    dict.switchToSortedList();
                    Logger.endTrack();
                    return dict;
                } catch (IOException e) {
                    LogInfo.error("Problem loading dictionary file: "
                            + file.getAbsolutePath());
                    Logger.endTrack();
                    return null;
                }
            }
        }

        public boolean contains(String fr, String en) {
            return containsKey(fr, en);
        }

        public Collection<String> ens(String fr) {
            throw new UnsupportedOperationException();
        }

        public Collection<String> frs(String fr) {
            return getMap(fr, true).keySet();
        }
    }
}
