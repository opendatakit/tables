/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.common.android.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Unfortunately, while the CSVWriter of opencsv works, the reader is broken
 * w.r.t. quoted strings and RFC4180.  Here is a working reader...
 * The key difference is that when a quotechar appears within a quoted
 * string, it is replaced with two quotechars.  So if we see two quotechars
 * in a row, then they are replaced with a single quotechar inside the
 * current string. quotechar, cr, lf are not permitted unless they are within
 * a quoted field.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class RFC4180CsvReader {

  private final BufferedReader br;

  private final char cr = 13;

  private final char lf = 10;

  private final char separator = ',';

  private final char quotechar = '"';


  enum ParseState {
    atStartOfLine,
    atStartOfField,
    naked,
    quoted,
    expectingComma
  };

 /**
   * Constructs CSVReader with supplied separator and quote char.
   *
   * @param reader
   *            the reader to an underlying CSV source.
   * @param separator
   *            the delimiter to use for separating entries
   * @param quotechar
   *            the character to use for quoted elements
   * @param escape
   *            the character to use for escaping a separator or quote
   */

  public RFC4180CsvReader(Reader reader) {
    this.br = new BufferedReader(reader);
  }

  /**
   * Reads the next line from the buffer and converts to a string array.
   *
   * @return a string array with each comma-separated element as a separate
   *         entry.
   *
   * @throws IOException
   *             if bad things happen during the read
   */
  public String[] readNext() throws IOException {

    List<String> results = new ArrayList<String>();
    StringBuilder b = new StringBuilder();

    boolean fetchNextChar = true;
    ParseState state = ParseState.atStartOfLine;
    int ch = -1;
    while (true) {
      if ( fetchNextChar ) {
        ch = br.read();
      }
      fetchNextChar = true;

      // Handle the start of the line differently
      // - if we hit end-of-file, return null.
      // - if we hit CR LF, return []
      //
      if ( state == ParseState.atStartOfLine ) {
        if ( ch == -1 ) {
          // no more lines in file
          return null;
        }
        if ( ch == cr ) {
          // special case -- if we have an immediate CR LF, return an empty array
          ch = br.read();
          if ( ch != lf ) {
            throw new IllegalStateException("Unexpected CR without LF!");
          }
          return results.toArray(new String[results.size()]);
        } else if ( ch == lf ) {
          // alternate line terminator (no cr, just lf)
          return results.toArray(new String[results.size()]);
        }
        state = ParseState.atStartOfField;
      }

      if ( ch == -1 ) {
        throw new IllegalStateException("Unexpected end of file - last line is missing the CR LF!");
      }

      // NOTE: we must be in a ParseState other than atStartOfLine
      if ( state == ParseState.atStartOfField ) {
        // If we are expecting the start of a field and
        // encounter a CR LF, then we emit a null value
        // and return the results array.
        if ( ch == cr ) {
          ch = br.read();
          if ( ch != lf ) {
            throw new IllegalStateException("Unexpected CR without LF!");
          }
          results.add(null);
          return results.toArray(new String[results.size()]);
        } else if ( ch == lf ) {
          // alternate line terminator
          results.add(null);
          return results.toArray(new String[results.size()]);
        } else if ( ch == separator ) {
          // no value in cell
          results.add(null);
          // the separator advances us to the next field
          state = ParseState.atStartOfField;
        } else if ( ch == quotechar ) {
          // start of a quoted string
          state = ParseState.quoted;
          b.setLength(0);
        } else {
          // start of an unquoted string
          state = ParseState.naked;
          b.setLength(0);
          b.append((char) ch);
        }
      } else if ( state == ParseState.expectingComma ) {
        if ( ch == cr ) {
          // We are expecting a comma but hit a CR LF
          // return the current results array.
          ch = br.read();
          if ( ch != lf ) {
            throw new IllegalStateException("Unexpected CR without LF!");
          }
          return results.toArray(new String[results.size()]);
        } else if ( ch == lf ) {
          // alternate line terminator
          return results.toArray(new String[results.size()]);
        } else if ( ch == separator ) {
          // found the comma -- transition to look for the start of the next field
          state = ParseState.atStartOfField;
        } else {
          throw new IllegalStateException("Expected a comma or CR LF, but found: " + String.valueOf(ch) );
        }
      } else if ( state == ParseState.naked ) {
        if ( ch == cr ) {
          // marks the end of this naked field (and the end of the line)
          ch = br.read();
          if ( ch != lf ) {
            throw new IllegalStateException("Unexpected CR without LF!");
          }
          results.add(b.toString());
          b.setLength(0);
          return results.toArray(new String[results.size()]);
        } else if ( ch == lf ) {
          results.add(b.toString());
          b.setLength(0);
          return results.toArray(new String[results.size()]);
        } else if ( ch == separator ) {
          // marks the end of this naked field
          results.add(b.toString());
          b.setLength(0);
          // look for the start of the next field
          state = ParseState.atStartOfField;
        } else if ( ch == quotechar ) {
          throw new IllegalStateException("Unexpected double-quote in an unquoted field value");
        } else {
          // anything else is just added to the field
          b.append((char) ch);
        }
      } else if ( state == ParseState.quoted ) {
        if ( ch == quotechar ) {
          // read the next character to see of it is an escaped quote
          ch = br.read();
          if ( ch != quotechar ) {
            // nope -- we are done with this quoted field
            // and expect a comma (or CR LF).
            results.add(b.toString());
            b.setLength(0);
            state = ParseState.expectingComma;
            // process the ch we read in but didn't use
            fetchNextChar = false;
          } else {
            // it was a escaped quote.
            // append a quote char to the string
            b.append((char) ch);
          }
        } else {
          // anything else is just added to the field
          b.append((char) ch);
        }
      }
    }
  }

  /**
   * Closes the underlying reader.
   *
   * @throws IOException if the close fails
   */
  public void close() throws IOException{
    br.close();
  }


}