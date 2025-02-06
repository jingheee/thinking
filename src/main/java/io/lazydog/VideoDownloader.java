package io.lazydog;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.*;
import java.nio.file.Files;

interface ProgressCallback {
    void update(int progress);
    void log(String message);
}

public class VideoDownloader {
    
    public void downloadWithProgress(String url, File outputFile, ProgressCallback callback) 
        throws IOException {
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new IOException("无内容");
                }
                
                long totalSize = entity.getContentLength();
                callback.log("文件大小: " + totalSize + " bytes");
                
                try (InputStream inputStream = entity.getContent();
                     OutputStream outputStream = Files.newOutputStream(outputFile.toPath())) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long downloaded = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloaded += bytesRead;
                        
                        int progress = (int) ((downloaded * 100) / totalSize);
                        callback.update(progress);
                    }
                }
            }
        }
    }
}