package by.trezor.android.EnglishDictApp.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class EnglishDictDescriptor {

    private EnglishDictDescriptor() {}
    public static final String AUTHORITY = "by.trezor.android.EnglishDictApp";

    public static class EnglishDictBaseColumns implements BaseColumns {

        private EnglishDictBaseColumns () {}

        public static enum SORT_ORDER {

            RATING("rating", false),
            RATING_REVERSE("rating", true),
            WORD("word", false),
            WORD_REVERSE("word", true);

            private String sortName;
            private Boolean sortReverse;

            SORT_ORDER(String sortString, boolean sortReverse) {
                this.sortName = sortString;
                this.sortReverse = sortReverse;
            }

            public String getValue() {
                return sortName;
            }

            public boolean isReverse() {
                return sortReverse;
            }

            public boolean isWordOrdering() {
                return this.getValue().equals("word");
            }

            public boolean isRatingOrdering() {
                return this.getValue().equals("rating");
            }

            @Override
            public String toString() {
                return String.format("%s%s", getValue(), isReverse() ? " DESC" : "");
            }
        }

        public static final String WORD = "word";
        public static final String ORDER_BY = "order_by";
        public static final String QUERY_PARAM_NAME = WORD;
        public static final String QUERY_PARAM_ORDER_BY = ORDER_BY;
        public static final String QUERY_RELATION_NAME = "relationId";
        public static final String QUERY_RELATION_WORD = "relationWord";
        public static final String RATING = "rating";
        public static final String LIMIT = "limit";
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.english.items";
        public static final String CONTENT_WORD_TYPE =
                "vnd.android.cursor.item/vnd.english.item";
        public static final int DEFAULT_RANDOM_LIMIT = 4;
        public static final int __ID = 0;
    }

    public static final class EnglishDictEnglishWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "english";
        // uri references all words
        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);
        public static final Uri CONTENT_URI = WORDS_URI;
    }

    public static final class EnglishDictRussianWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "russian";
        // uri references all words
        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);
        public static final Uri CONTENT_URI = WORDS_URI;
    }

    public static final class EnglishDictDetailEnglishWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "englishId";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;

    }

    public static final class EnglishDictDetailRussianWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "russianId";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;
    }

    public static final class EnglishDictRandomEnglishWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "englishRandom";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;

    }

    public static final class EnglishDictRandomRussianWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "russianRandom";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;
    }

    public static final class EnglishDictTrainingEnglishWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "englishTraining";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;

    }

    public static final class EnglishDictTrainingRussianWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "russianTraining";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;
    }

    public static final class EnglishDictDetailByNameEnglishWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "englishName";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;

    }

    public static final class EnglishDictDetailByNameRussianWords extends EnglishDictBaseColumns {

        public static final String WORDS_NAME = "russianName";

        public static final Uri WORDS_URI = Uri.parse("content://" +
                AUTHORITY + "/" + WORDS_NAME);

        public static final Uri CONTENT_URI = WORDS_URI;
    }
}
