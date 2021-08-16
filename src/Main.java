import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import javax.swing.JFrame;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class Main {

	public static void main(String[] args) throws FileNotFoundException, GRBException {
		// 1- reading date
		Scanner scanner = new Scanner(new File("/Users/m-store/Desktop/out_new.csv"));
		ArrayList<String> lines = new ArrayList<String>(); // X,Y [0]
		ArrayList<String> elevation = new ArrayList<String>(); // Z
		// skip the first line
		scanner.nextLine();
		// System.out.println(firstLiane);

		while (scanner.hasNext()) {
			// lines = scanner.nextLine();
			String[] splitted = scanner.nextLine().split(";");
			lines.add(splitted[0]);
			elevation.add(splitted[3]);

		}
		scanner.close();

		SimpleGraphicsView view = new SimpleGraphicsView();
		JFrame frame = new JFrame("Coun_Lines");
		frame.setSize(500, 500);
		frame.add(view);
		ArrayList<ArrayList<Point2D>> allLines = new ArrayList<>();

		for (int i = 0; i < 6; i++) {
			StringTokenizer fst = new StringTokenizer(lines.get(i), "LINESTRING (),\"");
			ArrayList<Point2D> lineCordes = new ArrayList<>();
			while (fst.hasMoreTokens()) {
				double xCor = Double.parseDouble(fst.nextToken());
				double yCor = Double.parseDouble(fst.nextToken());
				Point2D point = new Point2D.Double(xCor, yCor);
				lineCordes.add(WebMercartorProjection.WEBMERCATOR.fromLLtoPixel(point, 12));
			}
			// System.out.println(lineCordes.toString());
			allLines.add(lineCordes);

			// 2 Visualizing date

			int m = lineCordes.size() - 1;
			// System.out.println("M" + m);
			for (int n = 0; n < lineCordes.size() - 1; n++) {
				view.addShape(new Line2D.Double(lineCordes.get(m), lineCordes.get(n)));
				m = n;
			}

			// lineCordes.clear();
		}
		// System.out.println(allLines.toString());
		view.moveToCenter();
		frame.setVisible(true);

		/*
		 * short graph computation
		 */

		// time
		long start = System.currentTimeMillis();

		List<DiGraph<Point2D, Double>> graphs = new ArrayList<>();

		for (ArrayList<Point2D> contour : allLines) {

			DiGraph<Point2D, Double> diGraph = new DiGraph<>();

			double epsilon = 1.0;

			SimpleGraphicsView viewShortG = new SimpleGraphicsView();
			JFrame frameShortG = new JFrame("DiGraph");
			frameShortG.setSize(500, 500);
			frameShortG.add(viewShortG);
			/*
			 * adding Nodes
			 */

			for (int i = 0; i < contour.size() - 1; i++) {
				diGraph.addNode(contour.get(i));

			}
			/*
			 * Adding arcs
			 */

			for (int i = 0; i < diGraph.n() - 1; i++) {
				diGraph.addArc(diGraph.getNode(i), diGraph.getNode(i + 1), 1.0);
				// viewShortG.addShape(new Line2D.Double(diGraph.getNodeData(i),
				// diGraph.getNodeData(i + 1)), Color.RED);
			}

			for (int i = 0; i < diGraph.n(); i++) {
				for (int j = i + 1; j < diGraph.n(); j++) {
					boolean valid = true;
					for (int k = i + 1; k < j; k++) {
						// System.out.println(i + " - " +k+ " - "+ j);
						Line2D line = new Line2D.Double(diGraph.getNodeData(i), diGraph.getNodeData(j));
						// System.out.println(i + " - " + j + " distance from " + k + " is " +
						// line.ptLineDist(diGraph.getNodeData(k)));
						if (line.ptLineDist(diGraph.getNodeData(k)) > epsilon) {
							valid = false;
							break;
						}
					}

					// for removing invalid shortcuts

					if (valid) {
						Line2D line = new Line2D.Double(diGraph.getNodeData(i), diGraph.getNodeData(j));
						diGraph.addArc(diGraph.getNode(i), diGraph.getNode(j), 1.0);
						// viewShortG.addShape(line, Color.BLUE);
					}
				}
			}
			graphs.add(diGraph);
			// viewShortG.moveToCenter();
			// frameShortG.setVisible(true);

		}

		/*
		 * ILP FORMULATION
		 */

		// Creating the environment
		GRBEnv env = new GRBEnv("model.log");
		GRBModel model = new GRBModel(env);

		// Create variables
		List<List<GRBVar>> variables = new ArrayList<>();
		
		// --- new List of HashMap Variable
		List<HashMap<String, GRBVar>> bendVariables = new ArrayList<>();

		for (int i = 0; i < graphs.size(); i++) {
			List<GRBVar> variable = new ArrayList<>();
			for (int j = 0; j < graphs.get(i).m(); j++) {
				variable.add(model.addVar(0, 1, 0, GRB.BINARY, "graph" + i + "edge" + j));
			}

			variables.add(variable);
			
			//---  Adding variables to the model
			
			HashMap<String, GRBVar> y_var = new HashMap<>();
			// for each node of graph i
			for (int j = 0; j < graphs.get(i).n(); j++) {
		
				int out = graphs.get(i).getNode(j).getOutgoingArcs().size();
				int in = graphs.get(i).getNode(j).getIncomingArcs().size();
				
				for (int k = 0; k < out; k++) { 
					for (int l = 0; l < in; l++) {
						y_var.put("graph" + i + "node" + j + "bend" + k + "," + l,
								model.addVar(0, 1, 0, GRB.BINARY, "graph" + i + "node" + j + "bend" + k + "," + l));
					}
				}
			}

			bendVariables.add(y_var);

		}

		for (int g = 0; g < graphs.size(); g++) {
			// outgoing edges for the first vertex
			int outGoing = graphs.get(g).getNode(0).getOutgoingArcs().size();
			GRBLinExpr exp_startVer = new GRBLinExpr();
			for (int i = 0; i < outGoing; i++) {
				exp_startVer.addTerm(1,
						variables.get(g).get(graphs.get(g).getNode(0).getOutgoingArcs().get(i).getId()));
			}
			// constrain for the first node
			model.addConstr(exp_startVer, GRB.EQUAL, 1, "startVertex");

			// incoming edges for the last vertex

			int incomingEdges = graphs.get(g).getNode(graphs.get(g).n() - 1).getIncomingArcs().size();
			GRBLinExpr expLastVer = new GRBLinExpr();
			for (int i = 0; i < incomingEdges; i++) {
				expLastVer.addTerm(1.0, variables.get(g)
						.get(graphs.get(g).getNode(graphs.get(g).n() - 1).getIncomingArcs().get(i).getId()));
			}
			model.addConstr(expLastVer, GRB.EQUAL, 1, "lastVertex");
			// intermediate vertex

			for (int i = 1; i < graphs.get(g).n() - 1; i++) {

				int kOut = graphs.get(g).getNode(i).getOutgoingArcs().size();
				GRBLinExpr exp_out = new GRBLinExpr();
				for (int j = 0; j < kOut; j++) {
					exp_out.addTerm(1, variables.get(g).get(graphs.get(g).getNode(i).getOutgoingArcs().get(j).getId()));
				}

				int kIn = graphs.get(g).getNode(i).getIncomingArcs().size();
				GRBLinExpr exp_in = new GRBLinExpr();
				for (int k = 0; k < kIn; k++) {
					exp_in.addTerm(1, variables.get(g).get(graphs.get(g).getNode(i).getIncomingArcs().get(k).getId()));
				}
				model.addConstr(exp_in, GRB.EQUAL, exp_out, "intermediateVertex");
			}

			// --- Setting the Constr
			
			for (int j = 0; j < graphs.get(g).n(); j++) {
				//Ye,é
				HashMap<String, GRBVar> y_var = bendVariables.get(g);
				int out = graphs.get(g).getNode(j).getOutgoingArcs().size();
				int in = graphs.get(g).getNode(j).getIncomingArcs().size();
				for (int k = 0; k < out; k++) { 
					for (int l = 0; l < in; l++) {
						// Ye,é <= 1/2 xé + 1/2 xe
						GRBLinExpr rightSide = new GRBLinExpr();
						
						// 1/2 xé outgoing
						// Add a single term into a linear expression.
						
						rightSide.addTerm(0.5,
								variables.get(g).get(graphs.get(g).getNode(j).getOutgoingArcs().get(k).getId()));
						// 1/2 xe incoming
						rightSide.addTerm(0.5,
								variables.get(g).get(graphs.get(g).getNode(j).getIncomingArcs().get(l).getId()));
						
						model.addConstr(y_var.get("graph" + g + "node" + j + "bend" + k + "," + l), GRB.LESS_EQUAL,
								rightSide, "constraint1-graph" + g + "node" + j + "bend" + k + "," + l);

						// second constraint
						
						//Xe +xé <=Ye,é +1
						GRBLinExpr rhs2 = new GRBLinExpr();
						GRBLinExpr lhs2 = new GRBLinExpr();
						
						//Xé out
						lhs2.addTerm(1.0,variables.get(g).get(graphs.get(g).getNode(j).getOutgoingArcs().get(k).getId()));
						// Xe in
						lhs2.addTerm(1.0,variables.get(g).get(graphs.get(g).getNode(j).getIncomingArcs().get(l).getId()));
						   // +1
						rhs2.addConstant(1.0);
						// 
						rhs2.addTerm(1.0, y_var.get("graph" + g + "node" + j + "bend" + k + "," + l));
						model.addConstr(lhs2, GRB.LESS_EQUAL, rhs2,
								"constraint2-graph" + g + "node" + j + "bend" + k + "," + l);
					}
				}

			}

			// Area Computation:

			GRBLinExpr areaPresevation_expr = new GRBLinExpr();
			for (int i = 0; i < graphs.get(g).m(); i++) {
				int source = graphs.get(g).getArc(i).getSource().getId();
				int target = graphs.get(g).getArc(i).getTarget().getId();
				if (target - source > 1) {
					// System.out.println("Graph " + g + " Edge " + source + " -> " + target);
					List<Point2D> nodes = new ArrayList<>();
					for (int j = source; j <= target; j++) {
						nodes.add(graphs.get(g).getNode(j).getNodeData());
					}

					double areOfShortCut = area(nodes);
					// model.addTerm(coff, var)
					
					areaPresevation_expr.addTerm(areOfShortCut, variables.get(g).get(graphs.get(g).getArc(i).getId()));
					// areOfShortCut graph
					// System.out.println("Edge " + i + " area " + areOfShortCut);
				}

			}

			double percentage = 0.2;
			// for + 
			// areaPresevation <= area+ of g
			model.addConstr(areaPresevation_expr, GRB.LESS_EQUAL, percentage * Math.abs(area(allLines.get(g))),
					"area+" + g);
			
			// for areaPresevation >= area- of g of original graph
			model.addConstr(areaPresevation_expr, GRB.GREATER_EQUAL,
					-1.0 * percentage * Math.abs(area(allLines.get(g))), "area-" + g);

			System.out.println("Original Graph " + g + " " + Math.abs(area(allLines.get(g))));

		}

		// intersection constraint

		for (int i = 0; i < graphs.size(); i++) {
			for (int j = 0; j < graphs.size(); j++) {
				if (i != j) {
					for (int k = 0; k < graphs.get(i).m(); k++) {
						for (int l = 0; l < graphs.get(j).m(); l++) {
							if (k != l) {

								Line2D line1 = new Line2D.Double(graphs.get(i).getArc(k).getSource().getNodeData(),
										graphs.get(i).getArc(k).getTarget().getNodeData());
								Line2D line2 = new Line2D.Double(graphs.get(j).getArc(l).getSource().getNodeData(),
										graphs.get(j).getArc(l).getTarget().getNodeData());
								if (line1.intersectsLine(line2)) {
									GRBLinExpr intersection_expr = new GRBLinExpr();
									intersection_expr.addTerm(1, variables.get(i).get(k));
									intersection_expr.addTerm(1, variables.get(j).get(l));
									model.addConstr(intersection_expr, GRB.LESS_EQUAL, 1, "intersection");
								}
							}

						}
					}
				}
			}
		}

		model.update();

		double a = 1.0;
		double b = 1.0 - a;

		// Set objective:
		GRBLinExpr expr_obj = new GRBLinExpr();

		for (int g = 0; g < variables.size(); g++) {
			for (int i = 0; i < variables.get(g).size(); i++) {
				expr_obj.addTerm(a, variables.get(g).get(i));
			}
		}

		GRBLinExpr bendsObj = new GRBLinExpr();
		for (int i = 0; i < graphs.size(); i++) {
			for (int j = 0; j < graphs.get(i).n(); j++) {
				HashMap<String, GRBVar> y_var = bendVariables.get(i);
				int out = graphs.get(i).getNode(j).getOutgoingArcs().size();
				int in = graphs.get(i).getNode(j).getIncomingArcs().size();
				for (int k = 0; k < out; k++) {
					for (int l = 0; l < in; l++) {
						Point2D startPoint = graphs.get(i).getNode(j).getIncomingArcs().get(l).getSource()
								.getNodeData();
						Point2D endPoint = graphs.get(i).getNode(j).getOutgoingArcs().get(k).getTarget().getNodeData();
						Point2D centerPoint = graphs.get(i).getNode(j).getNodeData();
						double angle = Main.getAngle(centerPoint, startPoint, endPoint);
						System.out.println(angle);
						bendsObj.addTerm(b * angle, y_var.get("graph" + i + "node" + j + "bend" + k + "," + l));
					}
				}

			}
		}

		GRBLinExpr obj = new GRBLinExpr();
		obj.add(expr_obj);
		obj.add(bendsObj);

		model.setObjective(obj, GRB.MINIMIZE);

		model.optimize();
		model.write("solution.lp");
		long end = System.currentTimeMillis();
		float sec = (end - start) / 1000F;
		System.out.println(sec + " seconds");

		// visulize it
		SimpleGraphicsView lipResult = new SimpleGraphicsView();
		JFrame ILPF = new JFrame("Result");
		ILPF.setSize(500, 500);
		ILPF.add(lipResult);

		for (int i = 0; i < variables.size(); i++) {
			List<Point2D> nodes = new ArrayList<>();
			// System.out.println("my variables" + graphs.size());
			for (int j = 0; j < variables.get(i).size(); j++) {
				double x = model.getVarByName("graph" + i + "edge" + j).get(GRB.DoubleAttr.X);
				if (x != 0) {
					nodes.add(graphs.get(i).getArc(j).getSource().getNodeData());
					Line2D line = new Line2D.Double(graphs.get(i).getArc(j).getSource().getNodeData(),
							graphs.get(i).getArc(j).getTarget().getNodeData());
					lipResult.addShape(line, Color.RED);
				}
			}
			System.out.println("Area " + Math.abs(area(nodes)));
		}

		lipResult.moveToCenter();
		ILPF.setVisible(true);

		model.dispose();
		model.getEnv().dispose();

	}

	// Gauss's shoelace formula computing area of shortCut graph

	public static double area(List<Point2D> nodes) {
		double areOfShortCut = 0.0;
		int m = nodes.size() - 1;
		for (int k = 0; k < nodes.size(); k++) {
			areOfShortCut += (nodes.get(m).getX() + nodes.get(k).getX()) * (nodes.get(m).getY() - nodes.get(k).getY());
			m = k;
		}

		return areOfShortCut;
	}
	
        // angle computation fucntion of  center, start and end point
	public static double getAngle(Point2D p1, Point2D p2, Point2D p3) {

		double result = Math.atan2(p3.getY() - p1.getY(), p3.getX() - p1.getX())
				- Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX());
		double degrees = (Math.toDegrees(result) + 360) % 360;

		return degrees;
	}

}