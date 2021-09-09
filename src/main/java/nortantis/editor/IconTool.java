package nortantis.editor;

import hoten.geom.Point;
import hoten.voronoi.Center;
import hoten.voronoi.Corner;
import hoten.voronoi.Edge;
import hoten.voronoi.VoronoiGraph;
import nortantis.*;
import nortantis.util.ImageHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class IconTool extends EditorTool
{

	private JRadioButton mountainsButton;
	private JRadioButton treesButton;
	private BufferedImage mapWithouticons;
	private JComboBox<ImageIcon> brushSizeComboBox;
	private JPanel brushSizePanel;
	private JRadioButton hillsButton;
	private JRadioButton dunesButton;
	private IconTypeButtons mountainTypes;
	private IconTypeButtons hillTypes;
	private IconTypeButtons duneTypes;
	private IconTypeButtons treeTypes;
	private IconTypeButtons cityTypes;
	private JSlider densitySlider;
	private final Random rand;
	private JPanel densityPanel;
	private JRadioButton eraseButton;
	private JRadioButton eraseAllButton;
	private JRadioButton eraseMountainsButton;
	private JRadioButton eraseHillsButton;
	private JRadioButton eraseDunesButton;
	private JRadioButton eraseTreesButton;
	private JPanel eraseOptionsPanel;
	private JRadioButton riversButton;
	private JRadioButton citiesButton;
	private JPanel riverOptionPanel;
	private JSlider riverWidthSlider;
	private Corner riverStart;
	private JCheckBox showRiversOnTopCheckBox;
	private JRadioButton eraseRiversButton;
	private JRadioButton eraseCitiesButton;

	public IconTool(MapSettings settings, EditorFrame parent)
	{
		super(settings, parent);
		rand = new Random();
	}

	@Override
	public String getToolbarName()
	{
		return "Icon";
	}

	@Override
	public String getImageIconFilePath()
	{
		return Paths.get("assets/internal/Icon tool.png").toString();
	}

	@Override
	public void onBeforeSaving()
	{		
	}

	@Override
	public void onSwitchingAway()
	{
	}

	@Override
	protected JPanel createToolsOptionsPanel()
	{
		var toolOptionsPanel = new JPanel();
		toolOptionsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		toolOptionsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		toolOptionsPanel.setLayout(new BoxLayout(toolOptionsPanel, BoxLayout.Y_AXIS));
		
		// Tools
		{
			var group = new ButtonGroup();
			var radioButtons = new ArrayList<JComponent>();
			
			mountainsButton = new JRadioButton("Mountains");
		    group.add(mountainsButton);
		    radioButtons.add(mountainsButton);
		    mountainsButton.setSelected(true);
		    mountainsButton.addActionListener(event -> updateTypePanels());
	
			hillsButton = new JRadioButton("Hills");
		    group.add(hillsButton);
		    radioButtons.add(hillsButton);
		    hillsButton.addActionListener(event -> updateTypePanels());

			dunesButton = new JRadioButton("Dunes");
		    group.add(dunesButton);
		    radioButtons.add(dunesButton);
		    dunesButton.addActionListener(event -> updateTypePanels());
	
			treesButton = new JRadioButton("Trees");
		    group.add(treesButton);
		    radioButtons.add(treesButton);
		    treesButton.addActionListener(event -> updateTypePanels());
		    
			riversButton = new JRadioButton("Rivers");
		    group.add(riversButton);
		    radioButtons.add(riversButton);
		    riversButton.addActionListener(event -> updateTypePanels());
		    
			citiesButton = new JRadioButton("Cities");
		    group.add(citiesButton);
		    radioButtons.add(citiesButton);
		    citiesButton.addActionListener(event -> updateTypePanels());
		    
			eraseButton = new JRadioButton("Erase");
		    group.add(eraseButton);
		    radioButtons.add(eraseButton);
		    eraseButton.addActionListener(event -> updateTypePanels());

			addLabelAndComponentsToPanel(
					toolOptionsPanel,
					new JLabel("Brush:"),
		    		radioButtons);
		}
	    
		mountainTypes = createRadioButtonsForIconType(toolOptionsPanel, IconType.mountains);
		hillTypes = createRadioButtonsForIconType(toolOptionsPanel, IconType.hills);
		duneTypes = createRadioButtonsForIconType(toolOptionsPanel, IconType.sand);
		treeTypes = createRadioButtonsForIconType(toolOptionsPanel, IconType.trees);
		cityTypes = createRadioButtonsForCities(toolOptionsPanel);
		
		// River options
		{
			var widthLabel = new JLabel("Width:");
			riverWidthSlider = new JSlider(1, 15);
			riverWidthSlider.setValue(1);
			riverWidthSlider.setPreferredSize(new Dimension(160, 50));
		    riverOptionPanel = addLabelAndComponentToPanel(toolOptionsPanel, widthLabel, riverWidthSlider);
		}
		
		// Eraser options
		{
			var typeLabel = new JLabel("Erase:");
			var group = new ButtonGroup();
			var radioButtons = new ArrayList<JRadioButton>();
		    
		    eraseAllButton = new JRadioButton("All");
		    group.add(eraseAllButton);
		    radioButtons.add(eraseAllButton);
		    
		    eraseMountainsButton = new JRadioButton(mountainsButton.getText());
		    group.add(eraseMountainsButton);
		    radioButtons.add(eraseMountainsButton);

		    eraseHillsButton = new JRadioButton(hillsButton.getText());
		    group.add(eraseHillsButton);
		    radioButtons.add(eraseHillsButton);

		    eraseDunesButton = new JRadioButton(dunesButton.getText());
		    group.add(eraseDunesButton);
		    radioButtons.add(eraseDunesButton);

		    eraseTreesButton = new JRadioButton(treesButton.getText());
		    group.add(eraseTreesButton);
		    radioButtons.add(eraseTreesButton);
		    
		    eraseCitiesButton = new JRadioButton(citiesButton.getText());
		    group.add(eraseCitiesButton);
		    radioButtons.add(eraseCitiesButton);

		    eraseRiversButton = new JRadioButton(riversButton.getText());
		    group.add(eraseRiversButton);
		    radioButtons.add(eraseRiversButton);

		    eraseAllButton.setSelected(true);
		    eraseOptionsPanel = addLabelAndComponentsToPanel(toolOptionsPanel, typeLabel, radioButtons);
		}

		var densityLabel = new JLabel("density:");
		densitySlider = new JSlider(1, 50);
		densitySlider.setValue(10);
		densitySlider.setPreferredSize(new Dimension(160, 50));
		densityPanel = addLabelAndComponentToPanel(toolOptionsPanel, densityLabel, densitySlider);

		var brushSizeLabel = new JLabel("Brush size:");
	    brushSizeComboBox = new JComboBox<>();
	    var largest = Collections.max(brushSizes);
	    for (var brushSize : brushSizes)
	    {
	    	if (brushSize == 1)
	    	{
	    		brushSize = 4; // Needed to make it visible
	    	}
			var image = new BufferedImage(largest, largest, BufferedImage.TYPE_INT_ARGB);
			var g = image.createGraphics();
	    	g.setColor(Color.white);
	    	g.setColor(Color.black);
	    	g.fillOval(largest/2 - brushSize/2, largest/2 - brushSize/2, brushSize, brushSize);
	    	brushSizeComboBox.addItem(new ImageIcon(image));
	    }
	    brushSizePanel = addLabelAndComponentToPanel(toolOptionsPanel, brushSizeLabel, brushSizeComboBox);

		var showRiversLabel = new JLabel("");
	    showRiversOnTopCheckBox = new JCheckBox("Show rivers on top?");
	    showRiversOnTopCheckBox.setToolTipText("Temporarily show rivers on top of icons to make them visible in the editor.");
	    addLabelAndComponentToPanel(toolOptionsPanel, showRiversLabel, showRiversOnTopCheckBox);
	    showRiversOnTopCheckBox.addActionListener(e -> createAndShowMap());
	    		
	    
	    // Prevent the panel from shrinking when components are hidden.
	    toolOptionsPanel.add(Box.createRigidArea(new Dimension(EditorFrame.toolsPanelWidth - 25, 0)));
	    
		mountainsButton.doClick();
	    
	    return toolOptionsPanel;
	}
	
	private void updateTypePanels()
	{
		mountainTypes.panel.setVisible(mountainsButton.isSelected());
		hillTypes.panel.setVisible(hillsButton.isSelected());
		duneTypes.panel.setVisible(dunesButton.isSelected());
		treeTypes.panel.setVisible(treesButton.isSelected());
		cityTypes.panel.setVisible(citiesButton.isSelected());
		densityPanel.setVisible(treesButton.isSelected());
		eraseOptionsPanel.setVisible(eraseButton.isSelected());
		riverOptionPanel.setVisible(riversButton.isSelected());
		brushSizePanel.setVisible(!riversButton.isSelected());
	}
	
	private IconTypeButtons createRadioButtonsForIconType(JPanel toolOptionsPanel, IconType iconType)
	{
		var typeLabel = new JLabel("Type:");

		var radioButtons = IconDrawer.getIconGroupNames(iconType, Objects.equals(iconType, IconType.cities) ? settings.cityIconSetName : null)
				.map(JRadioButton::new)
				.collect(toList());
		radioButtons.stream().reduce(new ButtonGroup(), (acc, x) -> {
			acc.add(x);
			return acc;
		}, (a, b) -> a);
		radioButtons.stream().findFirst().ifPresent(x -> x.setSelected(true));
	    return new IconTypeButtons(addLabelAndComponentsToPanel(toolOptionsPanel, typeLabel, radioButtons), radioButtons);
	}
	
	private IconTypeButtons createRadioButtonsForCities(JPanel toolOptionsPanel)
	{
		var typeLabel = new JLabel("Cities:");
		var group = new ButtonGroup();
		var radioButtons = new ArrayList<JRadioButton>();
	    for (var fileNameWithoutWidthOrExtension : IconDrawer.getIconGroupFileNamesWithoutWidthOrExtension(IconType.cities, null, settings.cityIconSetName))
	    {
			var button = new JRadioButton(fileNameWithoutWidthOrExtension);
	    	group.add(button);
	    	radioButtons.add(button);
	    }
	    if (radioButtons.size() > 0)
	    {
	    	radioButtons.get(0).setSelected(true);
	    }
	    return new IconTypeButtons(addLabelAndComponentsToPanel(toolOptionsPanel, typeLabel, radioButtons), radioButtons);
	}

	@Override
	protected void handleMouseClickOnMap(MouseEvent e)
	{		
	}
	
	private void handleMousePressOrDrag(MouseEvent e)
	{
		if (riversButton.isSelected())
		{
			return;
		}
		
		boolean needsFullRedraw = false;

		var selected = getSelectedLandCenters(e.getPoint());

		if (mountainsButton.isSelected())
		{
			var rangeId = mountainTypes.getSelectedOption();
			for (var center : selected)
			{
				settings.edits.centerEdits.get(center.index).icon = new CenterIcon(CenterIconType.Mountain, rangeId, Math.abs(rand.nextInt()));
			}
		}
		else if (hillsButton.isSelected())
		{
			var rangeId = hillTypes.getSelectedOption();
			for (var center : selected)
			{
				settings.edits.centerEdits.get(center.index).icon = new CenterIcon(CenterIconType.Hill, rangeId, Math.abs(rand.nextInt()));
			}
		}
		else if (dunesButton.isSelected())
		{
			var rangeId = duneTypes.getSelectedOption();
			for (var center : selected)
			{
				settings.edits.centerEdits.get(center.index).icon = new CenterIcon(CenterIconType.Dune, rangeId, Math.abs(rand.nextInt()));
			}		
		}
		else if (treesButton.isSelected())
		{
			var treeType = treeTypes.getSelectedOption();
			for (var center : selected)
			{
				settings.edits.centerEdits.get(center.index).trees = new CenterTrees(treeType, densitySlider.getValue() / 10.0, 
						Math.abs(rand.nextLong()));
			}		
		}
		else if (citiesButton.isSelected())
		{
			var cityName = cityTypes.getSelectedOption();
			for (var center : selected)
			{
				settings.edits.centerEdits.get(center.index).icon = new CenterIcon(CenterIconType.City, cityName);
			}
		}
		else if (eraseButton.isSelected())
		{
			if (eraseAllButton.isSelected())
			{
				for (var center : selected)
				{
					settings.edits.centerEdits.get(center.index).trees = null;
					settings.edits.centerEdits.get(center.index).icon = null;
					for (var edge : center.borders)
					{
						var eEdit = settings.edits.edgeEdits.get(edge.index);
						if (eEdit.riverLevel >= VoronoiGraph.riversThinnerThanThisWillNotBeDrawn)
						{
							needsFullRedraw = true;
							eEdit.riverLevel = 0;
						}
					}
				}
			}
			else if (eraseMountainsButton.isSelected())
			{
				for (var center : selected)
				{
					var cEdit = settings.edits.centerEdits.get(center.index);
					if (cEdit.icon != null && cEdit.icon.iconType == CenterIconType.Mountain)
					{
						cEdit.icon = null;
					}
				}	
			}
			else if (eraseHillsButton.isSelected())
			{
				for (var center : selected)
				{
					var cEdit = settings.edits.centerEdits.get(center.index);
					if (cEdit.icon != null && cEdit.icon.iconType == CenterIconType.Hill)
					{
						cEdit.icon = null;
					}
				}	
			}
			else if (eraseDunesButton.isSelected())
			{
				for (var center : selected)
				{
					var cEdit = settings.edits.centerEdits.get(center.index);
					if (cEdit.icon != null && cEdit.icon.iconType == CenterIconType.Dune)
					{
						cEdit.icon = null;
					}
				}	
			}
			else if (eraseTreesButton.isSelected())
			{
				for (var center : selected)
				{
					var cEdit = settings.edits.centerEdits.get(center.index);
					cEdit.trees = null;
				}	
			}
			else if (eraseCitiesButton.isSelected())
			{
				for (var center : selected)
				{
					var cEdit = settings.edits.centerEdits.get(center.index);
					if (cEdit.icon != null && cEdit.icon.iconType == CenterIconType.City)
					{
						cEdit.icon = null;
					}
				}	
			}
			else if (eraseRiversButton.isSelected())
			{
				for (var center : selected)
				{
					for (var edge : center.borders)
					{
						var eEdit = settings.edits.edgeEdits.get(edge.index);
						if (eEdit.riverLevel >= VoronoiGraph.riversThinnerThanThisWillNotBeDrawn)
						{
							needsFullRedraw = true;
							eEdit.riverLevel = 0;
						}
					}
				}
			}
		}
		handleMapChange(selected, !needsFullRedraw);
	}
	
	private Set<Center> getSelectedLandCenters(java.awt.Point point)
	{
		return getSelectedCenters(point).stream().filter(c -> !c.isWater).collect(toSet());
	}

	@Override
	protected void handleMousePressedOnMap(MouseEvent e)
	{		
		handleMousePressOrDrag(e);
		
		if (riversButton.isSelected())
		{
			riverStart = mapParts.graph.findClosestCorner(new Point(e.getX(), e.getY()));
		}
	}

	@Override
	protected void handleMouseReleasedOnMap(MouseEvent e)
	{		
		if (riversButton.isSelected())
		{
			var end = mapParts.graph.findClosestCorner(new Point(e.getX(), e.getY()));
			var river = filterOutOceanAndCoastEdges(mapParts.graph.findPathGreedy(riverStart, end));
			for (var edge : river)
			{
				var base = (riverWidthSlider.getValue() - 1);
				if (base >= 2)
				{
					base += 1; // Level 3 looks just like level 2, so skip it.
				}
				settings.edits.edgeEdits.get(edge.index).riverLevel = (base * base * 6) + VoronoiGraph.riversThinnerThanThisWillNotBeDrawn + 1;
			}
			riverStart = null;
			mapEditingPanel.clearHighlightedEdges();
			mapEditingPanel.addAllProcessingEdges(river);
			mapEditingPanel.repaint();
			
			if (river.size() > 0)
			{
				createAndShowMap();
			}
		}
		
		setUndoPoint();
	}
	
	private Set<Edge> filterOutOceanAndCoastEdges(Set<Edge> edges)
	{
		return edges.stream().filter(e -> (e.d0 == null || !e.d0.isWater ) && (e.d1 == null || !e.d1.isWater)).collect(toSet());
	}

	@Override
	protected void handleMouseMovedOnMap(MouseEvent e)
	{
		
		if (!riversButton.isSelected())
		{
			highlightHoverCenters(e);
			mapEditingPanel.repaint();
		}
	}
	
	private void highlightHoverCenters(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedCenters();
		Set<Center> selected = getSelectedCenters(e.getPoint());
		mapEditingPanel.addAllHighlightedCenters(selected);
		mapEditingPanel.setCenterHighlightMode(HighlightMode.outlineEveryCenter);	
	}

	@Override
	protected void handleMouseDraggedOnMap(MouseEvent e)
	{
		if (riversButton.isSelected())
		{
			if (riverStart != null)
			{
				mapEditingPanel.clearHighlightedEdges();
				var end = mapParts.graph.findClosestCorner(new Point(e.getX(), e.getY()));
				var river = filterOutOceanAndCoastEdges(mapParts.graph.findPathGreedy(riverStart, end));
				mapEditingPanel.setHighlightedEdges(river);
				mapEditingPanel.repaint();
			}
		}
		else
		{
			highlightHoverCenters(e);
			handleMousePressOrDrag(e);
		}
	}

	@Override
	protected void handleMouseExitedMap(MouseEvent e)
	{
		mapEditingPanel.clearHighlightedCenters();
		mapEditingPanel.repaint();
	}

	@Override
	protected void onBeforeCreateMap()
	{
		// Change a few settings to make map creation faster.
		settings.resolution = zoom;
		settings.landBlur = 0;
		settings.oceanEffectSize = 0;
		settings.frayedBorder = false;
		settings.drawText = false;
		settings.grungeWidth = 0;
		settings.drawBorder = false;
		settings.drawIcons = false;
		settings.drawRivers = false;
	}

	@Override
	protected BufferedImage onBeforeShowMap(BufferedImage map, boolean  isQuickUpdate)
	{
		if (!isQuickUpdate)
		{
			mapWithouticons = ImageHelper.deepCopy(map);
			
			if (!showRiversOnTopCheckBox.isSelected())
			{
				MapCreator.drawRivers(settings, mapParts.graph, map, mapParts.sizeMultiplier);
			}
			mapParts.iconDrawer.drawAllIcons(map, mapParts.landBackground);
		}
		
		if (showRiversOnTopCheckBox.isSelected())
		{
			MapCreator.drawRivers(settings, mapParts.graph, map, mapParts.sizeMultiplier);
		}
				 		
 		return map;
	}
	
	@Override
	protected BufferedImage drawMapQuickUpdate()
	{
		var map = ImageHelper.deepCopy(mapWithouticons);
		if (!showRiversOnTopCheckBox.isSelected())
		{
			MapCreator.drawRivers(settings, mapParts.graph, map, mapParts.sizeMultiplier);
		}
		mapParts.iconDrawer.clearAndAddIconsFromEdits(settings.edits, mapParts.sizeMultiplier);
		mapParts.iconDrawer.drawAllIcons(map, mapParts.landBackground);
		if (showRiversOnTopCheckBox.isSelected())
		{
			MapCreator.drawRivers(settings, mapParts.graph, map, mapParts.sizeMultiplier);
		}

		return map;
	}
	
	@Override
	protected void onAfterUndoRedo(boolean requiresFullRedraw)
	{	
		createAndShowMap();
	}
	
	private Set<Center> getSelectedCenters(java.awt.Point point)
	{
		return getSelectedCenters(point, brushSizes.get(brushSizeComboBox.getSelectedIndex()));
	}
	
	private void handleMapChange(Set<Center> centers, boolean onlyUpdateIcons)
	{
		mapEditingPanel.addAllProcessingCenters(centers);
		mapEditingPanel.repaint();
		
		createAndShowMap(onlyUpdateIcons);
	}




}
