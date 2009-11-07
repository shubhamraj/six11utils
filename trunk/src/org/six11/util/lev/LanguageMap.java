package org.six11.util.lev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 * Utility for converting between source code strings and text suitable for use
 * in a non-English language environment.
 */
public class LanguageMap {
  public final static int ENGLISH     = 0;
  private static LanguageMap instance;

  /** The map between code strings and the Vector of language strings. */
  private Map mappings;
  private int languageCode;

  private LanguageMap(int languageCode_) {
    languageCode = languageCode_;
  }

  /**
   * Returns the singleton instance of the language map. If the instance hasn't
   * been created yet, one is created. By default the langauge map uses
   * English.
   */
  public static LanguageMap getInstance() {
    if (instance == null) {
      instance = new LanguageMap(ENGLISH);
    }

    return instance;
  }

  /**
   * Sets the current default language code.
   */
  public void setLanguageCode(int languageCode_) {
    languageCode = languageCode_;
  }

  /**
   * Returns the current default language code.
   */
  public int getLanguageCode() {
    return languageCode;
  }

  /**
   * Returns the translated value of the string in the current default language
   * code.
   */
  public String translate(String code) {
    return translate(code, getLanguageCode());
  }

  /**
   * Returns the translated value of the string code in the provided language.
   */
  public String translate(String code, int language) {
    return (String) getWords(code).get(language);
  }

  private List getWords(String name) {
    if (getMappings().containsKey(name) == false) {
      getMappings().put(name, new Vector());
    }

    return (List) getMappings().get(name);
  }

  private Map getMappings() {
    if (mappings == null) {
      mappings = new HashMap();
    }

    return mappings;
  }

  /**
   * Associates the given code and language key with the provided value.
   */
  public void addTranslation(String code, int language, String value) {
    getWords(code).set(language, value);
  }
}
