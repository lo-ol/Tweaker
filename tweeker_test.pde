import processing.opengl.*;

boolean drawItems = true, randomColors;
int ellWidth = 20, ellHeight = 20;
float testFloat;
String testString = "test" ;

Tweeker tweeker;

void setup ()
{
    size( 500, 200 );
    background( random( 200 ) );
    
    tweeker = new Tweeker(this);
}

void draw ()
{
    background( 120 );
    
    if ( drawItems )
    {
        if ( randomColors )
            fill( random( 255 ), random( 255 ), random( 255 ) );
        else
            fill( 20 );
            
        ellipse( random( width ), random( height ), ellWidth, ellHeight );
    }
}
