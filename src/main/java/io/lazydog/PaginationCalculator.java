package io.lazydog;

import java.util.ArrayList;
import java.util.List;

class PlatformQuery {
    private String platform;
    private int pageNum;
    private int pageSize;

    public PlatformQuery(String platform, int pageNum, int pageSize) {
        this.platform = platform;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "Platform: " + platform + ", PageNum: " + pageNum + ", PageSize: " + pageSize;
    }

    // Getters
    public String getPlatform() { return platform; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
}

public class PaginationCalculator {

    public static List<PlatformQuery> calculateQueries(int countA, int countB, int countC, int countD, int N) {
        List<PlatformQuery> queries = new ArrayList<>();
        int start = (N - 1) * 50 + 1;
        int end = N * 50;

        int[] counts = {countA, countB, countC, countD};
        String[] platformNames = {"A", "B", "C", "D"};
        List<Integer> prefixSums = new ArrayList<>();
        int currentSum = 0;
        for (int count : counts) {
            currentSum += count;
            prefixSums.add(currentSum);
        }

        int totalData = prefixSums.get(prefixSums.size() - 1);
        if (start > totalData) {
            return queries; // 超出数据范围，返回空列表
        }

        // 确定起始和结束平台
        int startPlatform = findPlatform(start, prefixSums);
        int endPlatform = findPlatform(end, prefixSums);

        if (startPlatform == -1 || endPlatform == -1) {
            return queries;
        }

        int currentPosition = start;
        for (int platformIdx = startPlatform; platformIdx <= endPlatform; platformIdx++) {
            int platformStart = (platformIdx == 0) ? 1 : (prefixSums.get(platformIdx - 1) + 1);
            int platformEnd = prefixSums.get(platformIdx);
            if (currentPosition > platformEnd) {
                continue;
            }

            int overlapStart = Math.max(currentPosition, platformStart);
            int overlapEnd = Math.min(end, platformEnd);
            int localStart = overlapStart - platformStart + 1;
            int localLength = overlapEnd - overlapStart + 1;

            // 计算分页参数
            int remaining = localLength;
            int currentLocal = localStart;
            while (remaining > 0) {
                int page = (currentLocal - 1) / 50;
                int pageStart = page * 50 + 1;
                int availableInPage = 50 - (currentLocal - pageStart);
                int chunk = Math.min(availableInPage, remaining);

                queries.add(new PlatformQuery(
                    platformNames[platformIdx],
                    page + 1,
                    chunk
                ));

                currentLocal += chunk;
                remaining -= chunk;
            }

            currentPosition = overlapEnd + 1;
        }

        return queries;
    }

    private static int findPlatform(int position, List<Integer> prefixSums) {
        for (int i = 0; i < prefixSums.size(); i++) {
            if (position <= prefixSums.get(i)) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        // 示例：平台A有100条，B有80条，C有120条，D有60条，查询第5页
        List<PlatformQuery> queries = calculateQueries(100, 80, 120, 60, 5);
        for (PlatformQuery query : queries) {
            System.out.println(query);
        }
    }
}