/*
 * Copyright (c) 2013, INSA of Rennes
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <stdlib.h>

#include "mapping.h"
#include "util.h"
#include "serialize.h"
#include "dataflow.h"

/**
 * Give the id of the mapped core of the given actor in the given mapping structure.
 */
int find_mapped_core(mapping_t *mapping, actor_t *actor) {
    int i;
    for (i = 0; i < mapping->number_of_threads; i++) {
        if (find_actor(actor->name, mapping->partitions_of_actors[i],
                mapping->partitions_size[i]) != NULL) {
            return i;
        }
    }
    return -1;
}

/**
 * Creates a mapping structure.
 */
mapping_t* allocate_mapping(int number_of_threads) {
    mapping_t *mapping = (mapping_t *) malloc(sizeof(mapping_t));
    mapping->number_of_threads = number_of_threads;
    mapping->threads_affinities = (int*) malloc(number_of_threads * sizeof(int));
    mapping->partitions_of_actors = (actor_t ***) malloc(number_of_threads * sizeof(actor_t **));
    mapping->partitions_size = (int*) malloc(number_of_threads * sizeof(int));
    return mapping;
}

/**
 * Releases memory of the given mapping structure.
 */
void delete_mapping(mapping_t* mapping, int clean_all) {
    if (clean_all) {
        int i;
        for (i = 0; i < mapping->number_of_threads; i++) {
            free(mapping->partitions_of_actors[i]);
        }
    }
    free(mapping->partitions_of_actors);
    free(mapping->partitions_size);
    free(mapping->threads_affinities);
    free(mapping);
}

/**
 * Computes a partitionment of actors on threads from an XML file given in parameter.
 */
mapping_t* map_actors(actor_t **actors, int actors_size) {
    if (mapping_file == NULL) {
        mapping_t *mapping = allocate_mapping(1);
        mapping->threads_affinities[0] = 0;
        mapping->partitions_size[0] = actors_size;
        mapping->partitions_of_actors[0] = actors;
        return mapping;
    } else {
        mappings_set_t *mappings_set = compute_mappings_from_file(mapping_file, actors, actors_size);
        return mappings_set->mappings[0];
    }
}