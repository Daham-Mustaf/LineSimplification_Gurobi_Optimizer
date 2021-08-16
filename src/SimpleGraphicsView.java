import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;




public class SimpleGraphicsView extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public class GraphicsObject{
		private Shape shape;
		private Color color;
		private double width;
		private boolean fill;
		public GraphicsObject(Shape shape, Color color, double width,boolean fill) {
			super();
			this.shape = shape;
			this.color = color;
			this.width = width;
			this.fill  = fill;
		}
		
	}
	
	private List<GraphicsObject> objects = new ArrayList<GraphicsObject>();
	
	private Rectangle2D boundingBox = null;
	private Point2D center = new Point2D.Double(0,0);
	
	private Point2D currentMousePos = null;
	
	private double zoom = 1.0;
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		g.setColor(new Color(255,255,255));
		g.fillRect(0, 0, this.getWidth(),this.getHeight());
		Graphics2D g2 = (Graphics2D) g;
		
		//g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        //        RenderingHints.VALUE_ANTIALIAS_ON);
		
		AffineTransform trans= new AffineTransform();
		trans.translate(getWidth()/2, getHeight()/2);
		trans.scale(zoom, zoom);
		trans.translate(-center.getX(), -center.getY());
		
		double z = zoom > 1.0 ? zoom : 1.0;
		g2.setStroke(new BasicStroke((float)(2.0/z)));
		
		
		//trans.translate(getWidth()/2, getHeight()/2);
		
		g2.setTransform(trans);
		g.setColor(new Color(0, 0, 0));
		for(GraphicsObject go : objects){
			g2.setColor(go.color);
			g2.setStroke(new BasicStroke((float)(go.width/z)));
			if(go.fill){
				g2.fill(go.shape);
			}else{
				g2.draw(go.shape);
			}
		}	
	}	
	
	
	void setZoom(double zoom){
		this.zoom = zoom;	
	}
	
	public void moveToCenter(){
		this.zoom = 1.0;
		
		SwingUtilities.invokeLater(new Runnable() {
			
		@Override
		public void run() {
			if(boundingBox != null){
				center.setLocation(boundingBox.getCenterX(),boundingBox.getCenterY());
			}
			refresh();			
		};
		});
	}
	
	
	public void addShape(Shape shape){
		addShape(shape, new Color(0, 0, 0),1);
	}
	
	public void addShape(Shape shape, Color color){
		addShape(shape, color,1);
	}

	
	
	public void addLine(Point2D p1, Point2D p2){
		addLine(p1,p2,new Color(0,0,0),1);
	}
	
	public void addLine(Point2D p1, Point2D p2, Color color, double width){
		addShape(new Line2D.Double(p1, p2),color,width);
	}
	
	public void addPoint(Point2D p){
		addPoint(p,new Color(0,0,0),1);
	}
	
	public void addPoint(Point2D p,Color color, double width) {
		addShape(new Ellipse2D.Double(p.getX()-2.0, p.getY()-2.0, 4.0,4.0),color,width);
	}
	
	public void addPoint(Point2D p,Color color, double radius, double width) {
		addShape(new Ellipse2D.Double(p.getX()-radius/2, p.getY()-radius/2, radius, radius),color,width);
	}

	public void addShape(Shape shape, Color color, double width){
		addShape(shape,color,width,false);
	}
	
	public void addShape(Shape shape, Color color, double width,boolean fill){
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				objects.add(new GraphicsObject(shape, color, width,fill));
				if(boundingBox == null){
					boundingBox = shape.getBounds2D();
				}else{
					boundingBox.add(shape.getBounds2D());
				}	
				refresh();
			}
		});

	}
	
	public void refresh(){
		repaint();
	}
	
	public void moveTo(Point2D p){
		if(currentMousePos != null){
			center.setLocation(center.getX()-(p.getX()-currentMousePos.getX())/zoom,center.getY()-(p.getY()-currentMousePos.getY())/zoom);
		}
		refresh();
	}
	

	public void zoomIn(){
		zoom = zoom *2;
		refresh();
	}
	
	public void zoomOut(){
		zoom = zoom*0.5;
		refresh();
	}

	public SimpleGraphicsView() {
		//setPreferredSize(new Dimension(100, 100));
		//this.setViewportView(content);
		
		addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.getWheelRotation() < 0){
					zoomIn();
				}
				if(e.getWheelRotation() > 0){
					zoomOut();
				}
				
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				moveTo(e.getPoint());
				currentMousePos = e.getPoint();
				
			}
			
		
			
			
		});
		

		 
		 addMouseListener(new MouseAdapter() {
			 @Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				moveTo(e.getPoint());				
				currentMousePos = e.getPoint();
			}
			
			 
			 
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				currentMousePos = null;
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				currentMousePos = null;
			}
		});
	
	}


	
	
	
}
