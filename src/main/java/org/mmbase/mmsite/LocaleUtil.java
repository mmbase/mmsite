package org.mmbase.mmsite;

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.bridge.*;

import org.mmbase.util.functions.*;
import org.mmbase.util.LocalizedString;

import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Utility methods for UrlConverter that support language postfixing in the URL.
 *
 * @author Andr&eacute; van Toly
 * @author Michiel Meeuwissen
 * @version $Id: UrlUtils.java 36206 2009-06-19 23:44:46Z michiel $
 */
public class LocaleUtil {
    private static final LocaleUtil INSTANCE = new LocaleUtil();

    public static LocaleUtil getInstance() {
        return INSTANCE;
    }

    private static final Logger log = Logging.getLoggerInstance(LocaleUtil.class);

    public static final Parameter<Locale> LOCALE  = new Parameter<Locale>("userlocale", Locale.class);
    public static final String LOCALE_KEY = "javax.servlet.jsp.jstl.fmt.locale.request";
    public static final String EXPLICIT_LOCALE_KEY = "org.mmbase.mmsite.language";

    protected final List<Locale> acceptedLocales = new ArrayList<Locale>();


    /**
     * now add also degrated locales, if not yet present
     */
    protected Collection<Locale> addDegraded(Collection<Locale> locales) {
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

    public void setLocales(String s) {
        acceptedLocales.clear();
        if (s != null && s.length() > 0) {
            for (String l : s.split(",")) {
                acceptedLocales.add(new Locale(l));
            }
            addDegraded(acceptedLocales);

        }
    }

    public boolean isMultiLanguage() {
        return acceptedLocales.size() > 0;
    }


    /**
     * Searches the request for the attribute 'org.mmbase.mmsite.language' which can contain
     * the prefered language setting for the site. If not found it returns an empty String.
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
    public String setLanguage(String path, HttpServletRequest request) {
        if (! isMultiLanguage()) return path;

        int lastDot = path.lastIndexOf(".");
        if (lastDot >= 0) {
            String lang = path.substring(lastDot + 1, path.length());
            Locale language =  new Locale(lang);
            if (! acceptedLocales.contains(language)) {
                throw new NotFoundException("Locale '" + language + "' is not supported");
            }

            request.setAttribute(EXPLICIT_LOCALE_KEY, language.toString());
            request.setAttribute(LOCALE_KEY, language);
            return path.substring(0, lastDot);
        } else {
            request.setAttribute(EXPLICIT_LOCALE_KEY, "");
            Locale inferredLocale = null;
            if (log.isDebugEnabled()) {
                log.debug("Matching " + addDegraded(Collections.list(request.getLocales())) + " to " + acceptedLocales);
            }
            LOC:
            for (Locale proposal : addDegraded((Collection<Locale>) Collections.list(request.getLocales()))) {
                log.trace("Considering user preference " + proposal);
                for (Locale serverLocale : acceptedLocales) {
                    log.trace("Comparing with " + serverLocale);
                    if (serverLocale.equals(proposal)) {
                        log.debug("" + proposal + " is a  hit!");
                        inferredLocale = proposal;
                        break LOC;
                    }
                }
            }
            if (inferredLocale == null) {
                inferredLocale = acceptedLocales.get(0);
                log.debug("No hit found, taking " + inferredLocale);

            }
            request.setAttribute(LOCALE_KEY, inferredLocale);

            return path;
        }
    }


}
