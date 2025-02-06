package io.lazydog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideoDownloaderApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("网页视频下载器");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            
            MainPanel mainPanel = new MainPanel();
            frame.add(mainPanel);
            
            frame.setVisible(true);
        });
    }
}

class MainPanel extends JPanel {
    private JTextField urlField;
    private JButton downloadButton;
    private JProgressBar progressBar;
    private JTextArea logArea;
    
    public MainPanel() {
        setLayout(new BorderLayout(10, 10));
        
        // 顶部输入区域
        JPanel topPanel = new JPanel(new BorderLayout());
        urlField = new JTextField();
        downloadButton = new JButton("下载");
        topPanel.add(new JLabel("视频URL:"), BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(downloadButton, BorderLayout.EAST);
        
        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        
        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        
        add(topPanel, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);
        
        // 绑定下载事件
        downloadButton.addActionListener(e -> startDownload());
    }
    
    private void startDownload() {
        String videoUrl = urlField.getText();
        if (videoUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入视频URL");
            return;
        }
        
        new DownloadWorker(videoUrl).execute();
    }
    
    private class DownloadWorker extends SwingWorker<Void, String> {
        private final String videoUrl;
        
        public DownloadWorker(String videoUrl) {
            this.videoUrl = videoUrl;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            publish("开始下载: " + videoUrl);
            
            File outputFile = new File(getFileNameFromUrl(videoUrl));
            VideoDownloader downloader = new VideoDownloader();
            downloader.downloadWithProgress(videoUrl, outputFile, 
                new ProgressCallback() {
                    @Override
                    public void update(int progress) {
                        setProgress(progress);
                    }
                    
                    @Override
                    public void log(String message) {
                        publish(message);
                    }
                });
            
            return null;
        }
        
        @Override
        protected void process(java.util.List<String> chunks) {
            for (String msg : chunks) {
                logArea.append(msg + "\n");
            }
        }
        
        @Override
        protected void done() {
            setProgress(100);
            logArea.append("下载完成!\n");
        }
        
        private String getFileNameFromUrl(String url) {
            return url.substring(url.lastIndexOf('/') + 1);
        }
    }
}