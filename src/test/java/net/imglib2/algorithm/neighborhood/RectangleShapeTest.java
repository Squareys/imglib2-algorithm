package net.imglib2.algorithm.neighborhood;


public class RectangleShapeTest extends AbstractShapeTest
{
	final static int span = 3;

	@Override
	protected Shape createShape()
	{
		final boolean skipCenter = false;
		return new RectangleShape( span, skipCenter );
	}

	@Override
	protected boolean isInside( final long[] pos, final long[] center )
	{
		for ( int d = 0; d < img.numDimensions(); d++ )
		{
			if ( pos[ d ] > center[ d ] + span || pos[ d ] < center[ d ] - span ) { return false; }
		}
		return true;

	}
}
