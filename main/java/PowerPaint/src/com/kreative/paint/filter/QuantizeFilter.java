package com.kreative.paint.filter;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.kreative.paint.form.Form;
import com.kreative.paint.form.IntegerOption;
import com.kreative.paint.form.PreviewGenerator;
import com.kreative.paint.util.ImageUtils;

public class QuantizeFilter extends AbstractFilter {

	public QuantizeFilter(int quantizeCount) {
		this.quantizeCount = quantizeCount;
	}
	private int quantizeCount = 1; // Quantization factor

//	public final boolean usesOptionForm() {
//		return true;
//	}

//	public Form getOptionForm(final Image src) {
//		Form f = new Form();
//		/*f.add(new PreviewGenerator() {
//			public String getName() { return null; }
//			public void generatePreview(Graphics2D g, Rectangle r) {
//				Shape clip = g.getClip();
//				BufferedImage i = (BufferedImage)filter(ImageUtils.toBufferedImage(src,200,200));
//				g.setClip(r);
//				g.drawImage(i, null, r.x + (r.width-i.getWidth())/2, r.y + (r.height-i.getHeight())/2);
//				g.setClip(clip);
//			}
//		});*/
//		f.add(new IntegerOption() {
//			public String getName() { return FilterUtilities.messages.getString("quantize.Quantize"); }
//			public int getMaximum() { return Integer.MAX_VALUE; }
//			public int getMinimum() { return 0; }
//			public int getStep() { return 1; }
//			public int getValue() { return quantizeCount; }
//			public void setValue(int v) { quantizeCount = v; if (ui != null) ui.update(); }
//			public boolean useSlider() { return false; }
//		});
//		return f;
//	}

	public Image filter(Image src) {
		BufferedImage srcBuffer = ImageUtils.toBufferedImage(src, true);
		AnimatedQuantizeFilter filter = new AnimatedQuantizeFilter(srcBuffer);
        return filter.applyAnimatedQuantizeFilter(5000);
	}


	public Image computeFilter(Image src) {
		BufferedImage bi = ImageUtils.toBufferedImage(src, true);
		int w = bi.getWidth();
		int h = bi.getHeight();
		int[] pixels = new int[w * h];
		bi.getRGB(0, 0, w, h, pixels, 0, w);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int pos = y * w + x;

				int pixel = pixels[pos];
				int a = (pixel >> 24) & 0xFF;
				int r = (pixel >> 16) & 0xFF;
				int g = (pixel >> 8) & 0xFF;
				int b = pixel & 0xFF;

				float q = (float) quantizeCount / 3;
				// Quantize each color channel individually
				r = (int) ((r / q) * q);
				g = (int) ((g / q) * q);
				b = (int) ((b / q) * q);

				// Combine the quantized channels back into a single pixel
				int newPixel = (a << 24) | (r << 16) | (g << 8) | b;
				pixels[pos] = newPixel;
			}
		}

		bi.setRGB(0, 0, w, h, pixels, 0, w);
		return bi;
	}

}


class AnimatedQuantizeFilter {
	private AtomicReference<Double> t = new AtomicReference<>(0.0);
	private BufferedImage currentImage;
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private JFrame frame;
	private JLabel label;

	public AnimatedQuantizeFilter(BufferedImage src) {
		this.currentImage = src;
		setupDisplay();
	}

	private void setupDisplay() {
		frame = new JFrame("Animated Quantize Filter");
		label = new JLabel(new ImageIcon(currentImage));
		frame.add(label);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	private void startAnimation(int duration) {
		scheduler.scheduleAtFixedRate(() -> {
			double newT = t.updateAndGet(v -> v + 0.005);
			int q = (int) (Math.sin(newT) * 150);
			updateImage(q);
		}, 0, 200, TimeUnit.MILLISECONDS);

		scheduler.schedule(() -> {
			scheduler.shutdown();
			takeSnapshot();
		}, duration, TimeUnit.MILLISECONDS);
	}

	private void updateImage(int q) {
		QuantizeFilter filter = new QuantizeFilter(q);
		Image filteredImage = filter.computeFilter(currentImage);
		label.setIcon(new ImageIcon(filteredImage));
		frame.repaint();
	}

	public BufferedImage takeSnapshot() {
		frame.dispose();
		return currentImage;
	}

	public BufferedImage applyAnimatedQuantizeFilter(int duration) {
		startAnimation(duration);
		try {
			scheduler.awaitTermination(duration + 1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return takeSnapshot();
	}
}
