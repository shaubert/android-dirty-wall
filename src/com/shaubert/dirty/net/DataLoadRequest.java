
package com.shaubert.dirty.net;

import com.shaubert.net.core.RequestBase;
import com.shaubert.net.core.RequestStateBase;
import com.shaubert.net.nutshell.ExecutionContext;
import com.shaubert.util.Shlog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DataLoadRequest extends RequestBase {
    
    private static final Shlog SHLOG = new Shlog(DataLoadRequest.class.getSimpleName());

    private static final int BUFFER_SIZE = 1024 * 8;

    public DataLoadRequest(String url, String filename) {
        super(null);
        getState().put("url", url);
        getState().put("file", filename);
    }

    public DataLoadRequest(RequestStateBase state) {
        super(state);
    }

    public String getUrl() {
        return getState().getString("url");
    }

    public String getOutputFilename() {
        return getState().getString("file");
    }

    public void execute() throws Exception {
        execute(null);
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        String urlString = getState().getString("url");
        SHLOG.d("parsing url " + urlString);
        URL url = new URL(urlString);
        SHLOG.d("start loading data from " + url);
        HttpURLConnection connection = null;
        FileOutputStream fileOutputStream = null;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            int length = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            if (executionContext != null) {
                inputStream = addProgressListener(inputStream, length, executionContext); 
            }
            fileOutputStream = new FileOutputStream(getState().getString("file"));
            if (!isCancelled()) {
                copy(inputStream, fileOutputStream);
            }
        } catch (IOException e) {
            getState().put("error", e.getMessage());
            throw e;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private InputStream addProgressListener(final InputStream stream, final int totalLength,
            final ExecutionContext executionContext) {
        return new InputStream() {
            private int read;

            @Override
            public int read(byte[] b, int offset, int length) throws IOException {
                int res = stream.read(b, offset, length);
                notifyProgress(res);
                return res;
            }

            @Override
            public int read() throws IOException {
                int res = stream.read();
                notifyProgress(res);
                return res;
            }

            private void notifyProgress(int count) {
                if (count > 0 && totalLength > 0) {
                    read += count;
                    float progress = (float)read / totalLength;
                    if (getState().getProgress() <= 0 || progress - getState().getProgress() > 0.1f) {
                        SHLOG.d("loaded " + progress * 100 + "%");
                        getState().setProgress(progress);
                        executionContext.getRepository().update(DataLoadRequest.this);
                    }
                }
            }

            @Override
            public void close() throws IOException {
                super.close();
                stream.close();
            }
        };
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        return count;
    }
}
