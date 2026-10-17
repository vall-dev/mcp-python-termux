import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.File;

public class IconConvert {
    public static void main(String[] args) throws Exception {
        File in = new File(args[0]);
        File out = new File(args[1]);
        int size = Integer.parseInt(args[2]);
        BufferedImage src = ImageIO.read(in);
        if (src == null) { System.err.println("read failed"); System.exit(1); }
        int w = src.getWidth(), h = src.getHeight();
        int s = Math.min(w, h);
        int x = (w - s)/2, y = (h - s)/2;
        BufferedImage crop = src.getSubimage(x, y, s, s);
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(crop, 0, 0, size, size, null);
        g.dispose();
        ImageIO.write(scaled, "png", out);
        System.out.println("wrote " + out + " " + size + "x" + size + " from " + w + "x" + h);
    }
}
