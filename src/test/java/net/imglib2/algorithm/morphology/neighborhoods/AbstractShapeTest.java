package net.imglib2.algorithm.morphology.neighborhoods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayLocalizingCursor;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractShapeTest
{

	protected final Shape shape;

	protected ArrayImg< UnsignedShortType, ShortArray > img;

	public AbstractShapeTest()
	{
		this.shape = createShape();
	}

	@Before
	public void setUp() throws Exception
	{
		final long[] dims = new long[] { 10, 10, 10 };
		this.img = ArrayImgs.unsignedShorts( dims );
	}

	/**
	 * Instantiates the shape under test.
	 * 
	 * @return a new {@link Shape}.
	 */
	protected abstract Shape createShape();

	/**
	 * Returns <code>true</code> iff the specified <code>pos</code> coordinates
	 * are within a neighborhood generated from the Shape under test, and
	 * positioned on <code>center</code>.
	 * 
	 * @param pos
	 *            the position to test.
	 * @param center
	 *            the neighborhood center.
	 * @return <code>true</code> if pos is inside the nieghborhood.
	 */
	protected abstract boolean isInside( final long[] pos, final long[] center );

	@Test
	public void testNeighborhoods()
	{
		final IterableInterval< Neighborhood< UnsignedShortType >> neighborhoods = shape.neighborhoods( img );
		for ( final Neighborhood< UnsignedShortType > neighborhood : neighborhoods )
		{
			final Cursor< UnsignedShortType > cursor = neighborhood.localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				if ( !Intervals.contains( img, cursor ) )
				{
					continue;
				}
				cursor.get().inc();
			}
		}

		/*
		 * Dummy test: we just make sure that the central point of the image has
		 * been walked over exactly the shape size times. Because the source
		 * image is larger than the Shape it is the case.
		 */

		final long[] center = new long[ img.numDimensions() ];
		for ( int d = 0; d < center.length; d++ )
		{
			center[ d ] = img.dimension( d ) / 2;
		}

		final ArrayRandomAccess< UnsignedShortType > ra = img.randomAccess();
		ra.setPosition( center );
		final long size = neighborhoods.iterator().next().size();
		assertEquals( "Bad value at image center.", size, ra.get().get() );
	}

	@Test
	public void testNeighborhoodsRandomAccessible()
	{
		final RandomAccessible< Neighborhood< UnsignedShortType >> neighborhoods = shape.neighborhoodsRandomAccessible( img );
		final long[] center = new long[ img.numDimensions() ];
		for ( int d = 0; d < center.length; d++ )
		{
			center[ d ] = img.dimension( d ) / 2;
		}

		final RandomAccess< Neighborhood< UnsignedShortType >> ra = neighborhoods.randomAccess();
		ra.setPosition( center );
		final Neighborhood< UnsignedShortType > neighborhood = ra.get();
		for ( final UnsignedShortType pixel : neighborhood )
		{
			pixel.inc();
		}

		/*
		 * Test we iterated solely over the neighborhood, exactly once.
		 */

		final ArrayLocalizingCursor< UnsignedShortType > cursor = img.localizingCursor();
		final long[] pos = new long[ cursor.numDimensions() ];
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			cursor.localize( pos );
			final int expected = isInside( pos, center ) ? 1 : 0;
			assertEquals( "Wrong value at position " + Util.printCoordinates( pos ), expected, cursor.get().get() );
		}

	}


	@Test
	public void testNeighborhoodsSafeBehavior()
	{
		final IterableInterval< Neighborhood< UnsignedShortType >> safe = shape.neighborhoodsSafe( img );
		final Neighborhood< UnsignedShortType > neighborhoodSafe = safe.firstElement();
		final Cursor< UnsignedShortType > csafe1 = neighborhoodSafe.cursor();
		final Cursor< UnsignedShortType > csafe2 = neighborhoodSafe.cursor();
		assertNotEquals( "The two cursors from the safe iterator are the same object.", csafe1, csafe2 );

		final IterableInterval< Neighborhood< UnsignedShortType >> unsafe = shape.neighborhoods( img );
		final Neighborhood< UnsignedShortType > neighborhoodUnsafe = unsafe.firstElement();
		final Cursor< UnsignedShortType > cunsafe1 = neighborhoodUnsafe.cursor();
		final Cursor< UnsignedShortType > cunsafe2 = neighborhoodUnsafe.cursor();
		assertEquals( "The two cursors from the unsafe iterator are not the same object.", cunsafe1, cunsafe2 );

		final RandomAccessible< Neighborhood< UnsignedShortType >> safeRA = shape.neighborhoodsRandomAccessibleSafe( img );
		final Neighborhood< UnsignedShortType > neighborhoodSafeRA = safeRA.randomAccess().get();
		final Cursor< UnsignedShortType > crasafe1 = neighborhoodSafeRA.cursor();
		final Cursor< UnsignedShortType > crasafe2 = neighborhoodSafeRA.cursor();
		assertNotEquals( "The two cursors from the safe iterator are the same object.", crasafe1, crasafe2 );

		final RandomAccessible< Neighborhood< UnsignedShortType >> unsafeRA = shape.neighborhoodsRandomAccessible( img );
		final Neighborhood< UnsignedShortType > neighborhoodUnsafeRA = unsafeRA.randomAccess().get();
		final Cursor< UnsignedShortType > craunsafe1 = neighborhoodUnsafeRA.cursor();
		final Cursor< UnsignedShortType > craunsafe2 = neighborhoodUnsafeRA.cursor();
		assertEquals( "The two cursors from the unsafe iterator are not the same object.", craunsafe1, craunsafe2 );
	}
}
