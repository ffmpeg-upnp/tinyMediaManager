/*
 * Copyright 2012 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.tinymediamanager.scraper.util.Similarity;

/**
 * The Class MetadataUtil.
 */
public class MetadataUtil {

  /** The Constant log. */
  private static final Logger LOGGER           = Logger.getLogger(MetadataUtil.class);

  /** The Constant MOVIE_MEDIA_TYPE. */
  public static final String  MOVIE_MEDIA_TYPE = "Movie";

  /** The Constant TV_MEDIA_TYPE. */
  public static final String  TV_MEDIA_TYPE    = "TV";

  /** The compressed regex. */
  private static String       compressedRegex  = "[^a-zA-Z]+";

  /**
   * Given a metadata id, id:###, return 2 parts, the id, and the ####
   * 
   * if the id is not a valid id, then only a 1 element array will be returned.
   * 
   * @param id
   *          the id
   * @return the metadata id parts
   */
  public static String[] getMetadataIdParts(String id) {
    if (id == null)
      return null;
    String parts[] = id.split(":");
    if (parts == null || parts.length != 2) {
      return new String[] { id };
    }
    return parts;
  }

  /**
   * Return the best score for a title when compared to the search string. It
   * uses 2 passes to find the best match. the first pass uses the matchTitle as
   * is, and the second pass uses the matchTitle will non search characters
   * removed.
   * 
   * @param searchTitle
   *          the search title
   * @param matchTitle
   *          the match title
   * @return the best out of the 2 scored attempts
   */
  public static float calculateScore(String searchTitle, String matchTitle) {
    float score1 = Similarity.getInstance().compareStrings(searchTitle, matchTitle);
    float score2 = Similarity.getInstance().compareStrings(searchTitle, removeNonSearchCharacters(matchTitle));
    return Math.max(score1, score2);
  }

  /**
   * Return the best score for a title when compared to the search string. It
   * uses 3 passes to find the best match. the first pass uses the matchTitle as
   * is, and the second pass uses the matchTitle will non search characters
   * removed, and the 3rd pass uses a compressed title search.
   * 
   * Compressed Scoring is useful when you are comparing a Sage recording
   * (csimiami to "CSI: Miami")
   * 
   * @param searchTitle
   *          the search title
   * @param matchTitle
   *          the match title
   * @return the best out of the 3 scored attempts
   */
  public static float calculateCompressedScore(String searchTitle, String matchTitle) {
    float score1 = calculateScore(searchTitle, matchTitle);
    if (searchTitle == null || matchTitle == null)
      return score1;

    float score2 = Similarity.getInstance().compareStrings(searchTitle.replaceAll(compressedRegex, ""), matchTitle.replaceAll(compressedRegex, ""));
    return Math.max(score1, score2);
  }

  /**
   * Sets the RELEASE_DATE in a consistent YYYY-MM-dd format using the passed
   * in.
   * 
   * @param md
   *          metadata object
   * @param strDate
   *          date in
   * @param dateInFormat
   *          date in format using {@link SimpleDateFormat} notation
   *          {@link SimpleDateFormat} mask that is passed in.
   */
  public static void setReleaseDateFromFormattedDate(MediaMetadata md, String strDate, String dateInFormat) {
    if (strDate == null || dateInFormat == null) {
      return;
    }

    try {
      DateFormat dateOutFormat = new SimpleDateFormat("yyyy-MM-dd");
      DateFormat dateInParser = new SimpleDateFormat(dateInFormat);

      Date in = dateInParser.parse(strDate);
      String out = dateOutFormat.format(in);

      md.setString(MetadataKey.RELEASE_DATE, out);
    }
    catch (Exception e) {
      LOGGER.warn("Failed to parse/format release dates; dateIn: " + strDate + "; dateInFormat: " + dateInFormat);
      md.setString(MetadataKey.RELEASE_DATE, null);
    }
  }

  /**
   * Gets the release date.
   * 
   * @param md
   *          the md
   * @return the release date
   */
  public static Date getReleaseDate(MediaMetadata md) {
    if (md == null)
      return null;
    String date = md.getString(MetadataKey.RELEASE_DATE);
    if (date == null)
      return null;

    try {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      Date d = dateFormat.parse(date);
      return d;
    }
    catch (Exception e) {
      LOGGER.warn("Failed to parse a date from the metadata date: " + date);
    }
    return null;
  }

  /**
   * Convert time to millisseconds for sage.
   * 
   * @param time
   *          the time
   * @return the string
   */
  public static String convertTimeToMillissecondsForSage(String time) {
    return String.valueOf(NumberUtils.toLong(time) * 60 * 1000);
  }

  /**
   * Parses the running time.
   * 
   * @param in
   *          the in
   * @param regex
   *          the regex
   * @return the string
   */
  public static String parseRunningTime(String in, String regex) {
    if (in == null || regex == null)
      return null;
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(in);
    if (m.find()) {
      return convertTimeToMillissecondsForSage(m.group(1));
    }
    else {
      LOGGER.warn("Could not find Running Time in " + in + "; using Regex: " + regex);
      return null;
    }
  }

  /**
   * Gets the bare title.
   * 
   * @param name
   *          the name
   * @return the bare title
   */
  public static String getBareTitle(String name) {
    if (name != null)
      return name.replaceAll("[^A-Za-z0-9']", " ");
    return name;
  }

  /**
   * Copy search query to search result.
   * 
   * @param query
   *          the query
   * @param sr
   *          the sr
   */
  public static void copySearchQueryToSearchResult(SearchQuery query, MediaSearchResult sr) {
    for (SearchQuery.Field f : SearchQuery.Field.values()) {
      if (f == SearchQuery.Field.QUERY)
        continue;
      String s = query.get(f);
      if (!StringUtils.isEmpty(s)) {
        sr.getExtra().put(f.name(), s);
      }
    }
  }

  /**
   * Search by id.
   * 
   * @param prov
   *          the prov
   * @param query
   *          the query
   * @param id
   *          the id
   * @return the list
   */
  public static List<MediaSearchResult> searchById(IMediaMetadataProvider prov, SearchQuery query, String id) {
    LOGGER.debug("searchById() for: " + query);
    MediaSearchResult res = new MediaSearchResult(prov.getInfo().getId(), id, query.get(SearchQuery.Field.RAW_TITLE),
        query.get(SearchQuery.Field.YEAR), 1.0f);
    MetadataUtil.copySearchQueryToSearchResult(query, res);

    // do the search by id...
    try {
      MediaMetadata md = prov.getMetaData(res);
      if (md == null)
        throw new Exception("metadata result was null.");
      res.setMetadata(md);
      res.setMediaType(query.getMediaType());
      res.setScore(1.0f);
      res.setTitle(md.getMediaTitle());
      res.setYear(md.getYear());
      LOGGER.info("searchById() was sucessful for: " + id);
    }
    catch (Exception e) {
      LOGGER.warn("searchById() failed for: " + query, e);
      return null;
    }

    return Arrays.asList(new MediaSearchResult[] { res });
  }

  /**
   * Checks for metadata.
   * 
   * @param result
   *          the result
   * @return true, if successful
   */
  public static boolean hasMetadata(MediaSearchResult result) {
    return result != null && (result instanceof MediaSearchResult) && ((MediaSearchResult) result).getMetadata() != null;
  }

  /**
   * Gets the metadata.
   * 
   * @param result
   *          the result
   * @return the metadata
   */
  public static MediaMetadata getMetadata(MediaSearchResult result) {
    if (result != null && (result instanceof MediaSearchResult)) {
      return ((MediaSearchResult) result).getMetadata();
    }
    return null;
  }

  /**
   * For the purposes of searching it will, keep only alpha numeric characters
   * and '&.
   * 
   * @param s
   *          the s
   * @return the string
   */
  public static String removeNonSearchCharacters(String s) {
    if (s == null)
      return null;
    return (s.replaceAll("[^A-Za-z0-9&']", " ")).replaceAll("[\\ ]+", " ");
  }

}
