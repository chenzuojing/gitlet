import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexPuzzles {
    public static List<String> urlRegex(String[] urls) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\(\\w*https?://(\\w+\\.)+[a-z]{2,3}/\\w+\\" +
                ".html\\w*\\)");

        for (int i = 0; i < urls.length; i++) {
            Matcher match = pattern.matcher(urls[i]);
            if (match.matches()) {
                result.add(urls[i]);
            }
        }
        return result;
    }

    public static List<String> findStartupName(String[] names) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(Data|App|my|on|un)[^i&&\\w]+(ly|sy|ify|\\.io|\\.fm|\\" +
                ".tv)");

        for (int i = 0; i < names.length; i++) {
            Matcher match = pattern.matcher(names[i]);
            if (match.matches()) {
                result.add(names[i]);
            }
        }
        return result;
    }

    public static BufferedImage imageRegex(String filename, int width, int height) {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("No such file found: " + filename);
        }

        // Initialize both Patterns and 3-d array
        Pattern pattern = Pattern.compile("");
        int[][][] arr = new int[1][1][1];

        try {
            String line;
            while ((line = br.readLine()) != null) {
                // Initialize both Matchers and find() for each
                Matcher match = pattern.matcher(line);
                // Parse each group as an Integer

                // Store in array
            }
        } catch (IOException e) {
            System.err.printf("Input error: %s%n", e.getMessage());
            System.exit(1);
        }
        // Return the BufferedImage of the array
        return null;
    }

    public static BufferedImage arrayToBufferedImage(int[][][] arr) {
        BufferedImage img = new BufferedImage(arr.length,
        	arr[0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr.length; j++) {
                int pixel = 0;
                for (int k = 0; k < 3; k++) {
                    pixel += arr[i][j][k] << (16 - 8*k);
                }
                img.setRGB(i, j, pixel);
            }
        }

        return img;
    }

    public static void main(String[] args) {
        String[] urls = {
                "(randomstuff1234https://www.eecs.berkeley.edu/blah.htmlyoullneverfindyourextracredit)",
                "(http://www.cs61bl.github.io/yolo.html)",
                "(https://en.wikipedia.org/greed.htmltry23andfindmenow)"};
        List<String> validUrls = urlRegex(urls);

        for (int i = 0; i < validUrls.size(); i++) {
            System.out.println(validUrls.get(i));
        }

        String[] startupNames = {
                "Data", "DataDog.io", "Apply", "MyAppify", "myApp.fm", "onTop.tv"};
        List<String> validNames = findStartupName(startupNames);

        for (int i = 0; i < validNames.size(); i++) {
            System.out.println(validNames.get(i));
        }

        /* For testing image regex */
        BufferedImage img = imageRegex("mystery.txt", 400, 400);

        File outputfile = new File("output_img.jpg");
        try {
            ImageIO.write(img, "jpg", outputfile);
        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }
}
