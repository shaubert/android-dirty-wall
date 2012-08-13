package com.shaubert.util;

import android.content.Context;
import android.os.AsyncTask;

public class AsyncTasks {

    public static abstract class Task<T extends Context> {
        private T context;

        public Task(T context) {
            this.context = context;
        }
        
        public abstract void run(T context);
        
        public void onUiThread(T context) {
            
        }
    }
    
    public static <T extends Context> void executeInBackground(final Task<T> task) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                task.run(task.context);
                return null;
            }
            
            protected void onPostExecute(Void result) {
                task.onUiThread(task.context);
            };
        }.execute();
    }
    
}
