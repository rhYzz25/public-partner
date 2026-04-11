package com.xz.match.job;

/**
 * 编辑距离工具类，计算两个字符串的相似度
 * 结果越小，字符串越相似
 */
public class EditDistanceUtils {

	/**
	 * 计算两个字符串的 Levenshtein 编辑距离
	 */
	public static int calculate(String s1, String s2) {
		int m = s1.length();
		int n = s2.length();

		// 创建dp数组
		int[][] dp = new int[m + 1][n + 1];

		// 初始化：空串到长度i的编辑距离就是i（删除i次）
		for (int i = 0; i <= m; i++) {
			dp[i][0] = i;
		}
		for (int j = 0; j <= n; j++) {
			dp[0][j] = j;
		}

		// 动态规划填表
		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					// 字符相同，不需要编辑，距离和左上角一样
					dp[i][j] = dp[i - 1][j - 1];
				} else {
					// 字符不同，从 插入/删除/替换 选最小操作 + 1
					dp[i][j] = Math.min(
							Math.min(dp[i - 1][j], dp[i][j - 1]),
							dp[i - 1][j - 1]
					) + 1;
				}
			}
		}

		return dp[m][n];
	}

	/**
	 * 计算两个标签列表的总相似度（最小距离累加）
	 * 返回值越小，越相似
	 */
	public static int calculateTotalDistance(java.util.List<String> tags1, java.util.List<String> tags2) {
		int totalDistance = 0;
		for (String tag1 : tags1) {
			int minDist = Integer.MAX_VALUE;
			// 对我的每个标签，找你标签中最相似的那个
			for (String tag2 : tags2) {
				int dist = calculate(tag1, tag2);
				minDist = Math.min(minDist, dist);
			}
			totalDistance += minDist;
		}
		return totalDistance;
	}
}