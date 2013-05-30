package by.trezor.android.EnglishDictApp.dict4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import net.sf.dict4j.DictSession;
import net.sf.dict4j.entity.Database;
import net.sf.dict4j.entity.Definition;
import net.sf.dict4j.entity.DatabaseWord;

import static by.trezor.android.EnglishDictApp.EnglishDictHelper.*;

class EnglishDictDict4j {

    private DictSession session;
    static private final String DICT_SERVER = "dict.mova.org";

    public static enum EnglishDictDict4jBase {

        EN (0),
        RU (1);

        private final String base;
        private final int lang;
        private final Pattern pattern;
        private static final int EN_LANG = 0;
        private static final int RU_LANG = 1;
        private static final String EN_BASE = "en-ru";
        private static final String RU_BASE = "ru-en";

        EnglishDictDict4jBase(int lang) {
            this.lang = lang;
            if (lang == RU_LANG) {
                this.base = RU_BASE;
                this.pattern = RUSSIAN_LETTERS_PATTERN;
            } else {
                this.base = EN_BASE;
                this.pattern = ENGLISH_LETTERS_PATTERN;
            }
        }

        public Database getDataBase() {
            return new Database(this.base);
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    private void openSession() {
        session = new DictSession(DICT_SERVER);
        session.open("hello");
    }

    private Collection<String> parseDefenition(String text, EnglishDictDict4jBase base) {
        String answer = text.replaceAll("(?ms)^\\w+$", "").replaceAll("(?ms)^\\s+|\\s+$", "");
        String[] lines = answer.split("\\d+>");
        Collection<String> result = new HashSet<String>();
        for (String line: lines) {
            String part = line.replaceAll("_\\S+", "").trim();
            if (part.isEmpty()) continue;
            String[] parts = part.trim().split("[;\n]");
            for (String str: parts) {
                String s = str.trim();
                if (s.isEmpty()) continue;                
                if (base.getPattern().matcher(s).matches()) result.add(s);
            }
        }
        return result;
    }
    
    private List<String> getDefenition(String word, EnglishDictDict4jBase base) {
        List<Definition> defs = session.define(word, base.getDataBase());
        Collection<String> result = new HashSet<String>();
        for (Definition def: defs) {
            result.addAll(parseDefenition(def.getText(), base));
        }
        List<String> lst = new ArrayList<String>();
        lst.addAll(result);
        return lst;
    }

    private void closeSession() {
        session.close();
    }

    private List<String> getMatch(String word) {
        List<DatabaseWord> matches = session.match(word, "!", "prefix");
        List<String> words = new ArrayList<String>();
        for (DatabaseWord match: matches) {
            words.add(match.getWord());  
        }
        return words;
    }

    public List<String> getWords(String word, int lang) {
        openSession();
        List<String> res = getDefenition(word, EnglishDictDict4jBase.valueOf(
                lang == EnglishDictDict4jBase.RU_LANG ? "RU" : "EN"));
        closeSession();
        return res;
    }
}
