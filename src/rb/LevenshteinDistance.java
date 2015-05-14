package rb;

/**
 * Created by IntelliJ IDEA.
 * User: gongze
 * Date: 5/14/2015
 * Time: 1:26 PM
 */
public class LevenshteinDistance {
    private static int LevenshteinDistance(String s, String t) {
        if (s.equals(t)) {
            return 0;
        }
        if (s.length() == 0) {
            return t.length();
        }
        if (t.length() == 0) {
            return s.length();
        }

        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];
        for (int i = 0; i < v0.length; i++)
            v0[i] = i;
        for (int i = 0; i < s.length(); i++) {
            v1[0] = i + 1;
            for (int j = 0; j < t.length(); j++) {
                int cost = s.charAt(i) == t.charAt(j) ? 0 : 1;
                int middle = Math.min(v1[j] + 1, v0[j + 1] + 1);
                v1[j + 1] = Math.min(middle, v0[j] + cost);
            }
            for (int j = 0; j < v0.length; j++) {
                v0[j] = v1[j];
            }
        }


        return v1[t.length()];
    }

    public static int getClosest(String target, String[] candidates) {
        int minimal = Integer.MAX_VALUE;
        int minimalIndex = -1;
        for (int i = 0; i < candidates.length; i++) {
            int distance = LevenshteinDistance(target, candidates[i]);
            if (distance < minimal) {
                minimal = distance;
                minimalIndex = i;
            }

        }
        return minimalIndex;
    }
}
