import java.util.ArrayList;

public class Atlas
{
	Atlas(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		images = new ArrayList<ImageReference>();
	}
	
	ArrayList<ImageReference> images;
	
	int width;
	int height;
	
	boolean tight;		
}
