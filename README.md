##LineSimplification_Gurobi_Optimizer<br/>

In topographic maps contour lines are used to depict the height reliefs of landscapes. However, extracting contour lines from raster data often results in highly detailed polylines. In a post-processing step the contour lines can be simplified to reduce storage consumption and visual clutter. In this exercise this post-processing step is implemented.<br/>
We can assume that the contour lines are given as pairwise non-intersecting polylines1 Each polyline P should then be simplified such that its self-intersection-free simplification lies in an error tolerance of P() and no two simplified polylines intersect.<br/>
The objective is to minimize the total number of vertices by using [Gurobi Optimizer](https://www.gurobi.com/).<br/>

Extract from a height map a set of contour lines, which will be used for testing the algorithms.<br/>
simple heuristic for simplifying contour lines is developed.<br/>

a) Use QGis to extract the contour lines (step size: 10m) from the given file **map.tiff**.<br/>
b) Store the polylines in a comma separated file (csv) using an appropriate projection.<br/>
With each polyline also store its height.<br/>
c) Write a Java-Class that can be used to read in the .csv file.<br/>

d) Visualize the polylines using the class Viewer (found in Viewer.java). Depending on the height, the color of the contour lines should be chosen.<br/>

Step 2. Develop an integer linear programming formulation for simplifying a single contour line respecting an error tolerance ε. The formulation should be based on a shortcut graph.<br/>
Step 3. Extend your formulation to multiple contour lines such that the result is a set of pairwise non-intersecting polylines.<br/>
Step 4. Implement the integer linear programming formulation.<br/>
Step 5. Test the integer linear programming formulation for different error tolerances and
different data sets.<br/>
Step 6. Develop a simple algorithm based on the shortcut graph that finds a solution in polynomial time.<br/>
Compare the results with those of the integer linear programming formulation.<br/>

