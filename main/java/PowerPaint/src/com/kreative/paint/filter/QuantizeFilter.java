package com.kreative.paint.filter;

import java.awt.*;


import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import com.kreative.paint.form.Form;

import com.kreative.paint.form.IntegerOption;
import com.kreative.paint.form.PreviewGenerator;
import com.kreative.paint.util.ImageUtils;

public class QuantizeFilter extends AbstractFilter {
	public boolean usesOptionForm() {
		return true;
	}

	public Form getOptionForm(final Image src) {
		Form f = new Form();
		f.add(new PreviewGenerator() {
			public String getName() { return null; }
			public void generatePreview(Graphics2D g, Rectangle r) {
				Shape clip = g.getClip();
				BufferedImage i = (BufferedImage)filter(ImageUtils.toBufferedImage(src,200,200));
				g.setClip(r);
				g.drawImage(i, null, r.x + (r.width-i.getWidth())/2, r.y + (r.height-i.getHeight())/2);
				g.setClip(clip);
			}
		});
		f.add(new IntegerOption() {
			public String getName() {return FilterUtilities.messages.getString("quantize.Quantize");}
			public int getMaximum() {return Integer.MAX_VALUE;}
			public int getMinimum() {return 1;}
			public int getStep() {return 1;}
			public int getValue() {return quantize;}
			public void setValue(int v) {quantize = v;}
			public boolean useSlider() {return false;}
		});
		return f;
	}

//	public Image filter(Image src, int quantize) {
//		return filter(src, quantize);
//	}

	private int quantize; // Quantization factor

//	public QuantizeFilter(int quantize) {
//		this.quantize = quantize;
//	}

	public final Image filter(Image img) {
		BufferedImage nim = ImageUtils.toBufferedImage(img, true);
		int w = nim.getWidth();
		int h = nim.getHeight();

		// Get the pixel data
		byte[] pixels = ((DataBufferByte) nim.getRaster().getDataBuffer()).getData();
		boolean hasAlphaChannel = nim.getAlphaRaster() != null;

		int pixelLength = hasAlphaChannel ? 4 : 3;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int pos = (y * w + x) * pixelLength;

				int r = pixels[pos] & 0xFF;
				int g = pixels[pos + 1] & 0xFF;
				int b = pixels[pos + 2] & 0xFF;

				int avg = (r + g + b) / 3;
				int quantized = (avg / quantize) * quantize;
				int error = avg - quantized;

				int newVal = error * 16;

				pixels[pos] = (byte) newVal;
				pixels[pos + 1] = (byte) newVal;
				pixels[pos + 2] = (byte) newVal;
			}
		}

		return nim;
	}
}
