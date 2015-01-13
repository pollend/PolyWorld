/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.polyworld.elevation;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.polyworld.voronoi.Graph;
import org.terasology.polyworld.voronoi.GraphFacet;
import org.terasology.polyworld.water.WaterModel;
import org.terasology.polyworld.water.WaterModelFacet;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetProvider;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.Requires;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * TODO Type description
 * @author Martin Steiger
 */
@Produces(ElevationModelFacet.class)
@Requires({
        @Facet(WaterModelFacet.class),
        @Facet(GraphFacet.class)
        })
public class ElevationModelFacetProvider implements FacetProvider {

    private static final Logger logger = LoggerFactory.getLogger(ElevationModelFacetProvider.class);

    private final Cache<Graph, ElevationModel> elevationCache = CacheBuilder.newBuilder().build();

    /**
     *
     */
    public ElevationModelFacetProvider() {
    }

    @Override
    public void setSeed(long seed) {
        // ignore
    }

    @Override
    public void process(GeneratingRegion region) {
        ElevationModelFacet elevationFacet = new ElevationModelFacet();

        GraphFacet graphFacet = region.getRegionFacet(GraphFacet.class);
        WaterModelFacet waterFacet = region.getRegionFacet(WaterModelFacet.class);

        for (Graph graph : graphFacet.getAllGraphs()) {
            WaterModel waterModel = waterFacet.get(graph);
            ElevationModel elevationModel = getOrCreate(graph, waterModel);
            elevationFacet.add(graph, elevationModel);
        }

        region.setRegionFacet(ElevationModelFacet.class, elevationFacet);
    }

    private ElevationModel getOrCreate(final Graph graph, final WaterModel waterModel) {
        try {
            return elevationCache.get(graph, new Callable<ElevationModel>() {

                @Override
                public ElevationModel call() {
                    return new DefaultElevationModel(graph, waterModel);
                }
            });
        } catch (ExecutionException e) {
            logger.error("Could not create elevation model", e.getCause());
            return null;
        }
    }
}