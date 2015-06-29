/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2015 Tobias Pietzsch, Stephan Preibisch, Barry DeZonia,
 * Stephan Saalfeld, Curtis Rueden, Albert Cardona, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Jonathan Hale, Lee Kamentsky, Larry Lindsey, Mark
 * Hiner, Michael Zinsmaier, Martin Horn, Grant Harris, Aivar Grislis, John
 * Bogovic, Steffen Jaensch, Stefan Helfrich, Jan Funke, Nick Perry, Mark Longair,
 * Melissa Linkert and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imglib2.algorithm.neighborhood;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.IntervalIndexer;

public final class RectangleNeighborhoodCursor< T > extends RectangleNeighborhoodLocalizableSampler< T > implements Cursor< Neighborhood< T > >
{
	private final long[] dimensions;

	/*
	 * dimensions[ 0 ] is accessed very frequently, so here is a shortcut which
	 * does not require array access:
	 */
	private final long dimensions0;

	private final long[] min;

	/*
	 * min[ 0 ] is accessed rather frequently in nextLine(), here is a shortcut
	 * which does not require array access:
	 */
	private final long min0;

	private final long[] max;

	private long index;

	private final long maxIndex;

	private long maxIndexOnLine;

	/*
	 * "safe" is used to keep track if accessing the pixels of a neighborhood
	 * can be done without an out of bounds check.
	 */
	private boolean safe;

	public RectangleNeighborhoodCursor( final RandomAccessibleInterval< T > source, final Interval span, final RectangleNeighborhoodFactory< T > factory )
	{
		super( source, span, factory, source );

		dimensions = new long[ n ];
		min = new long[ n ];
		max = new long[ n ];
		source.dimensions( dimensions );
		source.min( min );
		source.max( max );
		min0 = min[ 0 ];
		dimensions0 = dimensions[ 0 ];
		long size = dimensions0;
		for ( int d = 1; d < n; ++d )
			size *= dimensions[ d ];
		maxIndex = size;
		reset();
	}

	private RectangleNeighborhoodCursor( final RectangleNeighborhoodCursor< T > c )
	{
		super( c );
		dimensions = c.dimensions.clone();
		min = c.min.clone();
		max = c.max.clone();
		min0 = c.min0;
		dimensions0 = c.dimensions0;
		maxIndex = c.maxIndex;
		index = c.index;
		maxIndexOnLine = c.maxIndexOnLine;
	}

	@Override
	public void fwd()
	{
		++currentPos[ 0 ];
		++currentMin[ 0 ];
		++currentMax[ 0 ];
		
		safe = safe && currentPos[ 0 ] > innerInterval.min( 0 ) && currentPos[ 0 ] < innerInterval.max( 0 );
		if ( ++index > maxIndexOnLine )
			nextLine();
	}

	private void nextLine()
	{
		currentPos[ 0 ] = min0;
		currentMin[ 0 ] -= dimensions0;
		currentMax[ 0 ] -= dimensions0;
		maxIndexOnLine += dimensions0;
		for ( int d = 1; d < n; ++d )
		{
			++currentPos[ d ];
			++currentMin[ d ];
			++currentMax[ d ];
			if ( currentPos[ d ] > max[ d ] )
			{
				currentPos[ d ] = min[ d ];
				currentMin[ d ] -= dimensions[ d ];
				currentMax[ d ] -= dimensions[ d ];
				
				safe = safe && currentPos[ d ] > innerInterval.min( d ) && currentPos[ d ] < innerInterval.max( d );
			}
			else {
				safe = safe && currentPos[ d ] > innerInterval.min( d ) && currentPos[ d ] < innerInterval.max( d );
				break;
			}
		}
	}

	@Override
	public Neighborhood< T > get()
	{
		if ( safe )
		{
			return currentInnerNeighborhood;
		}
		else
		{
			return currentNeighborhood;
		}
	}

	@Override
	public void reset()
	{
		safe = true;
		index = 0;
		maxIndexOnLine = dimensions0;
		for ( int d = 0; d < n; ++d )
		{
			currentPos[ d ] = ( d == 0 ) ? min[ d ] - 1 : min[ d ];
			currentMin[ d ] = currentPos[ d ] + span.min( d );
			currentMax[ d ] = currentPos[ d ] + span.max( d );

			safe = safe && currentPos[ d ] > innerInterval.min( d ) && currentPos[ d ] < innerInterval.max( d );
		}
	}

	@Override
	public boolean hasNext()
	{
		return index < maxIndex;
	}

	@Override
	public void jumpFwd( final long steps )
	{
		index += steps;
		maxIndexOnLine = ( index < 0 ) ? dimensions0 : ( 1 + index / dimensions0 ) * dimensions0;
		IntervalIndexer.indexToPositionWithOffset( index + 1, dimensions, min, currentPos );
		for ( int d = 0; d < n; ++d )
		{
			currentMin[ d ] = currentPos[ d ] + span.min( d );
			currentMax[ d ] = currentPos[ d ] + span.max( d );
		}
	}

	@Override
	public Neighborhood< T > next()
	{
		fwd();
		return get();
	}

	@Override
	public void remove()
	{
		// NB: no action.
	}

	@Override
	public RectangleNeighborhoodCursor< T > copy()
	{
		return new RectangleNeighborhoodCursor< T >( this );
	}

	@Override
	public RectangleNeighborhoodCursor< T > copyCursor()
	{
		return copy();
	}
}
