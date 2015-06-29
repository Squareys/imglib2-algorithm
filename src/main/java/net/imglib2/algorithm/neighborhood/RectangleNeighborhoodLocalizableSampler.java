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

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.Sampler;

public abstract class RectangleNeighborhoodLocalizableSampler< T > extends AbstractEuclideanSpace implements Localizable, Sampler< Neighborhood< T > >
{
	protected final RandomAccessible< T > source;

	protected final Interval span;

	/*
	 * Interval containing all locations which might be accessed though
	 * neighborhoods of the accessInterval.
	 */
	protected final Interval sourceInterval;

	/*
	 * An interval defining the locations whose neighborhoods are entirely
	 * contained in the accessInterval.
	 */
	protected final Interval innerInterval;
	
	/*
	 * Interval containing all locations for which neighborhoods may be accessed.
	 */
	protected final Interval accessInterval;

	protected final RectangleNeighborhoodFactory< T > neighborhoodFactory;

	protected final Neighborhood< T > currentNeighborhood;

	/*
	 * The following neighborhood is potentially faster than the one above, and
	 * is used for "innter pixels" for which out of bounds checks are not
	 * needed.
	 */
	protected final Neighborhood< T > currentInnerNeighborhood;

	protected final long[] currentPos;

	protected final long[] currentMin;

	protected final long[] currentMax;

	public RectangleNeighborhoodLocalizableSampler( final RandomAccessible< T > source, final Interval span, final RectangleNeighborhoodFactory< T > factory, final Interval accessInterval )
	{
		super( source.numDimensions() );
		this.source = source;
		this.span = span;
		neighborhoodFactory = factory;
		currentPos = new long[ n ];
		currentMin = new long[ n ];
		currentMax = new long[ n ];
		if ( accessInterval == null )
		{
			sourceInterval = null;
			innerInterval = null;
			this.accessInterval = null;
		}
		else
		{
			final long[] accessMin = new long[ n ];
			final long[] accessMax = new long[ n ];
			final long[] innerMin = new long[ n ];
			final long[] innerMax = new long[ n ];

			accessInterval.min( accessMin );
			accessInterval.max( accessMax );
			accessInterval.max( innerMin );
			accessInterval.max( innerMax );

			for ( int d = 0; d < n; ++d )
			{
				accessMin[ d ] += span.min( d );
				accessMax[ d ] += span.max( d );
				innerMin[ d ] -= span.min( d );
				innerMax[ d ] -= span.max( d );
			}
			sourceInterval = new FinalInterval( accessMin, accessMax );
			innerInterval = new FinalInterval( innerMin, innerMax );
			this.accessInterval = accessInterval;
		}

		span.min( currentMin );
		span.max( currentMax );

		currentNeighborhood = neighborhoodFactory.create( currentPos, currentMin, currentMax, span,
				sourceInterval == null ? source.randomAccess() : source.randomAccess( sourceInterval ) );
		currentInnerNeighborhood = neighborhoodFactory.create( currentPos, currentMin, currentMax, span,
				sourceInterval == null ? source.randomAccess() : source.randomAccess( accessInterval ) );
	}

	protected RectangleNeighborhoodLocalizableSampler( final RectangleNeighborhoodLocalizableSampler< T > c )
	{
		super( c.n );
		source = c.source;
		span = c.span;
		sourceInterval = c.sourceInterval;
		innerInterval = c.innerInterval;
		accessInterval = c.accessInterval;
		neighborhoodFactory = c.neighborhoodFactory;
		currentPos = c.currentPos.clone();
		currentMin = c.currentMin.clone();
		currentMax = c.currentMax.clone();
		currentNeighborhood = neighborhoodFactory.create( currentPos, currentMin, currentMax, span,
				sourceInterval == null ? source.randomAccess() : source.randomAccess( sourceInterval ) );
		currentInnerNeighborhood = neighborhoodFactory.create( currentPos, currentMin, currentMax, span,
				sourceInterval == null ? source.randomAccess() : source.randomAccess( accessInterval ) );
	}

	@Override
	public Neighborhood< T > get()
	{
		return currentNeighborhood;
	}

	@Override
	public void localize( final int[] position )
	{
		currentNeighborhood.localize( position );
	}

	@Override
	public void localize( final long[] position )
	{
		currentNeighborhood.localize( position );
	}

	@Override
	public int getIntPosition( final int d )
	{
		return currentNeighborhood.getIntPosition( d );
	}

	@Override
	public long getLongPosition( final int d )
	{
		return currentNeighborhood.getLongPosition( d );
	}

	@Override
	public void localize( final float[] position )
	{
		currentNeighborhood.localize( position );
	}

	@Override
	public void localize( final double[] position )
	{
		currentNeighborhood.localize( position );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return currentNeighborhood.getFloatPosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return currentNeighborhood.getDoublePosition( d );
	}
}
