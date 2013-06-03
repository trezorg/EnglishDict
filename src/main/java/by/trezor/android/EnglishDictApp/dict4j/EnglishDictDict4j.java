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


public class EnglishDictDict4j {

    private DictSession session;
    static private final String DICT_SERVER = "dict.mova.org";
    static private final int MAX_RESULTS = 20;

    public static enum EnglishDictDict4jBase {

        EN (0),
        RU (1);

        private final String base;
        private final Pattern pattern;
        private static final String EN_BASE = "en-ru";
        private static final String RU_BASE = "ru-en";

        EnglishDictDict4jBase(int lang) {
            if (lang == RUSSIAN_WORDS) {
                this.base = RU_BASE;
                this.pattern = ENGLISH_LETTERS_PATTERN;
            } else {
                this.base = EN_BASE;
                this.pattern = RUSSIAN_LETTERS_PATTERN;
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

    private Collection<String> parseDefinition(String text, EnglishDictDict4jBase base) {
        String answer = text.replaceAll("(?ms)^\\w+$", "").replaceAll("(?ms)^\\s+|\\s+$", "");
        String[] lines = answer.split("\\d+>");
        Collection<String> result = new HashSet<String>();
        for (String line: lines) {
            String part = line.replaceAll("_\\S+", "").trim();
            if (part.isEmpty()) continue;
            String[] parts = part.trim().split("[;\n,]");
            for (String str: parts) {
                String s = str.trim();
                if (s.isEmpty()) continue;                
                if (base.getPattern().matcher(s).matches()) {
                    result.add(s);
                }
            }
        }
        return result;
    }
    
    private List<String> getDefenition(String word, EnglishDictDict4jBase base) {
        List<Definition> defs = session.define(word, base.getDataBase());
        Collection<String> result = new HashSet<String>();
        for (Definition def: defs) {
            result.addAll(parseDefinition(def.getText(), base));
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

    public List<String> getWords(String word, int lang, int maxResults) {
        openSession();
        List<String> res = getDefenition(word, lang == RUSSIAN_WORDS ?
                EnglishDictDict4jBase.RU : EnglishDictDict4jBase.EN);
        closeSession();
        if (res.size() > maxResults) res = res.subList(0, maxResults);
        return res;
    }

    public List<String> getWords(String word, int lang) {
        return getWords(word, lang, MAX_RESULTS);

    }

    public static void main(String[] args) {
        EnglishDictDict4j dict = new EnglishDictDict4j();
        List<String> list = dict.getWords("prime", ENGLISH_WORDS);
        for (String st: list) {
            System.out.println(st);
        }
    }

}
