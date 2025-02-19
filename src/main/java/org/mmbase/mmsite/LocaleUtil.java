/*

This file is part of the MMBase MMSite application, 
which is part of MMBase - an open source content management system.
    Copyright (C) 2009 André van Toly

MMBase MMSite is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MMBase MMSite is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MMBase. If not, see <http://www.gnu.org/licenses/>.

*/

package org.mmbase.mmsite;

import java.util.*;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.mmbase.bridge.NotFoundException;
import org.mmbase.core.event.EventManager;
import org.mmbase.core.event.SystemEvent;
import org.mmbase.core.event.SystemEventListener;
import org.mmbase.datatypes.DataTypes;
import org.mmbase.datatypes.StringDataType;


import org.mmbase.util.functions.*;
import org.mmbase.util.LocalizedString;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;
import org.mmbase.util.xml.UtilReader;


/**
 * Utility methods for UrlConverter to support language postfixing in the URL, 
 * f.e. '/articles/random.en' or '/articles/random.nl'.
 *
 * @author Andr&eacute; van Toly
 * @author Michiel Meeuwissen
 * @version $Id: UrlUtils.java 36206 2009-06-19 23:44:46Z michiel $
 */
public class LocaleUtil implements SystemEventListener {
    private static final Logger log = Logging.getLoggerInstance(LocaleUtil.class);

    public static final Parameter<Locale> LOCALE  = new Parameter<Locale>("userlocale", Locale.class);
    public static final String LOCALE_KEY = "javax.servlet.jsp.jstl.fmt.locale.request";
    public static final String EXPLICIT_LOCALE_KEY = "org.mmbase.mmsite.language";

    private static final LocaleUtil instance = new LocaleUtil();
    
    public static LocaleUtil getInstance() {
        return instance;
    }


    private final List<Locale> displayLocales = new ArrayList<Locale>();
    private final List<Locale> acceptedLocales = new ArrayList<Locale>();

    private Map<String, String> properties;
    {
        EventManager.getInstance().addEventListener(this);
    }
    
    @Override
    public void notify(SystemEvent se ){
        if (se instanceof SystemEvent.Up) {
            setProperties("localeutil.xml");
            configure();
        }
    }
    
    @Override
    public int getWeight() {
        return 0;
    }
    
    public void setProperties(String fileName) {
        properties = new UtilReader(fileName,
            new Runnable() {
                @Override
                public void run() {
                    LocaleUtil.this.configure();
                }
            }).getProperties();
    }
    
    protected void configure() {
        if (properties != null) {
            {
                String d = properties.get("displayLocales");
                log.info("Locales displayed: " + d);
                setDisplayLocales(d);
            }
            {
                String a = properties.get("acceptedLocales");
                log.info("Locales accepted: " + a);
                setAcceptedLocales(a);
            }
        }
    }

    /**
     * now add also degraded locales, if not yet present
     */
    protected static Collection<Locale> addDegraded(Collection<Locale> locales) {
        for (Locale original : new ArrayList<Locale>(locales)) {
            Locale loc = LocalizedString.degrade(original, original);
            while (loc != null) {
                if (! locales.contains(loc)) {
                    locales.add(loc);
                }
                loc = LocalizedString.degrade(loc, original);
            }

        }
        return locales;
    }


    protected static List<Locale> getLocales(String s) {
        if (s.startsWith("DATATYPE:")) {
            String dt = s.substring("DATATYPE:".length());
            s = ((StringDataType) DataTypes.getDataType(dt)).getPattern().pattern();
        }
        List<Locale> result = new ArrayList<Locale>();
        if (s != null && s.length() > 0) {
            for (String l : s.split("[,|]")) {
                result.add(LocalizedString.getLocale(l.trim()));
            }
            addDegraded(result);
        }
        if (log.isDebugEnabled()) {
            log.debug("result: " + result);
        }
        return result;
    }

    public void setDisplayLocales(String s) {
        displayLocales.clear();
        displayLocales.addAll(getLocales(s));
    }
    
    public List<Locale> getDisplayLocales() {
        return Collections.unmodifiableList(displayLocales);
    }

    public void setAcceptedLocales(String s) {
        acceptedLocales.clear();
        acceptedLocales.addAll(getLocales(s));
    }
    public List<Locale> getAcceptedLocales() {
        return Collections.unmodifiableList(acceptedLocales);
    }

    public boolean isMultiLanguage() {
        return displayLocales.size() > 0;
    }


    /**
     * Searches the request for the attribute 'org.mmbase.mmsite.language' which can contain
     * the preferred language setting for the site. If not found it returns an empty String.
     *
     * @param  request HttpServletRequest
     * @return language code or null if not found
     */
    public Locale getUserPreferedLanguage(HttpServletRequest request) {
        String lang = (String) request.getAttribute(EXPLICIT_LOCALE_KEY);
        if (lang != null && ! "".equals(lang)) {
            return request.getLocale();
        } else {
            return new Locale(lang);
        }
    }

    public void appendLanguage(StringBuilder buf, Parameters frameworkParameters) {
        if (! isMultiLanguage()) return;
        Locale locParam = frameworkParameters.get(LOCALE);
        String locale;
        if (locParam != null) {
            locale = locParam.toString();
        } else {
            HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);
            locale = (String) request.getAttribute(EXPLICIT_LOCALE_KEY);
        }
        if (locale != null && ! "".equals(locale)) {
            buf.append(".").append(locale);
        }
    }
    private final Pattern LANG_PATTERN = Pattern.compile("[a-z]{2}(_[A-Z]{2})?");
    
    public String setLanguage(String path, HttpServletRequest request) {
        if (! isMultiLanguage()) return path;

        int lastDot = path.lastIndexOf(".");
        if (lastDot >= 0) {
            String  language = path.substring(lastDot + 1, path.length());
            if ( ! LANG_PATTERN.matcher(language).matches()) {
                return path;
            }
            Locale locale = LocalizedString.getLocale(language);
            if (! acceptedLocales.isEmpty() && ! acceptedLocales.contains(locale)) {
                throw new NotFoundException("Locale '" + language + "' is not supported (path: " + path + ")");
            }

            request.setAttribute(EXPLICIT_LOCALE_KEY, locale.toString());
            request.setAttribute(LOCALE_KEY, locale);
            return path.substring(0, lastDot);
        } else {
            request.setAttribute(EXPLICIT_LOCALE_KEY, "");
            Locale inferredLocale = null;
            if (log.isDebugEnabled()) {
                log.debug("Matching " + addDegraded(Collections.list(request.getLocales())) + " to " + displayLocales);
            }
            if (acceptedLocales.isEmpty()) {
                if (request.getHeader("Accept-Language") != null ){
                    inferredLocale = (Locale) Collections.list(request.getLocales()).get(0);
                }
            } else {
                LOC:
                for (Locale proposal : (List<Locale>) addDegraded(Collections.list(request.getLocales()))) {
                    log.trace("Considering user preference " + proposal);
                    for (Locale serverLocale : acceptedLocales) {
                        log.trace("Comparing with " + serverLocale);
                        if (serverLocale.equals(proposal)) {
                            if (log.isDebugEnabled()) {
                                log.debug("" + proposal + " is a  hit!");
                            }
                            inferredLocale = proposal;
                            break LOC;
                        }
                    }
                }
            }
            if (inferredLocale == null) {
                inferredLocale = displayLocales.get(0);
                if (log.isDebugEnabled()) {
                    log.debug("No hit found, taking " + inferredLocale);
                }

            }
            request.setAttribute(LOCALE_KEY, inferredLocale);

            return path;
        }
    }


}
