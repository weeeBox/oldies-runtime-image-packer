public class ImageReference implements Comparable<ImageReference>
{		
	int width;
	int height;
	
	boolean flipped;
	
	Atlas atlas;
	int x;
	int y;
	
	public ImageReference(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	@Override
	public int compareTo(ImageReference other)
	{
		return -(width * height - other.width * other.height);
	}		
	
}
