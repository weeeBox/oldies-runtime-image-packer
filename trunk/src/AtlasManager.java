import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class AtlasManager
{
	private LinkedList<ImageReference> unprocessedImages;
	private ArrayList<Atlas> atlases;

	public AtlasManager()
	{
		unprocessedImages = new LinkedList<ImageReference>();
		atlases = new ArrayList<Atlas>();
	}

	public void enqueueImage(ImageReference image)
	{
		unprocessedImages.add(image);
		image.atlas = null;
	}

	public void dequeueImage(ImageReference image)
	{
		if (image.atlas == null)
		{
			unprocessedImages.remove(image);
		}
		else
		{
			Atlas atlas = image.atlas;
			atlas.images.remove(image);

			if (atlas.images.isEmpty())
			{
				atlases.remove(atlas);

			}
			else
			{
				atlas.tight = false;
			}
		}
	}

	public void packAll()
	{
		Collections.sort(unprocessedImages);

		while (!unprocessedImages.isEmpty())
		{
			ImageReference image = unprocessedImages.getFirst();
			prepareImage(image);
		}
	}

	public void prepareImage(ImageReference image)
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

	private void fillAtlas(Atlas atlas, LinkedList<ImageReference> processingQueue)
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
				ImageReference currentImage = processingQueue.get(imageIndex);

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
						skyline.add(minimumIndex + 1, new SkylineRect(minimumRect.left + minimumWidth, minimumRect.width - minimumWidth, minimumRect.height));

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
				skyline.get(minimumIndex).height = Math.min(minimumIndex > 0 ? skyline.get(minimumIndex - 1).height : atlas.height, minimumIndex < skyline.size() - 1 ? skyline.get(minimumIndex + 1).height : atlas.height);
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

				for (ImageReference image : atlas.images)
				{
					image.atlas = null;
					unprocessedImages.add(image);
				}
			}

		for (Atlas atlas : removed)
			atlases.remove(atlas);
	}

	private Atlas createNewAtlasFor(ImageReference image)
	{
		int width = 512;
		for (; width < image.width; width += width)
			;

		int height = 512;
		for (; height < image.height; height += height)
			;

		return new Atlas(width, height);
	}

	private ArrayList<SkylineRect> flattenSkyline(ArrayList<SkylineRect> skyline)
	{
		ArrayList<SkylineRect> flattened = new ArrayList<SkylineRect>();

		for (int i = 1; i <= skyline.size(); ++i)
			if (i == skyline.size() || skyline.get(i - 1).height != skyline.get(i).height)
			{
				int j = i - 1;
				while (j > 0 && skyline.get(j).height == skyline.get(j - 1).height)
					--j;

				int totalWidth = 0;
				for (int k = j; k < i; ++k)
					totalWidth += skyline.get(k).width;

				flattened.add(new SkylineRect(skyline.get(j).left, totalWidth, skyline.get(j).height));
			}

		return flattened;
	}	
	
	public ArrayList<Atlas> getAtlases()
	{
		return atlases;
	}
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
