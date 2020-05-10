package org.apache.archiva.redback.common.ldap;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * 
 *
 */
public final class LdapUtils
{

    private static String[] FILTER_ESCAPE_TABLE = new String['\\' + 1];


    // Characters that must be escaped in a user filter
    static {

        // Filter encoding table -------------------------------------
        // fill with char itself
        for (char c = 0; c < FILTER_ESCAPE_TABLE.length; c++) {
            FILTER_ESCAPE_TABLE[c] = String.valueOf(c);
        }

        // escapes (RFC2254)
        FILTER_ESCAPE_TABLE['*'] = "\\2a";
        FILTER_ESCAPE_TABLE['('] = "\\28";
        FILTER_ESCAPE_TABLE[')'] = "\\29";
        FILTER_ESCAPE_TABLE['\\'] = "\\5c";
        FILTER_ESCAPE_TABLE[0] = "\\00";
    }


    private LdapUtils()
    {
        // no op
    }

    @SuppressWarnings("unchecked")
    public static String getLabeledUriValue( Attributes attributes, String attrName, String label,
                                             String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            NamingEnumeration attrs;
            try
            {
                attrs = attribute.getAll();
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }

            while ( attrs.hasMoreElements() )
            {
                Object value = attrs.nextElement();

                String val = String.valueOf( value );

                if ( val.endsWith( " " + label ) )
                {
                    return val.substring( 0, val.length() - ( label.length() + 1 ) );
                }
            }
        }

        return null;
    }

    public static String getAttributeValue( Attributes attributes, String attrName, String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            try
            {
                Object value = attribute.get();

                return String.valueOf( value );
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }
        }

        return null;
    }

    public static String getAttributeValueFromByteArray( Attributes attributes, String attrName,
                                                         String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            try
            {
                byte[] value = (byte[]) attribute.get();

                return new String( value );
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }
        }

        return null;
    }

    /**
     * Returns a LDAP name from a given RDN string. The  <code>name</code> parameter must be a string
     * representation of a composite name (as returned by ldapsearch result getName())
     * @param name The string of the RDN (may be escaped)
     * @return The LdapName that corresponds to this string
     * @throws InvalidNameException If the string cannot be parsed as LDAP name
     */
    public static LdapName getLdapNameFromString(final String name) throws InvalidNameException
    {
        CompositeName coName = new CompositeName( name );
        LdapName ldapName = new LdapName( "" );
        ldapName.addAll( coName );
        return ldapName;
    }

    /**
     * Returns the first RDN value that matches the given type.
     * E.g. for the RDN ou=People,dc=test,dc=de, and type dc it will return 'test'.
     *
     * @param name the ldap name
     * @param type the type of the RDN entry
     * @return
     */
    public static String findFirstRdnValue(LdapName name, String type) {
        for ( Rdn rdn : name.getRdns() )
        {
            if ( rdn.getType( ).equals( type ) )
            {
                Object val = rdn.getValue( );
                if (val!=null) {
                    return val.toString( );
                } else {
                    return "";
                }
            }
        }
        return "";
    }

    /**
     * Escape a value for use in a filter.
     * This method is copied from the spring framework class org.springframework.security.ldap.authentication.LdapEncoder
     *
     * @param value the value to escape.
     * @return a properly escaped representation of the supplied value.
     */
    public static String encodeFilterValue(String value) {

            if (value == null) {
                return null;
            }

            // make buffer roomy
            StringBuilder encodedValue = new StringBuilder(value.length() * 2);

            int length = value.length();

            for (int i = 0; i < length; i++) {

                char c = value.charAt(i);

                if (c < FILTER_ESCAPE_TABLE.length) {
                    encodedValue.append(FILTER_ESCAPE_TABLE[c]);
                }
                else {
                    // default: add the char
                    encodedValue.append(c);
                }
            }

            return encodedValue.toString();
    }
}
