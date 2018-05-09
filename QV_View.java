// The QV_View class implements a pop-up window into which the process can draw text
// or simple graphics.

import javax.swing.*;
import java.awt.*;

// There are three classes in this file:
//  QV_View:   The top-level class for the viewer.  An QV_Proc can
//              instantiate one of these, it will pop up a window.
//              The proc can describe a scene, then have that draw
//              in the window.
//  QV_Viewer: This is an extention of JPanel, it holds the
//              window itself.  This window has one fixed instance
//              of an QV_ViewScene, which is what it draws to the
//              screen.
//  QV_ViewScene:  This is a simple 'placeholder' inside the viewer.
//              It holds the actual scene to be drawn.  When the
//              scene changes, the new scene is passed to the
//              QV_ViewScreen.  The Viewer doesn't have to change,
//              it just has to redraw.

//----------------------------------------------------------------------
// The placeholder for the scene to be drawn:
class QV_ViewScene extends JComponent
{
    private QV_Scene _curScene;

    public QV_ViewScene()
    {
        _curScene = null;
    }

    //------------------------------------------------------------------
    // Draw the scene to the screen
    public void paintComponent(Graphics g)
    {
        if (_curScene == null)
        {
            return;
        }
        double wid = getWidth();
        double hgt = getHeight();
        Graphics2D g2 = (Graphics2D) g;
        _curScene.paint(g2, wid, hgt);
    }

    //------------------------------------------------------------------
    // Change the scene, and cause the window to be redrawn
    public void setScene(QV_Scene scene)
    {
        _curScene = scene;
        repaint();
    }
}

//----------------------------------------------------------------------
// The QV_Viewer, a simple extention to JPanel.  It holds the
// ViewScene (which the QV_View also holds -- so that the QV_View
// can change the scene)
class QV_Viewer extends JPanel
{
    QV_ViewScene _viewScene;

    public QV_Viewer(JFrame frame, QV_ViewScene viewScene)
    {
        super(new BorderLayout());
        _viewScene = viewScene;
        add(_viewScene, BorderLayout.CENTER);
    }
}

//----------------------------------------------------------------------
// The top-level class.  An MPI_Proc can create (and delete) an instance
// of this.
public class QV_View
{
    private int _rank;
    private JFrame _frame;
    private QV_Viewer _viewer;
    private QV_ViewScene _viewScene;
    private QV_Scene _scene;

    //------------------------------------------------------------------

    public QV_View(int rank, int x, int y, int wid, int hgt)
    {
        _rank = rank;
        _frame = new JFrame();
        _frame.setLocation(x, y);
        _frame.setSize(wid, hgt);
        _frame.setTitle("Proc " + rank);
        _frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        _viewScene = new QV_ViewScene();
        _viewer = new QV_Viewer(_frame, _viewScene);
        _frame.add(_viewer);
        _frame.setVisible(true);
    }

    //------------------------------------------------------------------
    // This allocates a new scene structure, which is ready to be
    // filled with various shapes.  This won't appear on the screen
    // until endView is called.
    public void beginView()
    {
        _scene = new QV_Scene();
    }

    //------------------------------------------------------------------
    // Calling this signifies that the new scene is complete.  It is
    // passed to the viewer, so the image will change on the screen.
    // Before the graphics of the next scene can begin, beginView
    // should be called.
    public void endView()
    {
        if (_scene != null)
        {
            _viewScene.setScene(_scene);
            // TBD -- trigger redraw
            _scene = null;
        }
    }

    //------------------------------------------------------------------
    // Add a box to the scene.  The dimensions of the viewspace is 0.0:1.0
    // in both X and Y (a unit square).  So a box in the upper left that
    // covers half of the window would use coordinates 0.0, 0.0, 0.5, 0.5.
    public void box(Color color, double lx, double ly, double hx, double hy)
    {
        if (_scene != null)
        {
            _scene.box(color, lx, ly, hx, hy);
        }
    }

    //------------------------------------------------------------------
    // Add a line to the scene, from x0, y0 to x1, y1.  Again, these
    // coordinates are within a unit square.
    public void line(Color color, double x0, double y0, double x1, double y1)
    {
        if (_scene != null)
        {
            _scene.line(color, x0, y0, x1, y1);
        }
    }

    //------------------------------------------------------------------
    // Add a string of text to the scene.
    public void text(Color color, double x, double y, double size, String msg)
    {
        if (_scene != null)
        {
            _scene.text(color, x, y, size, msg);
        }
    }

    //------------------------------------------------------------------
    // Use this to dispose of (close) the popup window
    public void dispose()
    {
        _frame.dispose();
    }
}
