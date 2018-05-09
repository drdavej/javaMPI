import java.awt.*;
import javax.swing.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

class QV_Shape
{
    public void paint(Graphics2D g2, double wid, double hgt)
    {
        // Empty
    }
}

class QV_Box extends QV_Shape
{
    private double _x0, _y0, _x1, _y1;
    private Color _color;

    QV_Box(Color color, double x0, double y0, double x1, double y1)
    {
        _x0 = x0;
        _y0 = y0;
        _x1 = x1;
        _y1 = y1;
    }

    @Override
    public void paint(Graphics2D g2, double wid, double hgt)
    {
        g2.setColor(_color);
        int lx = (int) Math.round(_x0 * wid);
        int ly = (int) Math.round(_y0 * hgt);
        int w = (int) Math.round((_x1 - _x0) * wid);
        int h = (int) Math.round((_y1 - _y0) * hgt);
        g2.fillRect(lx, ly, w, h);
    }
}

class QV_Line extends QV_Shape
{
    private double _x0, _y0, _x1, _y1;
    private Color _color;

    QV_Line(Color color, double x0, double y0, double x1, double y1)
    {
        _color = color;
        _x0 = x0;
        _y0 = y0;
        _x1 = x1;
        _y1 = y1;
    }

    @Override
    public void paint(Graphics2D g2, double wid, double hgt)
    {
        g2.setColor(_color);
        Line2D line = new Line2D.Double(_x0 * wid, _y0 * hgt, _x1 * wid, _y1 * hgt);
        g2.draw(line);
    }
}

class QV_Text extends QV_Shape
{
    private double _x, _y, _size, _myHalfWid;
    private String _text;
    private Color _color;
    private final double CHARWID = 0.8;
    private final double FONTSIZE = 1.0;

    QV_Text(Color color, double x, double y, double size, String text)
    {
        _color = color;
        _x = x;
        _y = y;
        _size = size;
        _text = text;
        _myHalfWid = text.length() * size * CHARWID / 2.0;
    }

    @Override
    public void paint(Graphics2D g2, double wid, double hgt)
    {
        double scale = ((wid < hgt) ? wid : hgt) * _size;
        int ilx = (int) Math.round(wid * (_x - _myHalfWid));
        int icy = (int) Math.round(hgt * _y);
        int fnt = (int) Math.round(FONTSIZE * scale);

        g2.setColor(_color);
        Font myFont = new Font("Courier New", 1, fnt);
        g2.setFont(myFont);
        g2.drawString(_text, ilx, icy + fnt / 2);
    }
}

public class QV_Scene extends JComponent
{
    private Set<QV_Shape> _shapes;

    public QV_Scene()
    {
        _shapes = new HashSet<QV_Shape>();
    }

    public void paint(Graphics2D g2, double wid, double hgt)
    {
        for (QV_Shape shape : _shapes)
        {
            shape.paint(g2, wid, hgt);
        }
    }

    public void box(Color color, double x0, double y0, double x1, double y1)
    {
        _shapes.add(new QV_Box(color, x0, y0, x1, y1));
    }

    public void line(Color color, double x0, double y0, double x1, double y1)
    {
        _shapes.add(new QV_Line(color, x0, y0, x1, y1));
    }

    public void text(Color color, double x, double y, double size, String msg)
    {
        _shapes.add(new QV_Text(color, x, y, size, msg));
    }
}
