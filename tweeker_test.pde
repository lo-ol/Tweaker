import processing.pdf.*; // http://code.google.com/p/processing/issues/detail?id=459

int numberOfItems = 2, trans = 0;
int rectW = 10, rectH = 10;
int tx, ty;
float rot = 0;

void setup ()
{
    size( 400, 400 );
    background( random( 200 ) );
    tx = width/2;
    ty = height/2;
    
    Tweeker tweeker = new Tweeker(this);
    //tweeker.show();
}

void draw ()
{
    smooth();
    background( 120 );
    pushMatrix();
    
    translate( tx, ty );
    rotate( rot );
    
    for ( int i = 0; i < numberOfItems; i++ )
    {
        rect( 0,0, rectW,rectH );
        translate( trans, 0 );
        rotate( 0.1 );
    }
    
    popMatrix();
}
