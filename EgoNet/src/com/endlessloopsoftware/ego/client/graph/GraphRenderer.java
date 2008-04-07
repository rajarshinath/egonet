package com.endlessloopsoftware.ego.client.graph;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.*;

import edu.uci.ics.jung.visualization.PluggableRenderer;
import com.endlessloopsoftware.ego.*;
import org.egonet.util.listbuilder.Selection;
import com.endlessloopsoftware.ego.client.EgoClient;
import com.endlessloopsoftware.ego.QuestionList;
import com.endlessloopsoftware.ego.Question;
import com.endlessloopsoftware.ego.client.statistics.Statistics;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.graph.decorators.ConstantEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.ToolTipFunction;
import edu.uci.ics.jung.graph.decorators.EdgeShape;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.graph.decorators.PickableEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgePaintFunction;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.control.SatelliteVisualizationViewer;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.decorators.EdgeShapeFunction;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.decorators.VertexShapeFunction;

public class GraphRenderer extends PluggableRenderer implements
		VertexShapeFunction, VertexPaintFunction, EdgeShapeFunction,
		EdgePaintFunction, EdgeStringer, VertexStringer, EdgeStrokeFunction,
		ToolTipFunction {

	private GraphSettings graphSettings;

	private static VisualizationViewer visualizationViewer;

	private GraphZoomScrollPane visualizationViewerScrollPane;

	private SatelliteVisualizationViewer satelliteVisualizationViewer;

	private GraphZoomScrollPane satelliteVisualizationViewerScrollPane;

	private DefaultModalGraphMouse graphMouse;

	private VisualizationModel visualizationModel;

	private StringLabeller undirectedLabeler;

	private Vertex[] _vertexArray = null;

	private int[][] adjacencyMatrix;

	private String[] alterList;

	private Statistics stats;

	private GraphData graphData;

	private static Graph graph;

	public static boolean showEdgeWeights = false;

	public GraphRenderer() {
		graph = new UndirectedSparseGraph();
		stats = EgoClient.interview.getStats();
		try {
			alterList = stats.alterList;
			_vertexArray = new Vertex[alterList.length];
			for (int i = 0; i < alterList.length; ++i) {
				_vertexArray[i] = new SparseVertex();
				graph.addVertex(_vertexArray[i]);
			}
		} catch (NullPointerException ex) {
			ex.printStackTrace(System.err);
		}
		graphData = new GraphData();
		graphSettings = new GraphSettings(this);
		adjacencyMatrix = graphData.getAdjacencyMatrix();
		this.setVertexShapeFunction(this);
		this.setVertexPaintFunction(this);
		this.setEdgeShapeFunction(this);
		this.setEdgePaintFunction(this);
		this.setEdgeStrokeFunction(this);
		EdgeStringer stringer = new EdgeStringer() {
			public String getLabel(ArchetypeEdge e) {
				return "";
			}
		};
		this.setEdgeStringer(stringer);
		this.setVertexStringer(this);
	}

	/**
	 * Redraws the graph with the provided layout
	 * 
	 * @param Class
	 *            layout
	 */
	public void changeLayout(Class layout) {
		try {
			Constructor constructor = layout
					.getConstructor(new Class[] { Graph.class });
			Object o = constructor.newInstance(graph);
			Layout l = (Layout) o;

			if (l instanceof FRLayout) {
				FRLayout frLayout = (FRLayout) l;
				frLayout.setMaxIterations(1000);
			}

			// TODO: change required with spring layout not FR layout
			if (l instanceof SpringLayout) {
				SpringLayout springLayout = (SpringLayout) l;
				// springLayout.setMaxIterations(1000);
			}
			visualizationViewer.stop();
			visualizationViewer.setGraphLayout(l, false);
			visualizationViewer.restart();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * create the main viewable Graph by for display on a panel use the JUNG
	 * classes
	 * 
	 * @return
	 */
	public JComponent createGraph() {
		ToolTipManager.sharedInstance().setDismissDelay(10000);
		// setVertexStringer(undirectedLabeler);
		graphMouse = new DefaultModalGraphMouse();

		// create the model that drives layouts and view updates
		visualizationModel = new DefaultVisualizationModel(new FRLayout(graph));

		// create the regular viewer and scroller
		visualizationViewer = new VisualizationViewer(visualizationModel, this);
		visualizationViewer.setPickSupport(new ShapePickSupport());
		visualizationViewer.setToolTipFunction(this);
		visualizationViewer.setGraphMouse(graphMouse);
		visualizationViewer.setBackground(Color.WHITE);

		visualizationViewerScrollPane = new GraphZoomScrollPane(
				visualizationViewer);
		final ScalingControl scaler = new CrossoverScalingControl();

		JButton plus = new JButton("+");
		plus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(visualizationViewer, 1.1f, visualizationViewer
						.getCenter());
			}
		});
		// plus.setMaximumSize(new Dimension(20,20));
		JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				scaler.scale(visualizationViewer, 1 / 1.1f, visualizationViewer
						.getCenter());
			}
		});
		// create the sat viewer and scroller
		satelliteVisualizationViewer = new SatelliteVisualizationViewer(
				visualizationViewer, visualizationModel,
				new PluggableRenderer()); // TODO: fix renderer and change
		// back to this
		satelliteVisualizationViewer.setPreferredSize(new Dimension(150, 150));
		satelliteVisualizationViewer.setToolTipFunction(this);
		satelliteVisualizationViewer.setBackground(visualizationViewer
				.getBackground());

		satelliteVisualizationViewerScrollPane = new GraphZoomScrollPane(
				satelliteVisualizationViewer);

		return visualizationViewerScrollPane;
	}

	/**
	 * creates the nodes for every alter creates edges for entries in adjacency
	 * matrix
	 */
	 public void updateEdges(){
		graph.removeAllEdges();
		Iterator edgeIterator = graphSettings.getEdgeIterator();
		while(edgeIterator.hasNext()) {
			Edge edge = (Edge)edgeIterator.next();
			Iterator iterator = graph.getEdges().iterator();
			while(iterator.hasNext()) {
				if(iterator.next().equals(edge))
					System.out.println("Skipping an existing edge");
					return;
			}
			try {
			graph.addEdge(edge);
			} catch(edu.uci.ics.jung.exceptions.ConstraintViolationException ex) {
				System.err.println(ex.getMessage());
			}
		}
		
//		int counter = 0;
//		if (!vertexPair.isEmpty()) {
//			for (Pair alterPair : vertexPair) {
//				UndirectedSparseEdge edge = new UndirectedSparseEdge(
//						_vertexArray[(Integer) alterPair.getFirst()],
//						_vertexArray[(Integer) alterPair.getSecond()]);
//				edgeMap.put(edge, edgePropertyList.get(counter));
//
//				edgeLabelMap.put(edge,
//						((Integer) stats.proximityMatrix[(Integer) alterPair
//								.getFirst()][(Integer) alterPair.getSecond()])
//								.toString());
//
//				counter++;
//				graph.addEdge(edge);
//
//			}
//			return;
//		}
//		for (int i = 0; i < adjacencyMatrix.length; ++i) {
//			for (int j = i + 1; j < adjacencyMatrix[i].length; ++j) {
//				if (adjacencyMatrix[i][j] > 0) {
//					UndirectedSparseEdge edge = new UndirectedSparseEdge(
//							_vertexArray[i], _vertexArray[j]);
//					graph.addEdge(edge);
//
//					edgeLabelMap.put(edge,
//							((Integer) stats.proximityMatrix[i][j]).toString());
//
//				}
//			}
//		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.decorators.VertexStringer#getLabel(edu.uci.ics.jung.graph.ArchetypeVertex)
	 */
	public String getLabel(ArchetypeVertex v) {
		return graphSettings.getNodeLabel(v);
	}

	/**
	 * Creates the small thumdnail viewer for the main graph. You MUST call
	 * createGraph first.
	 * 
	 * @return
	 */
	public JComponent createSatellitePane() {
		return satelliteVisualizationViewerScrollPane;
	}

	/**
	 * Displays the edges of graph used to draw the edge
	 */
	public void drawEdgeLabels() {
		this.setEdgeStringer(this);
		this.setEdgePaintFunction(new PickableEdgePaintFunction(this,
				Color.black, Color.cyan));
		visualizationViewer.repaint();
	}

	/**
	 * Displays the labels of nodes
	 */
	public void drawNodeLabels() {

		this.setVertexStringer(this);
		visualizationViewer.repaint();
	}

	public Vertex[] get_vertexArray() {
		return _vertexArray;
	}

	/**
	 * Implemented for VertexPaintFunction Returns the color of the outline of
	 * the vertex Draw paint color is defaulted BLACK
	 */
	public Paint getDrawPaint(Vertex v) {
		Color fillColor = graphSettings.getNodeColor(v);
		ConstantVertexPaintFunction cvpf = new ConstantVertexPaintFunction(
				Color.BLACK, fillColor);
		return cvpf.getDrawPaint(v);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getDrawPaint(edu.uci.ics.jung.graph.Edge)
	 */
	public Paint getDrawPaint(Edge e) {
		Color fillColor = graphSettings.getEdgeColor(e);
		ConstantEdgePaintFunction cvpf = new ConstantEdgePaintFunction(
				Color.BLACK, fillColor);
		return cvpf.getDrawPaint(e);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getFillPaint(edu.uci.ics.jung.graph.Edge)
	 */
	public Paint getFillPaint(Edge e) {
		Color fillColor = graphSettings.getEdgeColor(e);
		ConstantEdgePaintFunction cvpf = new ConstantEdgePaintFunction(
				Color.BLACK, null);
		return cvpf.getFillPaint(e);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction#getStroke(edu.uci.ics.jung.graph.Edge)
	 */
	public Stroke getStroke(Edge e) {
		ConstantEdgeStrokeFunction edgeStrokeFunction = new ConstantEdgeStrokeFunction(
				(float) graphSettings.getEdgeSize(e));
		return edgeStrokeFunction.getStroke(e);
	}

	/**
	 * Implemented for VertexPaintFunction Returns the color with which the
	 * vertex needs to be filled The color is determined by map entry
	 */
	public Paint getFillPaint(Vertex v) {
		Color fillColor = graphSettings.getNodeColor(v);
		ConstantVertexPaintFunction cvpf = new ConstantVertexPaintFunction(
				Color.BLACK, fillColor);

		return cvpf.getFillPaint(v);
	}

	public static Graph getGraph() {
		return graph;
	}

	public DefaultModalGraphMouse getGraphMouse() {
		return graphMouse;
	}

	public JComponent getGzsp() {
		return visualizationViewerScrollPane;
	}

	/**
	 * Implementes for EdgeStringer Retruns the edgeLabel for a given edge
	 */
	public String getLabel(ArchetypeEdge e) {

		return graphSettings.getEdgeLabel((Edge)e);
	}

	/**
	 * Implemented for VertexShapeFunction Returns shape of vertex by looking
	 * for an entry in map
	 */
	public Shape getShape(Vertex v) {
		NodeProperty.NodeShape shape = graphSettings.getNodeShape(v);
		int size = graphSettings.getNodeSize(v);
		EllipseVertexShapeFunction basicCircle = new EllipseVertexShapeFunction();
		Shape returnShape = basicCircle.getShape(v, 10 + (5 * size));
		switch (shape) {
		case Circle:
			EllipseVertexShapeFunction circle = new EllipseVertexShapeFunction();
			returnShape = circle.getShape(v, 10 + (5 * size));
			break;
		case Square:
			PolygonVertexShapeFunction square = new PolygonVertexShapeFunction();
			returnShape = square.getShape(v, 10 + (5 * size), 4);
			break;
		case Pentagon:
			PolygonVertexShapeFunction pentagon = new PolygonVertexShapeFunction();
			returnShape = pentagon.getShape(v, 10 + (5 * size), 5);
			break;
		case Hexagon:
			PolygonVertexShapeFunction hexagon = new PolygonVertexShapeFunction();
			returnShape = hexagon.getShape(v, 10 + (5 * size), 6);
			break;
		case Triangle:
			PolygonVertexShapeFunction triangle = new PolygonVertexShapeFunction();
			returnShape = triangle.getShape(v, 10 + (5 * size), 3);
			break;
		case Star:
			EllipseVertexShapeFunction star = new EllipseVertexShapeFunction();
			returnShape = star.getShape(v, NodeProperty.NodeShape.Star,
					10 + (5 * size));
			break;
		case RoundedRectangle:
			EllipseVertexShapeFunction roundRect = new EllipseVertexShapeFunction();
			returnShape = roundRect.getShape(v,
					NodeProperty.NodeShape.RoundedRectangle, 10 + (5 * size));
			break;
		}
		return returnShape;
	}

	public VisualizationModel getVisualizationModel() {
		return visualizationModel;
	}

	public static VisualizationViewer getVv() {
		return visualizationViewer;
	}

	/**
	 * Hides edge labels
	 */
	public void hideEdgeLabels() {
		EdgeStringer stringer = new EdgeStringer() {
			public String getLabel(ArchetypeEdge e) {
				return "";
			}
		};
		this.setEdgeStringer(stringer);
		visualizationViewer.repaint();
	}

	/**
	 * Hides the labels of nodes
	 */
	public void hideNodeLabels() {
		VertexStringer vertexStringer = new VertexStringer() {
			public String getLabel(ArchetypeVertex v) {
				return null;
			}
		};
		this.setVertexStringer(vertexStringer);
		visualizationViewer.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.uci.ics.jung.graph.decorators.EdgeShapeFunction#getShape(edu.uci.ics.jung.graph.Edge)
	 */
	public Shape getShape(Edge e) {
		Shape returnShape = null;
		EdgeShapeFunction edgeShapeFunction;
		switch (graphSettings.getEdgeShape(e)) {
		case Line:
			edgeShapeFunction = new EdgeShape.Line();
			returnShape = edgeShapeFunction.getShape(e);
			break;
		case QuadCurve:
			edgeShapeFunction = new EdgeShape.QuadCurve();
			returnShape = edgeShapeFunction.getShape(e);
			break;
		case CubicCurve:
			edgeShapeFunction = new EdgeShape.CubicCurve();
			returnShape = edgeShapeFunction.getShape(e);
			break;
		}
		return returnShape;
		// edgeShapeFunction = new EdgeShape.Line();
		// returnShape = edgeShapeFunction.getShape(e);
		// return returnShape;
	}

	/**
	 * Resizes edges by adjusting thickness
	 */
	public void reSizeEdges(float strokeWeight) {
		this
				.setEdgeStrokeFunction(new ConstantEdgeStrokeFunction(
						strokeWeight));
		visualizationViewer.repaint();

	}

	public void updateGraphSettings() {
		Iterator iterator = graphSettings.getQAsettingsIterator();
		graphSettings.emptyEdgeSettingsMap();
		graph.removeAllEdges();
		while (iterator.hasNext()) {
			GraphSettingsEntry entry = (GraphSettingsEntry) iterator.next();
			GraphQuestion graphQuestion = entry.getGraphQuestion();
			if ((graphQuestion.getCategory() == Question.ALTER_QUESTION)
					&& (entry.getType() == GraphSettingType.Node)) {
				NodeProperty nodeProperty = (NodeProperty) entry.getProperty();
				NodeProperty.Property prop = nodeProperty.getProperty();
				Question question = graphQuestion.getQuestion();
				Selection selection = graphQuestion.getSelection();
				GraphData graphData = new GraphData();
				List<Integer> alterList = graphData.getAlterNumbers(question,
						selection);

				switch (prop) {
				case Color:
					for (int alter : alterList) {
						graphSettings.setNodeColor(_vertexArray[alter],
								nodeProperty.getColor());
					}
					break;
				case Shape:
					for (int alter : alterList) {
						graphSettings.setNodeShape(_vertexArray[alter],
								nodeProperty.getShape());
					}
					break;
				case Size:
					for (int alter : alterList) {
						graphSettings.setNodeSize(_vertexArray[alter],
								nodeProperty.getSize());
					}
					break;
				case Label:
					for (int alter : alterList) {
						graphSettings.setNodeLabel(_vertexArray[alter],
								nodeProperty.getLabel());
					}
					break;
				}
			} else if (graphQuestion.getCategory() == 0) // structural
			// measure
			{
				NodeProperty nodeProperty = (NodeProperty) entry.getProperty();
				NodeProperty.Property prop = nodeProperty.getProperty();
				if (graphQuestion.getSelection().getString() == "DegreeCentrality") {
					switch (prop) {
					case Color:
						applyDegreeCentrality(NodeProperty.Property.Color);
						break;
					case Size:
						applyDegreeCentrality(NodeProperty.Property.Size);
						break;
					}
				} else { // Degree centrality
					switch (prop) {
					case Color:
						applyBetweennessCentrality(NodeProperty.Property.Color);
						break;
					case Size:
						applyBetweennessCentrality(NodeProperty.Property.Size);
						break;
					}
				}
			}
			// Edge property manipulation
			else if ((graphQuestion.getCategory() == Question.ALTER_PAIR_QUESTION)
					&& (entry.getType() == GraphSettingType.Edge)) {
				EdgeProperty edgeProperty = (EdgeProperty) entry.getProperty();
				EdgeProperty.Property prop = edgeProperty.getProperty();
				GraphData graphData = new GraphData();
				List<Pair> vPair = graphData.getAlterPairs(graphQuestion);
				switch (prop) {
				case Color:
					for (Pair pair : vPair) {
						UndirectedSparseEdge edge = new UndirectedSparseEdge(
								_vertexArray[(Integer) pair.getFirst()],
								_vertexArray[(Integer) pair.getSecond()]);
						graphSettings.setEdgeColor(edge, edgeProperty
								.getColor());
					}
					break;
				case Shape:
					for (Pair pair : vPair) {
						UndirectedSparseEdge edge = new UndirectedSparseEdge(
								_vertexArray[(Integer) pair.getFirst()],
								_vertexArray[(Integer) pair.getSecond()]);
						graphSettings.setEdgeShape(edge, edgeProperty
								.getShape());
					}
					break;
				case Size:
					for (Pair pair : vPair) {
						UndirectedSparseEdge edge = new UndirectedSparseEdge(
								_vertexArray[(Integer) pair.getFirst()],
								_vertexArray[(Integer) pair.getSecond()]);
						graphSettings.setEdgeSize(edge, edgeProperty.getSize());
					}
				}
			}
		}
		updateEdges();
		visualizationViewer.repaint();
	}

	private float max(float[] array) {
		float max = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
			}
		}
		return max;
	}

	private void applyDegreeCentrality(NodeProperty.Property property) {

		float[] degreeCentrality = new float[EgoClient.interview.getNumAlters()];
		float[] scaledDegreeCentrality = new float[EgoClient.interview
				.getNumAlters()];
		for (int i = 0; i < EgoClient.interview.getNumAlters(); i++) {
			degreeCentrality[i] = new Float(
					EgoClient.interview.getStats().degreeArray[i]
							/ ((float) (EgoClient.interview.getStats().proximityMatrix.length - 1)));
		}
		// scale the values
		float maximum = max(degreeCentrality);
		for (int i = 0; i < degreeCentrality.length; i++) {
			scaledDegreeCentrality[i] = (1 / maximum) * degreeCentrality[i];
		}

		for (int i = 0; i < scaledDegreeCentrality.length; i++) {
			float grayPercentage = 1 - scaledDegreeCentrality[i];
			if (property == NodeProperty.Property.Color) {
				Color nodeColor = new Color(grayPercentage, grayPercentage,
						grayPercentage);
				graphSettings.setNodeColor(_vertexArray[i], nodeColor);
			} else if (property == NodeProperty.Property.Size) {
				int size = Math.round(1 + 2 * scaledDegreeCentrality[i]);
				graphSettings.setNodeSize(_vertexArray[i], size);
			}
		}
	}

	private void applyBetweennessCentrality(NodeProperty.Property property) {

		float[] betweennessCentrality = new float[EgoClient.interview
				.getNumAlters()];
		float[] scaledBetweennessCentrality = new float[EgoClient.interview
				.getNumAlters()];
		for (int i = 0; i < EgoClient.interview.getNumAlters(); i++) {
			double big = EgoClient.interview.getStats().proximityMatrix.length - 1;
			big *= big;
			betweennessCentrality[i] = new Float(
					EgoClient.interview.getStats().betweennessArray[i] / big);
		}
		// scale the values
		float maximum = max(betweennessCentrality);
		for (int i = 0; i < betweennessCentrality.length; i++) {
			scaledBetweennessCentrality[i] = (1 / maximum)
					* betweennessCentrality[i];
		}

		for (int i = 0; i < scaledBetweennessCentrality.length; i++) {
			float grayPercentage = 1 - scaledBetweennessCentrality[i];
			if (property == NodeProperty.Property.Color) {
				Color nodeColor = new Color(grayPercentage, grayPercentage,
						grayPercentage);
				graphSettings.setNodeColor(_vertexArray[i], nodeColor);
			} else if (property == NodeProperty.Property.Size) {
				int size = Math.round(1 + 2 * scaledBetweennessCentrality[i]);
				graphSettings.setNodeSize(_vertexArray[i], size);
			}
		}
	}

	public void updateGraphSettings(Object updateValue, int nodeIndex,
			int updateParam) {
		// update param
		// 1: Label
		// 2: Color
		// 3: Shape
		// 4: Size
		switch (updateParam) {
		case 1:
			graphSettings.setNodeLabel(_vertexArray[nodeIndex],
					(String) updateValue);
			break;
		case 2:
			graphSettings.setNodeColor(_vertexArray[nodeIndex],
					(Color) updateValue);
			break;
		case 3:
			graphSettings.setNodeShape(_vertexArray[nodeIndex],
					(NodeProperty.NodeShape) updateValue);
			break;
		case 4:
			graphSettings.setNodeSize(_vertexArray[nodeIndex], Integer
					.parseInt((String) updateValue));
			break;

		}
	}

	public void addQAsettings(GraphQuestion graphQuestion,
			NodeProperty nodeProperty) {
		graphSettings.addQAsetting(graphQuestion, nodeProperty);
	}

	public void addQAsettings(GraphQuestion graphQuestion,
			EdgeProperty edgeProperty) {
		graphSettings.addQAsetting(graphQuestion, edgeProperty);
	}

	public String getToolTipText(Vertex v) {
		String text = graphSettings.getNodeToolTipText(v);
		// System.out.println(text);
		return text;
	}

	public String getToolTipText(Edge e) {
		return e.toString();
	}

	public String getToolTipText(MouseEvent event) {
		return ((JComponent) event.getSource()).getToolTipText();
	}

	public Iterator getSettingsIterator() {
		return graphSettings.getQAsettingsIterator();
	}
}
