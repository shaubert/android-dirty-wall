package com.shaubert.util;

public class PluralHelper {

    public enum PluralForm {
        ZERO,
        ONE,
        FEW,
        MANY,
        OTHER
    }
    
    public static PluralForm getForm(int count) {
        if (count == 0) {
            return PluralForm.ZERO;
        } else {
            int v = Math.abs(count);
            int n = v % 10; 
            int nn = v % 100;
            if (n == 1 && nn != 11) {
                return PluralForm.ONE;
            } else if (n > 1 && n < 5 && (nn < 12 || nn > 14)) {
                return PluralForm.FEW;
            } else {
                return PluralForm.MANY;
            }
        }
    }
    
}
