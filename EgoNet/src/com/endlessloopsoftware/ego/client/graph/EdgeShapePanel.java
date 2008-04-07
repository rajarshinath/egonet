package com.endlessloopsoftware.ego.client.graph;

import org.jdesktop.layout.GroupLayout;
import javax.swing.*;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.*;
import java.awt.event.*;

import org.egonet.util.listbuilder.Selection;
import org.egonet.util.table.*;

import com.endlessloopsoftware.ego.*;
import com.endlessloopsoftware.ego.client.EgoClient;
import com.endlessloopsoftware.ego.client.graph.NodeProperty.NodeShape;

public class EdgeShapePanel extends JPanel {

	private JLabel questionLabel;

	private JComboBox questionCombo;

	private JComboBox shapeCombo;

	private ChoosablePropertyTableModel tableModel;

	private JTable table;

	private GroupLayout layout;

	private GraphRenderer graphRenderer;

	private GraphData graphData;

	private JButton applyButton;

	List<Selection> selectionList = new ArrayList<Selection>();

	public EdgeShapePanel(GraphRenderer renderer) {
		this.graphRenderer = renderer;
		layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setAutocreateGaps(true);
		layout.setAutocreateContainerGaps(true);
		graphData = new GraphData();
		createComponents();
	}

	private void createComponents() {

		questionLabel = new JLabel("Choose question to shape");
		questionLabel.setVisible(true);

		// create questionCombo
		List<Question> qList = new ArrayList<Question>();
		Study study = EgoClient.interview.getStudy();
		QuestionList questionList = study.getQuestions();
		Map<Long, Question> questionMap = questionList.getQuestionMap();
		for (Long key : questionMap.keySet()) {
			Question currentQuestion = questionMap.get(key);
			int questionType = currentQuestion.questionType;
			if (questionType == Question.ALTER_PAIR_QUESTION) {
				// populate the list box with only questions that have choices
				// as answers
				if (currentQuestion.selections.length >= 1)
					qList.add(currentQuestion);
			}
		}
		questionCombo = new JComboBox(qList.toArray());
		questionCombo.setVisible(true);
		questionCombo.setEnabled(true);
		questionCombo.setPreferredSize(new Dimension(20, 20));
		questionCombo.setMaximumSize(new Dimension(20, 30));
		questionCombo.setAutoscrolls(true);

		questionCombo.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					createTable();
					drawPanel();
				}
			}
		});

		// create shape combo for table cell
		shapeCombo = new JComboBox(EdgeProperty.EdgeShape.values());
		shapeCombo.setPreferredSize(new Dimension(20, 20));
		shapeCombo.setMaximumSize(new Dimension(20, 30));
		// shapeCombo.setSelectedIndex(0);

		// create apply button
		applyButton = new JButton("Apply Shape");
		applyButton.setVisible(true);
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateEdgeShape();
			}
		});

		createTable();
		drawPanel();

	}

	private void createTable() {
		Question question = (Question) questionCombo.getSelectedItem();
		int category = Question.ALTER_PAIR_QUESTION;
		int noOfRows = question.selections.length;
		Object[][] rowData = new Object[noOfRows][3];
		/* change the list of selections based on the selected question */
		if (!selectionList.isEmpty()) {
			selectionList.removeAll(selectionList);
		}
		for (Selection selection : question.selections) {
			selectionList.add(selection);
		}
		// populate the responses
		for (int i = 0; i < noOfRows; i++) {
			rowData[i][0] = Boolean.FALSE;
			rowData[i][1] = selectionList.get(i);
			String str = ((Selection) rowData[i][1]).getString();
		}
		// populate the shapes
		int maxNoOfShapes = EdgeProperty.EdgeShape.values().length;
		for (int i = 0; i < noOfRows; i++) {
			if (i < maxNoOfShapes) {
				EdgeProperty.EdgeShape shape = EdgeProperty.EdgeShape.values()[i];
				rowData[i][2] = shape;
			} else {
				EdgeProperty.EdgeShape shape = EdgeProperty.EdgeShape.values()[i % maxNoOfShapes];
				rowData[i][2] = shape;
			}
		}

		table = new JTable(new ChoosablePropertyTableModel(rowData));
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		table.setRowHeight(25);
		table.setVisible(true);
		table.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createLineBorder(Color.black), getBorder()));

		TableColumnModel columnModel = table.getColumnModel();
		LabelRenderer selectionRenderer = new LabelRenderer();
		columnModel.getColumn(1).setCellRenderer(selectionRenderer);

		DefaultCellEditor shapeEditor = new DefaultCellEditor(shapeCombo);
		columnModel.getColumn(2).setCellEditor(shapeEditor);
		columnModel.getColumn(2).setCellRenderer(
				new TableComboBoxRenderer(EdgeProperty.EdgeShape.values()));

		columnModel.getColumn(0).setMaxWidth(50);
		columnModel.getColumn(1).setMaxWidth(200);
		columnModel.getColumn(2).setMaxWidth(150);
	}

	private void updateEdgeShape() {

		int noOfAlters = EgoClient.interview.getNumAlters();
		Question question = (Question) questionCombo.getSelectedItem();
		for (int i = 0; i < question.selections.length; i++) {
			if(((Boolean)table.getValueAt(i,0)) == true) {
				Selection selection = question.selections[i];
				List<Integer> alterList = graphData.getAlterNumbers(question,
						selection);
				
				GraphQuestion graphQuestion = new GraphQuestion(question, selection, Question.ALTER_PAIR_QUESTION);
				EdgeProperty edgeProperty = new EdgeProperty();
				edgeProperty.setShape((EdgeProperty.EdgeShape)table.getValueAt(i, 2));
				edgeProperty.setProperty(EdgeProperty.Property.Shape);
				graphRenderer.addQAsettings(graphQuestion, edgeProperty);
				graphRenderer.updateGraphSettings();
			}
		}
	}

	private void drawPanel() {
		this.removeAll();

		GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
		hGroup.add(layout.createParallelGroup()
				.add(questionLabel).add(questionCombo)
				.add(table.getTableHeader()).add(table)
				.add(applyButton));

		layout.setHorizontalGroup(hGroup);

		GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
		vGroup.add(layout.createParallelGroup(
				GroupLayout.BASELINE).add(questionLabel));
		vGroup.add(layout.createParallelGroup(
				GroupLayout.BASELINE).add(questionCombo));
		vGroup.add(layout.createParallelGroup(
				GroupLayout.BASELINE).add(
				table.getTableHeader()));
		vGroup.add(layout.createParallelGroup(
				GroupLayout.BASELINE).add(table));
		vGroup.add(layout.createParallelGroup(
				GroupLayout.BASELINE).add(applyButton));

		layout.setVerticalGroup(vGroup);
	}
}
