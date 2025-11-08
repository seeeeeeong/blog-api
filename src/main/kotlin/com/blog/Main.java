import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st = new StringTokenizer(br.readLine());

        int N = Integer.parseInt(st.nextToken());
        int M = Integer.parseInt(st.nextToken());

        int[] arr = new int[N];
        st = new StringTokenizer(br.readLine());
        for (int i = 0; i < N; i++) {
            arr[i] = Integer.parseInt(st.nextToken());
        }

        Arrays.sort(arr);

        int left = 0;
        int right = arr[N - 1];
        int maxHeight = 0;

        while (left <= right) {
            int mid = (left + right) / 2;
            long tempHeight = 0;

            for (int i = 0; i < N; i++) {
                tempHeight += Math.max(0, arr[i] - mid);
            }

            if (tempHeight >= M) {
                maxHeight = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        System.out.println(maxHeight);
    }
}
