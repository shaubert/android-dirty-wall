package com.shaubert.dirty.db;

import android.text.TextUtils;

public class SqlHelper {

    public static String buildAndSelection(String expr1, String expr2) {
        boolean empty1 = TextUtils.isEmpty(expr1);
        boolean empty2 = TextUtils.isEmpty(expr2);
        if (empty1) {
            return expr2;
        } else if (empty2) {
            return  expr1;
        } else {
            return "(" + expr1 + ") AND (" + expr2 + ")";
        }
    }

}
