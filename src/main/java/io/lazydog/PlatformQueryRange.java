package io.lazydog;

import java.util.*;

public class PlatformQueryRange {

    // 用于返回查询范围的类
    public static class QueryRange {
        // 开始查询的平台名称、该平台内的起始偏移（从0计数）
        public String startPlatform;
        public int startOffset;
        // 结束查询的平台名称、该平台内的结束偏移
        public String endPlatform;
        public int endOffset;

        @Override
        public String toString() {
            return "QueryRange{" +
                    "startPlatform='" + startPlatform + '\'' +
                    ", startOffset=" + startOffset +
                    ", endPlatform='" + endPlatform + '\'' +
                    ", endOffset=" + endOffset +
                    '}';
        }
    }

    /**
     * 根据给定的页码和各平台数据总数，计算第pageNumber页（每页pageSize条）在平台数据中的起止位置
     *
     * @param pageNumber      页码（从1开始）
     * @param pageSize        每页记录数（本例中为50）
     * @param platformCounts  各平台数据总数，要求按查询顺序存储（这里用 LinkedHashMap 保证顺序为 A, B, C, D）
     * @return 如果页码超出总数据数，则返回 null，否则返回 QueryRange 对象
     */
    public static QueryRange getQueryRange(int pageNumber, int pageSize, LinkedHashMap<String, Integer> platformCounts) {
        // 计算所有平台的总记录数
        int total = 0;
        for (Integer count : platformCounts.values()) {
            total += count;
        }
        
        // 计算全局起始和结束下标（下标从0开始）
        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = startIndex + pageSize - 1;
        
        // 如果起始下标超过总记录数，则说明该页没有数据
        if (startIndex >= total) {
            return null;
        }
        
        // 如果结束下标超过总数，则调整为最后一条记录的下标
        if (endIndex >= total) {
            endIndex = total - 1;
        }
        
        QueryRange qr = new QueryRange();
        
        // 计算开始平台及在该平台中的偏移
        int cumCount = 0; // 累计记录数
        for (Map.Entry<String, Integer> entry : platformCounts.entrySet()) {
            String platform = entry.getKey();
            int count = entry.getValue();
            if (startIndex < cumCount + count) {
                qr.startPlatform = platform;
                qr.startOffset = startIndex - cumCount;
                break;
            }
            cumCount += count;
        }
        
        // 计算结束平台及在该平台中的偏移
        cumCount = 0;
        for (Map.Entry<String, Integer> entry : platformCounts.entrySet()) {
            String platform = entry.getKey();
            int count = entry.getValue();
            if (endIndex < cumCount + count) {
                qr.endPlatform = platform;
                qr.endOffset = endIndex - cumCount;
                break;
            }
            cumCount += count;
        }
        
        return qr;
    }

    public static void main(String[] args) {
        // 模拟各平台的记录总数，注意 LinkedHashMap 保证插入顺序
        LinkedHashMap<String, Integer> platformCounts = new LinkedHashMap<>();
        platformCounts.put("A", 20);  // 平台 A 有 20 条记录
        platformCounts.put("B", 40);  // 平台 B 有 40 条记录
        platformCounts.put("C", 60);  // 平台 C 有 60 条记录
        platformCounts.put("D", 30);  // 平台 D 有 30 条记录
        // 总记录数 = 20 + 40 + 60 + 30 = 150

        int pageSize = 130;
        
        // 测试：查询第1页、第2页、第3页和第4页（第4页应超过数据范围）
        for (int page = 1; page <= 4; page++) {
            QueryRange range = getQueryRange(page, pageSize, platformCounts);
            if (range == null) {
                System.out.println("第 " + page + " 页无数据！");
            } else {
                System.out.println("第 " + page + " 页查询范围：" + range);
            }
        }
    }
}
