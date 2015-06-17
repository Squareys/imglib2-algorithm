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

import java.util.Iterator;

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.AbstractLocalizable;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RealPositionable;

public class RectangleNeighborhood< T > extends AbstractLocalizable implements Neighborhood< T >
{
	public static < T > RectangleNeighborhoodFactory< T > factory()
	{
		return new RectangleNeighborhoodFactory< T >()
		{
			@Override
			public Neighborhood< T > create( final long[] position, final long[] currentMin, final long[] currentMax, final Interval span, final RandomAccess< T > sourceRandomAccess )
			{
				return new RectangleNeighborhood< T >( position, currentMin, currentMax, span, sourceRandomAccess );
			}
		};
	}

	private final long[] currentMin;

	private final long[] currentMax;

	private final long[] dimensions;

	private final RandomAccess< T > sourceRandomAccess;

	private final Interval structuringElementBoundingBox;

	private final long maxIndex;

	RectangleNeighborhood( final long[] position, final long[] currentMin, final long[] currentMax, final Interval span, final RandomAccess< T > sourceRandomAccess )
	{
		super( position );
		this.currentMin = currentMin;
		this.currentMax = currentMax;
		dimensions = new long[ n ];
		span.dimensions( dimensions );

		long mi = dimensions[ 0 ];
		for ( int d = 1; d < n; ++d )
			mi *= dimensions[ d ];
		maxIndex = mi;

		this.sourceRandomAccess = sourceRandomAccess;
		this.structuringElementBoundingBox = span;
	}

	@Override
	public Interval getStructuringElementBoundingBox()
	{
		return structuringElementBoundingBox;
	}

	@Override
	public long size()
	{
		return maxIndex; // -1 because we skip the center pixel
	}

	@Override
	public T firstElement()
	{
		return cursor().next();
	}

	@Override
	public Object iterationOrder()
	{
		return this; // iteration order is only compatible with ourselves
	}

	@Override
	public double realMin( final int d )
	{
		return currentMin[ d ];
	}

	@Override
	public void realMin( final double[] min )
	{
		for ( int d = 0; d < n; ++d )
			min[ d ] = currentMin[ d ];
	}

	@Override
	public void realMin( final RealPositionable min )
	{
		for ( int d = 0; d < n; ++d )
			min.setPosition( currentMin[ d ], d );
	}

	@Override
	public double realMax( final int d )
	{
		return currentMax[ d ];
	}

	@Override
	public void realMax( final double[] max )
	{
		for ( int d = 0; d < n; ++d )
			max[ d ] = currentMax[ d ];
	}

	@Override
	public void realMax( final RealPositionable max )
	{
		for ( int d = 0; d < n; ++d )
			max.setPosition( currentMax[ d ], d );
	}

	@Override
	public Iterator< T > iterator()
	{
		return cursor();
	}

	@Override
	public long min( final int d )
	{
		return currentMin[ d ];
	}

	@Override
	public void min( final long[] min )
	{
		for ( int d = 0; d < n; ++d )
			min[ d ] = currentMin[ d ];
	}

	@Override
	public void min( final Positionable min )
	{
		for ( int d = 0; d < n; ++d )
			min.setPosition( currentMin[ d ], d );
	}

	@Override
	public long max( final int d )
	{
		return currentMax[ d ];
	}

	@Override
	public void max( final long[] max )
	{
		for ( int d = 0; d < n; ++d )
			max[ d ] = currentMax[ d ];
	}

	@Override
	public void max( final Positionable max )
	{
		for ( int d = 0; d < n; ++d )
			max.setPosition( currentMax[ d ], d );
	}

	@Override
	public void dimensions( final long[] dimensions )
	{
		for ( int d = 0; d < n; ++d )
			dimensions[ d ] = this.dimensions[ d ];
	}

	@Override
	public long dimension( final int d )
	{
		return dimensions[ d ];
	}

	@Override
	public LocalCursor cursor()
	{
		return new LocalCursor( sourceRandomAccess.copyRandomAccess() );
	}

	@Override
	public LocalCursor localizingCursor()
	{
		return cursor();
	}

	public final class LocalCursor extends AbstractEuclideanSpace implements Cursor< T >
	{
		private final RandomAccess< T >[] strel;

		private int index;


		private long[] previousMin;

		public LocalCursor( final RandomAccess< T > source )
		{
			super( source.numDimensions() );

			strel = new RandomAccess[ ( int ) maxIndex ];
			
			long maxIndexOnLine = dimensions[ 0 ]-1;
			source.setPosition( currentMin );

			index = -1;
			while ( ++index < maxIndex )
			{
				strel[ index ] = source.copyRandomAccess();
				strel[ index ].setPosition( source );
				
				source.fwd( 0 );
				if ( index + 1 > maxIndexOnLine )
				{
					source.setPosition( currentMin[ 0 ], 0 );
					maxIndexOnLine += dimensions[ 0 ];
					for ( int d = 1; d < n; ++d )
					{
						source.fwd( d );
						if ( source.getLongPosition( d ) > currentMax[ d ] )
							source.setPosition( currentMin[ d ], d );
						else
							break;
					}
				}

			}

			index = -1;
			previousMin = currentMin.clone();
		}

		protected LocalCursor( final LocalCursor c )
		{
			super( c.numDimensions() );
			index = c.index;
			strel = c.strel.clone();
		}

		@Override
		public T get()
		{
			return strel[ index ].get();
		}

		@Override
		public void fwd()
		{
			++index;
		}

		@Override
		public void jumpFwd( final long steps )
		{
			for ( long i = 0; i < steps; ++i )
				fwd();
		}

		@Override
		public T next()
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
		public void reset()
		{
			index = -1;

			// calculate offset
			long[] offset = new long[currentMin.length];
			for (int d = 0; d < currentMin.length; ++d) {
				offset[d] = currentMin[ d ] - previousMin[ d ] ;
			}

			// store minimum for next reset
			previousMin = currentMin.clone();

			// apply offset
			for ( RandomAccess< T > ra : strel )
			{
				ra.move( offset );
			}
		}

		@Override
		public boolean hasNext()
		{
			return index + 1 < maxIndex;
		}

		@Override
		public float getFloatPosition( final int d )
		{
			return strel[ index ].getFloatPosition( d );
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return strel[ index ].getDoublePosition( d );
		}

		@Override
		public int getIntPosition( final int d )
		{
			return strel[ index ].getIntPosition( d );
		}

		@Override
		public long getLongPosition( final int d )
		{
			return strel[ index ].getLongPosition( d );
		}

		@Override
		public void localize( final long[] position )
		{
			strel[ index ].localize( position );
		}

		@Override
		public void localize( final float[] position )
		{
			strel[ index ].localize( position );
		}

		@Override
		public void localize( final double[] position )
		{
			strel[ index ].localize( position );
		}

		@Override
		public void localize( final int[] position )
		{
			strel[ index ].localize( position );
		}

		@Override
		public LocalCursor copy()
		{
			return new LocalCursor( this );
		}

		@Override
		public LocalCursor copyCursor()
		{
			return copy();
		}
	}
}
