package com.ptit.android;

public final class Constants {

    public static String STORE_FIREBASE_SERVER = "https://firebasestorage.googleapis.com/v0/b/musicapplication-f21a5.appspot.com/o/";
    public Constants() {
    }

    public static class MODE {
        public static final Long OFFLINE = 1L;
        public static final Long ONLINE = 2L;
    }

    public static class SEARCH_TYPE {
        public static final Long TITLE = 1L;
        public static final Long ARTIST = 2L;
    }
}
