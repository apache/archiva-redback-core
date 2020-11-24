package org.apache.archiva.redback.rest.services.v2;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * Helper class that returns combined filter and comparison objects for ordering.
 *
 * The query term may be consist of simple query terms separated by whitespace or attribute queries
 * in the form <code>attribute:query</code>, which means only the attribute is searched for the query string.
 * <br />
 * Example:
 * <dl>
 *     <dt>`user1 test`</dt>
 *     <dd>
 * searches for the tokens user1 and test in the default attributes.
 * </dd>
 * <dt>`user1 name:test`</dt>
 * <dd>searches for the token user1 in the default attributes and for the token test in the attribute name.</dd>
 * </dl>
 *
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class QueryHelper<T>
{

    private final Map<String, BiPredicate<String, T>> FILTER_MAP;
    private final Map<String, Comparator<T>> ORDER_MAP;
    private final String[] DEFAULT_SEARCH_FIELDS;
    private final Predicate<T> DEFAULT_FILTER = ( T att ) -> false;


    /**
     * Creates a new query helper with the given filters and comparators.
     *
     * @param filterMap a map of filters, where the key is the attribute name and the value is a predicate that matches
     *                  the filter value and the object instance.
     * @param orderMap a map of comparators, where key is the attribute name and the value is a comparator for the given
     *                 object instance
     * @param defaultSearchFields A array of attribute names, that are used as default search fields.
     */
    public QueryHelper(Map<String, BiPredicate<String, T>> filterMap, Map<String, Comparator<T>> orderMap,
                       String[] defaultSearchFields)
    {
        this.FILTER_MAP = filterMap;
        this.DEFAULT_SEARCH_FIELDS = defaultSearchFields;
        this.ORDER_MAP = new HashMap<>( orderMap );
    }

    public <U extends Comparable<? super U>> void addNullsafeFieldComparator( String fieldName, Function<? super T, U> keyExtractor) {
        ORDER_MAP.put( fieldName, Comparator.comparing( keyExtractor, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
    }

    public void addStringFilter(String attribute, Function<? super T, String> keyExtractor) {
        this.FILTER_MAP.put( attribute, ( String q, T r ) -> StringUtils.containsIgnoreCase( keyExtractor.apply( r ), q ) );
    }

    public void addBooleanFilter(String attribute, Function<? super T, Boolean> keyExtractor) {
        this.FILTER_MAP.put( attribute, ( String q, T r ) -> Boolean.valueOf( q ) == keyExtractor.apply( r ) );
    }

    /**
     * Get the comparator for a specific attribute.
     * @param attributeName the name of the attribute.
     * @return
     */
    Comparator<T> getAttributeComparator( String attributeName )
    {
        return ORDER_MAP.get( attributeName );
    }

    /**
     * Get the combined order for the given attributes in the given order.
     *
     * @param orderBy the attributes to compare. The first attribute in the list will be used first for comparing.
     * @param ascending
     * @return
     */
    Comparator<T> getComparator( List<String> orderBy, boolean ascending )
    {
        if ( ascending )
        {
            return orderBy.stream( ).map( ( String name ) -> getAttributeComparator( name ) ).filter( Objects::nonNull )
                .reduce( Comparator::thenComparing )
                .orElseThrow( () -> new IllegalArgumentException( "No attribute ordering found" ) );
        }

        else
        {
            return orderBy.stream( ).map( ( String name ) -> getAttributeComparator( name ) == null ? null : getAttributeComparator( name )
                .reversed( ) ).filter( Objects::nonNull ).reduce( Comparator::thenComparing )
                .orElseThrow( () -> new IllegalArgumentException( "No attribute ordering found" ) );
        }
    }

    /**
     * Returns a query filter for a specific attribute and query token.
     * @param attribute the attribute name to filter for.
     * @param queryToken the search token.
     * @return The predicate used to filter the token
     */
    Predicate<T> getAttributeQueryFilter( final String attribute, final String queryToken )
    {
        if ( FILTER_MAP.containsKey( attribute ) )
        {
            return ( T u ) -> FILTER_MAP.get( attribute ).test( queryToken, u );
        }
        else
        {
            return DEFAULT_FILTER;
        }
    }

    /**
     * Returns the combined query filter for the given query terms.
     * The query terms may be either simple strings separated by whitespace or use the
     * <code>attribute:query</code> syntax, that searches only the attribute for the query term.
     * @param queryTerms the query string
     * @return the combined query filter
     */
    Predicate<T> getQueryFilter( String queryTerms )
    {
        return Arrays.stream( queryTerms.split( "\\s+" ) )
            .map( s -> {
                    if ( s.contains( ":" ) )
                    {
                        String attr = StringUtils.substringBefore( s, ":" );
                        String term = StringUtils.substringAfter( s, ":" );
                        return getAttributeQueryFilter( attr, term );
                    }
                    else
                    {
                        return Arrays.stream( DEFAULT_SEARCH_FIELDS )
                            .map( att -> getAttributeQueryFilter( att, s ) ).reduce( Predicate::or ).get( );
                    }
                }
            ).reduce( Predicate::or ).get( );
    }

}
