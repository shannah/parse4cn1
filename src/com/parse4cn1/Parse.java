/*
 * Copyright 2015 Chidiebere Okwudire.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original implementation adapted from Thiago Locatelli's Parse4J project
 * (see https://github.com/thiagolocatelli/parse4j)
 */
package com.parse4cn1;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONObject;
import com.codename1.l10n.DateFormat;
import com.codename1.l10n.SimpleDateFormat;
import com.parse4cn1.operation.ParseOperationUtil;
import com.parse4cn1.operation.ParseOperationDecoder;
import com.parse4cn1.util.ParseRegistry;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The Parse class contains static functions, classes and interfaces that handle
 * global configuration for the Parse library.
 */
public class Parse {

    /**
     * An interface for a persistable entity.
     */
    public interface IPersistable {

        /**
         * An object is dirty if a change has been made to it that requires it
         * to be persisted.
         *
         * @return {@code true} if the object is dirty; otherwise {@code false}.
         */
        boolean isDirty();

        /**
         * Sets the dirty flag.
         *
         * @param dirty {@code true} if the object should be marked as dirty;
         * otherwise {@code false}.
         */
        void setDirty(boolean dirty);

        /**
         * Checks whether this persistable object has any data. This data may
         * (isDirty() = true) or may not (isDirty() = false) need to be
         * persisted.
         *
         * @return {@code true} if this object has data.
         */
        boolean isDataAvailable();

        /**
         * Saves the object. Calling this method on an object that is not dirty
         * should have no side effects.
         *
         * @throws ParseException if anything goes wrong during the save
         * operation.
         */
        void save() throws ParseException;
    }

    /**
     * A factory for instantiating ParseObjects of various concrete types
     */
    public interface IParseObjectFactory {

        /**
         * Creates a Parse object of the type matching the provided class name.
         * Defaults to the base ParseObject, i.e., call must always return a
         * non-null object.
         *
         * @param <T> The type of ParseObject to be instantiated
         * @param className The class name associated with type T
         * @return The newly created Parse object.
         */
        <T extends ParseObject> T create(final String className);
    }

    /**
     * A factory for instantiating reserved Parse Object classes such as users,
     * roles, installations and sessions. Such a factory is used to instantiate
     * the correct type upon retrieval of data, for example, via a query.
     */
    public static class DefaultParseObjectFactory implements IParseObjectFactory {

        public <T extends ParseObject> T create(String className) {
            T obj;

            if (ParseConstants.ENDPOINT_USERS.equals(className)
                    || ParseConstants.CLASS_NAME_USER.equals(className)) {
                obj = (T) new ParseUser();
            } else if (ParseConstants.ENDPOINT_ROLES.equals(className)
                    || ParseConstants.CLASS_NAME_ROLE.equals(className)) {
                obj = (T) new ParseRole();
            } else {
                obj = (T) new ParseObject(className);
            }
            // TODO: Extend with other 'default' parse object subtypes
            // e.g. Session and Installation.

            return obj;
        }
    }

    private static String mApplicationId;
    private static String mClientKey;
    private static final DateFormat dateFormat;

    static {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        ParseRegistry.registerDefaultSubClasses();
        ParseOperationDecoder.registerDefaultDecoders();
    }

    /**
     * Authenticates this client as belonging to your application.
     * <p>
     * This method must be called before your application can use the Parse
     * library. The recommended way is to put a call to Parse.initialize in your
     * CN! Application's state machine as follows:
     * <pre>
     * <code>
     * public class StateMachine extends StateMachineBase {
     *   protected void initVars(Resources res) {
     *     Parse.initialize(APP_ID, APP_REST_API_ID);
     *   }
     * }
     * </code>
     * </pre>
     *
     * @param applicationId The application id provided in the Parse dashboard.
     * @param clientKey The client key provided in the Parse dashboard.
     * <p>
     * <b>Note:</b> Developers are advised to use the CLIENT KEY instead of
     * using the REST API in production code (cf.
     * <a href='https://parse.com/docs/rest#general-callfromclient'>https://parse.com/docs/rest#general-callfromclient</a>).
     * Hence, the latter is not exposed via this library. The same security
     * consideration explains why the MASTER KEY is not exposed either.
     */
    static public void initialize(String applicationId, String clientKey) {
        mApplicationId = applicationId;
        mClientKey = clientKey;
    }

    /**
     * @return The application ID if one has been set or null.
     * @see #initialize(java.lang.String, java.lang.String)
     */
    static public String getApplicationId() {
        return mApplicationId;
    }

    /**
     * @return The client key if one has been set or null.
     * @see #initialize(java.lang.String, java.lang.String)
     */
    static public String getClientKey() {
        return mClientKey;
    }

    /**
     * Creates a valid Parse REST API URL using the predefined
     * {@link ParseConstants#API_ENDPOINT} and
     * {@link ParseConstants#API_VERSION}.
     *
     * @param endPoint The target endpoint/class name.
     * @return The created URL.
     */
    static public String getParseAPIUrl(String endPoint) {
        return ParseConstants.API_ENDPOINT + "/" + ParseConstants.API_VERSION
                + "/" + ((endPoint != null) ? endPoint : "");
    }

    /**
     * Encodes the provided date in the format required by Parse.
     *
     * @param date The date to be encoded.
     * @return {@code date} expressed in the format required by Parse.
     * @see <a href='https://www.parse.com/docs/rest#objects-types'>Parse date
     * type</a>.
     */
    static public synchronized String encodeDate(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Converts a Parse date string value into a Date object.
     *
     * @param dateString A string matching the Parse date type.
     * @return The date object corresponding to {@code dateString}.
     * @see <a href='https://www.parse.com/docs/rest#objects-types'>Parse date
     * type</a>.
     */
    public static synchronized Date parseDate(String dateString) {
        boolean parsed = false;
        Date parsedDate = null;
            
        // As at July 2015, the CN1 port for Windows Phone is not quite mature
        // For example, using the SimpleDateFormat.format() method raises an
        // org.xmlvm._nNotYetImplementedException (cf. https://groups.google.com/d/topic/codenameone-discussions/LHZeubG-sf0/discussion)
        // As a workaround, Parse dates are manually parsed if they meet the 
        // expected format:
        //
        // "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" e.g. "2015-07-14T15:55:52.133Z"
        // Note that a regex-based approach is not taken because it's simply
        // not necessary and regexes will mean yet another cn1lib dependency.

        if (dateString.length() == 24) { 
            try {
                int year  = Integer.valueOf(dateString.substring(0, 4));
                int month = Integer.valueOf(dateString.substring(5, 7));
                int day   = Integer.valueOf(dateString.substring(8, 10));
                int hour  = Integer.valueOf(dateString.substring(11, 13));
                int min   = Integer.valueOf(dateString.substring(14, 16));
                int sec   = Integer.valueOf(dateString.substring(17, 19));
                int milli = Integer.valueOf(dateString.substring(20, 23));

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month - 1); // month is 0-based.
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, sec);
                cal.set(Calendar.MILLISECOND, milli);
                
                parsedDate = cal.getTime();
                parsed = true;
            } catch (NumberFormatException ex) {
                parsedDate = null;
                parsed = false;
            }
        }
            
        try {
            if (!parsed) { 
                // Fallback to default and hope for the best
                parsedDate = dateFormat.parse(dateString);
            }
        } catch (com.codename1.l10n.ParseException e) {
            parsedDate = null;
        }
        
        return parsedDate;
    }

    /**
     * Checks if the provided {@code key} is a key with a special meaning in
     * Parse, for example {@code objectId}.
     *
     * @param key The key to be checked.
     * @return {@code true} if and only if {@code key} is a reserved key.
     */
    public static boolean isReservedKey(String key) {
        return ParseConstants.FIELD_OBJECT_ID.equals(key)
                || ParseConstants.FIELD_CREATED_AT.equals(key)
                || ParseConstants.FIELD_UPDATED_AT.equals(key);
    }

    /**
     * Checks if the provided {@code endPoint} is the name of an in-built Parse
     * end point, for examples (users and /classes/_User).
     *
     * @param endPoint The endpoint to be checked.
     * @return {@code true} if {@code endPoint} is a reserved class end point.
     */
    public static boolean isReservedEndPoint(String endPoint) {
        // Parse-reserved end points and classes
        // TODO: Extend with other endpoints when implemented.
        return ParseConstants.CLASS_NAME_USER.equals(endPoint)
                || ParseConstants.CLASS_NAME_ROLE.equals(endPoint)
                || ParseConstants.ENDPOINT_USERS.equals(endPoint)
                || ParseConstants.ENDPOINT_ROLES.equals(endPoint)
                || ParseConstants.ENDPOINT_SESSIONS.equals(endPoint);
    }

    /**
     * Checks if the provided type is a valid type for a Parse Object or any one
     * of its fields.
     *
     * @param value The object to be checked
     * @return {@code true} if {@code value} is valid as per the data types
     * supported by ParseObjects and their child fields.
     */
    public static boolean isValidType(Object value) {
        return ((value instanceof JSONObject))
                || ((value instanceof JSONArray))
                || ((value instanceof String))
                || (ParseOperationUtil.isSupportedNumberType(value))
                || ((value instanceof Boolean))
                || (value == JSONObject.NULL)
                || ((value instanceof ParseObject))
                || ((value instanceof ParseFile))
                || ((value instanceof ParseRelation))
                || ((value instanceof ParseGeoPoint))
                || ((value instanceof Date))
                || ((value instanceof byte[]))
                || ((value instanceof List))
                || ((value instanceof Map));
    }

    /**
     * Utility to concatenate the strings in {@code items} using the provided
     * {@code delimieter}.
     *
     * @param items The strings to be concatenated.
     * @param delimiter The delimiter.
     * @return The concatenated string.
     */
    public static String join(final Collection<String> items, final String delimiter) {
        StringBuilder buffer = new StringBuilder();
        Iterator iter = items.iterator();
        if (iter.hasNext()) {
            buffer.append((String) iter.next());
            while (iter.hasNext()) {
                buffer.append(delimiter);
                buffer.append((String) iter.next());
            }
        }
        return buffer.toString();
    }
}
