/*
 * Copyright 2007-2013 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.metis.core.common;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The name of the dataset (enumerated)
 * Created by ymamakis on 2/17/16.
 */

@JsonSerialize(using = LanguageSerializer.class)
@JsonDeserialize(using = LanguageDeserializer.class)
public enum Language {

  AR("Arabic"), AZ("Azerbaijani"), BE("Belarusian"), BG("Bulgarian"), BS("Bosnian"), CA(
      "Catalan"), CS("Czech"), CY("Welsh"), DA("Danish"), DE("German"), EL("Greek"), EN(
      "English"), ES("Spanish"), ET("Estonian"), EU("Basque"), FI("Finnish"), FR("French"), GA(
      "Irish"), GD("Gaelic (Scottish)"), GL("Galician"), HE("Hebrew"), HI("Hindi"), HR(
      "Croatian (hrvatski jezik)"), HU("Hungarian"), HY("Armenian"), IE("Interlingue"), IS(
      "Icelandic"), IT("Italian"), JA("Japanese"), KA("Georgian"), KO("Korean"), LT(
      "Lithuanian"), LV("Latvian (Lettish)"), MK(
      "Macedonian"), MT("Maltese"),
  MUL("Multilingual Content"), NL("Netherlands"), NO("Norwegian"), PL("Polish"), PT(
      "Portugese"), RO("Romanian"), RU("Russian"), SK("Slovak"), SL("Slovenian"), SQ(
      "Albanian"), SR("Serbian"), SV(
      "Swedish"), TR("Turkish"), UK("Ukrainian"), YI("Yiddish"), ZH("Chinese");

  private String name;

  Language(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static Language getLanguageFromEnumName(String name) {
    for (Language language : Language.values()) {
      if (language.name().equalsIgnoreCase(name)) {
        return language;
      }
    }
    return null;
  }
}
