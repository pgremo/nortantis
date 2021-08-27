package nortantis;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel
{
	public BufferedImage image;
	
	public ImagePanel()
	{
		
	}
	
	public ImagePanel(BufferedImage image)
	{
		this.image = image;
	}
	
	public void setImage(BufferedImage image)
	{
		this.image = image;
	}
	
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.drawImage(image, 0, 0, null);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return image == null ? super.getPreferredSize() : new Dimension(image.getWidth(), image.getHeight());
	}

}

