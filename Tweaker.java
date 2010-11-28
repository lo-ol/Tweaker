//import processing.pdf.*;
// http://code.google.com/p/processing/issues/detail?id=459
import processing.xml.*;
import processing.core.*;

import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import java.io.File;

import java.awt.Toolkit;
import java.awt.datatransfer.*;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;

/**
 *    Herr Inspektor meet Processing.
 *
 *    http://bezier.de/processing/tweaker/
 *    https://github.com/fjenett/Tweaker
 */

public class Tweaker
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
    
    boolean showTweaker = false, saveFrame = false, savePDF = false, savePDFBegin = false;
    boolean preCalled = false; // http://code.google.com/p/processing/issues/detail?id=455
    PApplet papplet;
    
    String dateString, sessionID;
    
    PFont fnt;
    
    Field[] fields;
    FieldInterface[] fieldInterfaces;
    Field currentField;
    
    int currentFieldType;
    int currentFieldIndex = 0;
    
    InterfaceElement upButton, downButton;
    
    Tweaker ( PApplet _a )
    {
        sayHi();
        
        papplet = _a;
        
        dateString = PApplet.year() + "-" + PApplet.nf(PApplet.month(),2) + "-" + PApplet.nf(PApplet.day(),2);
        sessionID = PApplet.nf(PApplet.hour(),2) + "-" + PApplet.nf(PApplet.minute(),2) + "-" + PApplet.nf(PApplet.second(),2);
        
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
        
        papplet.registerPre(this);
        papplet.registerPost(this);
        papplet.registerDraw(this);

        papplet.registerMouseEvent(this);
        papplet.registerKeyEvent(this);
        
        fnt = papplet.createFont("Verdana", 9);
        
        getFields();
        
        loadSettings();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run () {
                saveSettings();
            }
        });
    }
    
    void loadSettings ()
    {
        XMLElement sXML = null;
        
        try {
            sXML = new XMLElement(papplet, "settings.xml");
        } catch ( Exception e ) {
            //e.printStackTrace();
            return;
        }
        
        /* TODO: check same sketch?
        */
        
        XMLElement[] fieldsXML = sXML.getChildren( "fields" );
        
        if ( fieldsXML == null || fieldsXML.length <= 0 ) return;
        
        fieldsXML = fieldsXML[0].getChildren("field");
        
        if ( fieldsXML == null || fieldsXML.length <= 0 ) return;
        
        for ( XMLElement fieldXML : fieldsXML )
        {
            String name = fieldXML.getStringAttribute("name");
            String type = fieldXML.getStringAttribute("type");
            
            Field realField = null;
            try {
                realField = papplet.getClass().getDeclaredField( name );
            } catch ( Exception e ) {
                System.err.println( "settings.xml: \"" + name + "\" was not found" );
                continue;
            }
            
            Class fieldClass = realField.getType();
            if ( type != null && !fieldClass.getName().equals( type ) )
            {
                System.err.println( "settings.xml: \"" + name + "\" is not of type \"" + type + "\"" );
                continue;
            }
            
            XMLElement[] values = fieldXML.getChildren( "value" );
            if ( values == null && values.length <= 0 )
            {
                System.err.println( "settings.xml: \"" + name + "\" has no value" );
                continue;
            }
            
            String value = values[0].getContent();
                
            try 
            {
                if ( fieldClass == int.class )
                {
                    realField.setInt( papplet, Integer.parseInt(value) );
                }
                else if ( fieldClass == float.class )
                {
                    realField.setFloat( papplet, Float.parseFloat(value) );
                }
                
                if ( fieldClass == int.class || fieldClass == float.class )
                {
                    String mi = null, ma = null;
                    XMLElement[] miXML = fieldXML.getChildren( "minimum" );
                    if ( miXML != null && miXML.length > 0 )
                        mi = miXML[0].getContent();
                    XMLElement[] maXML = fieldXML.getChildren( "maximum" );
                    if ( maXML != null && maXML.length > 0 )
                        ma = maXML[0].getContent();
                    
                    FieldInterfaceNumberPrimitive fi = 
                        (FieldInterfaceNumberPrimitive)(getInterfaceForField( realField ));
                    if ( fi != null )
                    {
                        fi.setMinMaxFromStrings( mi, ma );
                    }
                }
                
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }
    
    FieldInterface getInterfaceForField ( Field f )
    {
        for ( FieldInterface fi : fieldInterfaces )
        {
            if ( fi.field.equals(f) ) return fi;
        }
        
        return null;
    }
    
    void saveSettings ()
    {
        java.util.Vector<String> sXML = new java.util.Vector();
        
        sXML.add( "<?xml ?>" );
        sXML.add( "<tweeker>" );
        sXML.add( "<sketch name=\""+papplet.getClass().getName()+"\" saved=\""+dateString+"-"+sessionID+"\" />" );
        
        sXML.add( "<fields>" );
        for ( int i = 0; i < fields.length; i++ )
        {
            if ( fieldInterfaces[i] == null ) continue;
            
            sXML.add( "<field name=\""+fields[i].getName()+"\" "+
                             "type=\""+fields[i].getType().getName()+"\" "+">" );
                             
            if ( fieldInterfaces[i].getClass().getSuperclass() == FieldInterfaceNumberPrimitive.class )
            {
                FieldInterfaceNumberPrimitive fi = (FieldInterfaceNumberPrimitive)fieldInterfaces[i];
                sXML.add( "<minimum>" + fi.getMinimumForXML() + "</minimum>" );
                sXML.add( "<maximum>" + fi.getMaximumForXML() + "</maximum>" );
                sXML.add( "<steps>" + fi.getStepsForXML() + "</steps>" );
                
                try {
                    sXML.add( "<value>" + fields[i].get(papplet).toString() + "</value>" );
                } catch ( Exception e ) {
                    // ignore
                }
            }
            else
            {
                try {
                    sXML.add( "<value><![CDATA[" + fields[i].get(papplet).toString() + "]]></value>" );
                } catch ( Exception e ) {
                    // ignore
                }
            }
            sXML.add( "</field>" );
        }
        sXML.add( "</fields>" );
        
        sXML.add( "</tweeker>" );
        
        papplet.saveStrings( "settings.xml", sXML.toArray(new String[0]) );
    }
    
    void sayHi ()
    {
        System.out.println("TWEEKER says hi! Some shortcuts ..");
        System.out.println("");
        System.out.println("Command-T show/hide Tweeker");
        System.out.println("Command-S save frame");
        System.out.println("Command-P save PDF");
        System.out.println("");
    }
    
    public void show ()
    {
        showTweaker = true;   
    }
    
    public void hide ()
    {
        showTweaker = false;   
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
    
    public void pre ()
    {
        preCalled = true;
        
        if ( savePDFBegin )
        {
            savePDFBegin = false;
            papplet.beginRecord( PApplet.PDF, 
                                 "saved" +
                                 File.separator + dateString + 
                                 File.separator + sessionID + 
                                 File.separator + "######.pdf");  
            savePDF = true;
        }
        
        papplet.pushMatrix();
    }
    
    public void post ()
    {
        if ( !preCalled ) return;
    }
 
    public void draw ()
    {
        if ( preCalled )
            papplet.popMatrix();
                
        if ( showTweaker && !(saveFrame || savePDF) )
        {
            papplet.pushStyle();
            papplet.fill( 20, 60 );
            papplet.noStroke();
            papplet.rectMode( PApplet.CORNER );
            papplet.rect( 0, papplet.height-40, papplet.width, 40 );
            
            renderField();
            
            papplet.popStyle();
        }
        else if ( saveFrame )
        {
            papplet.saveFrame( "saved" +
                               File.separator + dateString + 
                               File.separator + sessionID + 
                               File.separator + "######.png" );
            saveFrame = false;
            System.out.println( "Frame saved." );
        }
        else if ( savePDF )
        {
            papplet.endRecord();
            savePDF = false;
            System.out.println( "PDF saved." );
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
        if ( !showTweaker ) return;
        
        int x = mev.getX(), y = mev.getY();
        
        if ( y > papplet.height-40 )
        {
            upButton.mouseEvent( mev );
            downButton.mouseEvent( mev );
            if ( fieldInterfaces[currentFieldIndex] != null )
                fieldInterfaces[currentFieldIndex].mouseEvent( mev );
        }
    }
    
    public void keyEvent ( KeyEvent kev )
    {
        switch( kev.getID() )
        {
            case KeyEvent.KEY_RELEASED:
                if ( kev.isMetaDown() )
                {
                    switch ( kev.getKeyCode() )
                    {
                        case 84: /* t */
                            showTweaker = !showTweaker;
                            break;
                        case 83: /* s */
                            saveFrame = true;
                            break;
                        case 80: /* p */
                            savePDFBegin = true;
                            break;
                        case 73: /* i */
                            break;
                        default:
                            System.out.println(kev.getKeyCode());
                    }
                    return;
                }
                break;
        }
        
        if ( fieldInterfaces[currentFieldIndex] != null )
            fieldInterfaces[currentFieldIndex].keyEvent( kev );
    }
}

class InterfaceElementAction
{
    public int mouseX, mouseY;
    public int pmouseX, pmouseY;
    public boolean mousePressed, keyPressed;
    public char key;
    public int keyCode;
    public KeyEvent keyEvent;
    public MouseEvent mouseEvent;
    
    void mouseEnter () {;}
    void mouseExit () {;}
    void mousePressed () {;}
    void mouseReleased () {;}
    void mouseDragged () {;}
    
    void keyPressed () {;}
    void keyTyped () {;}
    void keyReleased () {;}
}

class InterfaceElement
extends InterfaceElementAction
{
    int x, y, width, height;
    InterfaceElementAction handler;
    
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
                    mousePressed = true;
                    handler.mousePressed();
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if ( mousePressed )
                        handler.mouseReleased();
                    mousePressed = false;
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    handler.mouseDragged();
                    break;
            }
        }
        
        return true;
    }
    
    public boolean keyEvent ( KeyEvent kev )
    {
        key = kev.getKeyChar();
        keyCode = kev.getKeyCode();
        
        switch ( kev.getID() )
        {
            case KeyEvent.KEY_PRESSED:
                keyPressed = true;
                handler.keyPressed();
                break;
            case KeyEvent.KEY_TYPED:
                handler.keyTyped();
                break;
            case KeyEvent.KEY_RELEASED:
                if ( keyPressed )
                    handler.keyReleased();
                keyPressed = false;
                break;   
        }
        
        return false;
    }

    void mouseEnter () {;}
    void mouseExit () {;}
    void mousePressed () {;}
    void mouseReleased () {;}
    void mouseDragged () {;}
    
    void keyPressed () {;}
    void keyTyped () {;}
    void keyReleased () {;}
    
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
    Object instance;
    
    FieldInterface () {}
    
    FieldInterface ( PApplet _p, Field _f )
    {
        papplet = _p;
        instance = papplet;
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
            b = field.getBoolean( instance );
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
            boolean b = field.getBoolean(instance);
            field.setBoolean( instance, !b );
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
        setSliderBasedOnValue();
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
    
    abstract void setMinMaxFromStrings ( String mi, String ma );
    abstract String getMinimumForXML();
    abstract String getMaximumForXML();
    abstract String getStepsForXML();
    abstract void setSliderBasedOnValue();
    abstract int getValueForSlider ();
    abstract String getValueForLabel ();
    abstract void setValueFromSlider ();
    
    public void mouseDragged ()
    {
        setValueFromSlider();
    }
    
    public void mouseReleased ()
    {
        setSliderBasedOnValue();
    }
}

class FieldInterfaceIntPrimitive
extends FieldInterfaceNumberPrimitive
{
    int sliderMinimum = -100, sliderMaximum = 100;
    int minimum = Integer.MIN_VALUE;
    int maximum = Integer.MAX_VALUE;
    
    FieldInterfaceIntPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f, _x, _y, _w, _h );
    }
    
    void setSliderBasedOnValue ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt( instance );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        sliderMinimum = intValue - 100;
        sliderMaximum = intValue + 100;
    }
    
    void setMinMaxFromStrings ( String mi, String ma ) {
        minimum = Integer.parseInt( mi );
        maximum = Integer.parseInt( ma );
    }
    
    String getMinimumForXML() { return minimum + ""; }
    String getMaximumForXML() { return maximum + ""; }
    String getStepsForXML()  { return "1"; }
    
    int getValueForSlider ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt(instance);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return (int)( PApplet.map( intValue, sliderMinimum, sliderMaximum, 0, width ) );
    }
    
    String getValueForLabel ()
    {
        int intValue = 0;
        try {
            intValue = field.getInt(instance);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return intValue + "";
    }
    
    void setValueFromSlider ()
    {
        int intValue = (int)( PApplet.map( mouseX, x, x + width, sliderMinimum, sliderMaximum ) );
                
        if ( intValue > maximum )
            intValue = maximum;
        
        if ( intValue < minimum )
            intValue = minimum;
        
        try {
            field.setInt( instance, intValue );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}

class FieldInterfaceFloatPrimitive
extends FieldInterfaceNumberPrimitive
{
    float minimum = Float.MIN_VALUE, maximum = Float.MAX_VALUE;
    float sliderMinimum, sliderMaximum;
    
    FieldInterfaceFloatPrimitive ( PApplet _p, Field _f, int _x, int _y, int _w, int _h )
    {
        super( _p, _f, _x, _y, _w, _h );
    }
    
    void setSliderBasedOnValue ()
    {
        float floatValue = getValue();
        sliderMinimum = floatValue - (width/2)/10;
        sliderMaximum = floatValue + (width/2)/10;
    }
    
    void setMinMaxFromStrings ( String mi, String ma ) {
        minimum = Float.parseFloat( mi );
        maximum = Float.parseFloat( ma );
    }
    
    String getMinimumForXML() { return minimum + ""; }
    String getMaximumForXML() { return maximum + ""; }
    String getStepsForXML()  { return "0.1"; }
    
    float getValue ()
    {
        float floatValue = 0.0f;
        try {
            floatValue = field.getFloat( instance );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        return floatValue;
    }
    
    int getValueForSlider ()
    {
        return (int)( PApplet.map( getValue(), sliderMinimum, sliderMaximum, 0.0f, width ) );
    }
    
    String getValueForLabel ()
    {
        return getValue() + "";
    }
    
    void setValueFromSlider ()
    {
        float floatValue = 
            PApplet.map( mouseX, x, x + width, 
                         sliderMinimum, sliderMaximum );
        floatValue = (int)(floatValue * 10.0f) / 10.0f;
        
        if ( floatValue > maximum ) floatValue = maximum;
        if ( floatValue < minimum ) floatValue = minimum;
        
        try {
            field.setFloat( instance, floatValue );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    public void mouseReleased ()
    {
        setSliderBasedOnValue();
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
    abstract void setValueFromClipboard ();
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
            strValue = (String)(field.get( instance ));
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return strValue;
    }
    
    void setValueFromClipboard ( )
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
                if ( st != null )
                {
                    try {
                        field.set(instance, st);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }   
                }
            }
        }
    }
    
    public boolean keyEvent ( KeyEvent kev )
    {
        super.keyEvent( kev );
        
        switch ( kev.getID() )
        {
            case KeyEvent.KEY_RELEASED:
                if ( kev.isMetaDown() && kev.getKeyCode() == 86 /* i */ )
                {
                    setValueFromClipboard();
                }
                break;
        }
        
        return false;
    }
}
