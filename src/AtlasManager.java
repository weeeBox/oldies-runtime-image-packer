import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import javax.imageio.ImageIO;

public class AtlasManager
{
	static class Image implements Comparable<Image>
	{		
		int width;
		int height;
		
		boolean flipped;
		
		Atlas atlas;
		int x;
		int y;
		
		public Image(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		@Override
		public int compareTo(Image other)
		{
			return -(width * height - other.width * other.height);
		}		
		
	}
	
	static class Atlas
	{
		Atlas(int width, int height)
		{
			this.width = width;
			this.height = height;
			
			images = new ArrayList<Image>();
		}
		
		ArrayList<Image> images;
		
		int width;
		int height;
		
		boolean tight;		
	}
	
	class SkylineRect
	{
		SkylineRect(int left, int width, int height)
		{
			this.left = left;
			this.width = width;
			this.height = height;
		}
		
		int left;
		int width;
		int height;		
	}
	
	LinkedList<Image> unprocessedImages;
	ArrayList<Atlas> atlases;
	
	AtlasManager()
	{
		unprocessedImages = new LinkedList<Image>();
		atlases = new ArrayList<Atlas>();
	}
	
	void enqueueImage(Image image)
	{
		unprocessedImages.add(image);
		image.atlas = null;
	}
	
	void dequeueImage(Image image)
	{
		if (image.atlas == null)
		{
			unprocessedImages.remove(image);
			
		} else
		{
			Atlas atlas = image.atlas;			
			atlas.images.remove(image);
			
			if (atlas.images.isEmpty())
			{
				atlases.remove(atlas);
				
			} else
			{
				atlas.tight = false; 
				
			}
			
		}
		
	}
	
	void packAll()
	{
		Collections.sort(unprocessedImages);
		
		while (!unprocessedImages.isEmpty())
		{
			Image image = unprocessedImages.getFirst();
			prepareImage(image);
		}			
	}
	
	void prepareImage(Image image)
	{
		if (image.atlas != null)
			return;
		
		Atlas atlas = createNewAtlasFor(image);
		
		destroyNontightAtlases();
			
		unprocessedImages.remove(image);
		Collections.sort(unprocessedImages);
		unprocessedImages.addFirst(image);
			
		fillAtlas(atlas, unprocessedImages);
			
		atlases.add(atlas);
	}
	
	private void fillAtlas(Atlas atlas, LinkedList<Image> processingQueue)
	{
		ArrayList<SkylineRect> skyline = new ArrayList<SkylineRect>();		
		skyline.add(new SkylineRect(0, atlas.width, 0));
				
		while (!processingQueue.isEmpty() && !degenerateSkyline(skyline, atlas))
		{		
			SkylineRect minimumRect = null;
			
			int minimumIndex = -1;
			int currentIndex = 0;
			
			for (SkylineRect currentRect : skyline)
			{
				if (minimumIndex == -1 || currentRect.height < minimumRect.height)
				{
					minimumRect = currentRect;
					minimumIndex = currentIndex;
				}
				 
				currentIndex++;
			}

			boolean anyFit = false;
			
			for (int imageIndex = 0; imageIndex < processingQueue.size(); ++imageIndex)
			{
				Image currentImage = processingQueue.get(imageIndex);
				
				int minimumHeight = -1;
				int minimumWidth = 0;
				boolean minimumFlipped = false;			
			
				if (currentImage.width <= minimumRect.width && currentImage.height <= atlas.height - minimumRect.height)
					if (minimumHeight == -1 || minimumHeight > currentImage.height)
					{
						minimumHeight = currentImage.height;
						minimumWidth = currentImage.width;
						minimumFlipped = false;
					}
				
				if (currentImage.height <= minimumRect.width && currentImage.width <= atlas.height - minimumRect.height)
					if (minimumHeight == -1 || minimumHeight > currentImage.width)
					{
						minimumHeight = currentImage.width;
						minimumWidth = currentImage.height;
						minimumFlipped = true;
					}
					
				if (minimumHeight != -1)
				{
					currentImage.flipped = minimumFlipped;
					
					skyline.remove(minimumIndex);
					skyline.add(minimumIndex, new SkylineRect(minimumRect.left, minimumWidth, minimumRect.height + minimumHeight));
					if (minimumWidth < minimumRect.width)
						skyline.add(minimumIndex+1, new SkylineRect(minimumRect.left + minimumWidth, minimumRect.width - minimumWidth, minimumRect.height));
					
					skyline = flattenSkyline(skyline);
					
					currentImage.atlas = atlas;
					currentImage.x = minimumRect.left;
					currentImage.y = atlas.height - (minimumRect.height + minimumHeight);
					
					atlas.images.add(currentImage);
					processingQueue.remove(imageIndex);
					
					anyFit = true;
					break;
				}	
			}
			
			if (!anyFit)
			{
				skyline.get(minimumIndex).height = Math.min(minimumIndex > 0 ? skyline.get(minimumIndex-1).height : atlas.height,
						minimumIndex < skyline.size()-1 ? skyline.get(minimumIndex+1).height : atlas.height);

				skyline = flattenSkyline(skyline);				
			}
			
		}
		
		atlas.tight = !processingQueue.isEmpty();
	}
	
	private boolean degenerateSkyline(ArrayList<SkylineRect> skyline, Atlas atlas)
	{
		return skyline.size() == 1 && skyline.get(0).left == 0 && skyline.get(0).height == atlas.height && skyline.get(0).width == atlas.width;
	}
	
	private void destroyNontightAtlases()
	{
		ArrayList<Atlas> removed = new ArrayList<Atlas>();
		
		for (Atlas atlas : atlases)
			if (!atlas.tight)
			{
				removed.add(atlas);
				
				for (Image image : atlas.images)
				{
					image.atlas = null;
					unprocessedImages.add(image);
				}
			}
		
		for (Atlas atlas : removed)
			atlases.remove(atlas);
	}
	
	private Atlas createNewAtlasFor(Image image)
	{
		int width = 512;
		for (; width < image.width; width += width) ;
		
		int height = 512;
		for (; height < image.height; height += height) ;
		
		return new Atlas(width, height);
	}
	
	private ArrayList<SkylineRect> flattenSkyline(ArrayList<SkylineRect> skyline)
	{
		ArrayList<SkylineRect> flattened = new ArrayList<SkylineRect>();
		
		for (int i = 1; i <= skyline.size(); ++i)
			if (i == skyline.size() || skyline.get(i-1).height != skyline.get(i).height)
			{
				int j = i-1;
				while (j > 0 && skyline.get(j).height == skyline.get(j-1).height)
					--j;
				
				int totalWidth = 0;
				for (int k = j; k < i; ++k)
					totalWidth += skyline.get(k).width;
				
				flattened.add(new SkylineRect(skyline.get(j).left, totalWidth, skyline.get(j).height));
			}
		
		return flattened;
	}
	
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
				Image image = new AtlasManager.Image(width, height);
				
				manager.enqueueImage(image);
			}
		}
		
		manager.packAll();
		
		int index = 0;
		for (Atlas atlas : manager.atlases)
		{
			System.out.println("------------------------");
			System.out.println("Atlas: " + atlas.width + "x" + atlas.height);
			
			BufferedImage atlasImage = new BufferedImage(atlas.width, atlas.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = atlasImage.createGraphics();
			
			ArrayList<Image> images = atlas.images;
			
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, atlasImage.getWidth(), atlasImage.getHeight());
			
			g.setColor(Color.WHITE);
			for (Image img : images)
			{
				if (img.flipped)
					g.drawRect(img.x, img.y, img.height - 1, img.width - 1);
				else
					g.drawRect(img.x, img.y, img.width - 1, img.height - 1);
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
