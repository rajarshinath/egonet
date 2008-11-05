/***
 * Copyright (c) 2008, Endless Loop Software, Inc.
 * 
 * This file is part of EgoNet.
 * 
 * EgoNet is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * EgoNet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.endlessloopsoftware.ego.client.graph;

import com.endlessloopsoftware.ego.client.graph.GraphSettingsEntry.GraphSettingType;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;

import java.util.*;
import java.awt.*;
import java.io.*;


import com.endlessloopsoftware.ego.client.*;
import com.endlessloopsoftware.ego.client.graph.NodeProperty.NodePropertyType;
import com.endlessloopsoftware.ego.client.graph.NodeProperty.NodeShape;
import com.endlessloopsoftware.ego.client.graph.EdgeProperty.EdgePropertyType;
import com.endlessloopsoftware.ego.client.graph.EdgeProperty.EdgeShape;
import com.endlessloopsoftware.egonet.Answer;
import com.endlessloopsoftware.egonet.Question;
import com.endlessloopsoftware.egonet.QuestionList;
import com.endlessloopsoftware.egonet.Shared;
import com.endlessloopsoftware.egonet.Study;
import com.endlessloopsoftware.egonet.Shared.QuestionType;

import org.egonet.util.listbuilder.Selection;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
//test comment
public class GraphSettings {

	private Map<ArchetypeVertex, NodeProperty> nodeSettingsMap = Collections
			.synchronizedMap(new HashMap<ArchetypeVertex, NodeProperty>());

	private Map<Edge, EdgeProperty> edgeSettingsMap = Collections
			.synchronizedMap(new HashMap<Edge, EdgeProperty>());

	private java.util.List<GraphSettingsEntry> QAsettings = Collections
			.synchronizedList(new ArrayList<GraphSettingsEntry>());

	GraphRenderer renderer;

	private EgoClient egoClient;
	public GraphSettings(EgoClient egoClient, GraphRenderer renderer) {
		this.egoClient = egoClient;
		this.renderer = renderer;
			init();
	}

	private void init() {
		int noOfAlters = egoClient.getInterview().getNumAlters();
		// initialize nodes with default settings
		for (int i = 0; i < noOfAlters; i++) {
			String alterName = egoClient.getInterview().getAlterList()[i];
			Color color = Color.RED;
			int size = 1;
			NodeShape shape = NodeShape.Circle;
			NodeProperty nodeProperty = new NodeProperty(alterName, color,
					shape, size);
			String toolTipText = getAlterInfo(i);
			nodeProperty.setToolTipText(toolTipText);
			nodeSettingsMap.put(renderer.getvertexArray()[i], nodeProperty);
		}
		// initialize edges with default settings
		GraphRenderer.getGraph().removeAllEdges();
		GraphData graphData = new GraphData(egoClient);
		int[][] adjacencyMatrix = graphData.getAdjacencyMatrix();
		for (int i = 0; i < adjacencyMatrix.length; ++i) {
			for (int j = i + 1; j < adjacencyMatrix[i].length; ++j) {
				if (adjacencyMatrix[i][j] > 0) {
					UndirectedSparseEdge edge = new UndirectedSparseEdge(
							renderer.getvertexArray()[i], renderer
									.getvertexArray()[j]);
					GraphRenderer.getGraph().addEdge(edge);
					String label = ((Integer) egoClient.getInterview().getStats().proximityMatrix[i][j])
							.toString();
					EdgeProperty edgeProperty = new EdgeProperty(label,
							Color.BLACK, EdgeShape.Line, 1);
					edgeProperty.setVisible(true);
					edgeSettingsMap.put(edge, edgeProperty);
				}
			}
		}
	}

	public void saveSettingsFile(File file) {
		try {
			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();

			Element studyElement = doc.createElement("GraphSettings");
			String studyID = ((Long) egoClient.getStudy().getStudyId()).toString();
			studyElement.setAttribute("StudyId", studyID);
			doc.appendChild(studyElement);

			Element graphElement = doc.createElement("GraphElement");

			// layoutElement
			Element layoutElement = doc.createElement("Layout");
			layoutElement.setAttribute("layout", renderer
					.getVisualizationModel().getGraphLayout().getClass()
					.getName());
			System.out
					.println("renderer.getVv().getLayout().getClass().getName() "
							+ renderer.getVisualizationModel().getGraphLayout()
									.getClass().getName());

			// zoomElement
			Element zoomElement = doc.createElement("Zoom");
			zoomElement.setAttribute("zoom", GraphRenderer.getVv().getLayout()
					.getClass().toString());

			// backGroundElement
			Element backGroundElement = doc.createElement("Background");
			String background = ((Integer) GraphRenderer.getVisualizationViewer()
					.getBackground().getRGB()).toString();
			backGroundElement.setAttribute("background", background);

			// showNodeLabelElement
			Element showNodeLabelElement = doc.createElement("ShowNodeLabel");
			showNodeLabelElement.setAttribute("shownodelabel", renderer
					.getShowNodeLabels() ? "true" : "false");

			graphElement.appendChild(layoutElement);
			graphElement.appendChild(backGroundElement);
			graphElement.appendChild(zoomElement);
			graphElement.appendChild(showNodeLabelElement);

			studyElement.appendChild(graphElement);

			for (GraphSettingsEntry entry : QAsettings)
				entry.writeEntryElement(doc, studyElement);

			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer aTransformer = tranFactory.newTransformer();
			aTransformer.setOutputProperty("indent", "yes");
			aTransformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");

			Source src = new DOMSource(doc);
			Result dest = new StreamResult(file);
			aTransformer.transform(src, dest);

		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	public void loadSettingsFile(File file) throws ParserConfigurationException, SAXException, IOException, Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);
		NodeList nodeList = document.getDocumentElement().getChildNodes();

		Study study = egoClient.getInterview().getStudy();
		QuestionList questionList = study.getQuestions();
		

		for (int i = 1; i < nodeList.getLength(); i++) {
			Node entryNode = nodeList.item(i);

			if (entryNode.getNodeType() == Node.ELEMENT_NODE
					&& ((Element) entryNode).getTagName()
							.equals("GraphElement")) {
				Element graphElement = (Element) entryNode;

				Element layoutElement = (Element) graphElement
						.getElementsByTagName("Layout").item(0);
				//Element zoomElement = (Element) graphElement.getElementsByTagName("Zoom").item(0);
				Element backgroundElement = (Element) graphElement
						.getElementsByTagName("Background").item(0);
				Element showNodeLabelElement = (Element) graphElement
						.getElementsByTagName("ShowNodeLabel").item(0);

				try {
					renderer.changeLayout(Class.forName(layoutElement
							.getAttribute("layout")));
				} catch (ClassNotFoundException e) {
					System.out.println("Specified class is not a Layout class");
					e.printStackTrace();
				}

				GraphRenderer.getVv().setBackground(
						Color.decode(backgroundElement
								.getAttribute("background")));
				if (showNodeLabelElement.getAttribute("shownodelabel").equals(
						"true"))
					renderer.drawNodeLabels();
			}

			if (entryNode.getNodeType() == Node.ELEMENT_NODE
					&& ((Element) entryNode).getTagName().equals("Entry")) {
				Element entryElement = (Element) entryNode;

				Element graphQuestionSelectionElement = (Element) entryElement
						.getElementsByTagName("GraphQuestionSelectionPair")
						.item(0);

				Element questionElement = (Element) graphQuestionSelectionElement
						.getElementsByTagName("Question").item(0);
				Element selectionElement = (Element) graphQuestionSelectionElement
						.getElementsByTagName("Selection").item(0);
				Element categoryElement = (Element) graphQuestionSelectionElement
						.getElementsByTagName("Category").item(0);

				Element propertyElement = (Element) entryElement
						.getElementsByTagName("Property").item(0);

				Element visibleElement = (Element) propertyElement
						.getElementsByTagName("Visible").item(0);

				Question question = questionList.get(Long
						.parseLong(questionElement.getAttribute("id")));
				QuestionType category = QuestionType.values()[Integer.parseInt(categoryElement.getAttribute("category"))];

				for (int j = 0; j < question.getSelections().length; j++) {
					Selection selection = question.getSelections()[j];

					if (selection.getString().equals(
							selectionElement.getAttribute("text"))) {
						GraphQuestionSelectionPair graphQuestion = new GraphQuestionSelectionPair(
								question, selection, category);

						if (propertyElement.getAttribute("type").equals("Edge")) {

							System.out.println(propertyElement
									.getAttribute("color"));
							if (!propertyElement.getAttribute("color").equals(
									"")) {
								EdgeProperty epColor = new EdgeProperty();
								epColor.setColor(Color.decode(propertyElement
										.getAttribute("color")));
								epColor.setProperty(EdgePropertyType.Color);

								epColor.setVisible(visibleElement.getAttribute(
										"visible").equals("true"));
								renderer.addQAsettings(graphQuestion, epColor);
							}

							if (!propertyElement.getAttribute("size")
									.equals("")) {
								EdgeProperty epSize = new EdgeProperty();
								epSize.setSize(Integer.parseInt(propertyElement
										.getAttribute("size")));
								epSize.setProperty(EdgePropertyType.Size);

								epSize.setVisible(visibleElement.getAttribute(
										"visible").equals("true"));
								renderer.addQAsettings(graphQuestion, epSize);
							}

							if (!propertyElement.getAttribute("shape").equals(
									"")) {
								EdgeProperty epShape = new EdgeProperty();
								epShape.setShapeFromString(propertyElement
										.getAttribute("shape"));
								epShape.setProperty(EdgePropertyType.Shape);

								epShape.setVisible(visibleElement.getAttribute(
										"visible").equals("true"));
								renderer.addQAsettings(graphQuestion, epShape);
							}

						} else {
							// do same for node property

                            if (!propertyElement.getAttribute("color").equals(""))
                            {
                                NodeProperty npColor = new NodeProperty();

                                npColor.setColor(Color.decode(propertyElement.getAttribute("color")));
                                npColor.setProperty(NodePropertyType.Color);
                                renderer.addQAsettings(graphQuestion, npColor);
                            }

                            if (!propertyElement.getAttribute("size").equals(""))
                            {
                                NodeProperty npSize = new NodeProperty();
                                npSize.setSize(Integer.parseInt(propertyElement.getAttribute("size")));
                                npSize.setProperty(NodePropertyType.Size);

                                renderer.addQAsettings(graphQuestion, npSize);
                            }

                            if (!propertyElement.getAttribute("shape").equals(""))
                            {
                                NodeProperty npShape = new NodeProperty();
                                npShape.setShapeFromString(propertyElement.getAttribute("shape"));
                                npShape.setProperty(NodePropertyType.Shape);

                                renderer.addQAsettings(graphQuestion, npShape);
                            }
						}

					}
				}
			}
		}
		renderer.updateGraphSettings();

	}

	public int getNodeSize(ArchetypeVertex node) {
		NodeProperty nodeProperty = nodeSettingsMap.get(node);
		return nodeProperty.getSize();
	}

	public void setNodeSize(ArchetypeVertex node, int nodeSize) {
		nodeSettingsMap.get(node).setSize(nodeSize);
	}

	public NodeShape getNodeShape(ArchetypeVertex node) {
		NodeProperty nodeProperty = nodeSettingsMap.get(node);
		return nodeProperty.getShape();
	}

	public void setNodeShape(ArchetypeVertex node, NodeShape nodeShape) {
		nodeSettingsMap.get(node).setShape(nodeShape);
	}

	public Color getNodeColor(ArchetypeVertex node) {
		NodeProperty nodeProperty = nodeSettingsMap.get(node);
		return nodeProperty.getColor();
	}

	public void setNodeColor(ArchetypeVertex node, Color nodeColor) {
		nodeSettingsMap.get(node).setColor(nodeColor);
	}

	public String getNodeLabel(ArchetypeVertex node) {
		NodeProperty nodeProperty = nodeSettingsMap.get(node);
		return nodeProperty.getLabel();
	}

	public void setNodeLabel(ArchetypeVertex node, String nodeLabel) {
		nodeSettingsMap.get(node).setLabel(nodeLabel);
	}

	public String getNodeToolTipText(ArchetypeVertex node) {
		NodeProperty nodeProperty = nodeSettingsMap.get(node);
		return nodeProperty.getToolTipText();
	}

	public int getEdgeSize(Edge edge) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		return edgeProperty.getSize();
	}

	public void setEdgeSize(Edge edge, int edgeSize) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		if (edgeProperty != null) {
			edgeProperty.setSize(edgeSize);
		} else {
			edgeProperty = new EdgeProperty(Color.BLACK, EdgeShape.Line,
					edgeSize);
		}
		edgeSettingsMap.put(edge, edgeProperty);
	}

	public EdgeShape getEdgeShape(Edge edge) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		return edgeProperty.getShape();
	}

	public void setEdgeShape(Edge edge, EdgeShape edgeShape) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		if (edgeProperty != null) {
			edgeProperty.setShape(edgeShape);
		} else {
			edgeProperty = new EdgeProperty(Color.BLACK, edgeShape, 1);
		}
		edgeSettingsMap.put(edge, edgeProperty);
	}

	public Color getEdgeColor(Edge edge) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		return edgeProperty.getColor();
	}

	public void setEdgeColor(Edge edge, Color edgeColor) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		if (edgeProperty != null) {
			edgeProperty.setColor(edgeColor);
		} else {
			edgeProperty = new EdgeProperty(edgeColor, EdgeShape.Line, 1);
		}
		edgeSettingsMap.put(edge, edgeProperty);
	}

	public String getEdgeLabel(Edge edge) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		return edgeProperty.getLabel();
	}

	public void setEdgeLabel(Edge edge, String edgeLabel) {
		edgeSettingsMap.get(edge).setLabel(edgeLabel);
	}

	public void addQAsetting(GraphQuestionSelectionPair graphQuestion,
			NodeProperty nodeProperty) {
		GraphSettingsEntry entry = new GraphSettingsEntry(graphQuestion,
				nodeProperty, GraphSettingType.Node);
		QAsettings.add(entry);
		displaySettings();
	}

	public void addQAsetting(GraphQuestionSelectionPair graphQuestion,
			EdgeProperty edgeProperty) {
		GraphSettingsEntry entry = new GraphSettingsEntry(graphQuestion,
				edgeProperty, GraphSettingType.Edge);
		QAsettings.add(entry);
		displaySettings();
	}

	private void displaySettings() {
		boolean debug = false;
		if(!debug)
			return;
		
		int size = QAsettings.size();
		System.out.println("Graph settings (" + size + " entries):");
		for (int i = 0; i < size; i++) {
			GraphSettingsEntry entry = QAsettings.get(i);
			System.out.println("Entry " + i + ": " + entry.toString());
		}
	}

	public Iterator<GraphSettingsEntry> getQAsettingsIterator() {
		return QAsettings.iterator();
	}

	private String getAlterInfo(int alterIndex) {
		String[] alterToolTip = new String[egoClient.getInterview().getNumAlters()];
		for (int i = 0; i < alterToolTip.length; i++) {
			alterToolTip[i] = "<html>" + egoClient.getInterview().getAlterList()[i]
					+ "<br>";
		}
		Answer[] answers = egoClient.getInterview().get_answers();
		for (Answer answer : answers) {
			String questionTitle = "";
			String answerString = "";
			Question question = egoClient.getStudy().getQuestion(answer.questionId);
			if (question.questionType == Shared.QuestionType.ALTER) {
				questionTitle = question.title;
				answerString = answer.string + " (index="+answer.getIndex()+",value="+answer.getValue()+")";
				int[] alters = answer.getAlters();
				for (int alter : alters) {
					alterToolTip[alter] += questionTitle + " : " + answerString
							+ "<br>";
				}
			}

		}
		return alterToolTip[alterIndex];
	}

	public Iterator<Edge> getEdgeIterator() {
		return edgeSettingsMap.keySet().iterator();
	}

	public static void writeSettings() {
		// Iterator iterator = getQAsettingsIterator();
	}

	public void emptyEdgeSettingsMap() {
		edgeSettingsMap.clear();
	}

	public boolean isEdgeVisible(Edge edge) {
		EdgeProperty edgeProperty = edgeSettingsMap.get(edge);
		return edgeProperty.isVisible();
	}

	public void setEdgeVisible(Edge edge, boolean b) {
		edgeSettingsMap.get(edge).setVisible(b);
	}

	public Map<Edge, EdgeProperty> getEdgeSettingsMap() {
		return edgeSettingsMap;
	}

	public void setEdgeSettingsMap(Map<Edge, EdgeProperty> edgeSettingsMap) {
		this.edgeSettingsMap = edgeSettingsMap;
	}

	public Map<ArchetypeVertex, NodeProperty> getNodeSettingsMap() {
		return nodeSettingsMap;
	}

	public void setNodeSettingsMap(
			Map<ArchetypeVertex, NodeProperty> nodeSettingsMap) {
		this.nodeSettingsMap = nodeSettingsMap;
	}

	public java.util.List<GraphSettingsEntry> getQAsettings() {
		return this.QAsettings;
	}

	public void setQAsettings(java.util.List<GraphSettingsEntry> asettings) {
		this.QAsettings = asettings;
	}
}
