package com.github.readutf.hermes;

public class TestMatcher {

    public static void main(String[] args) {
        System.out.println(isMatch("test", "t*st"));
    }

    public static boolean isMatch(String s, String p) {
        if (s == null || p == null) {
            return false;
        }
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[2][n + 1];
        char[] ss = s.toCharArray();
        char[] pp = p.toCharArray();
        int curr = 0;
        int prev = 1;

        // dp[0][j] = false; dp[i][0] = false;

        for (int i = 0; i <= m; i++) {
            prev = curr;
            curr = 1 - prev;
            for (int j = 0; j <= n; j++) {
                if (i == 0 && j == 0) {
                    dp[curr][j] = true;
                    continue;
                }
                if (j == 0) { // When p is empty but s is not empty, should not match
                    dp[curr][j] = false;
                    continue;
                }
                dp[curr][j] = false;
                if (pp[j - 1] != '*') {
                    if (i >= 1 && (ss[i - 1] == pp[j - 1] || pp[j - 1] == '?')) {
                        dp[curr][j] = dp[prev][j - 1];
                    }
                } else {
                    dp[curr][j] |= dp[curr][j - 1];// '*' -> empty
                    if (i >= 1) { // '*' matches one or more of any character
                        dp[curr][j] |= dp[prev][j];
                    }
                }
            }
        }
        return dp[curr][n];
    }

}
