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
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhood.LocalCursor;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;

public final class RectangleNeighborhoodCursor< T > extends RectangleNeighborhoodLocalizableSampler< T > implements Cursor< Neighborhood< T > >
{
	private final long[] dimensions;

	private final long[] min;

	private final long[] max;

	private long index;

	private long maxIndex;

	private long lastIndex;

	private long maxIndexOnLine;

	private final int numIntervals;

	private int nextIntervalIndex;

	private final int lastIntervalIndex;

	private Interval curInterval;

	private final Interval[] intervals;

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
		long size = dimensions[ 0 ];
		for ( int d = 1; d < n; ++d )
			size *= dimensions[ d ];
		maxIndex = size;

		/*
		 * Check if source is at least size of span. (contains() is false for
		 * equal intervals.)
		 */
		if ( !Intervals.contains( span, source ) )
		{
			/*
			 * separate the image into intervals in a border layout similar way,
			 * but for n dimensions
			 */
			lastIntervalIndex = 2 * n;
			numIntervals = lastIntervalIndex + 1;
			intervals = new Interval[ numIntervals ];

			long[] minBegin = new long[ n ];
			long[] maxBegin = new long[ n ];

			long[] minEnd = new long[ n ];
			long[] maxEnd = new long[ n ];

			sourceInterval.min( minBegin );
			sourceInterval.max( maxBegin );

			sourceInterval.min( minEnd );
			sourceInterval.max( maxEnd );

			FinalInterval restInterval = new FinalInterval( sourceInterval );
			FinalInterval oldRestInterval = null;

			for ( int d = n - 1; d >= 0; --d )
			{
				oldRestInterval = restInterval;

				/* restrain the interval in a certain dimension */
				restInterval = Intervals.expand( restInterval, -span.dimension( d ) + 1, d );

				oldRestInterval.min( minBegin );
				restInterval.max( maxBegin );
				maxBegin[ d ] = restInterval.min( d );

				restInterval.min( minEnd );
				oldRestInterval.max( maxEnd );
				minEnd[ d ] = restInterval.max( d );

				intervals[ 2 * d ] = new FinalInterval( minBegin, maxBegin );
				intervals[ 2 * d + 1 ] = new FinalInterval( minEnd, maxEnd );
			}

			intervals[ lastIntervalIndex ] = restInterval;
		}
		else
		{
			// span is bigger than the image.
			intervals = new Interval[] { sourceInterval };

			lastIntervalIndex = 0;
			numIntervals = 1;
		}
		reset();
	}

	private RectangleNeighborhoodCursor( final RectangleNeighborhoodCursor< T > c )
	{
		super( c );
		dimensions = c.dimensions.clone();
		min = c.min.clone();
		max = c.max.clone();
		maxIndex = c.maxIndex;
		lastIndex = c.lastIndex;
		index = c.index;
		maxIndexOnLine = c.maxIndexOnLine;
		lastIntervalIndex = c.lastIntervalIndex;
		numIntervals = c.numIntervals;
		intervals = c.intervals;
	}

	@Override
	public void fwd()
	{
		++currentPos[ 0 ];
		++currentMin[ 0 ];
		++currentMax[ 0 ];

		if ( ++index > maxIndexOnLine )
			nextLine();
	}

	private void nextLine()
	{
		currentPos[ 0 ] = curInterval.min( 0 );
		currentMin[ 0 ] -= curInterval.dimension( 0 );
		currentMax[ 0 ] -= curInterval.dimension( 0 );
		maxIndexOnLine += curInterval.dimension( 0 );
		for ( int d = 1; d < n; ++d )
		{
			++currentPos[ d ];
			++currentMin[ d ];
			++currentMax[ d ];
			if ( currentPos[ d ] > curInterval.max( d ) )
			{
				currentPos[ d ] = curInterval.min( d );
				currentMin[ d ] -= curInterval.dimension( d );
				currentMax[ d ] -= curInterval.dimension( d );
			}
		}

		if ( index > maxIndex )
		{
			nextInterval();
		}
	}

	private final void nextInterval()
	{
		curInterval = intervals[ nextIntervalIndex++ ];
		index = 0;
		maxIndexOnLine = maxIndex = curInterval.dimension( 0 );
		currentPos[ 0 ] = curInterval.min( 0 );
		currentMin[ 0 ] = currentPos[ 0 ] - span.dimension( 0 );
		currentMax[ 0 ] = currentPos[ 0 ] + span.dimension( 0 );

		for ( int d = 1; d < n; ++d )
		{
			maxIndex *= curInterval.dimension( d );
			currentPos[ d ] = curInterval.min( d );
			currentMin[ d ] = currentPos[ d ] - dimensions[ d ];
			currentMax[ d ] = currentPos[ d ] + dimensions[ d ];
		}

		lastIndex = maxIndex - 1;

		safe = ( nextIntervalIndex == numIntervals ) && numIntervals != 1;
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
		nextIntervalIndex = 0;
		nextInterval();

		// move all back one on dim 0
		--currentPos[ 0 ];
		--currentMin[ 0 ];
		--currentMax[ 0 ];
	}

	@Override
	public boolean hasNext()
	{
		if ( nextIntervalIndex == numIntervals ) { return index < lastIndex; }

		return true;
	}

	@Override
	public void jumpFwd( final long steps )
	{
		for ( long i = steps; i > 0; --i )
		{
			fwd();
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
