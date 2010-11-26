import processing.core.*;

import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import java.awt.Toolkit;
import java.awt.datatransfer.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;

/**
 *    Herr Inspektor meet Processing.
 */

public class Tweeker
{
    final static int TYPE_GENERIC = -1;
    final static int TYPE_PRIMITIVE_BOOLEAN = 0;
    final static int TYPE_PRIMITIVE_INT = 1;
    final static int TYPE_PRIMITIVE_SHORT = 2;
    final static int TYPE_PRIMITIVE_BYTE = 3;
    final static int TYPE_PRIMITIVE_CHAR = 4;
    final static int TYPE_PRIMITIVE_LONG = 5;
    final static int TYPE_PRIMITIVE_FLOAT = 6;
    final static int TYPE_PRIMITIVE_DOUBLE = 7;
    final static int TYPE_STRING = 8;
    
    boolean on = true;
    PApplet papplet;
    
    PFont fnt;
    
    Field[] fields;
    FieldInterface[] fieldInterfaces;
    Field currentField;
    
    int currentFieldType;
    int currentFieldIndex = 0;
    
    InterfaceElement upButton, downButton;
    
    Tweeker ( PApplet _a )
    {
        papplet = _a;
        
        upButton = new InterfaceElement( 0, papplet.height-40,
                                         papplet.width / 3, 20 );
        
        upButton.setHandler( new InterfaceElementAction () {
            public void mousePressed () {
                currentFieldIndex++;
                currentFieldIndex %= fields.length;
                updateCurrentField();
            }
        });
                                         
        downButton = new InterfaceElement( 0, papplet.height-20,
                                         papplet.width / 3, 20 );
        downButton.setHandler( new InterfaceElementAction () {
            public void mousePressed () {
                currentFieldIndex--;
                if ( currentFieldIndex < 0 ) currentFieldIndex = fields.length-1;
                updateCurrentField();
            }
        });
                                         
        papplet.registerDraw(this);
        papplet.registerMouseEvent(this);
        papplet.registerKeyEvent(this);
        
        fnt = papplet.createFont("Verdana", 9);
        
        getFields();
    }
    
    private void getFields ()
    {
        fields = papplet.getClass().getDeclaredFields();
        
        if ( fields.length > 0 )
        {
            updateCurrentField();
            fieldInterfaces = new FieldInterface[fields.length];
        }
        
        int left = papplet.width/3;
        int w23 = 2*left-15;
        int h = 40;
        int top = papplet.height-h;
        
        for ( int i = 0; i < fields.length; i++ )
        {
            Field f = fields[i];
            Class c = f.getType();
            if ( c == boolean.class )
            {
                fieldInterfaces[i] = new FieldInterfaceBooleanPrimitive( papplet, f, left+5, top, w23, h );
            }
            else if ( c == int.class )
            {
                fieldInterfaces[i] = new FieldInterfaceIntPrimitive( papplet, f, left+5, top, w23, h );
            }
            else if ( c == float.class )
            {
                fieldInterfaces[i] = new FieldInterfaceFloatPrimitive( papplet, f, left+5, top, w23, h );
            }
            else if ( c == String.class )
            {
                fieldInterfaces[i] = new FieldInterfaceString( papplet, f, left+5, top, w23+5, h );
            }
        }
    }
    
    private void updateCurrentField ()
    {
        currentField = fields[currentFieldIndex];
        try {
            Class c = currentField.getType();
            if ( c == boolean.class )
            {
                currentFieldType = TYPE_PRIMITIVE_BOOLEAN;
            }
            else if ( c == int.class )
            {
                currentFieldType = TYPE_PRIMITIVE_INT;
            }
            else if ( c == short.class )
            {
                currentFieldType = TYPE_PRIMITIVE_SHORT;
            }
            else if ( c == byte.class )
            {
                currentFieldType = TYPE_PRIMITIVE_BYTE;
            }
            else if ( c == char.class )
            {
                currentFieldType = TYPE_PRIMITIVE_CHAR;
            }
            else if ( c == long.class )
            {
                currentFieldType = TYPE_PRIMITIVE_LONG;
            }
            else if ( c == float.class )
            {
                currentFieldType = TYPE_PRIMITIVE_FLOAT;
            }
            else if ( c == double.class )
            {
                currentFieldType = TYPE_PRIMITIVE_DOUBLE;
            }
            else if ( c == String.class )
            {
                currentFieldType = TYPE_STRING;
            }
            else
            {
                currentFieldType = TYPE_GENERIC;
            }
        } 
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
 
    public void draw ()
    {
        if ( on )
        {
            papplet.pushStyle();
            papplet.fill( 20, 60 );
            papplet.noStroke();
            papplet.rectMode( PApplet.CORNER );
            papplet.rect( 0, papplet.height-40, papplet.width, 40 );
            
            renderField();
            
            papplet.popStyle();
        }
    }
    
    private void renderField ()
    {
        if ( currentField == null ) return;
        
        papplet.fill( 200 );
        papplet.textFont( fnt );
        papplet.textAlign( PApplet.RIGHT );
        papplet.text( currentField.getName(), papplet.width/3-5, papplet.height-16 );
        
        switch ( currentFieldType )
        {
            case TYPE_PRIMITIVE_BOOLEAN:
            case TYPE_PRIMITIVE_INT:
            case TYPE_PRIMITIVE_FLOAT:
            case TYPE_STRING:
                fieldInterfaces[currentFieldIndex].draw();
                break;
            default:
            //TODO
        }
    }
    
    public void mouseEvent ( MouseEvent mev )
    {
        if ( !on ) return;
        
        int x = mev.getX(), y = mev.getY();
        
        if ( y > papplet.height-40 )
        {
            upButton.mouseEvent( mev );
            downButton.mouseEvent( mev );
            if ( fieldInterfaces[currentFieldIndex] != null )
                fieldInterfaces[currentFieldIndex].mouseEvent( mev );
        }
    }
    
    public void keyEvent ( KeyEvent e )
    {
        switch( e.getID() )
        {
            case KeyEvent.KEY_RELEASED:
                if ( (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 && e.getKeyCode() == 73 /* i */ )
                {
                    on = !on;
                }
                break;
        }
    }
}

class InterfaceElementAction
{
    void mouseEnter () {;}
    void mouseExit () {;}
    void mousePressed () {;}
    void mouseReleased () {;}
    void mouseDragged () {;}
}

class InterfaceElement
extends InterfaceElementAction
{
    int x, y, width, height;
    InterfaceElementAction handler;
    
    public int mouseX, mouseY;
    public int pmouseX, pmouseY;
    private boolean pressed;
    
    InterfaceElement () {}
    
    InterfaceElement ( int _x, int _y, int _w, int _h )
    {
        setRegion( _x, _y, _w, _h );
        handler = this;
    }
    
    public void setRegion ( int _x, int _y, int _w, int _h )
    {
        x = _x; 
        y = _y; 
        width = _w; 
        height = _h;
    }
    
    public void setHandler ( InterfaceElementAction _h )
    {
        handler = _h;
    }
    
    public boolean mouseEvent ( MouseEvent mev )
    {
        mouseX = mev.getX();
        mouseY = mev.getY();
        
        if ( !isInside( mouseX, mouseY ) || handler == null )
            return false;
        else
        {
            switch ( mev.getID() )
            {
                case MouseEvent.MOUSE_ENTERED:
                    handler.mouseEnter();
                    break;
                case MouseEvent.MOUSE_EXITED:
                    handler.mouseExit();
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    pmouseX = mouseX;
                    pmouseY = mouseY;
                    pressed = true;
                    handler.mousePressed();
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if ( pressed )
                        handler.mouseReleased();
                    pressed = false;
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    handler.mouseDragged();
                    break;
            }
        }
        
        return true;
    }

    void mouseEnter () {;}
    void mouseExit () {;}
    void mousePressed () {;}
    void mouseReleased () {;}
    void mouseDragged () {;}
    
    boolean isInside ( int xx, int yy )
    {
        return x <= xx && xx <= x+width && y <= yy && yy <= y+height;   
    }
}

class FieldInterface
extends InterfaceElement
{
    Field field;
    PApplet papplet;
    
    FieldInterface () {}
    
    FieldInterface ( PApplet _p, Field _f )
    {
        papplet = _p;
        field = _f;
    }
    
    public void draw ()
    {
        // implement yourself   
    }
}

class FieldInterfaceBooleanPrimitive
extends FieldInterface
{
    FieldInterfaceBooleanPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f );
        setRegion( _x, _y, _w, _h );
        setHandler( this );
    }
    
    public void draw ()
    {
        papplet.stroke( 200 );
        
        boolean b = false;
        try {
            b = field.getBoolean( papplet );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        if ( b )
            papplet.fill( 255 );
        else
            papplet.fill( 120 );
            
        papplet.rect( x + 5, y + 10, 20, 20 );
        
        /*papplet.fill( 255 );
        papplet.textAlign( LEFT );
        papplet.text( "(" + (b ? "true" : "false") + ")", x + 15 + 20, y + height - 16 );*/
    }
    
    public void mouseReleased ()
    {
        try {
            boolean b = field.getBoolean(papplet);
            field.setBoolean( papplet, !b );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}

abstract class FieldInterfaceNumberPrimitive
extends FieldInterface
{
    FieldInterfaceNumberPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f );
        setRegion( _x, _y, _w, _h );
        setMinMaxBasedOnValue();
        setHandler( this );
    }
    
    public void draw ()
    {
        papplet.noStroke();
        papplet.fill( 120 );
        papplet.rect( x, y+10, width, 20 );
        
        int value = getValueForSlider();
        
        papplet.fill(0);
        if ( value > width / 2 )
        {
            papplet.textAlign( PApplet.RIGHT );
            papplet.text( getValueForLabel(), x - 5 + value, y+height-16 );
        }
        else
        {
            papplet.textAlign( PApplet.LEFT );
            papplet.text( getValueForLabel(), x + 5 + value, y+height-16 );
        }
        
        papplet.stroke( 255 );
        papplet.line( x + value, y + 10,
                      x + value, y + height - 11 );
    }
    
    abstract void setMinMaxBasedOnValue();
    abstract int getValueForSlider ();
    abstract String getValueForLabel ();
    abstract void setValueFromSlider ();
    
    public void mouseDragged ()
    {
        setValueFromSlider();
    }
    
    public void mouseReleased ()
    {
        setMinMaxBasedOnValue();
    }
}

class FieldInterfaceIntPrimitive
extends FieldInterfaceNumberPrimitive
{
    int minimum = -100, maximum = 100;
    
    FieldInterfaceIntPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f, _x, _y, _w, _h );
    }
    
    void setMinMaxBasedOnValue ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt( papplet );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        minimum = intValue - 100;
        maximum = intValue + 100;
    }
    
    int getValueForSlider ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt(papplet);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return (int)( PApplet.map( intValue, minimum, maximum, 0, width ) );
    }
    
    String getValueForLabel ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt(papplet);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return intValue + "";
    }
    
    void setValueFromSlider ()
    {
        int v = (int)( PApplet.map( mouseX, x, x + width, minimum, maximum ) );
        try {
            field.setInt( papplet, v );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}

class FieldInterfaceFloatPrimitive
extends FieldInterfaceNumberPrimitive
{
    float minimum, maximum;
    
    FieldInterfaceFloatPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f, _x, _y, _w, _h );
    }
    
    void setMinMaxBasedOnValue ()
    {
        float floatValue = getValue();
        minimum = floatValue - (width/2)/10;
        maximum = floatValue + (width/2)/10;
    }
    
    float getValue ()
    {
        float floatValue = 0.0f;
        try {
            floatValue = field.getFloat( papplet );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return floatValue;
    }
    
    int getValueForSlider ()
    {
        return (int)( PApplet.map( getValue(), minimum, maximum, 0.0f, width ) );
    }
    
    String getValueForLabel ()
    {
        return getValue() + "";
    }
    
    void setValueFromSlider ()
    {
        float value = PApplet.map( mouseX, x, x + width, minimum, maximum );
        value = (int)(value * 10.0f) / 10.0f;
        
        try {
            field.setFloat( papplet, value );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    public void mouseReleased ()
    {
        setMinMaxBasedOnValue();
    }
}

abstract class FieldInterfaceTextual
extends FieldInterface
{
    FieldInterfaceTextual ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f );
        setRegion( _x, _y, _w, _h );
        setHandler( this );
    }
    
    public void draw ()
    {
        papplet.noFill();
        papplet.stroke( 255 );
        papplet.rect( x, y+5, width, height-10 );
        
        String strValue = getValue();
        if ( strValue == null )
            strValue = "(null)";
        papplet.fill( 0 );
        papplet.textAlign( PApplet.LEFT );
        papplet.text( strValue, x+5, y+height-16 );
    }
    
    abstract String getValue ();
    abstract String setValueFromClipboard ( String s );
}

class FieldInterfaceString
extends FieldInterfaceTextual
{
    FieldInterfaceString ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f, _x, _y, _w, _h );
    }
    
    String getValue ()
    {
        String strValue = "";
        try {
            strValue = (String)(field.get( papplet ));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return strValue;
    }
    
    String setValueFromClipboard ( String s )
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();
        DataFlavor df = new DataFlavor( String.class, "String" );
        if ( clipboard.isDataFlavorAvailable( df ) )
        {
            Transferable transferable = clipboard.getContents(null);
            if ( transferable != null )
            {
                String st = null;
                try {
                    st = (String)(transferable.getTransferData( df ));
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
                return st;
            }
        }
        return null;
    }
}
