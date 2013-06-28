package com.shaubert.dirty;

import com.shaubert.util.PluralHelper;
import com.shaubert.util.PluralHelper.PluralForm;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.Random;

public class DirtyMessagesProvider {

    private static class Muden {
        private Random random = new Random();

        private int[] ebliki;
        private String[] lyalya;

        public Muden(int[] faces, String[] texts) {
            this.ebliki = faces;
            this.lyalya = texts;
        }

        public int getImageId() {
            return ebliki[random.nextInt(ebliki.length)];
        }
        
        public String getMessage() {
            return lyalya[random.nextInt(lyalya.length)];
        }
    }

    private Muden postLoadedMuden;
    private Muden errorMuden;
    private Context appContext;

    private static DirtyMessagesProvider instance;
    
    public static DirtyMessagesProvider getInstance(Context appContext) {
        if (instance == null) {
            instance = new DirtyMessagesProvider(appContext.getApplicationContext());
        }
        return instance;
    }
    
    private DirtyMessagesProvider(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        
        int[] faces = new int[] {
                R.drawable.jir_eblo1,
                R.drawable.jir_eblo10,
                R.drawable.jir_eblo11,
                R.drawable.jir_eblo13,
                R.drawable.jir_eblo9,
                
                R.drawable.med_eblo4,
                
                R.drawable.chak_chak_14,
                R.drawable.chak_chak_8,
                
                R.drawable.griyzl_eblo2,
                R.drawable.griyzl_eblo4,
                R.drawable.griyzl_eblo7,
                
                R.drawable.prohor_eblo8,
                
                R.drawable.ochishenko_2,
                R.drawable.ochishenko_3,
                R.drawable.ochishenko_4,
                R.drawable.ochishenko_7,
                R.drawable.ochishenko_10,
                
                R.drawable.put_eblo3,
                R.drawable.put_eblo4,
                R.drawable.put_eblo5,
                
                R.drawable.ziuga_eblo12,
                
                R.drawable.kirill_2,
                R.drawable.kirill_3,
                
                R.drawable.gera_4,
                R.drawable.gera_5,
                R.drawable.gera_6,
        };
        
        this.postLoadedMuden = new Muden(faces, appContext.getResources().getStringArray(R.array.new_post_formats)); 
        this.errorMuden = new Muden(faces, appContext.getResources().getStringArray(R.array.error_messages));
    }

    public CharSequence getNewPostsString(int count) {
    	PluralForm form = PluralHelper.getForm(count);
        String countSrt = null; 
        switch (form) {
            case ONE:
                countSrt = appContext.getString(R.string.one_new_post);
                break;
            case FEW:
                countSrt = appContext.getString(R.string.few_new_posts);
                break;
            default:
                countSrt = appContext.getString(R.string.many_new_posts);
                break;
        }
        return String.format(countSrt, count);
    }
    
    public CharSequence getMessageForNewPosts(int count) {
        String countSrt = getNewPostsString(count).toString();
        String mes = String.format(postLoadedMuden.getMessage(), countSrt);
        SpannableStringBuilder result = new SpannableStringBuilder(mes);
        int start = mes.indexOf(countSrt);
        result.setSpan(new ForegroundColorSpan(appContext.getResources().getColor(R.color.aqua)), start, start + countSrt.length(), 
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }

    public CharSequence getSimpleMessageForNewPosts(int count) {
        return getNewPostsString(count).toString();
    }
    
    public int getRandomFaceImageId() {
        return postLoadedMuden.getImageId();
    }

    public CharSequence getSimpleErrorMessage() {
        return appContext.getText(R.string.simple_error_text);
    }

    public CharSequence getErrorMessage() {
        SpannableStringBuilder result = new SpannableStringBuilder(errorMuden.getMessage());
        result.setSpan(new ForegroundColorSpan(appContext.getResources().getColor(R.color.error_red)), 
                0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return result;
    }
    
    public CharSequence getFavoritesExportFinishedMessage(String path) {
        return appContext.getText(R.string.favorites_export_format);
    }

    public CharSequence getZeroFavoritesMessage() {
        return appContext.getText(R.string.favorites_export_zero);
    }

    public CharSequence getZeroFavoritesMessageCompact() {
        return appContext.getText(R.string.favorites_export_zero_compact);
    }
}
