package io.lazydog;

import java.util.*;

public class QueryRangeDemo {

    // 假设各平台 API 的默认分页大小为 50
    private static final int PLATFORM_PAGE_SIZE = 50;

    /**
     * 用于描述单个平台一次查询任务的参数：
     * - pageNum：平台查询接口中的页码（从1开始）
     * - pageSize：本次查询的记录数
     * - startOffset：本次查询结果中，需要舍弃前面的记录数（例如当本次任务只取该页中的部分记录时）
     */
    public static class QueryTask {
        public int pageNum;
        public int pageSize;
        public int startOffset; // 查询结果中，真正开始需要的数据在该页内的偏移

        public QueryTask(int pageNum, int pageSize, int startOffset) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.startOffset = startOffset;
        }

        @Override
        public String toString() {
            return "{pageNum=" + pageNum + ", pageSize=" + pageSize + ", startOffset=" + startOffset + "}";
        }
    }

    /**
     * QueryRange 用于描述全局聚合查询时的范围信息，包括：
     * - startPlatform/startOffset：全局查询起始位置落在该平台及该平台内的偏移
     * - endPlatform/endOffset：全局查询结束位置落在该平台及该平台内的偏移
     * - platforms：本次全局查询涉及的所有平台列表（按全局顺序排列）
     * - platformTasks：对每个涉及平台，计算出需要查询的分页任务列表
     */
    public static class QueryRange {
        public String startPlatform;
        public int startOffset;
        public String endPlatform;
        public int endOffset;
        public List<String> platforms = new ArrayList<>();
        public Map<String, List<QueryTask>> platformTasks = new LinkedHashMap<>();

        @Override
        public String toString() {
            return "QueryRange{" +
                    "startPlatform='" + startPlatform + '\'' +
                    ", startOffset=" + startOffset +
                    ", endPlatform='" + endPlatform + '\'' +
                    ", endOffset=" + endOffset +
                    ", platforms=" + platforms +
                    ", platformTasks=" + platformTasks +
                    '}';
        }
    }

    /**
     * 根据全局页码（从1开始）、每次聚合查询的记录数 pageSize 以及各平台数据总数，
     * 计算全局查询的起始和结束下标，进而确定：
     * 1. 全局查询的起止平台及在该平台中的偏移；
     * 2. 涉及哪些平台；
     * 3. 针对每个平台，基于平台默认分页大小 PLATFORM_PAGE_SIZE，
     * 计算出需要查询的分页任务列表（QueryTask）。
     *
     * @param pageNumber     全局页码（从1开始）
     * @param pageSize       全局聚合查询的记录数（例如50）
     * @param platformCounts 各平台数据总数，要求按照查询顺序排列（例如用 LinkedHashMap 保持顺序）
     * @return 若页码超出总记录范围则返回 null，否则返回 QueryRange 对象
     */
    public static QueryRange getQueryRange(int pageNumber, int pageSize, LinkedHashMap<String, Integer> platformCounts) {
        // 计算所有平台总记录数
        int total = 0;
        for (Integer count : platformCounts.values()) {
            total += count;
        }
        // 计算全局起始和结束下标（全局下标从0开始）
        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = startIndex + pageSize - 1;
        if (startIndex >= total) {
            return null;
        }
        if (endIndex >= total) {
            endIndex = total - 1;
        }

        QueryRange qr = new QueryRange();

        // 先遍历一次，确定全局 startPlatform/startOffset 和 endPlatform/endOffset
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : platformCounts.entrySet()) {
            String platform = entry.getKey();
            int count = entry.getValue();
            int platformStart = cumulative;
            int platformEnd = cumulative + count - 1;

            // 如果全局 startIndex落在该平台
            if (startIndex >= platformStart && startIndex <= platformEnd && qr.startPlatform == null) {
                qr.startPlatform = platform;
                qr.startOffset = startIndex - platformStart;
            }
            // 如果全局 endIndex落在该平台
            if (endIndex >= platformStart && endIndex <= platformEnd) {
                qr.endPlatform = platform;
                qr.endOffset = endIndex - platformStart;
            }
            cumulative += count;
        }

        // 再次遍历，针对每个平台计算是否与全局查询区间有交集，并生成对应的查询任务列表
        cumulative = 0;
        for (Map.Entry<String, Integer> entry : platformCounts.entrySet()) {
            String platform = entry.getKey();
            int count = entry.getValue();
            int platformGlobalStart = cumulative;
            int platformGlobalEnd = cumulative + count - 1;

            // 判断该平台是否与全局查询区间 [startIndex, endIndex] 有交集
            if (platformGlobalEnd >= startIndex && platformGlobalStart <= endIndex) {
                // 加入涉及平台列表
                qr.platforms.add(platform);
                // 计算该平台内所需的局部查询范围
                int localStart = (startIndex > platformGlobalStart) ? startIndex - platformGlobalStart : 0;
                int localEnd = (endIndex < platformGlobalEnd) ? endIndex - platformGlobalStart : count - 1;
                // 生成查询任务列表（可能跨越多个平台内部分页）
                List<QueryTask> tasks = generatePlatformQueryTasks(localStart, localEnd, PLATFORM_PAGE_SIZE);
                qr.platformTasks.put(platform, tasks);
            }
            cumulative += count;
        }
        return qr;
    }

    /**
     * 根据平台内的局部起始和结束下标 [localStart, localEnd]（均从0开始），
     * 以及平台接口的分页大小 platformPageSize，计算出需要查询的分页任务列表。
     * <p>
     * 例如：若平台内 pageSize = 50，
     * - localStart = 30, localEnd = 70，
     * 则需要查询两次：
     * 第1次：pageNum = 1（对应记录 0~49），startOffset = 30, pageSize = 20（获取 30~49）；
     * 第2次：pageNum = 2（对应记录 50~99），startOffset = 0, pageSize = 21（获取 50~70）。
     *
     * @param localStart       平台内局部起始下标（从0开始）
     * @param localEnd         平台内局部结束下标（从0开始）
     * @param platformPageSize 平台接口默认分页大小
     * @return 查询任务列表，每个任务描述一次平台查询的参数
     */
    public static List<QueryTask> generatePlatformQueryTasks(int localStart, int localEnd, int platformPageSize) {
        List<QueryTask> tasks = new ArrayList<>();
        // 计算起始和结束的本地页号（页号从1开始）
        int startPage = localStart / platformPageSize + 1;
        int endPage = localEnd / platformPageSize + 1;
        for (int page = startPage; page <= endPage; page++) {
            int pageStartGlobal = (page - 1) * platformPageSize; // 该页在平台内的全局起始下标
            int queryStart; // 本次查询在该页中的起始偏移
            int queryCount; // 本次查询需要的记录数
            if (page == startPage) {
                queryStart = localStart % platformPageSize;
                if (startPage == endPage) {
                    // 同一页覆盖整个查询区间
                    queryCount = localEnd - localStart + 1;
                } else {
                    // 第一页，从 queryStart 到该页末尾
                    queryCount = platformPageSize - queryStart;
                }
            } else if (page == endPage) {
                queryStart = 0;
                queryCount = (localEnd % platformPageSize) + 1;
            } else {
                // 中间页，全部获取该页
                queryStart = 0;
                queryCount = platformPageSize;
            }
            tasks.add(new QueryTask(page, queryCount, queryStart));
        }
        return tasks;
    }

    public static void main(String[] args) {
        // 模拟各平台数据总数，使用 LinkedHashMap 保持顺序
        // 其中平台 "C" 拥有 350 条记录，模拟数据较多的情况
        LinkedHashMap<String, Integer> platformCounts = new LinkedHashMap<>();
        platformCounts.put("A", 20);
        platformCounts.put("B", 40);
        platformCounts.put("C", 350);
        platformCounts.put("D", 30);
        platformCounts.put("E", 25);
        // 总记录数 = 20 + 40 + 350 + 30 + 25 = 465

        int globalPageSize = 130;  // 全局聚合查询每页50条记录

        // 测试几个全局页码
        for (int page = 1; page <= 10; page++) {
            QueryRange range = getQueryRange(page, globalPageSize, platformCounts);
            if (range == null) {
                System.out.println("全局第 " + page + " 页无数据！");
            } else {
                System.out.println("全局第 " + page + " 页查询范围：");
                System.out.println(range);
                System.out.println("----------------------------------------");
            }
        }
    }
}
