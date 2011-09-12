import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

public class Test
{
	public static void main(final String[] args)
	{
		AtlasManager manager = new AtlasManager();

		Random random = new Random();

		for (int sizeBase = 10; sizeBase <= 300; sizeBase += sizeBase)
		{
			for (int i = 0; i < 100; ++i)
			{
				int width = sizeBase + random.nextInt(sizeBase);
				int height = sizeBase + random.nextInt(sizeBase);
				ImageReference image = new ImageReference(width, height);

				manager.enqueueImage(image);
			}
		}

		manager.packAll();

		int index = 0;
		for (Atlas atlas : manager.getAtlases())
		{
			System.out.println("------------------------");
			System.out.println("Atlas: " + atlas.width + "x" + atlas.height);

			BufferedImage atlasImage = new BufferedImage(atlas.width, atlas.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = atlasImage.createGraphics();

			ArrayList<ImageReference> images = atlas.images;

			g.setColor(Color.GRAY);
			g.fillRect(0, 0, atlasImage.getWidth(), atlasImage.getHeight());

			g.setColor(Color.WHITE);
			for (ImageReference img : images)
			{
				if (img.flipped)
					g.drawRect(img.x, img.y, img.height - 1, img.width - 1);
				else g.drawRect(img.x, img.y, img.width - 1, img.height - 1);
				System.out.println(String.format("%d %d %d %d", img.x, img.y, img.width, img.height));
			}

			g.dispose();

			try
			{
				ImageIO.write(atlasImage, "png", new File("d:/dev/temp/images/image_" + index + ".png"));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			index++;
		}

	}
}
