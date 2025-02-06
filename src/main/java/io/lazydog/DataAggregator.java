package io.lazydog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataAggregator {

    private static final List<String> PLATFORMS = Arrays.asList("A", "B", "C", "D");
    private static final int MAX_PAGE_SIZE = 50;

    /**
     *  模拟各平台数据总数 (实际应用中应从数据库或API获取)
     */
    private static Map<String, Integer> platformDataCounts = new HashMap<>();
    static {
        platformDataCounts.put("A", 20);
        platformDataCounts.put("B", 80);
        platformDataCounts.put("C", 150);
        platformDataCounts.put("D", 200);
    }


    /**
     *  计算查询的起始和结束平台信息
     * @param pageNum  页码 (从1开始)
     * @param pageSize 每页大小 (最大50)
     * @return 查询信息 (包含起始平台、起始平台内的偏移量、结束平台、结束平台内的偏移量)
     */
    public static QueryInfo calculateQueryInfo(int pageNum, int pageSize) {
        if (pageNum <= 0 || pageSize <= 0 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid pageNum or pageSize");
        }

        int startIndex = (pageNum - 1) * pageSize; //全局起始索引
        int endIndex = startIndex + pageSize -1; //全局结束索引

        QueryInfo queryInfo = new QueryInfo();
        int currentTotal = 0;
        String startPlatform = null;
        String endPlatform = null;
        int startPlatformOffset = -1;
        int endPlatformOffset = -1;

        for (String platform : PLATFORMS) {
            int platformCount = platformDataCounts.get(platform);

            // 查找起始平台
            if (startPlatform == null) {
                if (startIndex < currentTotal + platformCount) {
                    startPlatform = platform;
                    startPlatformOffset = startIndex - currentTotal;
                }
            }

            //查找结束平台
            if(endPlatform == null){
                if(endIndex < currentTotal + platformCount){
                    endPlatform = platform;
                    endPlatformOffset = endIndex - currentTotal;
                }
            }

            currentTotal += platformCount;

            if(startPlatform != null && endPlatform != null){
                break; //找到了起始和结束平台，退出循环
            }
        }

        if (startPlatform == null || endPlatform == null) {
            // 数据不足以填满请求的页
            return null; // 或者抛出异常，根据实际需求处理
        }
        queryInfo.setStartPlatform(startPlatform);
        queryInfo.setStartPlatformOffset(startPlatformOffset);
        queryInfo.setEndPlatform(endPlatform);
        queryInfo.setEndPlatformOffset(endPlatformOffset);
        queryInfo.setPageNum(pageNum);
        queryInfo.setPageSize(pageSize);

        return queryInfo;
    }


    /**
     * 模拟从平台查询数据 (实际应用中应调用平台的API)
     * @param platform 平台名称
     * @param offset   平台内的偏移量
     * @param limit    查询数量
     * @return 模拟数据列表
     */
    private static List<String> fetchDataFromPlatform(String platform, int offset, int limit) {
        // 在这里模拟从平台获取数据的逻辑，例如从数据库或API获取
        // 这里简单地返回一个模拟数据列表
        List<String> data = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            data.add(String.format("%s_Data_%d", platform, offset + i + 1));
        }
        return data;
    }


    /**
     *  聚合数据
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 聚合后的数据列表
     */
    public static List<String> aggregateData(int pageNum, int pageSize) {
        QueryInfo queryInfo = calculateQueryInfo(pageNum, pageSize);

        if (queryInfo == null) {
            return new ArrayList<>(); // 或者抛出异常，根据实际需求处理
        }

        List<String> aggregatedData = new ArrayList<>();
        String currentPlatform = queryInfo.getStartPlatform();
        int currentOffset = queryInfo.getStartPlatformOffset();
        int remaining = pageSize; // 剩余需要获取的数据量

        while (remaining > 0 && PLATFORMS.indexOf(currentPlatform) <= PLATFORMS.indexOf(queryInfo.getEndPlatform())) {

            int platformCount = platformDataCounts.get(currentPlatform);
            int limit = Math.min(remaining, MAX_PAGE_SIZE - currentOffset); // 考虑平台内最大查询限制

             // 如果当前平台是结束平台，则限制查询数量
            if (currentPlatform.equals(queryInfo.getEndPlatform())) {
                limit = Math.min(limit, queryInfo.getEndPlatformOffset() - currentOffset + 1);
            }

            List<String> platformData = fetchDataFromPlatform(currentPlatform, currentOffset, limit);
            aggregatedData.addAll(platformData);

            remaining -= platformData.size();
            currentOffset = 0; // 下一个平台的偏移量重置为0

            // 移动到下一个平台
            int nextPlatformIndex = PLATFORMS.indexOf(currentPlatform) + 1;
            if (nextPlatformIndex < PLATFORMS.size()) {
                currentPlatform = PLATFORMS.get(nextPlatformIndex);
            } else {
                break; // 已经到达最后一个平台
            }
        }

        return aggregatedData;
    }



    public static void main(String[] args) {
        int pageNum = 1;
        int pageSize = 50;

        QueryInfo queryInfo = calculateQueryInfo(pageNum, pageSize);
        System.out.println(queryInfo); // 打印查询信息

        List<String> data = aggregateData(pageNum, pageSize);
        System.out.println("Aggregated Data (Page " + pageNum + ", Size " + pageSize + "):");
        for (String item : data) {
            System.out.println(item);
        }

        System.out.println("--------------------");
        // 边界情况测试
        pageNum = 1;
        pageSize = 50;
        queryInfo = calculateQueryInfo(pageNum, pageSize);
        System.out.println(queryInfo);
        data = aggregateData(pageNum, pageSize);
        System.out.println("Aggregated Data (Page " + pageNum + ", Size " + pageSize + "):");
        for (String item : data) {
            System.out.println(item);
        }

        System.out.println("--------------------");
        pageNum = 11; // 最后一页
        pageSize = 50;
        queryInfo = calculateQueryInfo(pageNum, pageSize);
        System.out.println(queryInfo); // 打印查询信息
        data = aggregateData(pageNum, pageSize);
        System.out.println("Aggregated Data (Page " + pageNum + ", Size " + pageSize + "):");
        for (String item : data) {
            System.out.println(item);
        }
    }


    static class QueryInfo {
        private String startPlatform;
        private int startPlatformOffset;
        private String endPlatform;
        private int endPlatformOffset;
        private int pageNum;
        private int pageSize;

        // Getters and setters

        public String getStartPlatform() {
            return startPlatform;
        }

        public void setStartPlatform(String startPlatform) {
            this.startPlatform = startPlatform;
        }

        public int getStartPlatformOffset() {
            return startPlatformOffset;
        }

        public void setStartPlatformOffset(int startPlatformOffset) {
            this.startPlatformOffset = startPlatformOffset;
        }

        public String getEndPlatform() {
            return endPlatform;
        }

        public void setEndPlatform(String endPlatform) {
            this.endPlatform = endPlatform;
        }

        public int getEndPlatformOffset() {
            return endPlatformOffset;
        }

        public void setEndPlatformOffset(int endPlatformOffset) {
            this.endPlatformOffset = endPlatformOffset;
        }
        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        @Override
        public String toString() {
            return "QueryInfo{" +
                    "startPlatform='" + startPlatform + '\'' +
                    ", startPlatformOffset=" + startPlatformOffset +
                    ", endPlatform='" + endPlatform + '\'' +
                    ", endPlatformOffset=" + endPlatformOffset +
                    ", pageNum=" + pageNum +
                    ", pageSize=" + pageSize +
                    '}';
        }
    }
}