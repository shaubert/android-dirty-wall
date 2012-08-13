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
                R.drawable.jir_eblo2,
                R.drawable.jir_eblo3,
                R.drawable.jir_eblo4,
                R.drawable.jir_eblo5,
                
                R.drawable.med_eblo1,
                R.drawable.med_eblo2,
                R.drawable.med_eblo3,
                R.drawable.med_eblo4,
                R.drawable.med_eblo5,
                
                R.drawable.mudalcov_1,
                R.drawable.mudalcov_2,
                R.drawable.mudalcov_3,
                R.drawable.mudalcov_4,
                R.drawable.mudalcov_5,
                
                R.drawable.prohor_eblo1,
                R.drawable.prohor_eblo2,
                R.drawable.prohor_eblo3,
                R.drawable.prohor_eblo4,
                R.drawable.prohor_eblo5,
                
                R.drawable.put_eblo1,
                R.drawable.put_eblo2,
                R.drawable.put_eblo3,
                R.drawable.put_eblo4,
                R.drawable.put_eblo5,
                
                R.drawable.ziuga_eblo1,
                R.drawable.ziuga_eblo2,
                R.drawable.ziuga_eblo3,
                R.drawable.ziuga_eblo4,
                R.drawable.ziuga_eblo5,
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
    
    public int getRandomFaceImageId() {
        return postLoadedMuden.getImageId();
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
}
